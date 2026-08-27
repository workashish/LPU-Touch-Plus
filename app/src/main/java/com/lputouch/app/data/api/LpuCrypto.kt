package com.lputouch.app.data.api

import android.util.Base64
import com.google.gson.Gson
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Implements the exact AES-256-CBC scheme the official LPUTouch app uses
 * for its encrypted `/PVR` login (reversed from the app's obfuscated
 * FormatJSONPS "milkyway" format).
 *
 * Request body:  {"v": <base64 IV>, "d": <base64 ciphertext>}
 * Plaintext:     {"url":"milkyway","action":"post","data":{...},"guest":"ums.lovely.university","guestcount":"20.87"}
 */
object LpuCrypto {

    private const val KEY_B64 = "m0rDSdPyzt+bo/BuTLgmXssN6TSzRPACdahgiCt5SLs="
    private val gson = Gson()

    private fun key(): SecretKeySpec {
        val raw = Base64.decode(KEY_B64, Base64.DEFAULT)
        require(raw.size == 32) { "expected 32-byte AES key, got ${raw.size}" }
        return SecretKeySpec(raw, "AES")
    }

    data class EncryptedPayload(val v: String, val d: String)

    /** Build the encrypted {v, d} body for the /PVR login call. */
    fun encryptLogin(data: Map<String, String>): EncryptedPayload {
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)

        val plaintext = gson.toJson(
            mapOf(
                "url" to "milkyway",
                "action" to "post",
                "data" to data,
                "guest" to "ums.lovely.university",
                "guestcount" to "20.87",
            )
        )

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, key(), IvParameterSpec(iv))
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        return EncryptedPayload(
            v = Base64.encodeToString(iv, Base64.NO_WRAP),
            d = Base64.encodeToString(encrypted, Base64.NO_WRAP),
        )
    }
}
