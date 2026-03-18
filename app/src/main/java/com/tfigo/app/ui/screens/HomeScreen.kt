package com.tfigo.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tfigo.app.data.model.FavouriteStop
import com.tfigo.app.data.model.LocationResult
import com.tfigo.app.data.model.NearbyStop
import com.tfigo.app.ui.components.StopTypeIcon
import com.tfigo.app.ui.components.formatStopType

@OptIn(ExperimentalMaterial3Api::class)
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
    val focusManager = LocalFocusManager.current
    var searchActive by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onLocationPermissionGranted()
        }
    }

    // Request location on first load
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            onLoadNearby()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.isNotEmpty()) searchActive = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Search bar
        SearchBar(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            onSearch = { focusManager.clearFocus() },
            active = searchActive,
            onActiveChange = { active ->
                searchActive = active
                if (!active) {
                    onSearchQueryChange("")
                    focusManager.clearFocus()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text("Search stops, stations...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        onSearchQueryChange("")
                        searchActive = false
                        focusManager.clearFocus()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            }
        ) {
            // Search results
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
                                        result.shortCode?.let { append("Stop $it · ") }
                                        append(formatStopType(result.type))
                                    }
                                )
                            },
                            leadingContent = { StopTypeIcon(result.type) },
                            trailingContent = {
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            },
                            modifier = Modifier.clickable {
                                onStopSelected(result)
                                onSearchQueryChange("")
                                searchActive = false
                                focusManager.clearFocus()
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
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.NearMe,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "NEARBY",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isLoadingNearby) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                } else {
                    items(nearbyStops, key = { "nearby_${it.id}" }) { stop ->
                        Card(
                            onClick = { onNearbyStopSelected(stop) },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ListItem(
                                headlineContent = {
                                    Text(stop.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                },
                                supportingContent = {
                                    Text(
                                        buildString {
                                            stop.shortCode?.let { append("Stop $it · ") }
                                            append(formatStopType(stop.type))
                                            append(" · ")
                                            if (stop.distanceMeters < 1000) {
                                                append("${stop.distanceMeters.toInt()}m")
                                            } else {
                                                append("%.1fkm".format(stop.distanceMeters / 1000))
                                            }
                                        }
                                    )
                                },
                                leadingContent = { StopTypeIcon(stop.type) },
                                trailingContent = {
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = Color.Transparent
                                )
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }

            // Favourites section
            if (favourites.isNotEmpty()) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "FAVOURITES",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(favourites, key = { "fav_${it.id}" }) { fav ->
                    Card(
                        onClick = { onFavouriteSelected(fav) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ListItem(
                            headlineContent = {
                                Text(fav.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            },
                            supportingContent = {
                                Text(
                                    buildString {
                                        fav.shortCode?.let { append("Stop $it · ") }
                                        append(formatStopType(fav.type))
                                    }
                                )
                            },
                            leadingContent = { StopTypeIcon(fav.type) },
                            trailingContent = {
                                IconButton(onClick = { onRemoveFavourite(fav.id) }) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent
                            )
                        )
                    }
                }
            }

            // Empty state (no favourites, no nearby)
            if (favourites.isEmpty() && nearbyStops.isEmpty() && !isLoadingNearby) {
                item {
                    Box(
                        modifier = Modifier
                            .fillParentMaxSize()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.DirectionsBus,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Find your stop",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Search for a bus stop, train station, or Luas stop to see live departures.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
