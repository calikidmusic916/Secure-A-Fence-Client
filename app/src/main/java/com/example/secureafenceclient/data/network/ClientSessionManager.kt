package com.example.secureafenceclient.data.network

import android.content.Context
import android.content.Intent
import com.example.secureafenceclient.data.model.CustomerProfile
import com.example.secureafenceclient.ui.auth.ClientLoginActivity
import com.google.gson.Gson

object ClientSessionManager {
    private const val PREFS_NAME = "client_prefs"
    private const val KEY_TOKEN = "token"
    private const val KEY_CUSTOMER_ID = "customer_id"
    private const val KEY_CUSTOMER_EMAIL = "customer_email"
    private const val KEY_CUSTOMER_PROFILE = "customer_profile"

    fun getToken(context: Context): String? {
        val rawToken = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_TOKEN, null)
        if (rawToken.isNullOrEmpty()) return null
        return if (rawToken.startsWith("Bearer ", ignoreCase = true)) {
            rawToken.substring(7).trim()
        } else {
            rawToken.trim()
        }
    }

    fun saveSession(context: Context, token: String, customer: CustomerProfile) {
        val cleanToken = if (token.startsWith("Bearer ", ignoreCase = true)) {
            token.substring(7).trim()
        } else {
            token.trim()
        }
        val jsonProfile = Gson().toJson(customer)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_TOKEN, cleanToken)
            .putString(KEY_CUSTOMER_ID, customer.id ?: "")
            .putString(KEY_CUSTOMER_EMAIL, customer.email ?: "")
            .putString(KEY_CUSTOMER_PROFILE, jsonProfile)
            .apply()
    }

    fun getCustomerProfile(context: Context): CustomerProfile? {
        val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_CUSTOMER_PROFILE, null)
        if (json.isNullOrEmpty()) return null
        return try {
            Gson().fromJson(json, CustomerProfile::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun getCustomerId(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_CUSTOMER_ID, "") ?: ""
    }

    fun getCustomerEmail(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_CUSTOMER_EMAIL, "") ?: ""
    }

    fun clearSession(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
        val intent = Intent(context, ClientLoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }

    fun getStripePublishableKey(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("stripe_pk", "pk_test_51UGs9qERCsfh1i1Di6n7HvRCVbVwdt3Rh6CSGln2eVjUGCwSdXmRY3Af88zHFm5KOPKY7Smi1ZRZD16vmjKZWvhZ00fL302Or4")
            ?: "pk_test_51UGs9qERCsfh1i1Di6n7HvRCVbVwdt3Rh6CSGln2eVjUGCwSdXmRY3Af88zHFm5KOPKY7Smi1ZRZD16vmjKZWvhZ00fL302Or4"
    }
}
