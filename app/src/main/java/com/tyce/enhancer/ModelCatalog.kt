package com.tyce.enhancer

import android.content.Context

/** One selectable enhancement engine. */
data class EngineModel(
    val id: String,
    val label: String,
    val desc: String,
    val kind: Kind,
    val scale: Int,
    val prepadding: Int = 10,
    val assetParam: String? = null,
    val assetBin: String? = null,
    /** Set for models the user imported: absolute file paths instead of assets. */
    val userParam: String? = null,
    val userBin: String? = null,
) {
    enum class Kind { ONNX_BUILTIN, ONNX_BEST, NCNN }
    val isNcnn get() = kind == Kind.NCNN
    val isUser get() = userParam != null
}

/** Catalog of local models. Sizes are read from the real files, never hard-coded. */
object ModelCatalog {
    const val ID_HD = "onnx_hd"
    const val ID_BEST = "onnx_best"

    private val ncnn = listOf(
        EngineModel("av3_x2", "AnimeVideo v3 ×2 (fast)", "2× upscale · fast", EngineModel.Kind.NCNN, 2, 10,
            "models/realesr-animevideov3-x2.param", "models/realesr-animevideov3-x2.bin"),
        EngineModel("av3_x3", "AnimeVideo v3 ×3 (fast)", "3× upscale · fast", EngineModel.Kind.NCNN, 3, 10,
            "models/realesr-animevideov3-x3.param", "models/realesr-animevideov3-x3.bin"),
        EngineModel("av3_x4", "AnimeVideo v3 ×4 (fast)", "4× upscale · fast", EngineModel.Kind.NCNN, 4, 10,
            "models/realesr-animevideov3-x4.param", "models/realesr-animevideov3-x4.bin"),
        EngineModel("x4_anime", "Real-ESRGAN ×4 Anime", "4× upscale · large · illustrations", EngineModel.Kind.NCNN, 4, 10,
            "models/realesrgan-x4plus-anime.param", "models/realesrgan-x4plus-anime.bin"),
        EngineModel("x4_photo", "Real-ESRGAN ×4 Photo", "4× upscale · large · real photos", EngineModel.Kind.NCNN, 4, 10,
            "models/realesrgan-x4plus.param", "models/realesrgan-x4plus.bin"),
    )

    /** Models that can run right now on this device. */
    fun available(context: Context): List<EngineModel> {
        val out = ArrayList<EngineModel>()
        if (Enhancer.hasModel(context)) {
            out += EngineModel(ID_HD, "HD model (general)", "ONNX · built in, balanced", EngineModel.Kind.ONNX_BUILTIN, 4)
        }
        if (Enhancer.hasBest(context)) {
            out += EngineModel(ID_BEST, "Best model (x4plus, your file)", "ONNX · sharper but slower", EngineModel.Kind.ONNX_BEST, 4)
        }
        if (Engine_available()) { out += ncnn; out += UserNcnn.list(context).map { it.toEngine() } }
        return out
    }

    private fun Engine_available() = com.kaminari.imagine.Engine.nativeAvailable

    fun byId(context: Context, id: String?): EngineModel? = available(context).firstOrNull { it.id == id }

    /** Real on-disk/asset size in MB, or null if unknown. */
    fun sizeMb(context: Context, m: EngineModel): Double? = try {
        when (m.kind) {
            EngineModel.Kind.NCNN -> if (m.isUser) {
                (java.io.File(m.userParam!!).length() + java.io.File(m.userBin!!).length()) / 1048576.0
            } else {
                fun len(p: String) = context.assets.openFd(p).use { it.length }
                (len(m.assetParam!!) + len(m.assetBin!!)) / 1048576.0
            }
            EngineModel.Kind.ONNX_BEST -> Enhancer.bestModelFile(context).length() / 1048576.0
            EngineModel.Kind.ONNX_BUILTIN -> {
                val u = Enhancer.userModelFile(context)
                if (u.exists() && u.length() > 0) u.length() / 1048576.0
                else context.assets.openFd("realesrgan.onnx").use { it.length } / 1048576.0
            }
        }
    } catch (_: Throwable) { null }
}
