package com.lputouch.app.data.prefs

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Encrypted storage for sensitive credentials (password, tokens).
 * Uses Android Keystore-backed encryption — data is unreadable even on
 * a rooted device without the key, unlike plain DataStore.
 */
class SecureStore(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "secure_credentials",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun savePassword(userId: String, password: String) {
        prefs.edit().putString("password_$userId", password).apply()
    }

    fun getPassword(userId: String): String? = prefs.getString("password_$userId", null)

    fun saveValue(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun getValue(key: String): String? = prefs.getString(key, null)

    fun clear(userId: String? = null) {
        prefs.edit().clear().apply()
    }
}
