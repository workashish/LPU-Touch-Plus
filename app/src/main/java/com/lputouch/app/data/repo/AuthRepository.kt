package com.lputouch.app.data.repo

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lputouch.app.data.api.LpuCrypto
import com.lputouch.app.data.api.MobileApi
import com.lputouch.app.data.api.UmsApi
import com.lputouch.app.data.api.dto.CreateTokenRequest
import com.lputouch.app.data.api.dto.MenuItem
import com.lputouch.app.data.api.dto.UpdateRequest
import com.lputouch.app.data.prefs.SessionStore
import java.util.UUID
import kotlinx.coroutines.flow.first

class AuthRepository(
    private val mobileApi: MobileApi,
    private val umsApi: UmsApi,
    private val sessionStore: SessionStore,
) {
    private val gson = Gson()

    /**
     * Full login flow matching the official app exactly:
     * 1. POST /security/createToken (mobileapi.lpu.in) -> JWT
     * 2. Encrypted POST /PVR (ums.lpu.in) -> server-issued UMS AccessToken + menu
     * 3. Device registration via /Update (best-effort)
     */
    suspend fun login(userName: String, password: String): LoginResult {
        val tokenResp = try {
            mobileApi.createToken(CreateTokenRequest(userName, password))
        } catch (e: Exception) {
            return LoginResult.Error(
                if (e is javax.net.ssl.SSLHandshakeException)
                    "Network security error. Check your connection and try again."
                else
                    "Network error: ${e.message ?: "could not reach server"}"
            )
        }
        val jwt = tokenResp.token
        if (jwt.isNullOrEmpty()) {
            return LoginResult.Error("Login failed: ${tokenResp.message ?: "invalid credentials"}")
        }

        val deviceId = UUID.randomUUID().toString().replace("-", "").take(16)

        // The UMS AccessToken MUST come from the server (client-generated ones are rejected).
        val accessToken = try {
            val payload = LpuCrypto.encryptLogin(
                mapOf(
                    "UserId" to userName,
                    "password" to password,
                    "Identity" to "aphone",
                    "DeviceId" to deviceId,
                    "PlayerId" to "vbnxvcjhvbvcgghgjhgjhdddddjhgjf",
                )
            )
            val pvrResp = umsApi.pvr(mapOf("v" to payload.v, "d" to payload.d))
            extractAccessToken(pvrResp.pvrResult)
        } catch (e: Exception) {
            null
        }

        if (accessToken.isNullOrEmpty()) {
            return LoginResult.Error("Login failed: could not obtain session token")
        }

        sessionStore.saveSession(
            userId = userName,
            password = password,
            jwtToken = jwt,
            accessToken = accessToken,
            deviceId = deviceId,
            tokenParam = tokenResp.tokenParam,
        )

        // Best-effort device registration.
        try {
            umsApi.update(
                UpdateRequest(
                    uid = userName,
                    accessToken = accessToken,
                    deviceId = deviceId,
                    playerId = "",
                )
            )
        } catch (_: Exception) { /* non-fatal */ }

        return LoginResult.Success(emptyList())
    }

    private fun extractAccessToken(pvrResult: String?): String? {
        if (pvrResult.isNullOrBlank()) return null
        // PVRResult is a JSON-encoded string containing a menu array.
        val menus: List<MenuItem> = try {
            gson.fromJson(pvrResult, object : TypeToken<List<MenuItem>>() {}.type)
        } catch (e: Exception) {
            return null
        }
        return menus.firstOrNull { !it.accessToken.isNullOrEmpty() }?.accessToken
    }

    /**
     * Silently re-login using stored credentials when the UMS AccessToken
     * expires. Returns true if a fresh token was obtained and persisted.
     */
    suspend fun refreshSession(): Boolean {
        val userName = sessionStore.currentUserId() ?: return false
        val password = sessionStore.password(userName) ?: return false
        return try {
            val deviceId = sessionStore.deviceId.first() ?: UUID.randomUUID().toString().replace("-", "").take(16)
            val payload = LpuCrypto.encryptLogin(
                mapOf(
                    "UserId" to userName,
                    "password" to password,
                    "Identity" to "aphone",
                    "DeviceId" to deviceId,
                    "PlayerId" to "vbnxvcjhvbvcgghgjhgjhdddddjhgjf",
                )
            )
            val pvrResp = umsApi.pvr(mapOf("v" to payload.v, "d" to payload.d))
            val accessToken = extractAccessToken(pvrResp.pvrResult)
            if (accessToken.isNullOrEmpty()) return false
            sessionStore.saveAccessToken(accessToken)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun logout() = sessionStore.logout()
}

sealed class LoginResult {
    data class Success(val menus: List<MenuItem>) : LoginResult()
    data class Error(val message: String) : LoginResult()
}
