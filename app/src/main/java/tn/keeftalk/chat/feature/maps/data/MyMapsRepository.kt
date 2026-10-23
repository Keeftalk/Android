package tn.keeftalk.chat.feature.maps.data

import android.location.Location
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tn.keeftalk.chat.data.local.dao.SavedPlaceDao
import tn.keeftalk.chat.data.local.entities.toDomain
import tn.keeftalk.chat.data.local.entities.toEntity
import tn.keeftalk.chat.domain.model.CanonicalLocation
import tn.keeftalk.chat.domain.service.LocationService

class MyMapsRepository(
    private val savedPlaceDao: SavedPlaceDao,
    private val locationService: LocationService,
    private val geocodingProvider: GeocodingProvider
) {
    val savedPlaces: Flow<List<CanonicalLocation>> = savedPlaceDao.getAllSavedPlaces()
        .map { list -> list.map { it.toDomain() } }

    suspend fun getCurrentLocation(): Location? {
        return locationService.getCurrentLocation()
    }

    suspend fun searchPlaces(query: String): List<CanonicalLocation> {
        return geocodingProvider.search(query)
    }

    suspend fun reverseGeocode(latitude: Double, longitude: Double): CanonicalLocation? {
        return geocodingProvider.reverseGeocode(latitude, longitude)
    }

    suspend fun savePlace(location: CanonicalLocation) {
        savedPlaceDao.insertSavedPlace(location.toEntity())
    }

    suspend fun deleteSavedPlace(id: String) {
        savedPlaceDao.deleteSavedPlaceById(id)
    }
}
