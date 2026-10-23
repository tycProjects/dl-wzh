package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiServiceManager
import com.example.model.AiProvider
import com.example.model.AppSettings
import com.example.model.ChatMessage
import com.example.model.CodeSnippet
import com.example.model.CodeTaskType
import com.example.model.LaunchableApp
import com.example.model.VoiceState
import com.example.service.PendingActionConfirm
import com.example.service.SpeechAndTtsManager
import com.example.service.SystemActionHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val aiServiceManager = AiServiceManager()
    val speechAndTtsManager = SpeechAndTtsManager(application.applicationContext)
    val systemActionHelper = SystemActionHelper(application.applicationContext)

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                text = "Welcome to ARAFAT AI. I am your personal cybernetic AI assistant, created by ARAFAT MOJMUNDAR. Speak in English or Bengali (বাংলা), generate code, control system apps, analyze images, or craft elite prompts.",
                isUser = false,
                providerUsed = "ARAFAT AI Core"
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _selectedImageBase64 = MutableStateFlow<String?>(null)
    val selectedImageBase64: StateFlow<String?> = _selectedImageBase64.asStateFlow()

    private val _savedSnippets = MutableStateFlow<List<CodeSnippet>>(
        listOf(
            CodeSnippet(
                title = "Futuristic Jetpack Compose Card",
                language = "Kotlin",
                code = """
                    @Composable
                    fun CyberHUDCard(title: String, subtitle: String) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF0C1322)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF00F0FF)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().padding(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(title, color = Color(0xFF00F0FF), fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(subtitle, color = Color(0xFF9FB2C8))
                            }
                        }
                    }
                """.trimIndent(),
                explanation = "A futuristic cyber HUD card with cyan neon border and deep navy background."
            )
        )
    )
    val savedSnippets: StateFlow<List<CodeSnippet>> = _savedSnippets.asStateFlow()

    private val _installedApps = MutableStateFlow<List<LaunchableApp>>(emptyList())
    val installedApps: StateFlow<List<LaunchableApp>> = _installedApps.asStateFlow()

    private val _pendingAction = MutableStateFlow<PendingActionConfirm?>(null)
    val pendingAction: StateFlow<PendingActionConfirm?> = _pendingAction.asStateFlow()

    private val _screenSessionActive = MutableStateFlow(false)
    val screenSessionActive: StateFlow<Boolean> = _screenSessionActive.asStateFlow()

    private val _generatedPrompt = MutableStateFlow("")
    val generatedPrompt: StateFlow<String> = _generatedPrompt.asStateFlow()

    private var currentAiJob: Job? = null

    init {
        loadInstalledApps()
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _installedApps.value = systemActionHelper.getInstalledLaunchableApps()
        }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        _settings.value = transform(_settings.value)
    }

    fun attachImageUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>().applicationContext
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    val stream = ByteArrayOutputStream()
                    // Compress to reasonable resolution for AI
                    val scaled = if (bitmap.width > 1024 || bitmap.height > 1024) {
                        val ratio = minOf(1024f / bitmap.width, 1024f / bitmap.height)
                        Bitmap.createScaledBitmap(
                            bitmap,
                            (bitmap.width * ratio).toInt(),
                            (bitmap.height * ratio).toInt(),
                            true
                        )
                    } else bitmap

                    scaled.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                    val byteArray = stream.toByteArray()
                    _selectedImageBase64.value = Base64.encodeToString(byteArray, Base64.NO_WRAP)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun clearAttachedImage() {
        _selectedImageBase64.value = null
    }

    fun cancelCurrentAiRequest() {
        currentAiJob?.cancel()
        _isLoading.value = false
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank() && _selectedImageBase64.value == null) return

        val img = _selectedImageBase64.value
        _selectedImageBase64.value = null // consume image

        // Check if user input is an app voice/text command
        val sysResult = systemActionHelper.handleVoiceCommand(userText) { launchedName ->
            // Launched
        }

        if (sysResult != null) {
            _chatMessages.value = _chatMessages.value + ChatMessage(
                text = userText,
                isUser = true,
                imageBase64 = img
            ) + ChatMessage(
                text = sysResult.responseText,
                isUser = false,
                providerUsed = "System Automation"
            )
            if (_settings.value.ttsEnabled) {
                speechAndTtsManager.speak(sysResult.responseText, _settings.value.voiceLanguage)
            }
            return
        }

        val userMsg = ChatMessage(
            text = userText,
            isUser = true,
            imageBase64 = img
        )
        _chatMessages.value = _chatMessages.value + userMsg

        _isLoading.value = true

        currentAiJob = viewModelScope.launch {
            try {
                val currentSettings = _settings.value
                val result = aiServiceManager.generateResponse(
                    prompt = userText,
                    imageBase64 = img,
                    provider = currentSettings.primaryProvider,
                    geminiModel = currentSettings.geminiModel,
                    grokModel = currentSettings.grokModel,
                    customGeminiKey = currentSettings.customGeminiKey,
                    customGrokKey = currentSettings.customGrokKey
                )

                _chatMessages.value = _chatMessages.value + ChatMessage(
                    text = result.text,
                    isUser = false,
                    providerUsed = result.providerUsed,
                    isError = !result.isSuccess
                )

                if (currentSettings.ttsEnabled && result.isSuccess) {
                    speechAndTtsManager.speak(result.text, currentSettings.voiceLanguage)
                }
            } catch (e: Exception) {
                _chatMessages.value = _chatMessages.value + ChatMessage(
                    text = "Request error: ${e.localizedMessage ?: "Unknown error occurred"}",
                    isUser = false,
                    providerUsed = "System Error",
                    isError = true
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun startVoiceInput() {
        speechAndTtsManager.startListening(_settings.value.voiceLanguage) { recognizedSpeech ->
            if (recognizedSpeech.isNotBlank()) {
                sendMessage(recognizedSpeech)
            }
        }
    }

    fun stopVoiceInput() {
        speechAndTtsManager.stopListening()
    }

    fun handleCodingTask(
        taskType: CodeTaskType,
        language: String,
        codeOrDescription: String,
        onComplete: (CodeSnippet) -> Unit
    ) {
        if (codeOrDescription.isBlank()) return
        _isLoading.value = true

        val prompt = when (taskType) {
            CodeTaskType.GENERATE ->
                "Write clean, modern, production-grade $language code for: $codeOrDescription. Include brief explanation and best practices."
            CodeTaskType.EXPLAIN ->
                "Explain the following $language code in depth, detailing each core component, complexity, and possible improvements:\n\n```$language\n$codeOrDescription\n```"
            CodeTaskType.DEBUG ->
                "Analyze and debug this $language code. Identify syntax or logic bugs, explain the root cause, and provide the corrected code:\n\n```$language\n$codeOrDescription\n```"
            CodeTaskType.PROJECT_STRUCT ->
                "Generate an optimal production project folder structure and architecture layout for $language for: $codeOrDescription. List files and responsibilities."
            CodeTaskType.PROMPT_GEN ->
                "Create a master-grade development prompt for $language to build: $codeOrDescription. Include architecture constraints, UI specifications, state management, and edge cases."
        }

        currentAiJob = viewModelScope.launch {
            try {
                val s = _settings.value
                val result = aiServiceManager.generateResponse(
                    prompt = prompt,
                    provider = s.primaryProvider,
                    geminiModel = s.geminiModel,
                    grokModel = s.grokModel,
                    customGeminiKey = s.customGeminiKey,
                    customGrokKey = s.customGrokKey
                )

                val snippet = CodeSnippet(
                    title = "$language - ${taskType.label}",
                    language = language,
                    code = extractCodeOrReturnAll(result.text),
                    explanation = result.text
                )
                _savedSnippets.value = listOf(snippet) + _savedSnippets.value
                onComplete(snippet)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun extractCodeOrReturnAll(text: String): String {
        val regex = Regex("```(?:[a-zA-Z0-9]+)?\\n([\\s\\S]*?)```")
        val match = regex.find(text)
        return match?.groupValues?.getOrNull(1)?.trim() ?: text
    }

    fun deleteSnippet(id: String) {
        _savedSnippets.value = _savedSnippets.value.filter { it.id != id }
    }

    fun clearChatHistory() {
        _chatMessages.value = listOf(
            ChatMessage(
                text = "Chat history cleared. ARAFAT AI is ready for your instructions.",
                isUser = false,
                providerUsed = "ARAFAT AI Core"
            )
        )
    }

    fun generateChatGptPrompt(
        targetType: String,
        description: String,
        targetAi: String
    ) {
        if (description.isBlank()) return
        _isLoading.value = true

        val prompt = """
            Act as an elite Prompt Engineer. Generate an extensive, robust, high-performance master prompt for $targetAi to create a $targetType based on this user description:
            "$description"
            
            The generated prompt must include:
            1. Role & Identity definition
            2. Core architecture & technology stack
            3. Detailed UX/UI layout and cyber aesthetic guidelines
            4. Step-by-step implementation plan
            5. Edge cases and production hardening
        """.trimIndent()

        viewModelScope.launch {
            try {
                val s = _settings.value
                val result = aiServiceManager.generateResponse(
                    prompt = prompt,
                    provider = s.primaryProvider,
                    geminiModel = s.geminiModel,
                    grokModel = s.grokModel,
                    customGeminiKey = s.customGeminiKey,
                    customGrokKey = s.customGrokKey
                )
                _generatedPrompt.value = result.text
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun setPendingAction(action: PendingActionConfirm?) {
        _pendingAction.value = action
    }

    fun startScreenSession() {
        _screenSessionActive.value = true
    }

    fun stopScreenSession() {
        _screenSessionActive.value = false
    }

    fun analyzeScreenCapture(capturedDescription: String) {
        sendMessage("Screen Assistant Analysis: The active screen currently shows: $capturedDescription. Please analyze this view, summarize elements, and suggest actions.")
    }

    override fun onCleared() {
        super.onCleared()
        speechAndTtsManager.release()
    }
}
