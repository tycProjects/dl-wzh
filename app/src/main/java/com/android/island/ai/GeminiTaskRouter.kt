package com.android.island.ai

/** Tasks that the current Android Island client can route to a Gemini API model. */
enum class GeminiTask {
    CHAT,
    COMPLEX_REASONING,
    SCREEN_ANALYSIS,
    IMAGE_UNDERSTANDING,
    IMAGE_GENERATION,
    LIVE_VOICE,
    SPEECH_TRANSCRIPTION,
    TEXT_TO_SPEECH,
    VIDEO_GENERATION,
    EMBEDDINGS,
    DEEP_RESEARCH
}

/**
 * Capability-aware model selection. The live models.list response is the authority for
 * availability and supported API methods; the small modality catalog only narrows the
 * tasks that can safely be sent through this app's currently implemented REST clients.
 * No model is selected merely because its name looks plausible.
 */
object GeminiTaskRouter {
    private val visionFamilies = listOf(
        "gemini-3", "gemini-2.5", "gemini-2.0-flash"
    )

    fun supports(task: GeminiTask, model: GeminiModelDescriptor): Boolean {
        val id = model.id.lowercase().removePrefix("models/")
        return when (task) {
            GeminiTask.CHAT -> "generateContent" in model.supportedMethods && AiClient.isUsableGeminiChat(id)
            GeminiTask.COMPLEX_REASONING -> "generateContent" in model.supportedMethods &&
                (id.startsWith("gemini-3-pro") || id.startsWith("gemini-2.5-pro"))
            GeminiTask.SCREEN_ANALYSIS, GeminiTask.IMAGE_UNDERSTANDING ->
                "generateContent" in model.supportedMethods &&
                    AiClient.isUsableGeminiChat(id) && visionFamilies.any { id.startsWith(it) }
            GeminiTask.IMAGE_GENERATION ->
                "generateContent" in model.supportedMethods && id in ModelPicker.GEMINI_IMAGE_CATALOG
            // These need separate Live/TTS/transcription/video/embedding/Research transports.
            // They intentionally cannot fall through to ordinary generateContent chat.
            GeminiTask.LIVE_VOICE,
            GeminiTask.SPEECH_TRANSCRIPTION,
            GeminiTask.TEXT_TO_SPEECH,
            GeminiTask.VIDEO_GENERATION,
            GeminiTask.EMBEDDINGS,
            GeminiTask.DEEP_RESEARCH -> false
        }
    }

    /** Keep the currently selected model if valid; otherwise choose the preferred live option. */
    fun select(
        task: GeminiTask,
        catalog: List<GeminiModelDescriptor>,
        preferredModelId: String? = null
    ): String? {
        val available = catalog.filter { supports(task, it) }
        val preferred = preferredModelId?.removePrefix("models/")
        // A model explicitly selected by the user wins whenever it supports this task.
        // Automatic preference is used only when no valid user selection exists.
        if (preferred != null && available.any { it.id == preferred }) return preferred
        val ids = available.map { it.id }
        return when (task) {
            GeminiTask.CHAT -> ModelPicker.pickGeminiChat(ids)
            GeminiTask.COMPLEX_REASONING -> listOf(
                "gemini-3-pro-preview", "gemini-3-pro", "gemini-2.5-pro"
            ).firstOrNull { it in ids }
            GeminiTask.SCREEN_ANALYSIS, GeminiTask.IMAGE_UNDERSTANDING ->
                ModelPicker.pickGeminiChat(ids)
            GeminiTask.IMAGE_GENERATION -> ModelPicker.pickGeminiImage(ids)
            else -> null
        }
    }

    /** Conservative prompt classifier; the live catalog still decides whether a route is possible. */
    fun inferTask(prompt: String, hasImage: Boolean): GeminiTask {
        if (hasImage) return GeminiTask.SCREEN_ANALYSIS
        val text = prompt.lowercase().trim()
        // Device/action commands are latency-sensitive orchestration, not deep reasoning.
        // Route these through the fast chat tier even when their wording contains "aplikasi".
        val simpleCommand = Regex(
            """^(buka|bukain|bukakan|jalankan|jalanin|matikan|matiin|nyalakan|nyalain|aktifkan|nonaktifkan|atur|setel|putar|play|pause|jeda|scroll|gulir|klik|ketuk|tekan|ketik|kembali|home|screenshot|ambil screenshot|cek (memori|baterai|cpu|storage|penyimpanan)|shell:|run shell:|jalankan shell:)\b""",
            RegexOption.IGNORE_CASE
        )
        if (simpleCommand.containsMatchIn(text)) return GeminiTask.CHAT
        val complexSignals = listOf(
            "debug", "debugging", "bug", "refactor", "architecture", "implement", "program", "kode", "coding",
            "kotlin", "java", "compose", "stacktrace", "compile", "build error", "perbaiki kode", "source code", "error", "crash", "fix",
            "aplikasi", "buat kode", "buatkan kode", "develop", "software", "coding assistant",
            "matematika", "buktikan", "prove", "reasoning", "analisis mendalam", "analisis kompleks",
            "multi-step", "langkah demi langkah", "scientific", "ilmiah", "algoritma", "sql"
        )
        return if (complexSignals.any { it in text }) GeminiTask.COMPLEX_REASONING else GeminiTask.CHAT
    }

    fun clientSupportMessage(task: GeminiTask): String = when (task) {
        GeminiTask.LIVE_VOICE -> "Gemini Live API belum terhubung; gunakan voice Android yang tersedia."
        GeminiTask.SPEECH_TRANSCRIPTION -> "Model Gemini Transcribe belum terhubung; gunakan SpeechRecognizer Android yang tersedia."
        GeminiTask.TEXT_TO_SPEECH -> "Gemini TTS belum terhubung; gunakan TextToSpeech Android yang tersedia."
        GeminiTask.VIDEO_GENERATION -> "Transport video generation Gemini belum tersedia di klien Android Island."
        GeminiTask.EMBEDDINGS -> "Embedding Gemini belum terhubung ke indeks pencarian lokal."
        GeminiTask.DEEP_RESEARCH -> "Gemini Deep Research memerlukan alur agent khusus dan belum boleh dipanggil sebagai chat biasa."
        else -> "Tidak ada model Gemini yang tersedia untuk tugas ini."
    }
}
