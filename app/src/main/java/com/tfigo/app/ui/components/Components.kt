package com.tfigo.app.ui.components

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tfigo.app.data.model.Departure
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

// ===== Stop Type Badge (solid colored rounded square, matching HTML) =====

private val BUS_COLOR = Color(0xFF00B74F)
private val TRAIN_COLOR = Color(0xFF5C6BC0)
private val TRAM_COLOR = Color(0xFFAB47BC)
private val FERRY_COLOR = Color(0xFF29B6F6)
private val COACH_COLOR = Color(0xFFFF7043)
private val DEFAULT_COLOR = Color(0xFF78909C)

@Composable
fun StopTypeIcon(type: String) {
    val (icon, bgColor) = when {
        type.contains("BUS") -> Pair(Icons.Default.DirectionsBus, BUS_COLOR)
        type.contains("COACH") -> Pair(Icons.Default.AirportShuttle, COACH_COLOR)
        type.contains("TRAIN") -> Pair(Icons.Default.Train, TRAIN_COLOR)
        type.contains("TRAM") -> Pair(Icons.Default.Tram, TRAM_COLOR)
        type.contains("FERRY") -> Pair(Icons.Default.DirectionsBoat, FERRY_COLOR)
        else -> Pair(Icons.Default.Place, DEFAULT_COLOR)
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
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

// Hash-based route color (matches HTML routeColor function)
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
    val hue = (abs(hash) % 360).toFloat()
    return Color.hsl(hue, 0.55f, 0.42f)
}

// ===== Pulsing Real-time Dot =====

@Composable
fun RealtimeDot(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    Box(
        modifier = modifier
            .size(7.dp)
            .alpha(alpha)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
    )
}

// ===== Departure Card =====

@Composable
fun DepartureCard(departure: Departure, onClick: (() -> Unit)? = null) {
    val color = routeColor(departure.serviceNumber)
    val timeStr = departure.realTimeDeparture ?: departure.scheduledDeparture
    val isLive = departure.realTimeDeparture != null
    val mins = timeStr?.let { getMinutesUntil(it) } ?: 0
    val isDue = mins <= 0
    val delayMins = if (isLive && departure.scheduledDeparture != null) {
        val rtMins = departure.realTimeDeparture?.let { getMinutesUntil(it) } ?: 0
        val schedMins = getMinutesUntil(departure.scheduledDeparture)
        rtMins - schedMins
    } else 0

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp, 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Route badge
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 52.dp)
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color)
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    departure.serviceNumber,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1
                )
            }

            // Destination & operator
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isLive) {
                        RealtimeDot(modifier = Modifier.padding(end = 4.dp))
                    }
                    Text(
                        departure.destination,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 15.sp
                    )
                }
                val subtitle = departure.serviceDisplayName ?: departure.operator?.operatorName
                if (subtitle != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (isLive && delayMins > 1) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "\u00B7 ${delayMins} min late",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Time
            if (departure.cancelled) {
                Text(
                    "Cancelled",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        if (isDue) "Due" else "$mins",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDue) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary
                    )
                    if (!isDue && mins <= 60) {
                        Text(
                            "min",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 0.3.sp
                        )
                    }
                    departure.scheduledDeparture?.let { sched ->
                        val schedFormatted = formatTime(sched)
                        if (schedFormatted.isNotEmpty()) {
                            Text(
                                schedFormatted,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (delayMins > 1) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.outline,
                                fontSize = 12.sp
                            )
                        }
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

// ===== Alerts banner =====

@Composable
fun AlertsBanner(alerts: List<String>, onDismiss: () -> Unit) {
    if (alerts.isEmpty()) return

    Surface(
        color = Color(0xFFFFF3E0),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                alerts.joinToString(" \u2022 "),
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFE65100),
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss", modifier = Modifier.size(16.dp), tint = Color(0xFFE65100))
            }
        }
    }
}

// ===== Facilities row =====

@Composable
fun FacilitiesRow(facilities: List<String>) {
    if (facilities.isEmpty()) return

    val facilityInfo = mapOf(
        "SHELTER" to Pair(Icons.Default.NightShelter, "Shelter"),
        "TOILETS" to Pair(Icons.Default.Wc, "Toilets"),
        "TICKET_OFFICE" to Pair(Icons.Default.ConfirmationNumber, "Tickets"),
        "CAR_PARK" to Pair(Icons.Default.LocalParking, "Parking"),
        "WHEELCHAIR_ACCESS" to Pair(Icons.Default.WheelchairPickup, "Accessible"),
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
                    leadingIcon = { Icon(info.first, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    modifier = Modifier.height(28.dp)
                )
            }
        }
    }
}

// ===== Refresh progress bar =====

@Composable
fun RefreshProgressBar(progress: Float) {
    @Suppress("DEPRECATION")
    LinearProgressIndicator(
        progress = progress,
        modifier = Modifier
            .fillMaxWidth()
            .height(3.dp),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    )
}

// ===== Section Header =====

@Composable
fun SectionHeader(icon: ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 0.5.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ===== Time helpers =====

fun getMinutesUntil(isoTime: String): Int {
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
    } catch (_: Exception) { 0 }
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
    } catch (_: Exception) { "" }
}
