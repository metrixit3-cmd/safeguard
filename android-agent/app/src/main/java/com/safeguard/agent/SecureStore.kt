package com.safeguard.agent

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecureStore {
    private const val FILE = "safeguard_secure"
    private lateinit var preferences: android.content.SharedPreferences

    fun initialize(context: Context) {
        if (::preferences.isInitialized) return
        val master = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        preferences = EncryptedSharedPreferences.create(
            context, FILE, master,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun get(key: String, fallback: String = ""): String = preferences.getString(key, fallback).orEmpty()
    fun put(key: String, value: String) { preferences.edit().putString(key, value).apply() }
    fun bool(key: String, fallback: Boolean = false): Boolean = preferences.getBoolean(key, fallback)
    fun putBool(key: String, value: Boolean) { preferences.edit().putBoolean(key, value).apply() }
    fun clearAgent() {
        preferences.edit().remove("agent_token").remove("device_id").remove("device_name")
            .remove("owner_email").remove("session_cookies").remove("location_consent")
            .remove("protection_armed").remove("sim_guard_enabled").apply()
    }

    fun deviceId(): String = get("device_id")
    fun agentToken(): String = get("agent_token")
    fun deviceName(): String = get("device_name", "This Android phone")
    fun armed(): Boolean = bool("protection_armed")
}
