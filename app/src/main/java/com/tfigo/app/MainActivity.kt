package com.tfigo.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tfigo.app.ui.MainViewModel
import com.tfigo.app.ui.screens.DeparturesScreen
import com.tfigo.app.ui.screens.HomeScreen
import com.tfigo.app.ui.screens.MapScreen
import com.tfigo.app.ui.screens.TripScreen
import com.tfigo.app.ui.theme.TFIGoTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        viewModel.checkLocationPermission()

        setContent {
            TFIGoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    TFIGoApp(viewModel)
                }
            }
        }
    }

    @Deprecated("Use OnBackPressedDispatcher")
    override fun onBackPressed() {
        if (!viewModel.goBack()) {
            super.onBackPressed()
        }
    }
}

@Composable
fun ErrorFallback(message: String, onRetry: (() -> Unit)? = null) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Something went wrong",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (onRetry != null) {
                Spacer(modifier = Modifier.height(24.dp))
                OutlinedButton(onClick = onRetry) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Try again")
                }
            }
        }
    }
}

@Composable
fun TFIGoApp(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val currentStop by viewModel.currentStop.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val departures by viewModel.departures.collectAsState()
    val isLoadingDepartures by viewModel.isLoadingDepartures.collectAsState()
    val isFavourite by viewModel.isFavourite.collectAsState()
    val lastUpdated by viewModel.lastUpdated.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val favourites by viewModel.favourites.collectAsState()
    val nearbyStops by viewModel.nearbyStops.collectAsState()
    val isLoadingNearby by viewModel.isLoadingNearby.collectAsState()
    val hasLocationPermission by viewModel.hasLocationPermission.collectAsState()
    val alerts by viewModel.alerts.collectAsState()
    val facilities by viewModel.facilities.collectAsState()
    val currentTrip by viewModel.currentTrip.collectAsState()
    val tripData by viewModel.tripData.collectAsState()
    val isLoadingTrip by viewModel.isLoadingTrip.collectAsState()
    val refreshProgress by viewModel.refreshProgress.collectAsState()
    val mapStops by viewModel.mapStops.collectAsState()
    val isLoadingMapStops by viewModel.isLoadingMapStops.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()

    val showBottomNav = currentScreen is MainViewModel.Screen.Home

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                NavigationBar {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Search, contentDescription = null) },
                        label = { Text("Search") },
                        selected = activeTab == 0,
                        onClick = { viewModel.switchTab(0) }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Map, contentDescription = null) },
                        label = { Text("Map") },
                        selected = activeTab == 1,
                        onClick = { viewModel.switchTab(1) }
                    )
                }
            }
        }
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
        ) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    when {
                        targetState is MainViewModel.Screen.Trip ->
                            slideInHorizontally { it } + fadeIn() togetherWith
                                    slideOutHorizontally { -it / 3 } + fadeOut()
                        targetState is MainViewModel.Screen.Departures && initialState is MainViewModel.Screen.Home ->
                            slideInHorizontally { it } + fadeIn() togetherWith
                                    slideOutHorizontally { -it / 3 } + fadeOut()
                        targetState is MainViewModel.Screen.Home ->
                            slideInHorizontally { -it / 3 } + fadeIn() togetherWith
                                    slideOutHorizontally { it } + fadeOut()
                        targetState is MainViewModel.Screen.Departures && initialState is MainViewModel.Screen.Trip ->
                            slideInHorizontally { -it / 3 } + fadeIn() togetherWith
                                    slideOutHorizontally { it } + fadeOut()
                        else -> fadeIn() togetherWith fadeOut()
                    }
                },
                label = "navigation"
            ) { screen ->
                when (screen) {
                    is MainViewModel.Screen.Home -> {
                        if (activeTab == 0) {
                            HomeScreen(
                                searchQuery = searchQuery,
                                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                                searchResults = searchResults,
                                isSearching = isSearching,
                                favourites = favourites,
                                nearbyStops = nearbyStops,
                                isLoadingNearby = isLoadingNearby,
                                hasLocationPermission = hasLocationPermission,
                                onStopSelected = { viewModel.selectStop(it) },
                                onFavouriteSelected = { viewModel.selectFavourite(it) },
                                onNearbyStopSelected = { viewModel.selectNearbyStop(it) },
                                onRemoveFavourite = { viewModel.removeFavourite(it) },
                                onLocationPermissionGranted = { viewModel.onLocationPermissionGranted() },
                                onLoadNearby = { viewModel.loadNearbyStops() }
                            )
                        } else {
                            MapScreen(
                                mapStops = mapStops,
                                isLoadingMapStops = isLoadingMapStops,
                                userLocation = userLocation,
                                onLoadStops = { s, w, n, e -> viewModel.loadMapStops(s, w, n, e) },
                                onStopSelected = { viewModel.selectMapStop(it) }
                            )
                        }
                    }
                    is MainViewModel.Screen.Departures -> {
                        val stop = currentStop
                        if (stop != null) {
                            DeparturesScreen(
                                stopName = stop.name,
                                stopCode = stop.shortCode,
                                stopType = stop.type,
                                departures = departures,
                                isLoading = isLoadingDepartures,
                                isFavourite = isFavourite,
                                lastUpdated = lastUpdated,
                                errorMessage = errorMessage,
                                alerts = alerts,
                                facilities = facilities,
                                refreshProgress = refreshProgress,
                                onBack = { viewModel.goBack() },
                                onRefresh = { viewModel.refreshDepartures() },
                                onToggleFavourite = { viewModel.toggleFavourite() },
                                onClearError = { viewModel.clearError() },
                                onDismissAlerts = { viewModel.dismissAlert(0) },
                                onDepartureClicked = { viewModel.selectDeparture(it) }
                            )
                        }
                    }
                    is MainViewModel.Screen.Trip -> {
                        val trip = currentTrip
                        if (trip != null) {
                            TripScreen(
                                departure = trip,
                                tripData = tripData,
                                isLoading = isLoadingTrip,
                                currentStopId = currentStop?.id,
                                onBack = { viewModel.goBack() },
                                onStopClicked = { ref, name, type ->
                                    viewModel.selectStop(
                                        com.tfigo.app.data.model.LocationResult(
                                            id = ref, name = name, type = type
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
