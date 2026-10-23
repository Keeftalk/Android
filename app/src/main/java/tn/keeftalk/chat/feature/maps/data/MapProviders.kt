package tn.keeftalk.chat.feature.maps.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import tn.keeftalk.chat.domain.model.CanonicalLocation

enum class MapStyle(val title: String, val styleUrl: String) {
    LIBERTY("Liberty", "https://tiles.openfreemap.org/styles/liberty"),
    BRIGHT("Bright", "https://tiles.openfreemap.org/styles/bright"),
    POSITRON("Positron", "https://tiles.openfreemap.org/styles/positron")
}

interface GeocodingProvider {
    suspend fun search(query: String): List<CanonicalLocation>
    suspend fun reverseGeocode(latitude: Double, longitude: Double): CanonicalLocation?
}

class NominatimGeocodingProvider(private val client: OkHttpClient = OkHttpClient()) : GeocodingProvider {
    companion object {
        const val TAG = "NominatimGeocoding"
    }

    override suspend fun search(query: String): List<CanonicalLocation> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val encoded = java.net.URLEncoder.encode(query, "UTF-8")
            val url = "https://nominatim.openstreetmap.org/search?format=json&q=$encoded&limit=10"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "KeeftalkAndroidApp/1.0 (contact@keeftalk.tn)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val body = response.body.string()
                val jsonArray = JSONArray(body)
                val results = mutableListOf<CanonicalLocation>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val lat = obj.optDouble("lat", 0.0)
                    val lon = obj.optDouble("lon", 0.0)
                    val name = obj.optString("display_name", "Unknown Place")
                    val placeId = obj.optString("place_id", i.toString())
                    results.add(
                        CanonicalLocation(
                            latitude = lat,
                            longitude = lon,
                            name = name.split(",").firstOrNull() ?: name,
                            address = name,
                            placeId = placeId,
                            provider = "Nominatim"
                        )
                    )
                }
                results
            }
        } catch (e: Exception) {
            Log.e(TAG, "Search failed for query: $query", e)
            emptyList()
        }
    }

    override suspend fun reverseGeocode(latitude: Double, longitude: Double): CanonicalLocation? = withContext(Dispatchers.IO) {
        try {
            val url = "https://nominatim.openstreetmap.org/reverse?format=json&lat=$latitude&lon=$longitude"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "KeeftalkAndroidApp/1.0 (contact@keeftalk.tn)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body.string()
                val obj = JSONObject(body)
                val displayName = obj.optString("display_name", "Selected Location")
                val placeId = obj.optString("place_id", "rev_${latitude}_${longitude}")
                CanonicalLocation(
                    latitude = latitude,
                    longitude = longitude,
                    name = displayName.split(",").firstOrNull() ?: "Location",
                    address = displayName,
                    placeId = placeId,
                    provider = "Nominatim"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Reverse geocode failed for $latitude, $longitude", e)
            CanonicalLocation(
                latitude = latitude,
                longitude = longitude,
                name = "Pinned Location",
                address = "Lat: $latitude, Lon: $longitude",
                provider = "Offline"
            )
        }
    }
}
