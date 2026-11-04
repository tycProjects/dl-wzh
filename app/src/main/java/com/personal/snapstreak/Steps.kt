package com.personal.snapstreak

sealed class Step {
    /** settleMs = pause after this tap (null = use Steps.SETTLE_AFTER_TAP_MS). */
    data class Tap(
        val label: String,
        val x: Float,
        val y: Float,
        val settleMs: Long? = null,
        /** true = the pause after this tap is the "camera gap" you set in the app. */
        val useCameraGap: Boolean = false
    ) : Step()

    data class Wait(val ms: Long) : Step()

    /** Taps the tab at (x, y), then checks on screen that the tab really switched (retries, or stops). */
    data class EnsureTab(val label: String, val x: Float, val y: Float, val settleMs: Long) : Step()
}

/**
 * Everything you can tweak lives here.
 * Step 1 (launch Snapchat) is done by MainActivity once. Then the list below runs over and over:
 * drop down arrow, multi-snap, 16 camera taps, edit & send, send to, face melt tab, select all,
 * send, camera icon, and back to the top.
 * Coordinates are screen pixels for a 720 x 1600 display.
 */
object Steps {
    const val SNAPCHAT_PACKAGE = "com.snapchat.android"

    // ---- loop -------------------------------------------------------------------------------
    /** How many times to run the whole list. 0 = keep looping until you stop it. */
    const val MAX_LOOPS = 0

    /** Pause after "Send" (Snapchat is busy uploading) before tapping the camera icon. */
    const val AFTER_SEND_MS = 3000L

    /** Pause after the camera icon, before the next loop starts at the drop down arrow. */
    const val AFTER_CAMERA_ICON_MS = 2500L

    // ---- timings ----------------------------------------------------------------------------
    /** Time for Snapchat to finish opening before the first tap (not one of your numbered steps). */
    const val LAUNCH_SETTLE_MS = 5000L

    /** Pause after a tap that has no pause of its own. Set to 0L to remove. */
    const val SETTLE_AFTER_TAP_MS = 1500L

    /**
     * Starting value for the pause after each camera button tap. Change it live in the app with the
     * - / + buttons (no rebuild needed). With 0, Snapchat dropped most taps (about 5 of 16 photos).
     */
    const val CAMERA_GAP_MS = 800L

    /** How long each tap is held down. */
    const val TAP_DURATION_MS = 70L

    // ---- safety -----------------------------------------------------------------------------
    /** Floating red STOP button over every app while a run is active (drag it anywhere). */
    const val SHOW_STOP_BUTTON = true

    /** Where the STOP button starts (pixels from the top of the screen, left edge). */
    const val STOP_BUTTON_Y_PX = 800

    /** Stop automatically if another app comes to the front, so it never taps TikTok etc. */
    const val STOP_IF_LEFT_SNAPCHAT = true

    /**
     * Stop if the phone's keyboard pops up. That means a tap landed on a text box (the snap caption,
     * a search bar...), and the next taps would hit keyboard keys. Set to false to ignore keyboards.
     */
    const val STOP_IF_KEYBOARD_OPENS = true
    val IGNORED_PACKAGES: Set<String> = setOf("com.android.systemui", "android")

    /**
     * Face melt tab check (needs Android 11+): look at the screen to confirm the tab is selected
     * before "Select all" and "Send". If it can't be selected, the run STOPS instead of sending
     * to the wrong place. Set to false to go back to blind tapping.
     */
    const val VERIFY_TAB = true
    const val TAB_POLL_MS = 700L        // re-check interval while waiting for the Send To screen
    const val TAB_MAX_POLLS = 14        // about 10 seconds of waiting
    const val TAB_RECHECK_MS = 1200L    // pause after tapping the tab, before checking it took
    const val TAB_MAX_TAPS = 4          // tries before giving up

    val all: List<Step> = buildList<Step> {
        add(Step.Tap("Drop down arrow", 668f, 338f))
        add(Step.Tap("Multi-snap", 656f, 418f))

        // 16 camera taps with the adjustable gap between them. The last one keeps the normal pause
        // so that "Edit & send" doesn't fire in the middle of the last capture.
        repeat(15) { add(Step.Tap("Camera button", 354f, 1204f, useCameraGap = true)) }
        add(Step.Tap("Camera button", 354f, 1204f))

        add(Step.Tap("Edit & send", 359f, 1067f))
        add(Step.Tap("Send to", 590f, 1438f, settleMs = 3000L))
        add(Step.EnsureTab("Face melt emoji", 145f, 198f, settleMs = 1500L))
        add(Step.Tap("Select all", 606f, 487f))
        add(Step.Tap("Send", 668f, 1456f, settleMs = AFTER_SEND_MS))
        add(Step.Tap("Camera icon", 365f, 1464f, settleMs = AFTER_CAMERA_ICON_MS))
    }

    init {
        // 2 + 16 camera taps + 6 = 24 entries per loop
        check(all.size == 24) { "Loop should hold 24 steps but has ${all.size}" }
    }
}
