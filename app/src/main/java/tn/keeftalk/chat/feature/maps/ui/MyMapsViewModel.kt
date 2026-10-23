package tn.keeftalk.chat.feature.maps.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import tn.keeftalk.chat.di.AppModule
import tn.keeftalk.chat.data.service.LocationServiceImpl
import tn.keeftalk.chat.domain.model.CanonicalLocation
import tn.keeftalk.chat.feature.maps.data.MapStyle
import tn.keeftalk.chat.feature.maps.data.MyMapsRepository
import tn.keeftalk.chat.feature.maps.data.NominatimGeocodingProvider

data class MyMapsUiState(
    val currentLocation: CanonicalLocation? = null,
    val selectedLocation: CanonicalLocation? = null,
    val savedPlaces: List<CanonicalLocation> = emptyList(),
    val searchResults: List<CanonicalLocation> = emptyList(),
    val searchQuery: String = "",
    val mapStyle: MapStyle = MapStyle.LIBERTY,
    val isSearching: Boolean = false,
    val isDrawerOpen: Boolean = false,
    val toastMessage: String? = null,
    val isPickerMode: Boolean = false,
    val chatId: String? = null
)

class MyMapsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: MyMapsRepository = MyMapsRepository(
        savedPlaceDao = AppModule.provideDatabase(application).savedPlaceDao(),
        locationService = LocationServiceImpl(application),
        geocodingProvider = NominatimGeocodingProvider()
    )

    private val _uiState = MutableStateFlow(MyMapsUiState())
    val uiState: StateFlow<MyMapsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.savedPlaces.collect { saved ->
                _uiState.update { it.copy(savedPlaces = saved) }
            }
        }
        val tzOffsetHours = java.util.Calendar.getInstance().timeZone.rawOffset / (1000.0 * 60.0 * 60.0)
        val estLon = -tzOffsetHours * 15.0
        val estLat = 35.0
        val estimatedLoc = CanonicalLocation(
            latitude = estLat,
            longitude = estLon,
            name = "Estimated Location",
            address = "Estimated from your time zone / IP",
            provider = "Timezone"
        )
        _uiState.update { it.copy(selectedLocation = estimatedLoc) }

        fetchCurrentLocation()
    }

    fun fetchCurrentLocation() {
        viewModelScope.launch {
            try {
                val loc = repository.getCurrentLocation()
                if (loc != null) {
                    val canonical = CanonicalLocation(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        name = "Current Location",
                        address = "GPS Location",
                        provider = "GPS"
                    )
                    _uiState.update { it.copy(currentLocation = canonical, selectedLocation = canonical) }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(toastMessage = "Unable to get current location") }
            }
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.length > 2) {
            viewModelScope.launch {
                _uiState.update { it.copy(isSearching = true) }
                val results = repository.searchPlaces(query)
                _uiState.update { it.copy(searchResults = results, isSearching = false) }
            }
        } else {
            _uiState.update { it.copy(searchResults = emptyList()) }
        }
    }

    fun selectLocation(location: CanonicalLocation) {
        _uiState.update { it.copy(selectedLocation = location, searchQuery = "", searchResults = emptyList()) }
    }

    suspend fun reverseGeocode(lat: Double, lon: Double): CanonicalLocation? {
        val place = repository.reverseGeocode(lat, lon)
        if (place != null) {
            _uiState.update { it.copy(selectedLocation = place) }
        }
        return place
    }

    fun setMapStyle(style: MapStyle) {
        _uiState.update { it.copy(mapStyle = style) }
    }

    fun toggleDrawer(open: Boolean) {
        _uiState.update { it.copy(isDrawerOpen = open) }
    }

    fun saveCurrentSelectedPlace() {
        viewModelScope.launch {
            val sel = _uiState.value.selectedLocation
            if (sel != null) {
                repository.savePlace(sel)
                _uiState.update { it.copy(toastMessage = "Saved '${sel.name ?: "Location"}'") }
            }
        }
    }

    fun deleteSavedPlace(id: String) {
        viewModelScope.launch {
            repository.deleteSavedPlace(id)
            _uiState.update { it.copy(toastMessage = "Removed saved place") }
        }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    fun initPickerMode(isPicker: Boolean, chatId: String?) {
        _uiState.update { it.copy(isPickerMode = isPicker, chatId = chatId) }
    }
}
