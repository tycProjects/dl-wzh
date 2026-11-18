package com.glassdex.analyzer.auth

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.glassdex.analyzer.BuildConfig
import com.glassdex.analyzer.data.Store
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

data class Session(val key: String, val name: String, val email: String?, val isGuest: Boolean)

class AuthManager(private val store: Store) {

    val firebaseReady: Boolean get() = BuildConfig.FIREBASE_ENABLED && auth() != null

    private fun auth(): FirebaseAuth? = try {
        if (BuildConfig.FIREBASE_ENABLED) FirebaseAuth.getInstance() else null
    } catch (_: Throwable) { null }

    fun current(): Session? {
        auth()?.currentUser?.let { u ->
            return Session(u.uid, u.displayName ?: u.email?.substringBefore('@') ?: "User", u.email, false)
        }
        if (store.guestMode) return Session("guest", "Guest", null, true)
        return null
    }

    fun continueAsGuest(): Session {
        store.guestMode = true
        return Session("guest", "Guest", null, true)
    }

    suspend fun signInEmail(email: String, password: String, create: Boolean): Result<Session> = runCatching {
        val a = auth() ?: error("Login needs Firebase. Add app/google-services.json (see README).")
        if (create) a.createUserWithEmailAndPassword(email.trim(), password).await()
        else a.signInWithEmailAndPassword(email.trim(), password).await()
        store.guestMode = false
        current()!!
    }

    suspend fun resetPassword(email: String): Result<Unit> = runCatching {
        val a = auth() ?: error("Login needs Firebase.")
        a.sendPasswordResetEmail(email.trim()).await()
        Unit
    }

    suspend fun signInGoogle(activity: Activity): Result<Session> = runCatching {
        val a = auth() ?: error("Google sign-in needs Firebase. Add app/google-services.json (see README).")
        require(BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()) { "Set GOOGLE_WEB_CLIENT_ID in local.properties (see README)." }
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val result = CredentialManager.create(activity).getCredential(activity, request)
        val cred = result.credential
        require(cred is CustomCredential && cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) { "Unexpected credential type" }
        val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
        a.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
        store.guestMode = false
        current()!!
    }

    fun signOut() {
        try { auth()?.signOut() } catch (_: Throwable) { }
        store.guestMode = false
    }
}
