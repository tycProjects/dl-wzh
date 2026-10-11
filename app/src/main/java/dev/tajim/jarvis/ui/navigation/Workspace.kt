package dev.tajim.jarvis.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PermMedia
import androidx.compose.material.icons.outlined.ViewInAr
import androidx.compose.ui.graphics.vector.ImageVector
import dev.tajim.jarvis.R

/** Every full-screen workspace. Primary ones are the three fan blades; the rest live in the launcher rails. */
enum class Workspace(
    val key: String,
    @StringRes val title: Int,
    @StringRes val subtitle: Int,
    val icon: ImageVector,
) {
    STUDIO_3D("studio3d", R.string.ws_3d_studio, R.string.ws_3d_studio_sub, Icons.Outlined.ViewInAr),
    TEXT_LAB("textlab", R.string.ws_text_lab, R.string.ws_text_lab_sub, Icons.Outlined.EditNote),
    DRAW_LAB("drawlab", R.string.ws_draw_lab, R.string.ws_draw_lab_sub, Icons.Outlined.Brush),
    WEB_LAB("weblab", R.string.ws_web_lab, R.string.ws_web_lab_sub, Icons.Outlined.Language),
    CODE_LAB("codelab", R.string.ws_code_lab, R.string.ws_code_lab_sub, Icons.Outlined.Code),
    FILE_LAB("filelab", R.string.ws_file_lab, R.string.ws_file_lab_sub, Icons.Outlined.FolderOpen),
    MEDIA_LAB("medialab", R.string.ws_media_lab, R.string.ws_media_lab_sub, Icons.Outlined.PermMedia),
    AI_LAB("ailab", R.string.ws_ai_lab, R.string.ws_ai_lab_sub, Icons.Outlined.AutoAwesome),
    TOOLS("tools", R.string.ws_tools, R.string.ws_tools_sub, Icons.Outlined.Build),
    SYSTEM("system", R.string.ws_system, R.string.ws_system_sub, Icons.Outlined.Memory),
    MORE("more", R.string.ws_more, R.string.ws_more_sub, Icons.Outlined.MoreHoriz);

    companion object {
        fun fromKey(key: String?): Workspace? = entries.firstOrNull { it.key == key }
    }
}
