package ai.casper.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ai.casper.app.data.ChatStore
import ai.casper.app.data.ChatTurn
import ai.casper.app.data.ModelInfo
import ai.casper.app.data.StoredChat
import ai.casper.app.data.StoredMessage
import ai.casper.app.net.Api
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class ChatViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx = app.applicationContext

    var chats by mutableStateOf<List<StoredChat>>(ChatStore.load(ctx))
        private set
    var activeId by mutableStateOf<String?>(null)
        private set
    var messages by mutableStateOf<List<StoredMessage>>(emptyList())
        private set
    var defaultModel by mutableStateOf(ChatStore.defaultModel(ctx))
        private set
    var model by mutableStateOf(defaultModel)
        private set
    var models by mutableStateOf<List<ModelInfo>>(emptyList())
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private var job: Job? = null

    init {
        loadModels()
    }

    fun loadModels() {
        viewModelScope.launch {
            try {
                models = Api.fetchModels()
            } catch (e: Exception) {
                error = e.message ?: "Не удалось загрузить модели"
            }
        }
    }

    fun newChat() {
        if (busy) return
        activeId = null
        messages = emptyList()
        error = null
    }

    fun open(c: StoredChat) {
        if (busy) return
        activeId = c.id
        messages = c.messages
        model = c.model
        error = null
    }

    fun setModel(id: String) {
        model = id
        val current = activeId
        if (current != null) {
            chats = chats.map { if (it.id == current) it.copy(model = id) else it }
            ChatStore.save(ctx, chats)
        }
    }

    fun delete(c: StoredChat) {
        chats = chats.filter { it.id != c.id }
        ChatStore.save(ctx, chats)
        if (activeId == c.id) newChat()
    }

    fun send(text: String, images: List<String> = emptyList()) {
        val t = text.trim().ifEmpty { if (images.isNotEmpty()) "Что на изображении?" else "" }
        if (t.isEmpty() || busy) return
        if (images.isNotEmpty() && models.firstOrNull { it.id == model }?.supportsImages != true) {
            error = "Эта модель не понимает изображения. Выберите модель с пометкой изображения."
            return
        }

        var id = activeId
        if (id == null) {
            id = UUID.randomUUID().toString()
            activeId = id
            chats = listOf(StoredChat(id, t.take(40), model, System.currentTimeMillis())) + chats
        }
        val chatId = id
        val history = messages + StoredMessage("user", t, images)
        messages = history + StoredMessage("assistant", "")
        busy = true
        error = null
        val sb = StringBuilder()

        job = viewModelScope.launch {
            try {
                Api.streamChat(model, history.map { ChatTurn(it.role, it.content, it.images) }) { delta ->
                    sb.append(delta)
                    withContext(Dispatchers.Main) {
                        messages = history + StoredMessage("assistant", sb.toString())
                    }
                }
            } catch (e: Exception) {
                if (isActive) error = e.message ?: "Ошибка"
            } finally {
                // Сохраняем то, что успели получить, даже если генерацию остановили
                val finalMsgs = if (sb.isEmpty()) history else history + StoredMessage("assistant", sb.toString())
                if (activeId == chatId) messages = finalMsgs
                chats = chats.map {
                    if (it.id == chatId) it.copy(messages = finalMsgs, model = model, updatedAt = System.currentTimeMillis()) else it
                }
                ChatStore.save(ctx, chats)
                busy = false
            }
        }
    }

    fun setDefaultModel(id: String) {
        defaultModel = id
        ChatStore.setDefaultModel(ctx, id)
    }

    fun clearAll() {
        chats = emptyList()
        ChatStore.clear(ctx)
        newChat()
    }

    fun reportError(msg: String) {
        error = msg
    }

    fun stop() {
        job?.cancel()
    }
}
