package com.tfigo.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.tfigo.app.data.model.Coordinate
import com.tfigo.app.data.model.LocationResult
import com.tfigo.app.ui.components.StopTypeIcon
import com.tfigo.app.ui.components.formatStopType
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun MapScreen(
    mapStops: List<LocationResult>,
    isLoadingMapStops: Boolean,
    userLocation: Coordinate?,
    onLoadStops: (south: Double, west: Double, north: Double, east: Double) -> Unit,
    onStopSelected: (LocationResult) -> Unit
) {
    val context = LocalContext.current
    var selectedStop by remember { mutableStateOf<LocationResult?>(null) }

    // Configure OSMDroid
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Map view
        AndroidView(
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    controller.setZoom(14.0)

                    val startPoint = if (userLocation != null) {
                        GeoPoint(userLocation.latitude, userLocation.longitude)
                    } else {
                        GeoPoint(53.3498, -6.2603) // Dublin center
                    }
                    controller.setCenter(startPoint)

                    // Load stops when map moves
                    addOnFirstLayoutListener { _, _, _, _, _ ->
                        val bounds = boundingBox
                        onLoadStops(
                            bounds.latSouth,
                            bounds.lonWest,
                            bounds.latNorth,
                            bounds.lonEast
                        )
                    }

                    setOnTouchListener { v, _ ->
                        v.performClick()
                        false
                    }
                }
            },
            update = { mapView ->
                // Update markers when stops change
                mapView.overlays.removeAll { it is Marker }

                // User location marker
                if (userLocation != null) {
                    val userMarker = Marker(mapView).apply {
                        position = GeoPoint(userLocation.latitude, userLocation.longitude)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                        title = "You are here"
                        icon = context.getDrawable(android.R.drawable.presence_online)
                    }
                    mapView.overlays.add(userMarker)
                }

                // Stop markers
                mapStops.forEach { stop ->
                    stop.coordinate?.let { coord ->
                        val marker = Marker(mapView).apply {
                            position = GeoPoint(coord.latitude, coord.longitude)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = stop.name
                            snippet = formatStopType(stop.type)
                            setOnMarkerClickListener { _, _ ->
                                selectedStop = stop
                                true
                            }
                        }
                        mapView.overlays.add(marker)
                    }
                }

                mapView.invalidate()

                // Reload stops on map move
                mapView.addMapListener(object : org.osmdroid.events.MapListener {
                    override fun onScroll(event: org.osmdroid.events.ScrollEvent?): Boolean {
                        val bounds = mapView.boundingBox
                        onLoadStops(
                            bounds.latSouth,
                            bounds.lonWest,
                            bounds.latNorth,
                            bounds.lonEast
                        )
                        return false
                    }
                    override fun onZoom(event: org.osmdroid.events.ZoomEvent?): Boolean {
                        val bounds = mapView.boundingBox
                        onLoadStops(
                            bounds.latSouth,
                            bounds.lonWest,
                            bounds.latNorth,
                            bounds.lonEast
                        )
                        return false
                    }
                })
            },
            modifier = Modifier.fillMaxSize()
        )

        // Loading indicator
        if (isLoadingMapStops) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )
        }

        // Locate me button
        if (userLocation != null) {
            FloatingActionButton(
                onClick = { /* Map will re-center */ },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .padding(bottom = 8.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "My location")
            }
        }

        // Bottom sheet for selected stop
        selectedStop?.let { stop ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .padding(bottom = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
                            }
                        )
                    },
                    leadingContent = { StopTypeIcon(stop.type) },
                    trailingContent = {
                        FilledTonalButton(onClick = { onStopSelected(stop) }) {
                            Icon(
                                Icons.Default.DepartureBoard,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Departures")
                        }
                    },
                    modifier = Modifier.clickable { onStopSelected(stop) }
                )
            }
        }
    }
}
