package com.android.island.ai

/**
 * Picks the text field a "type" action should fill: the message box, not a search bar.
 * Chat composers sit at the bottom of the screen, so the lowest usable field wins. Pure logic, unit tested.
 */
object InputPicker {

    data class Field(val top: Int, val hint: String, val focused: Boolean, val password: Boolean = false)

    private val SEARCH_WORDS = listOf("cari", "search", "telusuri")

    /** Index of the field to type into, or null when only search or password fields are on screen. */
    fun pick(fields: List<Field>): Int? {
        val usable = fields.withIndex().filter { (_, f) ->
            !f.password && SEARCH_WORDS.none { f.hint.contains(it, ignoreCase = true) }
        }
        if (usable.isEmpty()) return null
        return usable.maxByOrNull { it.value.top }?.index
    }
}
