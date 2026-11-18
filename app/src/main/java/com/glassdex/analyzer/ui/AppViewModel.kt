package com.glassdex.analyzer.ui

import android.app.Activity
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.glassdex.analyzer.analysis.AnalysisReport
import com.glassdex.analyzer.analysis.Analyzer
import com.glassdex.analyzer.analysis.Progress
import com.glassdex.analyzer.auth.AuthManager
import com.glassdex.analyzer.auth.Session
import com.glassdex.analyzer.data.HistoryItem
import com.glassdex.analyzer.data.Settings
import com.glassdex.analyzer.data.Store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class Screen { LOGIN, HOME, ANALYZING, REPORT, HISTORY, SETTINGS, FILES, VIEWER }

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val store = Store(app)
    val auth = AuthManager(store)

    val session = MutableStateFlow(auth.current())
    val screen = MutableStateFlow(if (session.value == null) Screen.LOGIN else Screen.HOME)
    val settings = MutableStateFlow(store.loadSettings())
    val history = MutableStateFlow<List<HistoryItem>>(session.value?.let { store.history(it.key) } ?: emptyList())
    val progress = MutableStateFlow(Progress("", 0f))
    val report = MutableStateFlow<AnalysisReport?>(null)
    val error = MutableStateFlow<String?>(null)
    val authBusy = MutableStateFlow(false)
    val browseDir = MutableStateFlow<File?>(null)
    val browseRoot = MutableStateFlow<File?>(null)
    val viewFile = MutableStateFlow<File?>(null)

    fun go(s: Screen) { screen.value = s }

    fun back(): Boolean {
        when (screen.value) {
            Screen.LOGIN, Screen.HOME -> return false
            Screen.VIEWER -> screen.value = Screen.FILES
            Screen.FILES -> {
                val d = browseDir.value; val r = browseRoot.value
                if (d != null && r != null && d.canonicalPath != r.canonicalPath) browseDir.value = d.parentFile
                else screen.value = if (report.value != null) Screen.REPORT else Screen.HISTORY
            }
            Screen.ANALYZING -> return true // ignore back while working
            else -> screen.value = Screen.HOME
        }
        return true
    }

    // ---------- auth ----------
    private fun onSession(s: Session) {
        session.value = s
        history.value = store.history(s.key)
        screen.value = Screen.HOME
        error.value = null
    }

    fun guest() = onSession(auth.continueAsGuest())

    fun email(email: String, password: String, create: Boolean) {
        if (email.isBlank() || password.length < 6) { error.value = "Enter an email and a password of at least 6 characters."; return }
        viewModelScope.launch {
            authBusy.value = true
            auth.signInEmail(email, password, create).fold({ onSession(it) }, { error.value = friendly(it) })
            authBusy.value = false
        }
    }

    fun google(activity: Activity) {
        viewModelScope.launch {
            authBusy.value = true
            auth.signInGoogle(activity).fold({ onSession(it) }, { error.value = friendly(it) })
            authBusy.value = false
        }
    }

    fun resetPassword(email: String) {
        if (email.isBlank()) { error.value = "Type your email first."; return }
        viewModelScope.launch {
            auth.resetPassword(email).fold({ error.value = "Reset link sent to $email" }, { error.value = friendly(it) })
        }
    }

    fun signOut() {
        auth.signOut()
        session.value = null
        history.value = emptyList()
        report.value = null
        screen.value = Screen.LOGIN
    }

    private fun friendly(t: Throwable): String = when {
        t.message?.contains("INVALID_LOGIN", true) == true || t.message?.contains("credential is incorrect", true) == true -> "Wrong email or password."
        t.message?.contains("already in use", true) == true -> "That email already has an account. Use Sign in."
        t is androidx.credentials.exceptions.GetCredentialCancellationException -> "Sign-in cancelled."
        else -> t.message ?: t.javaClass.simpleName
    }

    // ---------- analysis ----------
    fun analyze(uri: Uri) {
        screen.value = Screen.ANALYZING
        error.value = null
        progress.value = Progress("Starting", 0f)
        viewModelScope.launch {
            try {
                val r = withContext(Dispatchers.IO) {
                    Analyzer.analyze(getApplication(), uri, settings.value.vtKey.ifBlank { null }) { progress.value = it }
                }
                report.value = r
                session.value?.let { history.value = store.addHistory(it.key, r) }
                screen.value = Screen.REPORT
            } catch (t: Throwable) {
                error.value = "Analysis failed: " + (t.message ?: t.javaClass.simpleName)
                screen.value = Screen.HOME
            }
        }
    }

    fun openFiles(dir: String?) {
        val d = dir?.let { File(it) } ?: return
        if (!d.exists()) { error.value = "Output folder no longer exists."; return }
        browseRoot.value = d; browseDir.value = d; screen.value = Screen.FILES
    }

    fun openViewer(f: File) { viewFile.value = f; screen.value = Screen.VIEWER }

    suspend fun zipOutput(dir: File): File = withContext(Dispatchers.IO) { Analyzer.zipOutput(dir) }

    fun deleteHistory(item: HistoryItem) {
        session.value?.let { history.value = store.deleteHistoryItem(it.key, item) }
    }

    fun updateSettings(s: Settings) { settings.value = s; store.saveSettings(s) }
}
