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
import com.tfigo.app.ui.theme.TFIGoTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
        if (viewModel.currentStop.value != null) {
            viewModel.goBack()
        } else {
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

    AnimatedContent(
        targetState = currentStop != null,
        transitionSpec = {
            if (targetState) {
                slideInHorizontally { it } + fadeIn() togetherWith
                    slideOutHorizontally { -it / 3 } + fadeOut()
            } else {
                slideInHorizontally { -it / 3 } + fadeIn() togetherWith
                    slideOutHorizontally { it } + fadeOut()
            }
        },
        label = "navigation"
    ) { showDepartures ->
        var screenError by remember { mutableStateOf<String?>(null) }

        if (screenError != null) {
            ErrorFallback(
                message = screenError ?: "An unexpected error occurred.",
                onRetry = {
                    screenError = null
                    if (showDepartures) viewModel.refreshDepartures() else viewModel.goBack()
                }
            )
        } else if (showDepartures && currentStop != null) {
            val stop = currentStop!!
            DeparturesScreen(
                stopName = stop.name,
                stopCode = stop.shortCode,
                stopType = stop.type,
                departures = departures,
                isLoading = isLoadingDepartures,
                isFavourite = isFavourite,
                lastUpdated = lastUpdated,
                errorMessage = errorMessage,
                onBack = {
                    try { viewModel.goBack() } catch (e: Exception) {
                        Log.e("TFIGoApp", "Error navigating back", e)
                    }
                },
                onRefresh = {
                    try { viewModel.refreshDepartures() } catch (e: Exception) {
                        Log.e("TFIGoApp", "Error refreshing departures", e)
                        screenError = e.message ?: "Failed to refresh."
                    }
                },
                onToggleFavourite = {
                    try { viewModel.toggleFavourite() } catch (e: Exception) {
                        Log.e("TFIGoApp", "Error toggling favourite", e)
                    }
                },
                onClearError = { viewModel.clearError() }
            )
        } else {
            HomeScreen(
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                searchResults = searchResults,
                isSearching = isSearching,
                favourites = favourites,
                onStopSelected = { result ->
                    try {
                        viewModel.selectStop(result)
                    } catch (e: Exception) {
                        Log.e("TFIGoApp", "Error selecting stop", e)
                        screenError = e.message ?: "Failed to load stop."
                    }
                },
                onFavouriteSelected = { fav ->
                    try {
                        viewModel.selectFavourite(fav)
                    } catch (e: Exception) {
                        Log.e("TFIGoApp", "Error selecting favourite", e)
                        screenError = e.message ?: "Failed to load favourite."
                    }
                },
                onRemoveFavourite = { viewModel.removeFavourite(it) }
            )
        }
    }
}
