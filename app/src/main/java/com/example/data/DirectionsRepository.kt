package com.example.data

import org.maplibre.android.geometry.LatLng
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@JsonClass(generateAdapter = true)
data class OsrmRouteResponse(
    @Json(name = "code") val code: String,
    @Json(name = "routes") val routes: List<OsrmRouteDto> = emptyList(),
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class OsrmRouteDto(
    @Json(name = "distance") val distance: Double = 0.0,
    @Json(name = "duration") val duration: Double = 0.0,
    @Json(name = "geometry") val geometry: OsrmGeometryDto? = null,
    @Json(name = "legs") val legs: List<OsrmLegDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class OsrmGeometryDto(
    @Json(name = "coordinates") val coordinates: List<List<Double>> = emptyList(),
    @Json(name = "type") val type: String = "LineString"
)

@JsonClass(generateAdapter = true)
data class OsrmLegDto(
    @Json(name = "distance") val distance: Double = 0.0,
    @Json(name = "duration") val duration: Double = 0.0,
    @Json(name = "summary") val summary: String? = null,
    @Json(name = "steps") val steps: List<OsrmStepDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class OsrmStepDto(
    @Json(name = "distance") val distance: Double = 0.0,
    @Json(name = "duration") val duration: Double = 0.0,
    @Json(name = "name") val name: String? = null,
    @Json(name = "maneuver") val maneuver: OsrmManeuverDto? = null
)

@JsonClass(generateAdapter = true)
data class OsrmManeuverDto(
    @Json(name = "type") val type: String? = null,
    @Json(name = "modifier") val modifier: String? = null
)

interface OsrmApi {
    @GET("route/v1/{profile}/{coordinates}")
    suspend fun getRoute(
        @Path("profile") profile: String,
        @Path(value = "coordinates", encoded = true) coordinates: String,
        @Query("overview") overview: String = "full",
        @Query("geometries") geometries: String = "geojson",
        @Query("steps") steps: Boolean = true
    ): OsrmRouteResponse
}

class DirectionsRepository {

    private val api: OsrmApi

    init {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "ZMAPS-Navigation-Android/1.0 (contact: jussargoldensarajena@gmail.com)")
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://router.project-osrm.org/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        api = retrofit.create(OsrmApi::class.java)
    }

    suspend fun calculateRoute(
        origin: LatLng,
        destination: LatLng,
        mode: RouteMode = RouteMode.DRIVING
    ): Result<ZRoute> {
        return try {
            val coords = "${origin.longitude},${origin.latitude};${destination.longitude},${destination.latitude}"
            val response = api.getRoute(
                profile = mode.profile,
                coordinates = coords
            )

            if (response.code != "Ok" || response.routes.isEmpty()) {
                val errorMsg = response.message ?: "No navigable path found between these coordinates."
                return Result.failure(IllegalStateException(errorMsg))
            }

            val routeDto = response.routes.first()
            val rawPoints = routeDto.geometry?.coordinates ?: emptyList()
            val points = rawPoints.mapNotNull { coord ->
                if (coord.size >= 2) LatLng(coord[1], coord[0]) else null
            }

            val totalMeters = routeDto.distance.toLong()
            val totalSeconds = routeDto.duration.toLong()

            val stepsList = mutableListOf<ZRouteStep>()
            routeDto.legs.forEach { leg ->
                leg.steps.forEach { step ->
                    val instruction = formatManeuverInstruction(step)
                    val stepDist = formatDistance(step.distance)
                    val stepDur = formatDuration(step.duration)
                    stepsList.add(
                        ZRouteStep(
                            instruction = instruction,
                            distanceText = stepDist,
                            durationText = stepDur,
                            maneuver = step.maneuver?.type,
                            modifier = step.maneuver?.modifier
                        )
                    )
                }
            }

            val route = ZRoute(
                points = points,
                distanceText = formatDistance(routeDto.distance),
                durationText = formatDuration(routeDto.duration),
                distanceMeters = totalMeters,
                durationSeconds = totalSeconds,
                startAddress = "Current Position",
                endAddress = "Destination",
                mode = mode,
                steps = stepsList
            )

            Result.success(route)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun formatManeuverInstruction(step: OsrmStepDto): String {
        val type = step.maneuver?.type ?: "continue"
        val modifier = step.maneuver?.modifier?.replace("-", " ") ?: ""
        val streetName = if (!step.name.isNullOrBlank()) "onto ${step.name}" else "ahead"

        return when (type) {
            "depart" -> if (!step.name.isNullOrBlank()) "Head onto ${step.name}" else "Start route"
            "arrive" -> "Arrive at destination"
            "turn" -> if (modifier.isNotBlank()) "Turn $modifier $streetName" else "Make a turn $streetName"
            "new name", "continue" -> if (!step.name.isNullOrBlank()) "Continue onto ${step.name}" else "Continue straight"
            "fork" -> if (modifier.isNotBlank()) "Take fork $modifier $streetName" else "Take fork $streetName"
            "roundabout" -> "Enter roundabout and take exit $streetName"
            "rotary" -> "Enter rotary and follow $streetName"
            "merge" -> "Merge $streetName"
            "on ramp" -> "Take on-ramp $streetName"
            "off ramp" -> "Take exit ramp $streetName"
            else -> if (modifier.isNotBlank()) "Turn $modifier $streetName" else "Proceed $streetName"
        }
    }

    private fun formatDistance(meters: Double): String {
        return if (meters < 1000.0) {
            "${meters.roundToInt()} m"
        } else {
            String.format(Locale.US, "%.1f km", meters / 1000.0)
        }
    }

    private fun formatDuration(seconds: Double): String {
        val totalMinutes = (seconds / 60.0).roundToInt()
        return if (totalMinutes < 60) {
            "$totalMinutes mins"
        } else {
            val hours = totalMinutes / 60
            val remainingMins = totalMinutes % 60
            if (remainingMins == 0) "${hours}h" else "${hours}h ${remainingMins}m"
        }
    }
}
