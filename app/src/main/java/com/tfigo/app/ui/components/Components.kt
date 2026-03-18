package com.tfigo.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tfigo.app.data.model.Departure
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StopTypeIcon(type: String) {
    val (icon, bgColor, fgColor) = when {
        type.contains("BUS") || type.contains("COACH") -> Triple(Icons.Default.DirectionsBus, Color(0xFFE8F0FE), Color(0xFF1967D2))
        type.contains("TRAIN") -> Triple(Icons.Default.Train, Color(0xFFFCE8E6), Color(0xFFC5221F))
        type.contains("TRAM") -> Triple(Icons.Default.Tram, Color(0xFFE6F4EA), Color(0xFF137333))
        type.contains("FERRY") -> Triple(Icons.Default.DirectionsBoat, Color(0xFFE0F7FA), Color(0xFF007B83))
        else -> Triple(Icons.Default.Place, Color(0xFFFEF7E0), Color(0xFFEA8600))
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = fgColor, modifier = Modifier.size(22.dp))
    }
}

fun formatStopType(type: String): String = when (type) {
    "BUS_STOP" -> "Bus Stop"
    "COACH_STOP" -> "Coach Stop"
    "TRAIN_STATION" -> "Train Station"
    "TRAM_STOP", "TRAM_STOP_AREA" -> "Luas Stop"
    "FERRY_PORT" -> "Ferry Port"
    "AIR_PORT" -> "Airport"
    "LOCALITY" -> "Area"
    else -> type.replace("_", " ").lowercase()
        .replaceFirstChar { it.uppercase() }
}

fun routeColor(route: String): Color {
    val known = mapOf(
        "DART" to Color(0xFF00A651),
        "dart" to Color(0xFF00A651),
        "Luas Green" to Color(0xFF00A651),
        "Luas Red" to Color(0xFFE53935),
        "Green" to Color(0xFF00A651),
        "Red" to Color(0xFFE53935),
    )
    known[route]?.let { return it }

    var hash = 0
    for (c in route) {
        hash = c.code + ((hash shl 5) - hash)
    }
    val hue = (kotlin.math.abs(hash) % 360).toFloat()
    return Color.hsl(hue, 0.55f, 0.42f)
}

fun badgeColor(mode: String): Color = when {
    mode.contains("TRAIN") -> Color(0xFFC5221F)
    mode.contains("TRAM") -> Color(0xFF137333)
    mode.contains("COACH") -> Color(0xFF9334E6)
    mode.contains("FERRY") -> Color(0xFF007B83)
    else -> Color(0xFF1967D2)
}

@Composable
fun DepartureCard(departure: Departure, onClick: (() -> Unit)? = null) {
    val mode = departure.transportMode
    val color = routeColor(departure.serviceNumber)

    val timeStr = departure.realTimeDeparture ?: departure.scheduledDeparture
    val isLive = departure.realTimeDeparture != null
    val mins = timeStr?.let { getMinutesUntil(it) } ?: 0
    val isDue = mins <= 1
    val delayMins = if (isLive && departure.scheduledDeparture != null) {
        val rtMins = departure.realTimeDeparture?.let { getMinutesUntil(it) } ?: 0
        val schedMins = getMinutesUntil(departure.scheduledDeparture)
        rtMins - schedMins
    } else 0

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Route badge
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 52.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    departure.serviceNumber,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1
                )
            }

            // Destination & operator
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    departure.destination,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    departure.operator?.operatorName?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (isLive && delayMins > 1) {
                        departure.operator?.operatorName?.let {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("·", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            "${delayMins} min late",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE65100),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Time + chevron
            if (departure.cancelled) {
                Text(
                    "Cancelled",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isLive) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00C853))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            if (isDue) "Due" else "$mins min",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDue) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary
                        )
                    }
                    departure.scheduledDeparture?.let { sched ->
                        Text(
                            formatTime(sched),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (onClick != null) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// Alerts banner
@Composable
fun AlertsBanner(
    alerts: List<String>,
    onDismiss: () -> Unit
) {
    if (alerts.isEmpty()) return

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFE65100),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                alerts.joinToString(" • "),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFE65100),
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Dismiss",
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFFE65100)
                )
            }
        }
    }
}

// Facilities row
@Composable
fun FacilitiesRow(facilities: List<String>) {
    if (facilities.isEmpty()) return

    val facilityInfo = mapOf(
        "SHELTER" to Pair(Icons.Default.NightShelter, "Shelter"),
        "TOILETS" to Pair(Icons.Default.Wc, "Toilets"),
        "TICKET_OFFICE" to Pair(Icons.Default.ConfirmationNumber, "Tickets"),
        "CAR_PARK" to Pair(Icons.Default.LocalParking, "Parking"),
        "WHEELCHAIR_ACCESS" to Pair(Icons.Default.Accessible, "Accessible"),
        "BIKE_PARK" to Pair(Icons.Default.PedalBike, "Bike Park"),
        "WAITING_ROOM" to Pair(Icons.Default.Weekend, "Waiting Room"),
        "WIFI" to Pair(Icons.Default.Wifi, "WiFi"),
        "ATM" to Pair(Icons.Default.Atm, "ATM"),
        "SHOP" to Pair(Icons.Default.ShoppingBag, "Shop"),
        "CAFE" to Pair(Icons.Default.Coffee, "Cafe"),
        "LIFT" to Pair(Icons.Default.Elevator, "Lift"),
        "TAXI_RANK" to Pair(Icons.Default.LocalTaxi, "Taxi"),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        facilities.forEach { facility ->
            val info = facilityInfo[facility]
            if (info != null) {
                AssistChip(
                    onClick = {},
                    label = { Text(info.second, style = MaterialTheme.typography.labelSmall) },
                    leadingIcon = {
                        Icon(
                            info.first,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.height(28.dp)
                )
            }
        }
    }
}

// Refresh progress bar
@Composable
fun RefreshProgressBar(progress: Float) {
    @Suppress("DEPRECATION")
    LinearProgressIndicator(
        progress = progress,
        modifier = Modifier
            .fillMaxWidth()
            .height(2.dp),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
    )
}

private fun getMinutesUntil(isoTime: String): Int {
    return try {
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",
            "yyyy-MM-dd'T'HH:mm:ssX"
        )
        var date: Date? = null
        for (fmt in formats) {
            try {
                date = SimpleDateFormat(fmt, Locale.US).parse(isoTime)
                if (date != null) break
            } catch (_: Exception) {}
        }
        date?.let { ((it.time - System.currentTimeMillis()) / 60000).toInt() } ?: 0
    } catch (e: Exception) { 0 }
}

fun formatTime(isoTime: String): String {
    return try {
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSSX",
            "yyyy-MM-dd'T'HH:mm:ssX"
        )
        var date: Date? = null
        for (fmt in formats) {
            try {
                date = SimpleDateFormat(fmt, Locale.US).parse(isoTime)
                if (date != null) break
            } catch (_: Exception) {}
        }
        date?.let {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(it)
        } ?: ""
    } catch (e: Exception) { "" }
}
