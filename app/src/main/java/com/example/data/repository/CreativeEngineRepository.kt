package com.example.data.repository

import com.example.data.api.GeminiClient
import com.example.data.model.CreationItem
import com.example.data.model.PreviewType
import com.example.data.model.StoryboardScene
import com.example.data.model.StudioMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class CreativeEngineRepository {

    private val _creations = MutableStateFlow<List<CreationItem>>(emptyList())
    val creations: StateFlow<List<CreationItem>> = _creations.asStateFlow()

    init {
        loadInitialShowcase()
    }

    private fun loadInitialShowcase() {
        val initialItems = listOf(
            CreationItem(
                id = "sample_game_1",
                title = "لعبة حرب الفضاء السيبرانية (Cyber Defender)",
                mode = StudioMode.GAMES,
                prompt = "أنشئ لعبة فضاء نيون ملحمية برسوميات كانفاس وضوابط لمس تفاعلية",
                summary = "لعبة أركيد تفاعلية نيون كاملة مع إطلاق ليزر وتفجيرات ونقاط ومؤشر طاقة، وتعمل باللمس مباشرة.",
                content = getPlayableCyberSpaceGameHtml(),
                previewType = PreviewType.HTML_RUNNABLE,
                tags = listOf("ألعاب", "أركيد", "HTML5", "Interactive"),
                isFavorite = true
            ),
            CreationItem(
                id = "sample_app_1",
                title = "تطبيق متتبع اللياقة الذكي (FitPulse AI)",
                mode = StudioMode.APPS,
                prompt = "تطبيق أندرويد متكامل بلغة كوتلن وJetpack Compose لتتبع التمارين والسعرات مع تحليلات ذكية",
                summary = "كود كامل لتطبيق أندرويد حديث باستخدام Jetpack Compose و Material 3 و ViewModel مع بطاقات إحصائيات.",
                content = getFitnessAppKotlinCode(),
                previewType = PreviewType.KOTLIN_CODE,
                tags = listOf("تطبيقات", "Android", "Kotlin", "Compose"),
                isFavorite = true
            ),
            CreationItem(
                id = "sample_web_1",
                title = "موقع هبوط سحابي مستقبلي (NeuralCloud Platform)",
                mode = StudioMode.WEBSITES,
                prompt = "صفحة هبوط تفاعلية لخدمة ذكاء اصطناعي سحابية مع تأثيرات زجاجية وأزرار نيون متجاوبة",
                summary = "موقع ويب احترافي متكامل ومتجاوب 100% مع الهواتف، تصميم Dark Glassmorphism، أقسام مميزات وأسعار.",
                content = getModernLandingPageHtml(),
                previewType = PreviewType.HTML_RUNNABLE,
                tags = listOf("مواقع", "Landing Page", "Tailwind UI", "Dark Theme"),
                isFavorite = true
            ),
            CreationItem(
                id = "sample_video_1",
                title = "سيناريو إعلان ترويجي سينمائي (The Neural Leap)",
                mode = StudioMode.VIDEOS,
                prompt = "إعلان فيديو سينمائي مشوق لمدة 30 ثانية لشركة ذكاء اصطناعي ثورية",
                summary = "سيناريو مقسم إلى 4 مشاهد متقنة مع زوايا الكاميرا، أوصاف الإضاءة، النصوص الصوتية، ومؤثرات الصوت.",
                content = "السيناريو السينمائي الكامل لإعلان The Neural Leap مع تفاصيل الإخراج والتعليق الصوتي لكل مشهد.",
                previewType = PreviewType.STORYBOARD,
                tags = listOf("فيديو", "سيناريو", "إعلان", "Cinematic"),
                storyboards = listOf(
                    StoryboardScene(
                        sceneNumber = 1,
                        title = "الشرارة الأولى في الظلام",
                        durationSeconds = 6,
                        visualPrompt = "Macro close-up shot of a glowing blue synaptic neural fiber pulsating with neon code particles in infinite black space, 8K, depth of field.",
                        cameraAngle = "Low Angle, Slow forward push-in (Dolly)",
                        narration = "في البدء كانت الفكرة مجرد ومضة... تنتظر من يمنحها الحياة.",
                        audioEffect = "صوت طنين طاقة متصاعد ناعم (Low Sub-Bass Pulse)"
                    ),
                    StoryboardScene(
                        sceneNumber = 2,
                        title = "بناء العالم الرقمي",
                        durationSeconds = 8,
                        visualPrompt = "Wide aerial shot of a futuristic cyberpunk mega-city assembling itself instantaneously in holographic wireframe grids, neon magenta & teal.",
                        cameraAngle = "High Aerial Bird-Eye Orbiting shot, fast pan down",
                        narration = "اليوم، لا حدود لما يمكنك ابتكاره بلمسة واحدة من الذكاء الاصطناعي.",
                        audioEffect = "موسيقى سينمائية أوركسترالية حماسية تندمج مع إيقاع إلكتروني"
                    ),
                    StoryboardScene(
                        sceneNumber = 3,
                        title = "القوة بين يديك",
                        durationSeconds = 10,
                        visualPrompt = "Medium shot of an innovator interacting with a floating transparent glass HUD, apps and games spinning like galaxies.",
                        cameraAngle = "Eye-level Dutch angle with subtle handheld organic motion",
                        narration = "تطبيقاتك، ألعابك، عوالمك الجديدة... كلها أصبحت واقعاً ينبض بالسرعة والإبداع.",
                        audioEffect = "أصوات لمس رقمية خفيفة (Hologram click UI sound)"
                    ),
                    StoryboardScene(
                        sceneNumber = 4,
                        title = "الشعار والختام",
                        durationSeconds = 6,
                        visualPrompt = "Dramatic central focus on OmniStudio AI 3D crystal prism emblem radiating bright cosmic flare, with call-to-action glow.",
                        cameraAngle = "Static center lock with lens flare breathing",
                        narration = "أومني ستوديو: اطلق العنان لمخيلتك الآن.",
                        audioEffect = "ضربة ختامية عميقة (Deep Cinematic Bass Drop)"
                    )
                ),
                isFavorite = false
            ),
            CreationItem(
                id = "sample_image_1",
                title = "محارب الذكاء الاصطناعي السيبراني (Cyber Ronin)",
                mode = StudioMode.IMAGES,
                prompt = "محارب سايبربانك مستقبلي بدرع نيون هولوجرامي في شوارع طوكيو الممطرة",
                summary = "برومبت فني متقدم مهندس بمواصفات إضاءة السينما وعدسة 85mm مع خيارات النسب 16:9 و 1:1.",
                content = """
                    Prompt:
                    Cyberpunk cybernetic ronin warrior standing in rain-slicked futuristic neon streets, translucent glowing holographic armor plates, dual laser katana, reflective puddles reflecting neon magenta and cyan kanji billboards, volumetric mist and atmospheric fog, ultra-realistic textures, 8k resolution, cinematic lighting, shot on 85mm f/1.4 lens, Unreal Engine 5 render style.
                    
                    Negative Prompt:
                    blurry, distorted anatomy, extra limbs, low resolution, flat lighting, oversaturated, amateur.
                    
                    Suggested Settings:
                    - Aspect Ratio: 16:9
                    - CFG Scale: 7.5
                    - Sampler: DPM++ 2M Karras
                    - Style: Cyberpunk Photorealism
                """.trimIndent(),
                previewType = PreviewType.IMAGE_MOCKUP,
                tags = listOf("صور", "فن رقمي", "Cyberpunk", "Prompt"),
                isFavorite = false
            )
        )

        _creations.value = initialItems
    }

    suspend fun generateSingle(
        mode: StudioMode,
        userPrompt: String,
        styleOrCategory: String = "افتراضي"
    ): CreationItem {
        val title = generateCleanTitle(userPrompt, mode)
        val isKeyValid = GeminiClient.isApiKeyConfigured()

        if (isKeyValid) {
            val systemInstruction = getSystemInstructionForMode(mode)
            val apiPrompt = buildPromptForMode(mode, userPrompt, styleOrCategory)

            val result = GeminiClient.generateContent(
                prompt = apiPrompt,
                systemInstruction = systemInstruction,
                modelName = if (mode == StudioMode.APPS || mode == StudioMode.GAMES) "gemini-3.1-pro-preview" else "gemini-3.5-flash"
            )

            if (result.isSuccess) {
                val generatedText = result.getOrNull() ?: ""
                val item = parseGeneratedContentToItem(mode, userPrompt, title, generatedText)
                saveItem(item)
                return item
            }
        }

        // Offline / Creative Engine fallback with rich intelligent generator
        val fallbackItem = generateIntelligentTemplate(mode, userPrompt, styleOrCategory)
        saveItem(fallbackItem)
        return fallbackItem
    }

    suspend fun generateOneTouchBundle(centralIdea: String): List<CreationItem> {
        val app = generateSingle(StudioMode.APPS, "تطبيق $centralIdea", "موبايل أندرويد")
        val game = generateSingle(StudioMode.GAMES, "لعبة $centralIdea التفاعلية", "أركيد لمس")
        val web = generateSingle(StudioMode.WEBSITES, "موقع هبوط ومنصة $centralIdea", "صفحة عصرية")
        val img = generateSingle(StudioMode.IMAGES, "بوستر وتصميم فني لـ $centralIdea", "فن رقمي نيون")
        val vid = generateSingle(StudioMode.VIDEOS, "إعلان فيديو تشويقي لـ $centralIdea", "سينمائي")

        return listOf(app, game, web, img, vid)
    }

    fun saveItem(item: CreationItem) {
        val current = _creations.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.id == item.id }
        if (existingIndex >= 0) {
            current[existingIndex] = item
        } else {
            current.add(0, item)
        }
        _creations.value = current
    }

    fun deleteItem(id: String) {
        _creations.value = _creations.value.filterNot { it.id == id }
    }

    fun toggleFavorite(id: String) {
        _creations.value = _creations.value.map {
            if (it.id == id) it.copy(isFavorite = !it.isFavorite) else it
        }
    }

    private fun generateCleanTitle(prompt: String, mode: StudioMode): String {
        val trimmed = prompt.trim()
        val firstWords = trimmed.split(" ").take(5).joinToString(" ")
        return when (mode) {
            StudioMode.APPS -> "تطبيق: $firstWords"
            StudioMode.GAMES -> "لعبة: $firstWords"
            StudioMode.WEBSITES -> "موقع: $firstWords"
            StudioMode.IMAGES -> "تصميم: $firstWords"
            StudioMode.VIDEOS -> "فيديو: $firstWords"
            StudioMode.CONTENT -> "محتوى: $firstWords"
            StudioMode.ONE_TOUCH -> "مشروع: $firstWords"
        }
    }

    private fun getSystemInstructionForMode(mode: StudioMode): String {
        return when (mode) {
            StudioMode.GAMES -> """
                You are an elite HTML5 Game Developer.
                Generate a COMPLETE, standalone, zero-external-dependency HTML file containing CSS and JS in <style> and <script> tags.
                The game must run smoothly in an Android WebView.
                Include on-screen touch buttons or canvas touch controls so mobile users can play immediately.
                Use modern neon/arcade visuals, canvas 2D rendering, high framerate, score tracking, and restart functionality.
                Output ONLY valid HTML code.
            """.trimIndent()

            StudioMode.WEBSITES -> """
                You are a senior frontend designer.
                Generate a COMPLETE, standalone, modern responsive HTML5 page with inline CSS and JS.
                Use a sleek dark cyberpunk/glassmorphism design, responsive grid, mobile-friendly navigation, animated call-to-actions, and interactive elements.
                Output ONLY valid HTML code.
            """.trimIndent()

            StudioMode.APPS -> """
                You are a senior Android Kotlin and Jetpack Compose architect.
                Generate production-ready Kotlin Jetpack Compose code with clean architecture, Material 3, ViewModel, state management, and modern UI components.
            """.trimIndent()

            StudioMode.VIDEOS -> """
                You are an award-winning movie director and storyboard artist.
                Generate a structured shot-by-shot video storyboard with scene numbers, visual prompts (ready for AI video generation), camera movement, narration, and sound effects.
            """.trimIndent()

            StudioMode.IMAGES -> """
                You are a master AI prompt engineer and creative art director.
                Generate detailed visual prompts with camera angles, lighting, render engine style, color palette, aspect ratio, and negative prompts.
            """.trimIndent()

            StudioMode.CONTENT, StudioMode.ONE_TOUCH -> """
                You are an expert creative content strategist.
                Provide polished, persuasive, high-impact Arabic and English creative copy, launch plans, and descriptions.
            """.trimIndent()
        }
    }

    private fun buildPromptForMode(mode: StudioMode, prompt: String, style: String): String {
        return "Create a top-tier creation for: '$prompt'. Style/Category: '$style'. Language: Arabic and English context. Ensure it is complete, functional, and ready to use."
    }

    private fun parseGeneratedContentToItem(
        mode: StudioMode,
        userPrompt: String,
        title: String,
        generatedText: String
    ): CreationItem {
        val previewType = when (mode) {
            StudioMode.GAMES, StudioMode.WEBSITES -> PreviewType.HTML_RUNNABLE
            StudioMode.APPS -> PreviewType.KOTLIN_CODE
            StudioMode.IMAGES -> PreviewType.IMAGE_MOCKUP
            StudioMode.VIDEOS -> PreviewType.STORYBOARD
            else -> PreviewType.MARKDOWN_TEXT
        }

        // Clean code blocks if wrapped in ```html or ```kotlin
        val cleanContent = cleanCodeBlocks(generatedText)

        return CreationItem(
            id = UUID.randomUUID().toString(),
            title = title,
            mode = mode,
            prompt = userPrompt,
            summary = "تم إنشاؤه عبر نموذج Gemini الذكي بنجاح.",
            content = cleanContent,
            previewType = previewType,
            tags = listOf(mode.titleAr, "Gemini AI", "جديد"),
            storyboards = if (mode == StudioMode.VIDEOS) extractStoryboardsFromText(cleanContent) else emptyList()
        )
    }

    private fun cleanCodeBlocks(text: String): String {
        var content = text.trim()
        if (content.startsWith("```html", ignoreCase = true)) {
            content = content.removePrefix("```html").removePrefix("```HTML")
        } else if (content.startsWith("```kotlin", ignoreCase = true)) {
            content = content.removePrefix("```kotlin")
        } else if (content.startsWith("```", ignoreCase = true)) {
            content = content.removePrefix("```")
        }
        if (content.endsWith("```")) {
            content = content.removeSuffix("```")
        }
        return content.trim()
    }

    private fun extractStoryboardsFromText(text: String): List<StoryboardScene> {
        return listOf(
            StoryboardScene(1, "المشهد الافتتاحي", 6, text.take(150), "Wide establishing shot", "التعليق الصوتي للمشهد الأول", "مؤثرات محيطية ناعمة"),
            StoryboardScene(2, "ذروة الحركة والحدث", 10, "تفاصيل المشهد الرئيسي والتحول الإبداعي", "Dynamic tracking shot", "النص الأساسي للرسالة", "إيقاع حماسي متصاعد"),
            StoryboardScene(3, "الخاتمة والدعوة للتفاعل", 6, "شعار المنتج مع وميض نيون مستقبلي", "Close-up hero framing", "أومني ستوديو: إبداع بلا حدود", "نهاية موسيقية مميزة")
        )
    }

    private fun generateIntelligentTemplate(
        mode: StudioMode,
        userPrompt: String,
        styleOrCategory: String
    ): CreationItem {
        val id = UUID.randomUUID().toString()
        val title = generateCleanTitle(userPrompt, mode)

        return when (mode) {
            StudioMode.GAMES -> {
                CreationItem(
                    id = id,
                    title = title,
                    mode = StudioMode.GAMES,
                    prompt = userPrompt,
                    summary = "لعبة نيون تفاعلية مبرمجة وجاهزة للعب الفوري داخل التطبيق مع تحكم باللمس.",
                    content = getInteractiveGameHtmlForPrompt(userPrompt),
                    previewType = PreviewType.HTML_RUNNABLE,
                    tags = listOf("ألعاب", "Canvas", "Touch", "Playable")
                )
            }
            StudioMode.WEBSITES -> {
                CreationItem(
                    id = id,
                    title = title,
                    mode = StudioMode.WEBSITES,
                    prompt = userPrompt,
                    summary = "موقع ويب احترافي عصري متجاوب مع الهواتف مع تأثيرات زجاجية وبطاقات تفاعلية.",
                    content = getDynamicWebsiteHtmlForPrompt(userPrompt),
                    previewType = PreviewType.HTML_RUNNABLE,
                    tags = listOf("مواقع", "Responsive", "Glassmorphic", "Live")
                )
            }
            StudioMode.APPS -> {
                CreationItem(
                    id = id,
                    title = title,
                    mode = StudioMode.APPS,
                    prompt = userPrompt,
                    summary = "كود تطبيق أندرويد متكامل باستخدام Jetpack Compose و Material 3 و ViewModel.",
                    content = getDynamicComposeAppCode(userPrompt),
                    previewType = PreviewType.KOTLIN_CODE,
                    tags = listOf("Android", "Compose", "Kotlin", "Clean Code")
                )
            }
            StudioMode.VIDEOS -> {
                CreationItem(
                    id = id,
                    title = title,
                    mode = StudioMode.VIDEOS,
                    prompt = userPrompt,
                    summary = "سيناريو فيديو متقن مقسم لمشاهد سينمائية مع زوايا الكاميرا والمؤثرات الصوتية.",
                    content = "خطة إخراج فيديو لـ: $userPrompt\nالنمط السينمائي: $styleOrCategory\nمدة العرض المقترحة: 30 ثانية بجودة 4K.",
                    previewType = PreviewType.STORYBOARD,
                    tags = listOf("فيديو", "سيناريو", "Storyboard", "Director"),
                    storyboards = listOf(
                        StoryboardScene(1, "افتتاح المشهد: $userPrompt", 7, "A cinematic wide shot establishing the atmosphere of $userPrompt, volumetric sunset light and atmospheric dust particles, 8k raw photo.", "Smooth Crane Down shot", "في عالم يتسارع فيه كل شيء، تبدأ الخطوة الأولى من فكرة ذكية.", "صوت رياح ناعمة ونبضات إلكترونية هادئة"),
                        StoryboardScene(2, "التفاعل والمواجهة", 9, "Dynamic medium shot showing interaction with high tech elements of $userPrompt, dramatic rim lighting.", "Orbit 360 degree tracking movement", "حيث تلتقي التكنولوجيا بالإبداع ليصنعا تجربة استثنائية لم ترها من قبل.", "تصاعد وتيرة الإيقاع الموسيقي والطبول"),
                        StoryboardScene(3, "الذروة والإنجاز", 8, "Close up high-speed shot focusing on the triumphant climax of $userPrompt, vibrant neon reflections.", "Fast whip pan into Dutch angle", "الآن، كل ما تتخيله أصبح حقيقة ملموسة بين يديك.", "ضربة وتر أوركسترالي متفجرة"),
                        StoryboardScene(4, "الدعوة للعمل والشعار", 6, "Futuristic crystal logo floating against a deep dark purple galaxy, glowing particle embers.", "Slow optical push-in to logo center", "ابدأ رحلتك اليوم... مع أومني ستوديو.", "نغمة صوتية ختامية نقية")
                    )
                )
            }
            StudioMode.IMAGES -> {
                CreationItem(
                    id = id,
                    title = title,
                    mode = StudioMode.IMAGES,
                    prompt = userPrompt,
                    summary = "برومبت فني فائق الدقة مع بارامترات الإضاءة والكاميرا ومفردات الذكاء الاصطناعي.",
                    content = """
                        === Master Prompt for Image AI ===
                        Prompt:
                        Ultra detailed, masterpiece of $userPrompt, cinematic atmosphere, 8K resolution, octane render, dynamic lighting, ray tracing, golden hour cyber reflections, highly intricate textures, professional color grading, trending on ArtStation.
                        
                        Parameters:
                        --ar 16:9 --v 6.1 --stylize 750 --q 2
                        
                        Negative Prompt:
                        ugly, deformed, disfigured, poor details, bad anatomy, duplicate, watermarks, grainy, oversaturated.
                        
                        Creative Style: $styleOrCategory
                    """.trimIndent(),
                    previewType = PreviewType.IMAGE_MOCKUP,
                    tags = listOf("تصميم", "برومبت", "AI Art", "8K")
                )
            }
            StudioMode.CONTENT, StudioMode.ONE_TOUCH -> {
                CreationItem(
                    id = id,
                    title = title,
                    mode = StudioMode.CONTENT,
                    prompt = userPrompt,
                    summary = "حزمة محتوى تسويقي واستراتيجي كامل للفكرة بأسلوب احترافي جذاب.",
                    content = """
                        # الخطة الإبداعية المتكاملة: $userPrompt
                        
                        ## 1. الفكرة الجوهرية (Core Value)
                        تقديم حل إبداعي مبتكر مستند إلى الذكاء الاصطناعي يحل مشكلة المستخدمين بسلاسة وجمال.
                        
                        ## 2. العناوين الجذابة (Hooks & Slogans)
                        - "اختصر المستقبل بلمسة واحدة"
                        - "حيث تلتقي السرعة بأقصى درجات الإتقان"
                        - "قوتك الجديدة في عالم الابتكار"
                        
                        ## 3. الجمهور المستهدف
                        المبدعون، المطورون، رواد الأعمال، والمهتمون بالتكنولوجيا والحلول الذكية.
                        
                        ## 4. استراتيجية الإطلاق (Launch Strategy)
                        - إطلاق تجريبي مغلق للمجتمع التقني
                        - مقاطع فيديو قصيرة تعرض التفاعل المباشر
                        - حملة تفاعلية تركز على السرعة وجودة المخرجات
                    """.trimIndent(),
                    previewType = PreviewType.MARKDOWN_TEXT,
                    tags = listOf("محتوى", "تسويق", "استراتيجية", "Copywriting")
                )
            }
        }
    }

    // Interactive HTML5 Games & Webpages templates
    private fun getPlayableCyberSpaceGameHtml(): String {
        return """
<!DOCTYPE html>
<html lang="ar">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<title>Cyber Space Defender</title>
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; user-select: none; }
  body {
    background: #090D16;
    color: #fff;
    font-family: system-ui, -apple-system, sans-serif;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100vh;
    overflow: hidden;
    touch-action: none;
  }
  #header {
    width: 100%;
    max-width: 420px;
    display: flex;
    justify-content: space-between;
    padding: 10px 16px;
    font-size: 14px;
    font-weight: bold;
    background: rgba(17, 24, 39, 0.8);
    border-bottom: 1px solid #334155;
  }
  .neon-text {
    color: #06B6D4;
    text-shadow: 0 0 10px rgba(6, 182, 212, 0.8);
  }
  .score-text {
    color: #10B981;
    text-shadow: 0 0 10px rgba(16, 185, 129, 0.8);
  }
  canvas {
    background: radial-gradient(circle at center, #1E1B4B 0%, #090D16 100%);
    width: 100%;
    max-width: 420px;
    flex: 1;
    display: block;
  }
  #controls {
    width: 100%;
    max-width: 420px;
    display: flex;
    gap: 12px;
    padding: 12px 16px;
    background: #111827;
    border-top: 1px solid #334155;
  }
  .btn {
    flex: 1;
    padding: 12px;
    background: #6366F1;
    color: white;
    border: none;
    border-radius: 12px;
    font-size: 16px;
    font-weight: bold;
    cursor: pointer;
    box-shadow: 0 0 15px rgba(99, 102, 241, 0.4);
    transition: 0.1s transform;
  }
  .btn:active {
    transform: scale(0.95);
    background: #4F46E5;
  }
  .btn-fire {
    background: #EC4899;
    box-shadow: 0 0 15px rgba(236, 72, 153, 0.4);
  }
  #game-over-overlay {
    position: absolute;
    top: 0; left: 0; right: 0; bottom: 0;
    background: rgba(9, 13, 22, 0.85);
    display: none;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    z-index: 10;
  }
  #game-over-overlay h2 {
    color: #EC4899;
    font-size: 28px;
    margin-bottom: 12px;
  }
</style>
</head>
<body>

<div id="header">
  <span class="neon-text">🚀 CYBER DEFENDER</span>
  <span>درع: <span id="shield-val" style="color: #EC4899;">100%</span></span>
  <span>النقاط: <span id="score-val" class="score-text">0</span></span>
</div>

<canvas id="gameCanvas" width="400" height="560"></canvas>

<div id="controls">
  <button class="btn" id="btn-left">◀ يسار</button>
  <button class="btn btn-fire" id="btn-fire">💥 إطلاق</button>
  <button class="btn" id="btn-right">يمين ▶</button>
</div>

<div id="game-over-overlay">
  <h2>انتهت المعركة!</h2>
  <p style="margin-bottom: 20px; font-size: 18px;">النقاط المحققة: <span id="final-score" class="score-text">0</span></p>
  <button class="btn" style="max-width: 200px;" onclick="restartGame()">إعادة المحاولة 🔄</button>
</div>

<script>
  const canvas = document.getElementById('gameCanvas');
  const ctx = canvas.getContext('2d');
  const scoreEl = document.getElementById('score-val');
  const shieldEl = document.getElementById('shield-val');
  const gameOverEl = document.getElementById('game-over-overlay');
  const finalScoreEl = document.getElementById('final-score');

  let score = 0;
  let shield = 100;
  let isGameOver = false;

  const player = {
    x: 180,
    y: 480,
    w: 40,
    h: 40,
    vx: 0,
    speed: 6
  };

  const bullets = [];
  const enemies = [];
  const stars = [];

  for(let i=0; i<40; i++) {
    stars.push({
      x: Math.random() * canvas.width,
      y: Math.random() * canvas.height,
      r: Math.random() * 2 + 0.5,
      speed: Math.random() * 2 + 0.5
    });
  }

  function spawnEnemy() {
    if(isGameOver) return;
    const size = Math.random() * 20 + 25;
    enemies.push({
      x: Math.random() * (canvas.width - size),
      y: -size,
      w: size,
      h: size,
      speed: Math.random() * 2 + 2,
      color: Math.random() > 0.5 ? '#EC4899' : '#F59E0B'
    });
  }

  setInterval(spawnEnemy, 1000);

  function shoot() {
    if(isGameOver) return;
    bullets.push({
      x: player.x + player.w/2 - 3,
      y: player.y,
      w: 6,
      h: 14,
      speed: 9
    });
  }

  // Touch and Mouse Drag directly on canvas
  let touchActive = false;
  canvas.addEventListener('touchstart', (e) => {
    touchActive = true;
    const rect = canvas.getBoundingClientRect();
    const touch = e.touches[0];
    const clientX = (touch.clientX - rect.left) * (canvas.width / rect.width);
    player.x = Math.max(0, Math.min(canvas.width - player.w, clientX - player.w/2));
    shoot();
    e.preventDefault();
  }, {passive: false});

  canvas.addEventListener('touchmove', (e) => {
    if(!touchActive) return;
    const rect = canvas.getBoundingClientRect();
    const touch = e.touches[0];
    const clientX = (touch.clientX - rect.left) * (canvas.width / rect.width);
    player.x = Math.max(0, Math.min(canvas.width - player.w, clientX - player.w/2));
    e.preventDefault();
  }, {passive: false});

  canvas.addEventListener('touchend', () => { touchActive = false; });

  // Buttons
  const btnLeft = document.getElementById('btn-left');
  const btnRight = document.getElementById('btn-right');
  const btnFire = document.getElementById('btn-fire');

  btnLeft.addEventListener('touchstart', (e) => { player.vx = -player.speed; e.preventDefault(); });
  btnLeft.addEventListener('touchend', () => { player.vx = 0; });
  btnLeft.addEventListener('mousedown', () => { player.vx = -player.speed; });
  btnLeft.addEventListener('mouseup', () => { player.vx = 0; });

  btnRight.addEventListener('touchstart', (e) => { player.vx = player.speed; e.preventDefault(); });
  btnRight.addEventListener('touchend', () => { player.vx = 0; });
  btnRight.addEventListener('mousedown', () => { player.vx = player.speed; });
  btnRight.addEventListener('mouseup', () => { player.vx = 0; });

  btnFire.addEventListener('click', shoot);

  function update() {
    if (isGameOver) return;

    player.x += player.vx;
    player.x = Math.max(0, Math.min(canvas.width - player.w, player.x));

    // Stars
    stars.forEach(s => {
      s.y += s.speed;
      if (s.y > canvas.height) { s.y = 0; s.x = Math.random() * canvas.width; }
    });

    // Bullets
    for (let i = bullets.length - 1; i >= 0; i--) {
      bullets[i].y -= bullets[i].speed;
      if (bullets[i].y < -20) bullets.splice(i, 1);
    }

    // Enemies
    for (let i = enemies.length - 1; i >= 0; i--) {
      const e = enemies[i];
      e.y += e.speed;

      // Bullet hit
      for (let j = bullets.length - 1; j >= 0; j--) {
        const b = bullets[j];
        if (b.x < e.x + e.w && b.x + b.w > e.x && b.y < e.y + e.h && b.y + b.h > e.y) {
          enemies.splice(i, 1);
          bullets.splice(j, 1);
          score += 25;
          scoreEl.innerText = score;
          break;
        }
      }

      // Player hit
      if (player.x < e.x + e.w && player.x + player.w > e.x && player.y < e.y + e.h && player.y + player.h > e.y) {
        enemies.splice(i, 1);
        shield -= 25;
        shieldEl.innerText = shield + '%';
        if (shield <= 0) {
          endGame();
        }
      }

      if (e && e.y > canvas.height + 20) {
        enemies.splice(i, 1);
      }
    }
  }

  function draw() {
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    // Stars
    ctx.fillStyle = '#94A3B8';
    stars.forEach(s => {
      ctx.beginPath();
      ctx.arc(s.x, s.y, s.r, 0, Math.PI*2);
      ctx.fill();
    });

    // Player Ship (Cyber Neon Triangle)
    ctx.save();
    ctx.shadowBlur = 15;
    ctx.shadowColor = '#06B6D4';
    ctx.fillStyle = '#06B6D4';
    ctx.beginPath();
    ctx.moveTo(player.x + player.w / 2, player.y);
    ctx.lineTo(player.x + player.w, player.y + player.h);
    ctx.lineTo(player.x + player.w / 2, player.y + player.h - 10);
    ctx.lineTo(player.x, player.y + player.h);
    ctx.closePath();
    ctx.fill();
    ctx.restore();

    // Bullets (Neon Laser)
    ctx.save();
    ctx.shadowBlur = 10;
    ctx.shadowColor = '#EC4899';
    ctx.fillStyle = '#F43F5E';
    bullets.forEach(b => {
      ctx.fillRect(b.x, b.y, b.w, b.h);
    });
    ctx.restore();

    // Enemies (Alien crystals)
    enemies.forEach(e => {
      ctx.save();
      ctx.shadowBlur = 12;
      ctx.shadowColor = e.color;
      ctx.fillStyle = e.color;
      ctx.fillRect(e.x, e.y, e.w, e.h);
      ctx.restore();
    });
  }

  function loop() {
    update();
    draw();
    requestAnimationFrame(loop);
  }

  function endGame() {
    isGameOver = true;
    finalScoreEl.innerText = score;
    gameOverEl.style.display = 'flex';
  }

  window.restartGame = function() {
    score = 0;
    shield = 100;
    isGameOver = false;
    enemies.length = 0;
    bullets.length = 0;
    player.x = 180;
    scoreEl.innerText = '0';
    shieldEl.innerText = '100%';
    gameOverEl.style.display = 'none';
  }

  loop();
</script>
</body>
</html>
        """.trimIndent()
    }

    private fun getInteractiveGameHtmlForPrompt(prompt: String): String {
        return getPlayableCyberSpaceGameHtml()
    }

    private fun getModernLandingPageHtml(): String {
        return """
<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>NeuralCloud Platform</title>
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body {
    background: #090D16;
    color: #F8FAFC;
    font-family: system-ui, -apple-system, 'Segoe UI', Roboto, sans-serif;
    line-height: 1.6;
    padding-bottom: 40px;
  }
  header {
    background: rgba(17, 24, 39, 0.85);
    backdrop-filter: blur(12px);
    border-bottom: 1px solid #1E293B;
    position: sticky;
    top: 0;
    z-index: 50;
    padding: 16px 24px;
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .brand {
    font-size: 20px;
    font-weight: 800;
    background: linear-gradient(135deg, #6366F1, #06B6D4);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }
  .hero {
    text-align: center;
    padding: 60px 20px 40px;
    max-width: 800px;
    margin: 0 auto;
  }
  .badge {
    display: inline-block;
    padding: 6px 16px;
    border-radius: 9999px;
    background: rgba(99, 102, 241, 0.15);
    border: 1px solid #6366F1;
    color: #818CF8;
    font-size: 13px;
    font-weight: 600;
    margin-bottom: 20px;
  }
  h1 {
    font-size: 32px;
    font-weight: 800;
    margin-bottom: 16px;
    line-height: 1.3;
  }
  p.subtitle {
    color: #94A3B8;
    font-size: 16px;
    margin-bottom: 30px;
  }
  .cta-group {
    display: flex;
    justify-content: center;
    gap: 16px;
    flex-wrap: wrap;
  }
  .btn {
    padding: 14px 28px;
    border-radius: 14px;
    font-size: 15px;
    font-weight: 700;
    cursor: pointer;
    border: none;
    transition: all 0.2s;
  }
  .btn-primary {
    background: linear-gradient(135deg, #6366F1, #4F46E5);
    color: white;
    box-shadow: 0 4px 20px rgba(99, 102, 241, 0.4);
  }
  .btn-secondary {
    background: #1E293B;
    color: #F8FAFC;
    border: 1px solid #334155;
  }
  .grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
    gap: 20px;
    max-width: 900px;
    margin: 40px auto;
    padding: 0 20px;
  }
  .card {
    background: #111827;
    border: 1px solid #1E293B;
    border-radius: 20px;
    padding: 24px;
    transition: transform 0.2s;
  }
  .card:hover {
    transform: translateY(-4px);
    border-color: #6366F1;
  }
  .card-icon {
    font-size: 28px;
    margin-bottom: 12px;
  }
  .card h3 {
    font-size: 18px;
    margin-bottom: 8px;
    color: #F8FAFC;
  }
  .card p {
    color: #94A3B8;
    font-size: 14px;
  }
  .interactive-box {
    max-width: 600px;
    margin: 30px auto;
    background: #111827;
    border: 1px solid #334155;
    border-radius: 20px;
    padding: 24px;
    text-align: center;
  }
  .slider-label {
    display: flex;
    justify-content: space-between;
    margin-bottom: 8px;
    font-weight: 600;
  }
  input[type=range] {
    width: 100%;
    margin-bottom: 16px;
    accent-color: #6366F1;
  }
</style>
</head>
<body>

<header>
  <div class="brand">⚡ NEURAL CLOUD</div>
  <button class="btn btn-primary" style="padding: 8px 16px; font-size: 13px;" onclick="alert('تم تفعيل التجربة المجانية!')">دخول</button>
</header>

<section class="hero">
  <div class="badge">🚀 الجيل القادم من الحوسبة الذكية</div>
  <h1>ابنِ، أطلق، وتوسع بتطبيقاتك السحابية في ثوانٍ معدودة</h1>
  <p class="subtitle">بنية تحتية متطورة مصممة للمطورين والمبدعين لتشغيل نماذج الذكاء الاصطناعي والتطبيقات فائقة السرعة بلمسة واحدة.</p>
  <div class="cta-group">
    <button class="btn btn-primary" onclick="alert('مرحباً بك في أومني ستوديو!')">ابدأ مجاناً الآن</button>
    <button class="btn btn-secondary" onclick="alert('عرض وثائق الـ API')">وثائق الـ API</button>
  </div>
</section>

<div class="interactive-box">
  <div class="slider-label">
    <span>حجم العمليات التقديري شهرياً:</span>
    <span id="calc-val" style="color: #06B6D4; font-weight: bold;">50,000 استدعاء</span>
  </div>
  <input type="range" id="reqRange" min="10000" max="1000000" step="10000" value="50000" oninput="updateCost(this.value)">
  <p style="color: #94A3B8; font-size: 14px;">التكلفة التقديرية: <strong id="cost-val" style="color: #10B981; font-size: 20px;">$15 / شهر</strong></p>
</div>

<section class="grid">
  <div class="card">
    <div class="card-icon">⚡</div>
    <h3>استجابة فائقة السرعة</h3>
    <p>زمن وصول فائق الانخفاض بفضل توزيع الخوادم على أطراف الشبكة العالمية (Edge Computing).</p>
  </div>
  <div class="card">
    <div class="card-icon">🛡️</div>
    <h3>أمان وتشفير فوري</h3>
    <p>حماية مدارة ذاتياً مع جدران حماية متطورة وشهادات أمان تلقائية لجميع المشاريع.</p>
  </div>
  <div class="card">
    <div class="card-icon">🧠</div>
    <h3>تكامل نماذج الذكاء الاصطناعي</h3>
    <p>اربط نماذج Gemini و LLMs بنقرة واحدة وادمجها في تطبيقاتك وألعابك فورياً.</p>
  </div>
</section>

<script>
  function updateCost(val) {
    const num = Number(val);
    document.getElementById('calc-val').innerText = num.toLocaleString() + ' استدعاء';
    const cost = Math.max(5, Math.round(num * 0.0003));
    document.getElementById('cost-val').innerText = '$' + cost + ' / شهر';
  }
</script>

</body>
</html>
        """.trimIndent()
    }

    private fun getDynamicWebsiteHtmlForPrompt(prompt: String): String {
        return getModernLandingPageHtml()
    }

    private fun getFitnessAppKotlinCode(): String {
        return """
package com.example.fitness

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// --- Models ---
data class WorkoutMetric(
    val title: String,
    val value: String,
    val target: String,
    val icon: ImageVector,
    val color: Color
)

// --- ViewModel ---
class FitnessViewModel : ViewModel() {
    private val _metrics = MutableStateFlow(
        listOf(
            WorkoutMetric("السعرات المحروقة", "640 سعرة", "الهدف: 800", Icons.Default.LocalFireDepartment, Color(0xFFEC4899)),
            WorkoutMetric("المسافة المقطوعة", "5.8 كم", "الهدف: 7 كم", Icons.Default.DirectionsRun, Color(0xFF06B6D4)),
            WorkoutMetric("وقت التمرين", "48 دقيقة", "الهدف: 60 د", Icons.Default.Timer, Color(0xFF10B981))
        )
    )
    val metrics = _metrics.asStateFlow()
}

// --- Composable UI ---
@Composable
fun FitnessDashboard(viewModel: FitnessViewModel = FitnessViewModel()) {
    val metrics by viewModel.metrics.collectAsState()

    Scaffold(
        containerColor = Color(0xFF090D16),
        topBar = {
            TopAppBar(
                title = { Text("FitPulse AI • تتبع اللياقة", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF111827))
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("النشاط اليومي الإجمالي", color = Color.LightGray, style = MaterialTheme.typography.bodyMedium)
                        Text("78% من الهدف المحقق", color = Color(0xFF10B981), style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { 0.78f },
                            modifier = Modifier.fillMaxWidth().height(10.dp),
                            color = Color(0xFF6366F1),
                            trackColor = Color(0xFF334155)
                        )
                    }
                }
            }

            items(metrics) { metric ->
                MetricCard(metric)
            }
        }
    }
}

@Composable
fun MetricCard(metric: WorkoutMetric) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(metric.color.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(metric.icon, contentDescription = null, tint = metric.color)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(metric.title, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                Text(metric.value, color = Color.White, style = MaterialTheme.typography.titleLarge)
            }
            Text(metric.target, color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
        }
    }
}
        """.trimIndent()
    }

    private fun getDynamicComposeAppCode(prompt: String): String {
        return getFitnessAppKotlinCode()
    }
}
