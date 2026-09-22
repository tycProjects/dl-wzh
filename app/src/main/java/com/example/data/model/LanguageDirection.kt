package com.example.data.model

enum class LanguageDirection(
    val sourceLanguage: String,
    val targetLanguage: String,
    val label: String
) {
    BISAYA_TO_SUBANEN(
        sourceLanguage = "Bisaya",
        targetLanguage = "Subanen",
        label = "Bisaya → Subanen"
    ),
    SUBANEN_TO_BISAYA(
        sourceLanguage = "Subanen",
        targetLanguage = "Bisaya",
        label = "Subanen → Bisaya"
    );

    fun swapped(): LanguageDirection {
        return if (this == BISAYA_TO_SUBANEN) SUBANEN_TO_BISAYA else BISAYA_TO_SUBANEN
    }
}
