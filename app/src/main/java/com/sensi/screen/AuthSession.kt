package com.sensi.screen

import android.content.Context

object AuthSession {
    private const val PREFS = "auth_session"
    private const val TOKEN = "token"
    private const val API_URL = "api_url"

    fun token(context: Context): String? = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(TOKEN, null)
    fun saveToken(context: Context, token: String) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(TOKEN, token).apply()
    fun clear(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(TOKEN).apply()
    fun apiUrl(context: Context): String = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(API_URL, "") ?: ""
    fun saveApiUrl(context: Context, url: String) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(API_URL, url.trim().trimEnd('/')).apply()
}
