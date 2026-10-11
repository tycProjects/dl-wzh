package dev.tajim.jarvis.ui.navigation

object Routes {
    const val HOME = "home"
    const val CHAT = "chat"
    const val SETTINGS = "settings"
    const val UPDATES = "updates"
    const val ABOUT = "about"
    const val MEMORY = "memory"
    const val S_HANDSFREE = "settings/handsfree"
    const val S_LANGUAGE = "settings/language"
    const val S_PERSONA = "settings/persona"
    const val S_AI = "settings/ai"
    const val S_WEATHER = "settings/weather"
    const val S_PERMISSIONS = "settings/permissions"
    const val S_APPEARANCE = "settings/appearance"
    const val WORKSPACE_ARG = "id"
    const val WORKSPACE = "workspace/{id}"
    fun workspace(w: Workspace) = "workspace/${w.key}"

    /** Routes that show the bottom bar. Everything else (sub-pages, About, workspaces) is full screen. */
    val tabs = listOf(HOME, CHAT, SETTINGS, UPDATES)
}
