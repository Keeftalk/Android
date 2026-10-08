package tn.keeftalk.chat.data.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import tn.keeftalk.chat.domain.service.LocationService
import kotlinx.coroutines.tasks.await

class LocationServiceImpl(private val context: Context) : LocationService {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): Location? {
        return try {
            val loc = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
            if (loc != null) {
                loc
            } else {
                fusedLocationClient.lastLocation.await()
            }
        } catch (e: Exception) {
            try {
                fusedLocationClient.lastLocation.await()
            } catch (_: Exception) {
                null
            }
        }
    }
}
