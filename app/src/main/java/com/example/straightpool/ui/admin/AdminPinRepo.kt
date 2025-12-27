package com.example.straightpool.ui.admin

import android.content.Context

class AdminPinRepo(ctx: Context) {

    private val prefs = ctx.getSharedPreferences("admin_prefs", Context.MODE_PRIVATE)

    fun getPin(defaultPin: String = "7777"): String {
        val saved = prefs.getString(KEY_PIN, null)
        return saved ?: defaultPin
    }

    fun setPin(pin: String) {
        prefs.edit().putString(KEY_PIN, pin).apply()
    }

    fun hasCustomPin(): Boolean = prefs.contains(KEY_PIN)

    companion object {
        private const val KEY_PIN = "admin_pin"
    }
}


