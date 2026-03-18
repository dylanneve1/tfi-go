package com.tfigo.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tfigo.app.data.model.FavouriteStop
import com.tfigo.app.data.model.LocationResult
import com.tfigo.app.data.model.NearbyStop
import com.tfigo.app.ui.components.SectionHeader
import com.tfigo.app.ui.components.StopTypeIcon
import com.tfigo.app.ui.components.formatStopType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchResults: List<LocationResult>,
    isSearching: Boolean,
    favourites: List<FavouriteStop>,
    nearbyStops: List<NearbyStop>,
    isLoadingNearby: Boolean,
    hasLocationPermission: Boolean,
    onStopSelected: (LocationResult) -> Unit,
    onFavouriteSelected: (FavouriteStop) -> Unit,
    onNearbyStopSelected: (NearbyStop) -> Unit,
    onRemoveFavourite: (String) -> Unit,
    onLocationPermissionGranted: () -> Unit,
    onLoadNearby: () -> Unit
) {
    val searchBarState = rememberSearchBarState()
    val textFieldState = rememberTextFieldState()
    val scope = rememberCoroutineScope()

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onLocationPermissionGranted()
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) onLoadNearby()
        else locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    // Sync textFieldState changes to ViewModel
    LaunchedEffect(textFieldState.text) {
        onSearchQueryChange(textFieldState.text.toString())
    }

    val inputField = @Composable {
        SearchBarDefaults.InputField(
            searchBarState = searchBarState,
            textFieldState = textFieldState,
            onSearch = { scope.launch { searchBarState.animateToCollapsed() } },
            placeholder = {
                Text(modifier = Modifier.clearAndSetSemantics {}, text = "Search stops, stations...")
            },
            leadingIcon = {
                if (searchBarState.currentValue == SearchBarValue.Expanded) {
                    IconButton(
                        onClick = {
                            scope.launch {
                                searchBarState.animateToCollapsed()
                                textFieldState.edit { replace(0, length, "") }
                            }
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                } else {
                    Icon(Icons.Default.Search, contentDescription = null)
                }
            },
            trailingIcon = {
                if (textFieldState.text.isNotEmpty()) {
                    IconButton(onClick = {
                        textFieldState.edit { replace(0, length, "") }
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Collapsed search bar
        SearchBar(
            state = searchBarState,
            inputField = inputField,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        // Expanded full-screen search
        ExpandedFullScreenSearchBar(
            state = searchBarState,
            inputField = inputField,
        ) {
            if (isSearching) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (searchResults.isEmpty() && searchQuery.length >= 2) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No stops found", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(searchResults.take(15)) { result ->
                        ListItem(
                            headlineContent = {
                                Text(result.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                            supportingContent = {
                                Text(
                                    buildString {
                                        result.shortCode?.let { append("Stop $it \u00B7 ") }
                                        append(formatStopType(result.type))
                                    }
                                )
                            },
                            leadingContent = { StopTypeIcon(result.type) },
                            trailingContent = {
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                            },
                            modifier = Modifier.clickable {
                                onStopSelected(result)
                                scope.launch {
                                    searchBarState.animateToCollapsed()
                                    textFieldState.edit { replace(0, length, "") }
                                }
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Nearby stops section
            if (hasLocationPermission && (isLoadingNearby || nearbyStops.isNotEmpty())) {
                item { SectionHeader(icon = Icons.Default.NearMe, title = "NEARBY") }
                if (isLoadingNearby) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                } else {
                    items(nearbyStops, key = { "nearby_${it.id}" }) { stop ->
                        Surface(
                            onClick = { onNearbyStopSelected(stop) },
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ListItem(
                                headlineContent = { Text(stop.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                supportingContent = {
                                    Text(buildString {
                                        stop.shortCode?.let { append("Stop $it \u00B7 ") }
                                        append(formatStopType(stop.type))
                                        append(" \u00B7 ")
                                        if (stop.distanceMeters < 1000) append("${stop.distanceMeters.toInt()}m")
                                        else append("%.1fkm".format(stop.distanceMeters / 1000))
                                    })
                                },
                                leadingContent = { StopTypeIcon(stop.type) },
                                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            // Favourites section
            if (favourites.isNotEmpty()) {
                item { SectionHeader(icon = Icons.Default.Star, title = "FAVOURITES") }
                items(favourites, key = { "fav_${it.id}" }) { fav ->
                    Surface(
                        onClick = { onFavouriteSelected(fav) },
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ListItem(
                            headlineContent = { Text(fav.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            supportingContent = {
                                Text(buildString {
                                    fav.shortCode?.let { append("Stop $it \u00B7 ") }
                                    append(formatStopType(fav.type))
                                })
                            },
                            leadingContent = { StopTypeIcon(fav.type) },
                            trailingContent = {
                                IconButton(onClick = { onRemoveFavourite(fav.id) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(18.dp))
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }

            // Empty state
            if (favourites.isEmpty() && nearbyStops.isEmpty() && !isLoadingNearby) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize().padding(48.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.DirectionsBus, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Find your stop", style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Search for a bus stop, train station, or Luas stop to see live departures.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 24.dp))
                        }
                    }
                }
            }
        }
    }
}
