package ai.casper.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class ModelInfo(
    val id: String,
    val name: String,
    val provider: String = "",
    val contextLength: Long? = null,
    val pricePrompt: Double? = null,
    val isFree: Boolean = false,
    val supportsImages: Boolean = false,
    val supportsTools: Boolean = false,
)

@Serializable
data class ModelsResponse(val models: List<ModelInfo> = emptyList())

@Serializable
// images — только в памяти: в хранилище картинки не сохраняются (слишком большие)
data class StoredMessage(val role: String, val content: String, @Transient val images: List<String> = emptyList())

@Serializable
data class StoredChat(
    val id: String,
    val title: String,
    val model: String,
    val updatedAt: Long,
    val messages: List<StoredMessage> = emptyList(),
)

// Сообщение для отправки на сервер (только текст)
data class ChatTurn(val role: String, val content: String, val images: List<String> = emptyList())
