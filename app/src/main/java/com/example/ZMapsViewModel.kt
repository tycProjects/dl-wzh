package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppLanguage
import com.example.data.DirectionsRepository
import com.example.data.LocationUiState
import com.example.data.PlacesRepository
import com.example.data.PreferencesRepository
import com.example.data.RouteMode
import com.example.data.ZPlace
import com.example.data.ZRoute
import com.example.location.ZLocationClient
import com.example.ui.theme.ZMapsAccent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLng

enum class ScreenTab {
    MAP, SEARCH, SETTINGS
}

sealed interface RouteUiState {
    data object Idle : RouteUiState
    data object Loading : RouteUiState
    data class Success(val route: ZRoute) : RouteUiState
    data class Error(val message: String) : RouteUiState
}

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Loading : SearchUiState
    data class Success(val results: List<ZPlace>) : SearchUiState
    data object Empty : SearchUiState
    data class Error(val message: String) : SearchUiState
}

class ZMapsViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesRepository = PreferencesRepository(application)
    private val locationClient = ZLocationClient(application)
    private val directionsRepository = DirectionsRepository()
    private val placesRepository = PlacesRepository()

    val accentFlow: StateFlow<ZMapsAccent> = preferencesRepository.accentFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ZMapsAccent.GREEN)

    val languageFlow: StateFlow<AppLanguage> = preferencesRepository.languageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppLanguage.ENGLISH)

    val recentSearchesFlow: StateFlow<List<String>> = preferencesRepository.recentSearchesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarksFlow: StateFlow<List<ZPlace>> = preferencesRepository.bookmarksFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentTab = MutableStateFlow(ScreenTab.MAP)
    val currentTab: StateFlow<ScreenTab> = _currentTab.asStateFlow()

    private val _locationState = MutableStateFlow<LocationUiState>(LocationUiState.Idle)
    val locationState: StateFlow<LocationUiState> = _locationState.asStateFlow()

    private val _userLocation = MutableStateFlow<LatLng?>(null)
    val userLocation: StateFlow<LatLng?> = _userLocation.asStateFlow()

    private val _manualStartLocation = MutableStateFlow<LatLng?>(null)
    val manualStartLocation: StateFlow<LatLng?> = _manualStartLocation.asStateFlow()

    private val _selectedDestination = MutableStateFlow<ZPlace?>(null)
    val selectedDestination: StateFlow<ZPlace?> = _selectedDestination.asStateFlow()

    private val _selectedRouteMode = MutableStateFlow(RouteMode.DRIVING)
    val selectedRouteMode: StateFlow<RouteMode> = _selectedRouteMode.asStateFlow()

    private val _routeState = MutableStateFlow<RouteUiState>(RouteUiState.Idle)
    val routeState: StateFlow<RouteUiState> = _routeState.asStateFlow()

    private val _isNavigating = MutableStateFlow(false)
    val isNavigating: StateFlow<Boolean> = _isNavigating.asStateFlow()

    private val _activeStepIndex = MutableStateFlow(0)
    val activeStepIndex: StateFlow<Int> = _activeStepIndex.asStateFlow()

    private val _searchUiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val searchUiState: StateFlow<SearchUiState> = _searchUiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private var searchJob: Job? = null
    private var debounceJob: Job? = null

    init {
        // Attempt initial location acquisition if permission is already granted
        if (locationClient.hasLocationPermission()) {
            fetchLocation()
        }
    }

    fun switchTab(tab: ScreenTab) {
        _currentTab.value = tab
    }

    fun fetchLocation() {
        viewModelScope.launch {
            _locationState.value = LocationUiState.Locating
            val state = locationClient.getCurrentLocation()
            _locationState.value = state
            if (state is LocationUiState.Success) {
                _userLocation.value = state.latLng
                // Clear manual start location if real GPS fix is acquired
                _manualStartLocation.value = null
            }
        }
    }

    fun setManualStartLocation(latLng: LatLng) {
        _manualStartLocation.value = latLng
        // If a destination is already chosen, recalculate route with this start point
        if (_selectedDestination.value != null) {
            calculateRoute()
        }
    }

    fun selectDestination(place: ZPlace, navigateToMap: Boolean = true) {
        _selectedDestination.value = place
        _routeState.value = RouteUiState.Idle
        _isNavigating.value = false
        _activeStepIndex.value = 0

        viewModelScope.launch {
            preferencesRepository.addRecentSearch(place.name)
        }
        if (navigateToMap) {
            _currentTab.value = ScreenTab.MAP
        }
    }

    fun clearDestination() {
        _selectedDestination.value = null
        _routeState.value = RouteUiState.Idle
        _isNavigating.value = false
        _activeStepIndex.value = 0
    }

    fun setRouteMode(mode: RouteMode) {
        _selectedRouteMode.value = mode
        if (_selectedDestination.value != null && (_userLocation.value != null || _manualStartLocation.value != null)) {
            calculateRoute()
        }
    }

    fun calculateRoute() {
        val destination = _selectedDestination.value ?: return
        val origin = _userLocation.value ?: _manualStartLocation.value

        if (origin == null) {
            _routeState.value = RouteUiState.Error(
                "Starting point needed. Grant GPS permission or set a starting point manually."
            )
            return
        }

        viewModelScope.launch {
            _routeState.value = RouteUiState.Loading
            val mode = _selectedRouteMode.value
            val result = directionsRepository.calculateRoute(origin, destination.latLng, mode)
            result.onSuccess { route ->
                _routeState.value = RouteUiState.Success(route)
            }.onFailure { error ->
                _routeState.value = RouteUiState.Error(
                    error.localizedMessage ?: "Could not compute route. Verify your internet connection."
                )
            }
        }
    }

    fun calculateDrivingRoute() {
        _selectedRouteMode.value = RouteMode.DRIVING
        calculateRoute()
    }

    fun startNavigation() {
        if (_routeState.value is RouteUiState.Success) {
            _isNavigating.value = true
            _activeStepIndex.value = 0
            _currentTab.value = ScreenTab.MAP
        }
    }

    fun nextManeuverStep() {
        val current = _routeState.value as? RouteUiState.Success ?: return
        if (_activeStepIndex.value < current.route.steps.lastIndex) {
            _activeStepIndex.value += 1
        }
    }

    fun previousManeuverStep() {
        if (_activeStepIndex.value > 0) {
            _activeStepIndex.value -= 1
        }
    }

    fun clearManualStartLocation() {
        _manualStartLocation.value = null
        if (_selectedDestination.value != null && _userLocation.value != null) {
            calculateRoute()
        }
    }

    fun selectCategory(category: String) {
        if (_selectedCategory.value == category) {
            _selectedCategory.value = null
            _searchQuery.value = ""
            _searchUiState.value = SearchUiState.Idle
        } else {
            _selectedCategory.value = category
            _searchQuery.value = category
            performSearch(category)
        }
    }

    fun endNavigation() {
        _isNavigating.value = false
        _activeStepIndex.value = 0
    }

    fun clearRoute() {
        _routeState.value = RouteUiState.Idle
        _isNavigating.value = false
        _activeStepIndex.value = 0
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query

        debounceJob?.cancel()
        if (query.isBlank()) {
            searchJob?.cancel()
            _searchUiState.value = SearchUiState.Idle
            return
        }

        // Live suggestion search debounced by 600ms to respect Nominatim policy
        if (query.trim().length >= 3) {
            debounceJob = viewModelScope.launch {
                delay(600)
                performSearchInternal(query.trim(), saveToHistory = false)
            }
        }
    }

    fun performSearch(query: String) {
        debounceJob?.cancel()
        if (query.isBlank()) return
        _searchQuery.value = query
        performSearchInternal(query.trim(), saveToHistory = true)
    }

    private fun performSearchInternal(cleanQuery: String, saveToHistory: Boolean) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _searchUiState.value = SearchUiState.Loading
            if (saveToHistory) {
                preferencesRepository.addRecentSearch(cleanQuery)
            }

            val result = placesRepository.searchPlaces(cleanQuery)
            result.onSuccess { list ->
                if (list.isEmpty()) {
                    _searchUiState.value = SearchUiState.Empty
                } else {
                    _searchUiState.value = SearchUiState.Success(list)
                }
            }.onFailure { err ->
                _searchUiState.value = SearchUiState.Error(
                    err.localizedMessage ?: "Network error searching places. Please check internet connection."
                )
            }
        }
    }

    fun retrySearch() {
        val q = _searchQuery.value
        if (q.isNotBlank()) {
            performSearch(q)
        }
    }

    fun toggleBookmark(place: ZPlace) {
        viewModelScope.launch {
            preferencesRepository.toggleBookmark(place)
        }
    }

    fun setAccent(accent: ZMapsAccent) {
        viewModelScope.launch {
            preferencesRepository.setAccent(accent)
        }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            preferencesRepository.setLanguage(language)
        }
    }

    fun clearSearchHistory() {
        viewModelScope.launch {
            preferencesRepository.clearRecentSearches()
        }
    }
}
