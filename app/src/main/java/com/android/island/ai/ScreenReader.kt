package com.android.island.ai

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.view.Display
import android.os.Build
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import java.util.concurrent.Executor
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo

/**
 * On-demand screen reading for the AI assistant.
 *
 * Nothing here runs continuously: [onWindowStateChanged] only caches the last
 * foreground package/activity class (a few strings, zero parsing), and [capture]
 * walks the accessibility tree ONLY at the moment the user explicitly asks the AI
 * to analyze the screen. The result gives the model the real on-screen situation:
 * which app is open, its window title, and the text the user can actually see.
 */
object ScreenReader {

    data class ScreenContext(
        val appLabel: String,
        val packageName: String,
        val activityClass: String,
        val windowTitle: String,
        val visibleText: String,
        val nodeCount: Int,
        /** Numbered tappable elements, see [ScreenElements.format]. Empty when none were found. */
        val elements: String = "",
        /** Deterministic hint from [ScreenStateAnalyzer]: is search reachable here, or must the agent go back first. */
        val navigationHint: String = ""
    ) {
        fun toPromptBlock(): String = buildString {
            append("[Konteks layar pengguna saat ini]\n")
            append("Aplikasi aktif: $appLabel ($packageName)\n")
            if (activityClass.isNotBlank()) append("Activity: $activityClass\n")
            if (windowTitle.isNotBlank()) append("Judul jendela: $windowTitle\n")
            if (visibleText.isNotBlank()) {
                append("Teks yang terlihat di layar (urutan dari atas):\n")
                append(visibleText)
            }
            if (elements.isNotBlank()) {
                append("\n\nElemen interaktif (pakai click_index dengan nomornya, atau tap dengan x,y tengahnya):\n")
                append(elements)
            }
            if (navigationHint.isNotBlank()) append("\n\n").append(navigationHint)
        }
    }

    /** Result of a capture attempt: a context, or an Indonesian hint why it failed. */
    data class CaptureResult(val context: ScreenContext?, val hint: String?)

    // ---- foreground cache (updated from the service's window-state events) ----

    @Volatile private var lastPackage: String = ""
    @Volatile private var lastActivityClass: String = ""
    @Volatile private var lastAtMs: Long = 0L
    /** Tappable elements of the latest capture; click_index and tap resolve against this snapshot. */
    @Volatile private var lastElements: List<ScreenElements.Element> = emptyList()

    fun elementByIndex(index: Int): ScreenElements.Element? = ScreenElements.byIndex(lastElements, index)
    fun labelAt(x: Int, y: Int): String? = ScreenElements.labelAt(lastElements, x, y)

    /** Package of the last app window seen by the accessibility service. */
    fun currentPackage(): String = lastPackage

    /** Cheap: called for every window transition of every app, stores three fields only. */
    fun onWindowStateChanged(packageName: CharSequence?, className: CharSequence?) {
        val pkg = packageName?.toString() ?: return
        if (pkg == "com.android.island") return
        lastPackage = pkg
        lastActivityClass = className?.toString().orEmpty()
        lastAtMs = android.os.SystemClock.elapsedRealtime()
    }

    /**
     * Read the CURRENT screen. Must be called from the accessibility service (any thread).
     * Returns [CaptureResult] with a failed-state hint in Indonesian when reading is
     * impossible (permission revoked after an update, no active window, ...).
     */
    fun capture(service: AccessibilityService?, appContext: Context): CaptureResult {
        if (service == null) return CaptureResult(null, "Accessibility Android Island tidak aktif.")
        val info: AccessibilityServiceInfo? = service.serviceInfo
        if (info != null && !info.canRetrieveWindowContent) {
            return CaptureResult(
                null,
                "Perlu matikan lalu nyalakan lagi Accessibility Android Island (Pengaturan > Accessibility) sekali saja agar analisis layar aktif."
            )
        }
        // Prefer the focused application window rather than the AI overlay window.
        // The panel itself is focusable while the keyboard is open, so rootInActiveWindow
        // can otherwise point back to Android Island instead of the app underneath it.
        val appWindowRoot = runCatching {
            service.windows?.firstOrNull { window ->
                window.type == AccessibilityWindowInfo.TYPE_APPLICATION &&
                    window.root?.packageName?.toString() != service.packageName &&
                    window.isFocused
            }?.root
        }.getOrNull() ?: runCatching {
            service.windows?.firstOrNull { window ->
                window.type == AccessibilityWindowInfo.TYPE_APPLICATION &&
                    window.root?.packageName?.toString() != service.packageName
            }?.root
        }.getOrNull()
        val root: AccessibilityNodeInfo? = appWindowRoot ?: service.rootInActiveWindow
        if (root == null) return CaptureResult(null, "Tidak ada jendela aplikasi yang bisa dibaca sekarang.")

        val pkg = root.packageName?.toString().orEmpty()
        if (pkg.isEmpty() || pkg == service.packageName) {
            return CaptureResult(null, "Jendela aplikasi di belakang panel AI tidak dapat diakses.")
        }

        val pm = appContext.packageManager
        val appLabel = runCatching {
            if (pkg.isNotEmpty()) pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() else ""
        }.getOrDefault("")

        // Window title comes from the focused application window (API 21+), when the app provides one.
        val title = runCatching {
            val focusedWindow: AccessibilityWindowInfo? = service.windows
                ?.firstOrNull { window: AccessibilityWindowInfo ->
                    window.type == AccessibilityWindowInfo.TYPE_APPLICATION && window.isFocused &&
                        window.root?.packageName?.toString() != service.packageName
                }
            focusedWindow?.title?.toString().orEmpty()
        }.getOrDefault("")

        // Activity class: use the fresh window-state cache when it matches the active window.
        val activityClass = if (lastPackage == pkg &&
            android.os.SystemClock.elapsedRealtime() - lastAtMs < 20_000
        ) lastActivityClass else root.className?.toString().orEmpty()

        val sb = StringBuilder()
        var nodes = 0
        val seen = HashSet<String>()
        traverse(root, sb, seen, 0) { nodes++ }
        val text = if (sb.length > MAX_TEXT_CHARS) sb.substring(0, MAX_TEXT_CHARS) else sb.toString()

        val elementList = runCatching { collectElements(root) }.getOrDefault(emptyList())
        lastElements = elementList

        val ctx = ScreenContext(
            appLabel = appLabel,
            packageName = pkg,
            activityClass = activityClass,
            windowTitle = title,
            visibleText = text,
            nodeCount = nodes,
            elements = ScreenElements.format(elementList),
            navigationHint = runCatching { ScreenStateAnalyzer.hint(elementList) }.getOrDefault("")
        )
        return CaptureResult(ctx, null)
    }


    /**
     * Captures the foreground application window as a JPEG. API 34+ window capture
     * deliberately excludes Android Island's accessibility overlay so the attached
     * image contains the app the user wants analyzed, not the chat panel itself.
     */
    suspend fun captureScreenshotJpeg(service: AccessibilityService?): ByteArray? {
        if (service == null || Build.VERSION.SDK_INT < 30) return null
        return suspendCancellableCoroutine { continuation ->
            val callback = object : AccessibilityService.TakeScreenshotCallback {
                override fun onSuccess(result: AccessibilityService.ScreenshotResult) {
                    val hardwareBuffer = result.hardwareBuffer
                    var hardwareBitmap: Bitmap? = null
                    var softwareBitmap: Bitmap? = null
                    try {
                        hardwareBitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, result.colorSpace)
                        // JPEG has no alpha: RGB_565 halves the native memory of the copy.
                        softwareBitmap = hardwareBitmap?.copy(Bitmap.Config.RGB_565, false)
                        val source = softwareBitmap
                        // Screenshots are capped at 1024 px in every mode; the model does not need more.
                        val maxEdge = 1024
                        val scale = if (source != null && maxOf(source.width, source.height) > maxEdge) {
                            maxEdge.toFloat() / maxOf(source.width, source.height).toFloat()
                        } else 1f
                        val scaled = if (source != null && scale < 1f) {
                            Bitmap.createScaledBitmap(source, (source.width * scale).toInt().coerceAtLeast(1),
                                (source.height * scale).toInt().coerceAtLeast(1), true)
                        } else source
                        val output = ByteArrayOutputStream()
                        val ok = scaled?.compress(Bitmap.CompressFormat.JPEG, 78, output) == true
                        val bytes = if (ok) output.toByteArray() else null
                        if (scaled !== source) runCatching { scaled?.recycle() }
                        if (continuation.isActive) continuation.resume(bytes)
                    } catch (_: Throwable) {
                        if (continuation.isActive) continuation.resume(null)
                    } finally {
                        runCatching { softwareBitmap?.recycle() }
                        runCatching { hardwareBitmap?.recycle() }
                        runCatching { hardwareBuffer.close() }
                    }
                }

                override fun onFailure(errorCode: Int) {
                    if (continuation.isActive) continuation.resume(null)
                }
            }

            val executor = Executor { command ->
                Thread(command, "AI-ScreenshotCapture").apply { isDaemon = true; start() }
            }
            try {
                if (Build.VERSION.SDK_INT >= 34) {
                    val target = runCatching {
                        val windows = service.windows.orEmpty().filter { window ->
                            window.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_APPLICATION &&
                                window.root?.packageName?.toString() != service.packageName
                        }
                        windows.firstOrNull { it.isFocused } ?: windows.firstOrNull()
                    }.getOrNull()
                    if (target != null) {
                        service.takeScreenshotOfWindow(target.id, executor, callback)
                    } else {
                        service.takeScreenshot(Display.DEFAULT_DISPLAY, executor, callback)
                    }
                } else {
                    service.takeScreenshot(Display.DEFAULT_DISPLAY, executor, callback)
                }
            } catch (_: Throwable) {
                if (continuation.isActive) continuation.resume(null)
            }
        }
    }

    // ---- tree walk -----------------------------------------------------------

    /** Visible clickable or editable nodes, top-to-bottom, numbered from 1. Password fields are left out. */
    private fun collectElements(root: AccessibilityNodeInfo): List<ScreenElements.Element> {
        val found = ArrayList<ScreenElements.Element>()
        val seen = HashSet<String>()
        fun walk(node: AccessibilityNodeInfo, depth: Int) {
            if (depth > MAX_DEPTH || found.size >= 200) return
            if (node.isVisibleToUser && (node.isClickable || node.isEditable) && !node.isPassword) {
                val r = android.graphics.Rect()
                node.getBoundsInScreen(r)
                if (!r.isEmpty && seen.add("${r.left},${r.top},${r.right},${r.bottom}|${node.className}")) {
                    found += ScreenElements.Element(
                        index = 0,
                        label = elementLabel(node),
                        editable = node.isEditable,
                        focused = node.isFocused,
                        left = r.left, top = r.top, right = r.right, bottom = r.bottom
                    )
                }
            }
            for (i in 0 until node.childCount) node.getChild(i)?.let { walk(it, depth + 1) }
        }
        walk(root, 0)
        return found
            .sortedWith(compareBy({ it.top }, { it.left }))
            .take(ScreenElements.MAX_ELEMENTS)
            .mapIndexed { i, e -> e.copy(index = i + 1) }
    }

    /** Best human label: own text, description, hint, a nearby child's text, then the view id. */
    private fun elementLabel(node: AccessibilityNodeInfo): String {
        fun clean(v: CharSequence?) = v?.toString()?.trim()?.replace(Regex("\\s+"), " ").orEmpty()
        clean(node.text).ifBlank { clean(node.contentDescription) }.ifBlank { clean(node.hintText) }
            .takeIf { it.isNotBlank() }?.let { return it }
        fun childText(n: AccessibilityNodeInfo, depth: Int): String {
            if (depth > 3) return ""
            for (i in 0 until n.childCount) {
                val c = n.getChild(i) ?: continue
                clean(c.text).ifBlank { clean(c.contentDescription) }.takeIf { it.isNotBlank() }?.let { return it }
                childText(c, depth + 1).takeIf { it.isNotBlank() }?.let { return it }
            }
            return ""
        }
        childText(node, 0).takeIf { it.isNotBlank() }?.let { return it }
        node.viewIdResourceName?.substringAfterLast('/')?.takeIf { it.isNotBlank() }?.let { return "id:$it" }
        return node.className?.toString()?.substringAfterLast('.').orEmpty().ifBlank { "tanpa label" }
    }

    private const val MAX_DEPTH = 40
    private const val MAX_NODES = 600
    private const val MAX_TEXT_CHARS = 4_000

    private fun traverse(
        node: AccessibilityNodeInfo,
        out: StringBuilder,
        seen: HashSet<String>,
        depth: Int,
        counter: () -> Unit
    ) {
        if (depth > MAX_DEPTH) return
        counter()
        if (out.length >= MAX_TEXT_CHARS) return

        val text = node.text?.toString()?.trim().orEmpty()
        val desc = node.contentDescription?.toString()?.trim().orEmpty()
        val line = if (text.isNotEmpty() && text != desc) {
            if (desc.isNotEmpty()) "$text | $desc" else text
        } else desc
        // Keep lines short and skip empty/duplicate lines so a big feed stays readable for the model.
        if (line.isNotEmpty() && line.length <= 400 && seen.add(line)) {
            if (out.isNotEmpty()) out.append('\n')
            out.append(line)
        }
        if (depth < MAX_DEPTH && out.length < MAX_TEXT_CHARS) {
            val count = node.childCount
            for (i in 0 until count) {
                val child = node.getChild(i) ?: continue
                traverse(child, out, seen, depth + 1, counter)
                if (out.length >= MAX_TEXT_CHARS) break
            }
        }
    }
}
