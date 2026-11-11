package com.logalyze.app.ui

data class AnalysisResult(
    val errorType: String,
    val summary: String,
    val confidence: Int,
    val rootCause: String,
    val evidence: List<String>,
    val fixes: List<String>,
    val launcher: String? = null,
    val deviceInfo: List<Pair<String, String>> = emptyList(),
    val mods: List<String> = emptyList()
)

// Sample data for now. Real results will come from the backend in a later step.
object SampleData {
    val outOfMemory = AnalysisResult(
        errorType = "OutOfMemoryError",
        summary = "The game ran out of available Java heap memory.",
        confidence = 92,
        rootCause = "Not enough memory is allocated to Java, or the modpack needs more RAM than the launcher gives it. Heavy mods and high render distance make it worse.",
        evidence = listOf(
            "Exception in thread \"Render thread\" java.lang.OutOfMemoryError: Java heap space",
            "Allocated memory: 1024 MB, Max memory: 1024 MB",
            "Loaded mods: 87"
        ),
        fixes = listOf(
            "Increase the Java heap size in the launcher (try 4–6 GB)",
            "Remove or replace memory-heavy mods",
            "Lower the render distance",
            "Use a 64-bit Java version if possible"
        )
    )
}

data class HistoryItem(
    val id: Long,
    val fileName: String,
    val result: AnalysisResult,
    val createdAt: Long,
    val resolved: Boolean = false
)
