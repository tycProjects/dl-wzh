package dev.tajim.jarvis.phone

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.provider.ContactsContract
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import dev.tajim.jarvis.weather.Weather
import java.net.URLEncoder
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Executes parsed commands. Calls and messages are never sent without a spoken "yes" first.
 * It cannot enter your PIN, pattern or password and does not try to: unlocking a secured phone is left to you.
 */
class PhoneControl(
    private val context: Context,
    private val currentWeather: suspend () -> Weather?,
) {
    private val app = context.applicationContext
    private var torchOn = false

    private fun a11y() = JarvisAccessibilityService.instance

    private val needA11y = ActionResult.Failed("Phone control is off. Turn on JARVIS in Accessibility settings first.")

    private fun launch(intent: Intent): Boolean {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        // An enabled accessibility service is allowed to open screens while the app is in the background.
        val ctx: Context = a11y() ?: app
        return try { ctx.startActivity(intent); true } catch (_: Exception) { false }
    }

    suspend fun execute(cmd: Command): ActionResult = when (cmd) {
        is Command.OpenApp -> openApp(cmd.name)
        Command.Home -> global(AccessibilityService.GLOBAL_ACTION_HOME, "Going home.")
        Command.Back -> global(AccessibilityService.GLOBAL_ACTION_BACK, "Going back.")
        Command.Recents -> global(AccessibilityService.GLOBAL_ACTION_RECENTS, "Showing recent apps.")
        Command.Notifications -> global(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS, "Opening notifications.")
        Command.LockScreen -> lockScreen()
        Command.UnlockScreen -> unlockScreen()
        Command.ScrollDown -> scroll(down = true)
        Command.ScrollUp -> scroll(down = false)
        Command.VolumeUp -> volume(AudioManager.ADJUST_RAISE, "Volume up.")
        Command.VolumeDown -> volume(AudioManager.ADJUST_LOWER, "Volume down.")
        is Command.Flashlight -> flashlight(cmd.on)
        is Command.Call -> call(cmd.target)
        is Command.WhatsApp -> whatsApp(cmd.contact, cmd.message)
        is Command.ClickText -> {
            val s = a11y()
            if (s == null) needA11y
            else if (s.clickText(cmd.text)) ActionResult.Done("Tapped ${cmd.text}.") else ActionResult.Failed("I could not find ${cmd.text} on the screen.")
        }
        is Command.TypeText -> {
            val s = a11y()
            if (s == null) needA11y
            else if (s.typeText(cmd.text)) ActionResult.Done("Typed it.") else ActionResult.Failed("There is no text field selected to type into.")
        }
        is Command.WebSearch -> webSearch(cmd)
        Command.Time -> ActionResult.Done("It is " + DateFormat.getTimeInstance(DateFormat.SHORT).format(Date()) + ".")
        Command.Battery -> battery()
        Command.WeatherNow -> currentWeather()?.let {
            ActionResult.Done("It is ${it.tempC} degrees and ${it.description.lowercase()} in ${it.place}.")
        } ?: ActionResult.Failed("I do not have weather yet. Set a location in Settings.")
    }

    private fun global(action: Int, ok: String): ActionResult {
        val s = a11y() ?: return needA11y
        return if (s.global(action)) ActionResult.Done(ok) else ActionResult.Failed("Android did not allow that action.")
    }

    private fun lockScreen(): ActionResult {
        val s = a11y() ?: return needA11y
        if (Build.VERSION.SDK_INT < 28) return ActionResult.Failed("Locking by voice needs Android 9 or newer.")
        return if (s.global(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)) ActionResult.Done("Locking the phone.") else ActionResult.Failed("Could not lock the screen.")
    }

    @Suppress("DEPRECATION")
    private suspend fun unlockScreen(): ActionResult {
        val pm = app.getSystemService(PowerManager::class.java)
        val wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP, "jarvis:wake")
        wl.acquire(5_000)
        val km = app.getSystemService(KeyguardManager::class.java)
        delay(700)
        val s = a11y()
        if (s != null && km.isKeyguardLocked) {
            val (w, h) = s.screenSize()
            s.swipe(w / 2f, h * 0.85f, w / 2f, h * 0.25f, 350) // dismisses a plain swipe-to-unlock screen
            delay(500)
        }
        return if (km.isKeyguardLocked && km.isDeviceSecure) {
            ActionResult.Done("The screen is on. Unlock with your PIN, pattern or fingerprint. I cannot enter that for you.")
        } else {
            ActionResult.Done("The screen is on.")
        }
    }

    private suspend fun scroll(down: Boolean): ActionResult {
        val s = a11y() ?: return needA11y
        val (w, h) = s.screenSize()
        val (from, to) = if (down) h * 0.7f to h * 0.3f else h * 0.3f to h * 0.7f
        return if (s.swipe(w / 2f, from, w / 2f, to, 350)) ActionResult.Done("Scrolled.") else ActionResult.Failed("The scroll did not work.")
    }

    private fun volume(direction: Int, ok: String): ActionResult {
        app.getSystemService(AudioManager::class.java).adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
        return ActionResult.Done(ok)
    }

    private fun flashlight(on: Boolean): ActionResult = runCatching {
        val cm = app.getSystemService(CameraManager::class.java)
        val id = cm.cameraIdList.firstOrNull { cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true }
            ?: return ActionResult.Failed("This phone has no flashlight.")
        cm.setTorchMode(id, on)
        torchOn = on
        ActionResult.Done(if (on) "Flashlight on." else "Flashlight off.")
    }.getOrElse { ActionResult.Failed("Could not change the flashlight.") }

    private fun battery(): ActionResult {
        val pct = app.getSystemService(BatteryManager::class.java).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        return if (pct in 0..100) ActionResult.Done("Battery is at $pct percent.") else ActionResult.Failed("I could not read the battery level.")
    }

    // ---------------- apps ----------------
    private fun openApp(name: String): ActionResult {
        val pm = app.packageManager
        val launchers = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .map { it.loadLabel(pm).toString() to it.activityInfo.packageName }
        val pkg = AppMatcher.best(name, launchers) ?: return ActionResult.Failed("I could not find an app called $name.")
        val intent = pm.getLaunchIntentForPackage(pkg) ?: return ActionResult.Failed("I cannot open $name.")
        return if (launch(intent)) ActionResult.Done("Opening $name.") else ActionResult.Failed("Android blocked opening $name.")
    }

    private fun webSearch(cmd: Command.WebSearch): ActionResult {
        val q = URLEncoder.encode(cmd.query, "UTF-8")
        val url = if (cmd.youtube) "https://www.youtube.com/results?search_query=$q" else "https://www.google.com/search?q=$q"
        return if (launch(Intent(Intent.ACTION_VIEW, Uri.parse(url)))) ActionResult.Done("Searching for ${cmd.query}.") else ActionResult.Failed("No browser is available.")
    }

    // ---------------- contacts, calls, messages ----------------
    private suspend fun lookupContact(target: String): Pair<String, String>? = withContext(Dispatchers.IO) {
        val digits = target.filter { it.isDigit() || it == '+' }
        if (digits.length >= 6 && digits.length >= target.count { !it.isWhitespace() } - 2) return@withContext target to digits
        if (ContextCompat.checkSelfPermission(app, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return@withContext null
        runCatching {
            app.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?", arrayOf("%$target%"),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC",
            )?.use { c -> if (c.moveToFirst()) c.getString(0) to c.getString(1) else null }
        }.getOrNull()
    }

    private suspend fun call(target: String): ActionResult {
        val hasContacts = ContextCompat.checkSelfPermission(app, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        val found = lookupContact(target)
            ?: return ActionResult.Failed(if (hasContacts) "I could not find $target in your contacts." else "Contacts permission is off. Allow it in JARVIS Settings, Permissions.")
        val (name, number) = found
        return ActionResult.Confirm("Call $name?") {
            if (ContextCompat.checkSelfPermission(app, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                ActionResult.Failed("Phone permission is off. Allow it in JARVIS Settings, Permissions.")
            } else if (launch(Intent(Intent.ACTION_CALL, Uri.parse("tel:" + Uri.encode(number))))) ActionResult.Done("Calling $name.")
            else ActionResult.Failed("Android blocked the call.")
        }
    }

    private fun whatsAppNumber(raw: String): String? {
        val d = raw.filter { it.isDigit() }
        return when {
            raw.trim().startsWith("+") -> d
            d.startsWith("00") -> d.drop(2)
            d.startsWith("01") && (app.getSystemService(TelephonyManager::class.java)?.simCountryIso.orEmpty().equals("bd", true)) -> "880" + d.drop(1)
            d.startsWith("0") -> null
            else -> d
        }
    }

    private suspend fun whatsApp(contact: String, message: String): ActionResult {
        val found = lookupContact(contact) ?: return ActionResult.Failed("I could not find $contact in your contacts. Allow Contacts in Settings, Permissions.")
        val (name, raw) = found
        val number = whatsAppNumber(raw) ?: return ActionResult.Failed("$name's number has no country code, so WhatsApp cannot find it. Save it with +country code.")
        return ActionResult.Confirm("Send \"$message\" to $name on WhatsApp?") {
            sendWhatsApp(name, number, message)
        }
    }

    private suspend fun sendWhatsApp(name: String, number: String, message: String): ActionResult {
        val uri = Uri.parse("https://wa.me/$number?text=" + URLEncoder.encode(message, "UTF-8"))
        if (!launch(Intent(Intent.ACTION_VIEW, uri))) return ActionResult.Failed("WhatsApp could not be opened.")
        val s = a11y() ?: return ActionResult.Done("WhatsApp is open with your message. Phone control is off, so press send yourself.")
        delay(3_000)
        return if (s.clickText("Send")) ActionResult.Done("Sent to $name.")
        else ActionResult.Done("WhatsApp is open with your message. I could not find the send button, so press it yourself.")
    }
}
