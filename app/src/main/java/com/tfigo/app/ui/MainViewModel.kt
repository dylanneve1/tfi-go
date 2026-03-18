package com.tfigo.app.ui

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.tfigo.app.data.model.*
import com.tfigo.app.data.repository.ApiResult
import com.tfigo.app.data.repository.FavouritesStore
import com.tfigo.app.data.repository.TfiRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TfiRepository()
    private val favouritesStore = FavouritesStore(application)
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)

    // Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchResults = MutableStateFlow<List<LocationResult>>(emptyList())
    val searchResults: StateFlow<List<LocationResult>> = _searchResults

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    // Departures
    private val _currentStop = MutableStateFlow<LocationResult?>(null)
    val currentStop: StateFlow<LocationResult?> = _currentStop

    private val _departures = MutableStateFlow<List<Departure>>(emptyList())
    val departures: StateFlow<List<Departure>> = _departures

    private val _isLoadingDepartures = MutableStateFlow(false)
    val isLoadingDepartures: StateFlow<Boolean> = _isLoadingDepartures

    private val _isFavourite = MutableStateFlow(false)
    val isFavourite: StateFlow<Boolean> = _isFavourite

    private val _lastUpdated = MutableStateFlow("")
    val lastUpdated: StateFlow<String> = _lastUpdated

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // Favourites
    val favourites: StateFlow<List<FavouriteStop>> = favouritesStore.favourites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Nearby stops
    private val _nearbyStops = MutableStateFlow<List<NearbyStop>>(emptyList())
    val nearbyStops: StateFlow<List<NearbyStop>> = _nearbyStops

    private val _isLoadingNearby = MutableStateFlow(false)
    val isLoadingNearby: StateFlow<Boolean> = _isLoadingNearby

    private val _hasLocationPermission = MutableStateFlow(false)
    val hasLocationPermission: StateFlow<Boolean> = _hasLocationPermission

    // Service alerts
    private val _alerts = MutableStateFlow<List<String>>(emptyList())
    val alerts: StateFlow<List<String>> = _alerts

    // Stop facilities
    private val _facilities = MutableStateFlow<List<String>>(emptyList())
    val facilities: StateFlow<List<String>> = _facilities

    // Trip details
    private val _currentTrip = MutableStateFlow<Departure?>(null)
    val currentTrip: StateFlow<Departure?> = _currentTrip

    private val _tripData = MutableStateFlow<TimetableResponse?>(null)
    val tripData: StateFlow<TimetableResponse?> = _tripData

    private val _isLoadingTrip = MutableStateFlow(false)
    val isLoadingTrip: StateFlow<Boolean> = _isLoadingTrip

    // Navigation
    sealed class Screen {
        data object Home : Screen()
        data object Departures : Screen()
        data object Trip : Screen()
    }

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen

    private val _activeTab = MutableStateFlow(0) // 0=Search, 1=Map
    val activeTab: StateFlow<Int> = _activeTab

    // Map stops
    private val _mapStops = MutableStateFlow<List<LocationResult>>(emptyList())
    val mapStops: StateFlow<List<LocationResult>> = _mapStops

    private val _isLoadingMapStops = MutableStateFlow(false)
    val isLoadingMapStops: StateFlow<Boolean> = _isLoadingMapStops

    private val _userLocation = MutableStateFlow<Coordinate?>(null)
    val userLocation: StateFlow<Coordinate?> = _userLocation

    // Refresh progress
    private val _refreshProgress = MutableStateFlow(1f)
    val refreshProgress: StateFlow<Float> = _refreshProgress

    // Auto-refresh
    private var refreshJob: Job? = null
    private var searchJob: Job? = null
    private var progressJob: Job? = null

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()

        if (query.length < 2) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        searchJob = viewModelScope.launch {
            delay(300)
            _isSearching.value = true
            when (val result = repository.searchStops(query)) {
                is ApiResult.Success -> _searchResults.value = result.data
                is ApiResult.Error -> {
                    _searchResults.value = emptyList()
                    _errorMessage.value = result.message
                }
            }
            _isSearching.value = false
        }
    }

    fun selectStop(stop: LocationResult) {
        _currentStop.value = stop
        _currentScreen.value = Screen.Departures
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        _errorMessage.value = null
        _alerts.value = emptyList()
        _facilities.value = emptyList()
        loadDepartures(stop)
        loadAlerts(stop.id)
        loadFacilities(stop.id)
        checkFavourite(stop.id)
        startAutoRefresh()
    }

    fun selectFavourite(fav: FavouriteStop) {
        val location = LocationResult(
            id = fav.id,
            name = fav.name,
            type = fav.type,
            shortCode = fav.shortCode,
            coordinate = Coordinate(fav.latitude, fav.longitude)
        )
        selectStop(location)
    }

    fun selectNearbyStop(stop: NearbyStop) {
        val location = LocationResult(
            id = stop.id,
            name = stop.name,
            type = stop.type,
            shortCode = stop.shortCode,
            coordinate = Coordinate(stop.latitude, stop.longitude)
        )
        selectStop(location)
    }

    fun selectMapStop(stop: LocationResult) {
        selectStop(stop)
    }

    fun goBack(): Boolean {
        return when (_currentScreen.value) {
            is Screen.Trip -> {
                _currentTrip.value = null
                _tripData.value = null
                _currentScreen.value = Screen.Departures
                true
            }
            is Screen.Departures -> {
                _currentStop.value = null
                _departures.value = emptyList()
                _lastUpdated.value = ""
                _errorMessage.value = null
                _alerts.value = emptyList()
                _facilities.value = emptyList()
                stopAutoRefresh()
                _currentScreen.value = Screen.Home
                true
            }
            else -> false
        }
    }

    fun refreshDepartures() {
        _currentStop.value?.let { loadDepartures(it) }
        resetRefreshProgress()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun toggleFavourite() {
        val stop = _currentStop.value ?: return
        viewModelScope.launch {
            if (_isFavourite.value) {
                favouritesStore.removeFavourite(stop.id)
                _isFavourite.value = false
            } else {
                favouritesStore.addFavourite(
                    FavouriteStop(
                        id = stop.id,
                        name = stop.name,
                        shortCode = stop.shortCode,
                        type = stop.type,
                        latitude = stop.coordinate?.latitude ?: 0.0,
                        longitude = stop.coordinate?.longitude ?: 0.0
                    )
                )
                _isFavourite.value = true
            }
        }
    }

    fun removeFavourite(stopId: String) {
        viewModelScope.launch {
            favouritesStore.removeFavourite(stopId)
        }
    }

    fun switchTab(tab: Int) {
        _activeTab.value = tab
    }

    fun dismissAlert(index: Int) {
        val current = _alerts.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _alerts.value = current
        }
    }

    // Trip details
    fun selectDeparture(departure: Departure) {
        _currentTrip.value = departure
        _tripData.value = null
        _currentScreen.value = Screen.Trip
        loadTripDetails(departure)
    }

    // Location
    fun checkLocationPermission() {
        val app = getApplication<Application>()
        _hasLocationPermission.value = ContextCompat.checkSelfPermission(
            app, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun onLocationPermissionGranted() {
        _hasLocationPermission.value = true
        loadNearbyStops()
    }

    fun loadNearbyStops() {
        val app = getApplication<Application>()
        if (ContextCompat.checkSelfPermission(
                app, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        _isLoadingNearby.value = true
        try {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).addOnSuccessListener { location: Location? ->
                location?.let {
                    _userLocation.value = Coordinate(it.latitude, it.longitude)
                    viewModelScope.launch {
                        when (val result = repository.getNearbyStops(it.latitude, it.longitude)) {
                            is ApiResult.Success -> _nearbyStops.value = result.data
                            is ApiResult.Error -> { /* silently fail */ }
                        }
                        _isLoadingNearby.value = false
                    }
                } ?: run {
                    _isLoadingNearby.value = false
                }
            }.addOnFailureListener {
                _isLoadingNearby.value = false
            }
        } catch (e: SecurityException) {
            _isLoadingNearby.value = false
        }
    }

    fun loadMapStops(south: Double, west: Double, north: Double, east: Double) {
        viewModelScope.launch {
            _isLoadingMapStops.value = true
            when (val result = repository.getMapStops(south, west, north, east)) {
                is ApiResult.Success -> _mapStops.value = result.data
                is ApiResult.Error -> { /* silently fail */ }
            }
            _isLoadingMapStops.value = false
        }
    }

    private fun loadDepartures(stop: LocationResult) {
        viewModelScope.launch {
            _isLoadingDepartures.value = true
            _errorMessage.value = null
            when (val result = repository.getDepartures(stop)) {
                is ApiResult.Success -> {
                    _departures.value = result.data
                    _lastUpdated.value = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                }
                is ApiResult.Error -> {
                    if (_departures.value.isEmpty()) {
                        _errorMessage.value = result.message
                    }
                }
            }
            _isLoadingDepartures.value = false
        }
    }

    private fun loadAlerts(stopId: String) {
        viewModelScope.launch {
            when (val result = repository.getAlerts(stopId)) {
                is ApiResult.Success -> _alerts.value = result.data
                is ApiResult.Error -> { /* silently fail */ }
            }
        }
    }

    private fun loadFacilities(stopId: String) {
        viewModelScope.launch {
            when (val result = repository.getFacilities(stopId)) {
                is ApiResult.Success -> _facilities.value = result.data
                is ApiResult.Error -> { /* silently fail */ }
            }
        }
    }

    private fun loadTripDetails(departure: Departure) {
        viewModelScope.launch {
            _isLoadingTrip.value = true
            when (val result = repository.getTripDetails(departure)) {
                is ApiResult.Success -> _tripData.value = result.data
                is ApiResult.Error -> _errorMessage.value = result.message
            }
            _isLoadingTrip.value = false
        }
    }

    private fun checkFavourite(stopId: String) {
        viewModelScope.launch {
            _isFavourite.value = favouritesStore.isFavourite(stopId)
        }
    }

    private fun startAutoRefresh() {
        stopAutoRefresh()
        resetRefreshProgress()
        refreshJob = viewModelScope.launch {
            while (isActive) {
                delay(30_000)
                _currentStop.value?.let { loadDepartures(it) }
                resetRefreshProgress()
            }
        }
    }

    private fun resetRefreshProgress() {
        progressJob?.cancel()
        _refreshProgress.value = 1f
        progressJob = viewModelScope.launch {
            val steps = 100
            for (i in steps downTo 0) {
                _refreshProgress.value = i / steps.toFloat()
                delay(300) // 30s / 100 steps = 300ms per step
            }
        }
    }

    private fun stopAutoRefresh() {
        refreshJob?.cancel()
        refreshJob = null
        progressJob?.cancel()
        progressJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoRefresh()
    }
}
