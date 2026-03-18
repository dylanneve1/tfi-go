package com.tfigo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tfigo.app.data.model.*
import com.tfigo.app.ui.components.formatTime
import com.tfigo.app.ui.components.routeColor
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripScreen(
    departure: Departure,
    tripData: TimetableResponse?,
    isLoading: Boolean,
    currentStopId: String?,
    onBack: () -> Unit,
    onStopClicked: (String, String, String) -> Unit // stopRef, stopName, stopType
) {
    val route = departure.serviceNumber
    val destination = departure.destination
    val color = routeColor(route)
    val serviceName = departure.serviceDisplayName ?: departure.operator?.operatorName ?: ""

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("$route → $destination", maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Trip header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 56.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(color)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        route,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        destination,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (serviceName.isNotEmpty()) {
                        Text(
                            serviceName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Divider(modifier = Modifier.padding(horizontal = 16.dp))

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Loading trip details...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                tripData == null || tripData.rows.isNullOrEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.Route,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Trip details unavailable",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Real-time trip data isn't available for this service",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                else -> {
                    val rows = tripData.rows!!
                    val column = tripData.columns?.firstOrNull()
                    val events = column?.events ?: emptyMap()
                    val now = Date()

                    // Find current stop index
                    val currentIdx = remember(rows, events, currentStopId) {
                        var idx = -1
                        rows.forEachIndexed { i, row ->
                            val evt = events[row.rowIndex.toString()] ?: events[i.toString()]
                            if (evt != null) {
                                val t = parseIsoDate(evt.realTimeOfEvent ?: evt.timeOfEvent ?: "")
                                if (t != null && t.before(now)) idx = i
                            }
                            if (currentStopId != null && row.stopReference == currentStopId) {
                                idx = i
                            }
                        }
                        idx
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        itemsIndexed(rows, key = { i, row -> "${row.rowIndex}_$i" }) { i, row ->
                            val evt = events[row.rowIndex.toString()] ?: events[i.toString()]
                            val isPast = i < currentIdx
                            val isCurrent = i == currentIdx
                            val isFirst = i == 0
                            val isLast = i == rows.lastIndex

                            TripStopItem(
                                row = row,
                                event = evt,
                                isPast = isPast,
                                isCurrent = isCurrent,
                                isFirst = isFirst,
                                isLast = isLast,
                                accentColor = color,
                                onClick = {
                                    row.stopReference?.let { ref ->
                                        onStopClicked(
                                            ref,
                                            row.stopName ?: "Unknown",
                                            row.type ?: "BUS_STOP"
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TripStopItem(
    row: TimetableRow,
    event: TimetableEvent?,
    isPast: Boolean,
    isCurrent: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    val dotColor = when {
        isCurrent -> accentColor
        isPast -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        else -> accentColor.copy(alpha = 0.7f)
    }
    val lineColor = when {
        isPast -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        else -> accentColor.copy(alpha = 0.4f)
    }
    val textAlpha = if (isPast) 0.5f else 1f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Timeline column
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(IntrinsicSize.Max),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxHeight()
            ) {
                // Top line
                if (!isFirst) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(12.dp)
                            .background(lineColor)
                    )
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Dot
                Box(
                    modifier = Modifier
                        .size(if (isCurrent) 14.dp else 10.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )

                // Bottom line
                if (!isLast) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(32.dp)
                            .background(lineColor)
                    )
                } else {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        // Stop content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp, top = 4.dp, bottom = 4.dp)
        ) {
            Text(
                row.stopName ?: "Unknown",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            row.shortCode?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = textAlpha)
                )
            }
        }

        // Times
        if (event != null) {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                val scheduled = event.timeOfEvent
                val realtime = event.realTimeOfEvent

                if (realtime != null && realtime != scheduled) {
                    // Real-time available
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00C853))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            formatTime(realtime),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha)
                        )
                    }
                    if (scheduled != null) {
                        val isDelayed = parseIsoDate(realtime)?.after(parseIsoDate(scheduled)) == true
                        Text(
                            formatTime(scheduled),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDelayed) Color(0xFFE65100).copy(alpha = textAlpha)
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = textAlpha),
                            textDecoration = if (isDelayed) TextDecoration.LineThrough else null
                        )
                    }
                } else if (scheduled != null) {
                    Text(
                        formatTime(scheduled),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = textAlpha)
                    )
                }
            }
        }
    }
}

private fun parseIsoDate(isoTime: String): Date? {
    if (isoTime.isBlank()) return null
    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSX",
        "yyyy-MM-dd'T'HH:mm:ssX"
    )
    for (fmt in formats) {
        try {
            return SimpleDateFormat(fmt, Locale.US).parse(isoTime)
        } catch (_: Exception) {}
    }
    return null
}
