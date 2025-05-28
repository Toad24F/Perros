package com.example.perros

import android.content.Context

// En un archivo SessionManager.kt o similar
object SessionManager {
    fun saveUserData(context: Context, name: String, email: String) {
        val sharedPref = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("user_name", name)
            putString("user_email", email)
            apply()
        }
    }

    fun getUserName(context: Context): String? {
        val sharedPref = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        return sharedPref.getString("user_name", null)
    }

    fun getUserEmail(context: Context): String? {
        val sharedPref = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        return sharedPref.getString("user_email", null)
    }

    fun clearSession(context: Context) {
        val sharedPref = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            clear()
            apply()
        }
    }
}