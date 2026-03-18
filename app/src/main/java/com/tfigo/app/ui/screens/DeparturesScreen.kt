package com.tfigo.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.tfigo.app.data.model.Departure
import com.tfigo.app.ui.components.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DeparturesScreen(
    stopName: String,
    stopCode: String?,
    stopType: String,
    departures: List<Departure>,
    isLoading: Boolean,
    isFavourite: Boolean,
    lastUpdated: String,
    errorMessage: String?,
    alerts: List<String>,
    facilities: List<String>,
    refreshProgress: Float,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onToggleFavourite: () -> Unit,
    onClearError: () -> Unit,
    onDismissAlerts: () -> Unit,
    onDepartureClicked: (Departure) -> Unit
) {
    var activeFilter by remember { mutableStateOf<String?>(null) }
    val services = remember(departures) {
        departures.map { it.serviceNumber }.distinct().sorted()
    }
    val filteredDepartures = remember(departures, activeFilter) {
        if (activeFilter == null) departures
        else departures.filter { it.serviceNumber == activeFilter }
    }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    var toolbarExpanded by remember { mutableStateOf(true) }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(message = it, duration = SnackbarDuration.Short)
            onClearError()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stopName, maxLines = 1)
                        stopCode?.let {
                            Text(
                                "Stop $it \u00B7 ${formatStopType(stopType)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onToggleFavourite) {
                        Icon(
                            if (isFavourite) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (isFavourite) "Remove from favourites" else "Add to favourites"
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            HorizontalFloatingToolbar(
                expanded = toolbarExpanded,
                floatingActionButton = {
                    FloatingToolbarDefaults.VibrantFloatingActionButton(
                        onClick = onRefresh
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                content = {}
            )
        },
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Refresh progress bar
            RefreshProgressBar(progress = refreshProgress)

            // Facilities chips
            FacilitiesRow(facilities = facilities)

            // Alerts banner
            AlertsBanner(alerts = alerts, onDismiss = onDismissAlerts)

            // Service filter chips
            if (services.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(selected = activeFilter == null, onClick = { activeFilter = null }, label = { Text("All") })
                    services.forEach { service ->
                        FilterChip(
                            selected = activeFilter == service,
                            onClick = { activeFilter = if (activeFilter == service) null else service },
                            label = { Text(service) }
                        )
                    }
                }
            }

            when {
                isLoading && departures.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Loading departures...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                filteredDepartures.isEmpty() && !isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No departures", style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("No upcoming departures found.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(24.dp))
                            OutlinedButton(onClick = onRefresh) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Try again")
                            }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isLoading) {
                            item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) }
                        }
                        items(filteredDepartures, key = { "${it.serviceID}_${it.scheduledDeparture}_${it.destination}" }) { departure ->
                            DepartureCard(departure = departure, onClick = { onDepartureClicked(departure) })
                        }
                        item { Spacer(modifier = Modifier.height(88.dp)) }
                    }
                }
            }
        }
    }
}
