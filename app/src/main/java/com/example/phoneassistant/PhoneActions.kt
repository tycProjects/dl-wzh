package com.example.phoneassistant

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.provider.AlarmClock
import org.json.JSONArray
import org.json.JSONObject

/**
 * Всё, что ассистент умеет делать с телефоном.
 * Опасные действия (звонок, SMS) только открывают системный экран —
 * финальное нажатие всегда за пользователем.
 */
class PhoneActions(private val ctx: Context) {

    private fun start(i: Intent) {
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i)
    }

    fun run(name: String, a: JSONObject): String = when (name) {
        "set_alarm" -> {
            start(
                Intent(AlarmClock.ACTION_SET_ALARM)
                    .putExtra(AlarmClock.EXTRA_HOUR, a.getInt("hour"))
                    .putExtra(AlarmClock.EXTRA_MINUTES, a.getInt("minute"))
                    .putExtra(AlarmClock.EXTRA_MESSAGE, a.optString("label"))
                    .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            )
            "Будильник установлен на %02d:%02d".format(a.getInt("hour"), a.getInt("minute"))
        }
        "set_timer" -> {
            start(
                Intent(AlarmClock.ACTION_SET_TIMER)
                    .putExtra(AlarmClock.EXTRA_LENGTH, a.getInt("seconds"))
                    .putExtra(AlarmClock.EXTRA_MESSAGE, a.optString("label"))
                    .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            )
            "Таймер запущен на ${a.getInt("seconds")} сек."
        }
        "dial" -> {
            start(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${a.getString("number")}")))
            "Номер набран в звонилке, пользователь должен нажать «Позвонить»"
        }
        "compose_sms" -> {
            start(
                Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${a.getString("number")}"))
                    .putExtra("sms_body", a.getString("text"))
            )
            "Сообщение подготовлено, пользователь должен нажать «Отправить»"
        }
        "open_app" -> openApp(a.getString("name"))
        "open_url" -> {
            start(Intent(Intent.ACTION_VIEW, Uri.parse(a.getString("url"))))
            "Ссылка открыта"
        }
        "open_maps" -> {
            start(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(a.getString("query")))))
            "Карта открыта"
        }
        "flashlight" -> {
            val cm = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            cm.setTorchMode(cm.cameraIdList.first(), a.getBoolean("on"))
            if (a.getBoolean("on")) "Фонарик включён" else "Фонарик выключен"
        }
        "set_volume" -> {
            val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            am.setStreamVolume(AudioManager.STREAM_MUSIC, max * a.getInt("percent").coerceIn(0, 100) / 100, 0)
            "Громкость установлена"
        }
        "battery" -> {
            val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            "Заряд батареи: ${bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)}%"
        }
        else -> "Неизвестный инструмент: $name"
    }

    private fun openApp(query: String): String {
        val pm = ctx.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val match = pm.queryIntentActivities(launcher, 0).firstOrNull {
            it.loadLabel(pm).toString().contains(query, ignoreCase = true)
        } ?: return "Приложение «$query» не найдено"
        start(pm.getLaunchIntentForPackage(match.activityInfo.packageName)!!)
        return "Открыто: ${match.loadLabel(pm)}"
    }

    // ---- Описание инструментов для Claude ----
    private fun tool(name: String, desc: String, vararg props: Triple<String, String, String>): JSONObject {
        val p = JSONObject()
        props.forEach { (n, t, d) -> p.put(n, JSONObject().put("type", t).put("description", d)) }
        val schema = JSONObject().put("type", "object").put("properties", p)
            .put("required", JSONArray(props.map { it.first }))
        return JSONObject().put("name", name).put("description", desc).put("input_schema", schema)
    }

    fun toolSchemas(): JSONArray = JSONArray()
        .put(tool("set_alarm", "Поставить будильник",
            Triple("hour", "integer", "Час, 0-23"), Triple("minute", "integer", "Минуты, 0-59"),
            Triple("label", "string", "Название будильника")))
        .put(tool("set_timer", "Запустить таймер",
            Triple("seconds", "integer", "Длительность в секундах"), Triple("label", "string", "Название")))
        .put(tool("dial", "Открыть звонилку с номером", Triple("number", "string", "Номер телефона")))
        .put(tool("compose_sms", "Подготовить SMS",
            Triple("number", "string", "Номер получателя"), Triple("text", "string", "Текст сообщения")))
        .put(tool("open_app", "Открыть приложение по названию", Triple("name", "string", "Название приложения")))
        .put(tool("open_url", "Открыть ссылку в браузере", Triple("url", "string", "Полный URL")))
        .put(tool("open_maps", "Найти место или построить маршрут на карте", Triple("query", "string", "Адрес или название места")))
        .put(tool("flashlight", "Включить или выключить фонарик", Triple("on", "boolean", "true — включить")))
        .put(tool("set_volume", "Установить громкость медиа", Triple("percent", "integer", "Громкость, 0-100")))
        .put(tool("battery", "Узнать уровень заряда батареи"))
}
