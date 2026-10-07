package com.stela.agent.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class ApiKeyStore(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "stela_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getApiKey(): String = prefs.getString("gemini_api_key", "") ?: ""

    fun saveApiKey(value: String) {
        prefs.edit().putString("gemini_api_key", value).apply()
    }

    fun clearApiKey() {
        prefs.edit().remove("gemini_api_key").apply()
    }
}
