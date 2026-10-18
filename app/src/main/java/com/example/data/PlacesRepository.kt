package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class NominatimResultDto(
    @Json(name = "place_id") val placeId: Long? = null,
    @Json(name = "lat") val lat: String? = null,
    @Json(name = "lon") val lon: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "display_name") val displayName: String? = null,
    @Json(name = "class") val placeClass: String? = null,
    @Json(name = "type") val type: String? = null
)

interface NominatimApi {
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("format") format: String = "json",
        @Query("addressdetails") addressDetails: Int = 1,
        @Query("limit") limit: Int = 15
    ): List<NominatimResultDto>
}

class PlacesRepository {

    private val api: NominatimApi

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
            .baseUrl("https://nominatim.openstreetmap.org/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        api = retrofit.create(NominatimApi::class.java)
    }

    suspend fun searchPlaces(query: String): Result<List<ZPlace>> {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) {
            return Result.success(emptyList())
        }

        return try {
            val results = api.search(query = cleanQuery)
            val places = results.mapNotNull { item ->
                val lat = item.lat?.toDoubleOrNull() ?: return@mapNotNull null
                val lon = item.lon?.toDoubleOrNull() ?: return@mapNotNull null
                val rawDisplay = item.displayName ?: "Unknown Place"

                val primaryTitle = if (!item.name.isNullOrBlank()) {
                    item.name
                } else {
                    rawDisplay.substringBefore(",")
                }

                val subtitle = if (rawDisplay.contains(",")) {
                    rawDisplay.substringAfter(", ").trim()
                } else {
                    rawDisplay
                }

                ZPlace(
                    id = item.placeId?.toString() ?: "osm_${lat}_${lon}",
                    name = primaryTitle.ifBlank { "Location Pin" },
                    address = subtitle,
                    latitude = lat,
                    longitude = lon,
                    category = item.type ?: item.placeClass ?: "place"
                )
            }
            Result.success(places)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
