package com.droidstore.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.droidstore.app.data.Api
import com.droidstore.app.data.ApiException
import com.droidstore.app.data.Session
import com.droidstore.app.ui.components.ErrorText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Вход через Telegram:
 * 1. Сервер создаёт сессию и отдаёт ссылку на бота.
 * 2. Приложение открывает Telegram, пользователь нажимает Start.
 * 3. Бот подтверждает вход на сервере, приложение забирает токен.
 */
@Composable
fun LoginScreen(onLoggedIn: () -> Unit) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var waiting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    fun startLogin() {
        scope.launch {
            error = null
            waiting = true
            try {
                val start = Api.telegramStart()
                val sid = start.getString("sid")
                val expires = start.optInt("expiresInSec", 600)
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(start.getString("url"))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

                // Ждём подтверждения в боте, опрашивая сервер раз в 2 секунды
                val deadline = System.currentTimeMillis() + expires * 1000L
                while (System.currentTimeMillis() < deadline) {
                    delay(2000)
                    val st = Api.telegramStatus(sid)
                    if (st.getString("status") == "ok") {
                        Session.save(st.getString("token"))
                        onLoggedIn()
                        return@launch
                    }
                }
                error = "Время входа истекло. Попробуйте ещё раз."
            } catch (e: ApiException) {
                error = when (e.message) {
                    "telegram_not_configured" -> "Вход через Telegram не настроен на сервере."
                    "account_blocked" -> "Аккаунт заблокирован."
                    "login_expired" -> "Сессия входа истекла. Попробуйте ещё раз."
                    else -> "Ошибка сервера: ${e.message}"
                }
            } catch (e: Exception) {
                error = "Нет связи с сервером: ${e.message}"
            } finally {
                waiting = false
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedVisibility(visible = visible, enter = fadeIn() + slideInVertically { it / 4 }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("DroidStore", style = MaterialTheme.typography.headlineLarge)
                Text("Приложения для Android", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Button(onClick = { startLogin() }, enabled = !waiting, modifier = Modifier.padding(top = 32.dp)) {
            Text(if (waiting) "Ждём подтверждения в Telegram..." else "Войти через Telegram")
        }
        if (waiting) CircularProgressIndicator(Modifier.padding(top = 16.dp))
        Text(
            "Откроется бот. Нажмите Start, и вход подтвердится автоматически.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 12.dp),
        )
        ErrorText(error)
    }
}
