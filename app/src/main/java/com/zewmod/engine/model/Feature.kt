package com.zewmod.engine.model

/** Feature IDs — phải khớp với key trong config JSON */
object FeatureId {
    const val TRACK       = "track"
    const val HEAD        = "head"
    const val LEGIT       = "legit"
    const val FREEZE      = "freeze"
    const val SCOPE       = "scope"
    const val AIM_ASSIST  = "aim_assist"
    const val AIM_LAG     = "aim_lag"
    const val CIRCLE      = "circle"
    const val MOCO_ESP    = "moco_esp"
    const val HIDE_MENU   = "hide_menu"
}

enum class FeatureCategory {
    COMBAT,     // Tấn công
    VISION,     // Hiển thị
    MOVEMENT,   // Di chuyển
    UTILITY     // Tiện ích
}

data class Feature(
    val id: String,
    val nameVi: String,
    val descVi: String,
    val category: FeatureCategory,
    val hasSlider: Boolean = false,
    val sliderMin: Float = 0f,
    val sliderMax: Float = 1f,
    val sliderDefault: Float = 0.5f,
    val sliderLabel: String = "",
    val requiresRoot: Boolean = false,
    var enabled: Boolean = false,
    var sliderValue: Float = 0.5f
)

object ZewModFeatures {

    private val NAME_TO_ID: Map<String, String> = mapOf(
        "Track"       to FeatureId.TRACK,
        "Head"        to FeatureId.HEAD,
        "Legit"       to FeatureId.LEGIT,
        "Freeze"      to FeatureId.FREEZE,
        "Scope"       to FeatureId.SCOPE,
        "AimAssist"   to FeatureId.AIM_ASSIST,
        "AimLag"      to FeatureId.AIM_LAG,
        "Circle"      to FeatureId.CIRCLE,
        "MocoEsp"     to FeatureId.MOCO_ESP,
        "HideMenu"    to FeatureId.HIDE_MENU
    )

    val ALL: List<Feature> = listOf(
        Feature(
            id = FeatureId.TRACK,
            nameVi = "THEO DÕI",
            descVi = "Tự động theo dõi và khóa mục tiêu kẻ địch",
            category = FeatureCategory.COMBAT,
            hasSlider = true,
            sliderMin = 1f,
            sliderMax = 10f,
            sliderDefault = 5f,
            sliderLabel = "Mục tiêu tối đa"
        ),
        Feature(
            id = FeatureId.HEAD,
            nameVi = "ĐẦU",
            descVi = "Tăng tỉ lệ bắn vào đầu kẻ địch",
            category = FeatureCategory.COMBAT,
            hasSlider = true,
            sliderMin = 0f,
            sliderMax = 1f,
            sliderDefault = 0.8f,
            sliderLabel = "Tỉ lệ headshot"
        ),
        Feature(
            id = FeatureId.LEGIT,
            nameVi = "LEGIT",
            descVi = "Chế độ hợp lệ - ít bị phát hiện hơn",
            category = FeatureCategory.COMBAT
        ),
        Feature(
            id = FeatureId.FREEZE,
            nameVi = "ĐÓNG BĂNG",
            descVi = "Đóng băng vị trí kẻ địch trong thời gian ngắn",
            category = FeatureCategory.COMBAT,
            hasSlider = true,
            sliderMin = 0.1f,
            sliderMax = 2f,
            sliderDefault = 0.5f,
            sliderLabel = "Thời gian (s)"
        ),
        Feature(
            id = FeatureId.SCOPE,
            nameVi = "NGẮM",
            descVi = "Tự động ngắm chính xác khi bắn",
            category = FeatureCategory.COMBAT
        ),
        Feature(
            id = FeatureId.AIM_ASSIST,
            nameVi = "HỖ TRỢ NGẮM",
            descVi = "Hỗ trợ ngắm mục tiêu tự động",
            category = FeatureCategory.COMBAT,
            hasSlider = true,
            sliderMin = 0f,
            sliderMax = 1f,
            sliderDefault = 0.6f,
            sliderLabel = "Độ mạnh"
        ),
        Feature(
            id = FeatureId.AIM_LAG,
            nameVi = "AIM LAG / GHOST",
            descVi = "Hiệu ứng lag ngắm - khó bị phát hiện",
            category = FeatureCategory.COMBAT
        ),
        Feature(
            id = FeatureId.CIRCLE,
            nameVi = "VÒNG TRÒN",
            descVi = "Hiển thị vòng tròn ESP xung quanh kẻ địch",
            category = FeatureCategory.VISION
        ),
        Feature(
            id = FeatureId.MOCO_ESP,
            nameVi = "MOCO (MARK)",
            descVi = "Hiển thị vị trí kẻ địch trên bản đồ thu nhỏ",
            category = FeatureCategory.VISION
        ),
        Feature(
            id = FeatureId.HIDE_MENU,
            nameVi = "ẨN MENU",
            descVi = "Ẩn menu nổi khi trong trận đấu",
            category = FeatureCategory.UTILITY
        )
    )

    fun getById(id: String): Feature? = ALL.firstOrNull { it.id == id }

    fun getByName(name: String): Feature? = NAME_TO_ID[name]?.let { getById(it) }

    fun getByCategory(cat: FeatureCategory): List<Feature> =
        ALL.filter { it.category == cat }

    fun applyEnabledFromConfig(features: String) {
        val enabled = features.split(",").map { it.trim() }.toSet()
        ALL.forEach { f -> f.enabled = f.id in enabled }
    }

    fun buildFeaturesString(): String =
        ALL.filter { it.enabled }.joinToString(",") { it.id }
}
