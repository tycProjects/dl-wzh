package com.android.island.ai

/**
 * Numbered, tappable elements of the latest observed screen. The model picks one by index or by
 * coordinates, so icon-only controls (magnifier, send arrow, keyboard action key) are reachable
 * even though they carry no visible text. Pure logic, no Android types, so it is unit tested.
 */
object ScreenElements {
    const val MAX_ELEMENTS = 60

    data class Element(
        val index: Int,
        val label: String,
        val editable: Boolean,
        val focused: Boolean,
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int
    ) {
        val centerX: Int get() = (left + right) / 2
        val centerY: Int get() = (top + bottom) / 2
        val area: Long get() = (right - left).toLong() * (bottom - top).toLong()
    }

    fun format(elements: List<Element>): String = elements.joinToString("\n") { e ->
        val kind = when {
            e.editable && e.focused -> "kolom teks, sedang fokus"
            e.editable -> "kolom teks"
            else -> "bisa diketuk"
        }
        "[${e.index}] ${e.label.take(60)} ($kind, tengah ${e.centerX},${e.centerY})"
    }

    fun byIndex(elements: List<Element>, index: Int): Element? = elements.firstOrNull { it.index == index }

    /** Smallest element containing the point, so a tap's label can be checked against ClickGuard. */
    fun labelAt(elements: List<Element>, x: Int, y: Int): String? = elements
        .filter { x in it.left..it.right && y in it.top..it.bottom }
        .minByOrNull { it.area }
        ?.label
}
