package com.example.data.model

import java.util.UUID

enum class StudioMode(
    val titleAr: String,
    val titleEn: String,
    val subtitleAr: String,
    val subtitleEn: String,
    val iconName: String
) {
    ONE_TOUCH(
        "لمسة إبداعية شاملة",
        "One-Touch Studio",
        "توليد متزامن لتطبيق ولعبة وموقع وصورة وفيديو",
        "Simultaneously generate App, Game, Web, Image & Video",
        "AutoAwesome"
    ),
    APPS(
        "صانع التطبيقات",
        "App Studio",
        "توليد تطبيقات أندرويد وCompose وواجهات متكاملة",
        "Generate Android Jetpack Compose apps & components",
        "Smartphone"
    ),
    GAMES(
        "صانع الألعاب التفاعلية",
        "Game Studio",
        "إنشاء ألعاب كاملة قابلة للعب مباشرة داخل التطبيق",
        "Create playable HTML5 Canvas games with live sandbox",
        "SportsEsports"
    ),
    WEBSITES(
        "صانع المواقع",
        "Web Studio",
        "إنشاء مواقع عصرية وصفحات هبوط مع معاينة حية",
        "Build responsive modern landing pages with live preview",
        "Language"
    ),
    IMAGES(
        "استوديو الصور والفنون",
        "Image & Art Studio",
        "هندسة برومبت فنية وتوليد تصاميم بأساليب سينمائية",
        "Craft visual prompts & creative mockups with styles",
        "Palette"
    ),
    VIDEOS(
        "استوديو الفيديو والسيناريو",
        "Video Studio",
        "توليد سيناريو المشاهد، لقطات الكاميرا، والتعليق الصوتي",
        "Generate multi-scene cinematic video storyboards & cues",
        "MovieFilter"
    ),
    CONTENT(
        "صانع المحتوى الذكي",
        "Content & Copy",
        "خطط تسويق، أفكار إعلانية، ومحتوى احترافي متعدد",
        "Marketing strategies, pitches & high-impact copy",
        "Article"
    )
}

enum class PreviewType {
    HTML_RUNNABLE,
    KOTLIN_CODE,
    IMAGE_MOCKUP,
    STORYBOARD,
    MARKDOWN_TEXT
}

data class StoryboardScene(
    val sceneNumber: Int,
    val title: String,
    val durationSeconds: Int,
    val visualPrompt: String,
    val cameraAngle: String,
    val narration: String,
    val audioEffect: String
)

data class CreationItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val mode: StudioMode,
    val prompt: String,
    val summary: String,
    val content: String,
    val previewType: PreviewType,
    val tags: List<String> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val storyboards: List<StoryboardScene> = emptyList(),
    val imageUrl: String? = null
)
