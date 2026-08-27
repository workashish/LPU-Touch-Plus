package com.lputouch.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

class SessionStore(private val context: Context, private val secureStore: SecureStore) {

    /** In-memory JWT cache so OkHttp interceptors never need to block on disk I/O. */
    @Volatile
    private var cachedJwt: String? = null

    /** Synchronous access for the OkHttp interceptor (no runBlocking needed). */
    fun jwtSync(): String? = cachedJwt

    /** Populate the in-memory cache after process restart. */
    fun warmJwt(token: String) {
        cachedJwt = token
    }

    private object Keys {
        val USER_ID = stringPreferencesKey("user_id")
        val JWT_TOKEN = stringPreferencesKey("jwt_token")
        val ACCESS_TOKEN = stringPreferencesKey("access_token")   // UMS GUID token
        val TOKEN_PARAM = stringPreferencesKey("token_param")
        val DEVICE_ID = stringPreferencesKey("device_id")
        val PLAYER_ID = stringPreferencesKey("player_id")
        val STUDENT_NAME = stringPreferencesKey("student_name")
        val PROGRAM = stringPreferencesKey("program")
        val LOGGED_IN = booleanPreferencesKey("logged_in")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val PIN_ENABLED = booleanPreferencesKey("pin_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val LAST_NOTIFIED_ANNOUNCEMENT = stringPreferencesKey("last_notified_announcement")
    }

    val loggedIn: Flow<Boolean> = context.dataStore.data.map { it[Keys.LOGGED_IN] ?: false }
    val userId: Flow<String?> = context.dataStore.data.map { it[Keys.USER_ID] }
    val jwtToken: Flow<String?> = context.dataStore.data.map { it[Keys.JWT_TOKEN] }
    val accessToken: Flow<String?> = context.dataStore.data.map { it[Keys.ACCESS_TOKEN] }
    val tokenParam: Flow<String?> = context.dataStore.data.map { it[Keys.TOKEN_PARAM] }
    val deviceId: Flow<String?> = context.dataStore.data.map { it[Keys.DEVICE_ID] }
    val studentName: Flow<String?> = context.dataStore.data.map { it[Keys.STUDENT_NAME] }
    val program: Flow<String?> = context.dataStore.data.map { it[Keys.PROGRAM] }
    val biometricEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.BIOMETRIC_ENABLED] ?: false }
    val darkMode: Flow<Boolean> = context.dataStore.data.map { it[Keys.DARK_MODE] ?: false }
    val pinEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.PIN_ENABLED] ?: false }
    val pinHash: Flow<String?> = context.dataStore.data.map { it[Keys.PIN_HASH] }

    suspend fun currentUserId(): String? = context.dataStore.data.first()[Keys.USER_ID]

    suspend fun password(userId: String): String? = secureStore.getPassword(userId)

    suspend fun saveSession(
        userId: String,
        password: String,
        jwtToken: String,
        accessToken: String,
        deviceId: String,
        tokenParam: String? = null,
    ) {
        cachedJwt = jwtToken
        secureStore.savePassword(userId, password)
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_ID] = userId
            prefs[Keys.JWT_TOKEN] = jwtToken
            prefs[Keys.ACCESS_TOKEN] = accessToken
            if (tokenParam != null) prefs[Keys.TOKEN_PARAM] = tokenParam
            prefs[Keys.DEVICE_ID] = deviceId
            prefs[Keys.LOGGED_IN] = true
        }
    }

    suspend fun saveProfile(name: String, program: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.STUDENT_NAME] = name
            prefs[Keys.PROGRAM] = program
        }
    }

    suspend fun updateJwtToken(token: String) {
        cachedJwt = token
        context.dataStore.edit { it[Keys.JWT_TOKEN] = token }
    }

    suspend fun saveAccessToken(token: String) {
        context.dataStore.edit { it[Keys.ACCESS_TOKEN] = token }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_MODE] = enabled }
    }

    suspend fun setPin(enabled: Boolean, hash: String? = null) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PIN_ENABLED] = enabled
            if (hash != null) prefs[Keys.PIN_HASH] = hash else prefs.remove(Keys.PIN_HASH)
        }
    }

    suspend fun lastNotifiedAnnouncementId(): String? =
        context.dataStore.data.first()[Keys.LAST_NOTIFIED_ANNOUNCEMENT]

    suspend fun saveLastNotifiedAnnouncementId(id: String) {
        context.dataStore.edit { it[Keys.LAST_NOTIFIED_ANNOUNCEMENT] = id }
    }

    suspend fun logout() {
        val uid = currentUserId()
        cachedJwt = null
        secureStore.clear(uid)
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.JWT_TOKEN)
            prefs.remove(Keys.ACCESS_TOKEN)
            prefs.remove(Keys.TOKEN_PARAM)
            prefs.remove(Keys.DEVICE_ID)
            prefs.remove(Keys.PLAYER_ID)
            prefs.remove(Keys.STUDENT_NAME)
            prefs.remove(Keys.PROGRAM)
            prefs[Keys.LOGGED_IN] = false
        }
    }
}
