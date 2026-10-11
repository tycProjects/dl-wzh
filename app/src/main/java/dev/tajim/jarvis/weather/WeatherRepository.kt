package dev.tajim.jarvis.weather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import androidx.core.content.ContextCompat
import dev.tajim.jarvis.ai.Http
import dev.tajim.jarvis.ai.safely
import java.net.URLEncoder
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class Weather(
    val tempC: Int, val feelsC: Int, val highC: Int, val lowC: Int,
    val humidity: Int, val description: String, val place: String, val fetchedAt: Long,
)

data class GeoPlace(val name: String, val detail: String, val lat: Double, val lon: Double)

/**
 * Real weather from Open-Meteo (open-meteo.com, no API key). Location comes from the device (coarse, last known)
 * or a city the user picked. Nothing here is simulated: on failure the Home card shows the error.
 */
class WeatherRepository(private val context: Context) {
    private val app = context.applicationContext

    fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Last known coarse position, or null if permission/position is unavailable. */
    @Suppress("MissingPermission")
    suspend fun deviceLocation(): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) return@withContext null
        val lm = app.getSystemService(LocationManager::class.java) ?: return@withContext null
        runCatching {
            lm.getProviders(true).mapNotNull { lm.getLastKnownLocation(it) }.maxByOrNull { it.time }
        }.getOrNull()?.let { it.latitude to it.longitude }
    }

    @Suppress("DEPRECATION")
    suspend fun placeName(lat: Double, lon: Double): String? = withContext(Dispatchers.IO) {
        runCatching {
            Geocoder(app, Locale.getDefault()).getFromLocation(lat, lon, 1)?.firstOrNull()?.let {
                it.locality ?: it.subAdminArea ?: it.adminArea
            }
        }.getOrNull()
    }

    suspend fun fetch(lat: Double, lon: Double, place: String): Result<Weather> = safely {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code" +
            "&daily=temperature_2m_max,temperature_2m_min&timezone=auto&forecast_days=1"
        val (code, body) = Http.request(url, readTimeoutMs = 20_000)
        if (code !in 200..299) error("Weather service error (HTTP $code).")
        val root = JSONObject(body)
        val cur = root.getJSONObject("current")
        val daily = root.getJSONObject("daily")
        Weather(
            tempC = cur.getDouble("temperature_2m").toInt(),
            feelsC = cur.getDouble("apparent_temperature").toInt(),
            highC = daily.getJSONArray("temperature_2m_max").getDouble(0).toInt(),
            lowC = daily.getJSONArray("temperature_2m_min").getDouble(0).toInt(),
            humidity = cur.getInt("relative_humidity_2m"),
            description = describe(cur.getInt("weather_code")),
            place = place,
            fetchedAt = System.currentTimeMillis(),
        )
    }

    suspend fun searchCity(query: String): Result<List<GeoPlace>> = safely {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val (code, body) = Http.request("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=6&language=en&format=json", readTimeoutMs = 20_000)
        if (code !in 200..299) error("City search failed (HTTP $code).")
        val arr = JSONObject(body).optJSONArray("results") ?: return@safely emptyList()
        (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            GeoPlace(
                o.getString("name"),
                listOf(o.optString("admin1"), o.optString("country")).filter { s -> s.isNotBlank() }.joinToString(", "),
                o.getDouble("latitude"), o.getDouble("longitude"),
            )
        }
    }

    /** WMO weather interpretation codes used by Open-Meteo. */
    private fun describe(code: Int) = when (code) {
        0 -> "Clear Sky"
        1 -> "Mainly Clear"
        2 -> "Partly Cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        in 51..57 -> "Drizzle"
        in 61..67 -> "Rain"
        in 71..77 -> "Snow"
        in 80..82 -> "Rain Showers"
        85, 86 -> "Snow Showers"
        95 -> "Thunderstorm"
        96, 99 -> "Thunderstorm with Hail"
        else -> "Unknown"
    }
}
