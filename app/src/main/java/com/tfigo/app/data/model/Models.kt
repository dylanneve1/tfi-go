package com.tfigo.app.data.model

import com.google.gson.annotations.SerializedName

// Location search
data class LocationResult(
    val status: Status? = null,
    val name: String = "",
    val id: String = "",
    val shortCode: String? = null,
    val coordinate: Coordinate? = null,
    val type: String = ""
)

data class Status(val success: Boolean = false)

data class Coordinate(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

// Departures
data class DepartureRequest(
    val clientTimeZoneOffsetInMS: Long,
    val departureDate: String,
    val departureTime: String,
    val stopIds: List<String>,
    val stopType: String,
    val stopName: String,
    val requestTime: String,
    val departureOrArrival: String = "DEPARTURE",
    val refresh: Boolean = false
)

data class DepartureResponse(
    val status: Status? = null,
    val stopDepartures: List<Departure>? = null,
    val errorMessage: String? = null
)

data class Departure(
    val destination: String = "",
    val realTimeDeparture: String? = null,
    val scheduledDeparture: String? = null,
    val cancelled: Boolean = false,
    val serviceNumber: String = "",
    val serviceID: String = "",
    val serviceDisplayName: String? = null,
    val serviceDirection: String? = null,
    val operator: Operator? = null,
    val transportMode: String = "BUS",
    val vehicle: Vehicle? = null,
    val stopRef: String? = null
)

data class Operator(
    val operatorCode: String? = null,
    val operatorName: String? = null,
    val phone: String? = null,
    val url: String? = null
)

data class Vehicle(
    val reference: String? = null,
    val location: VehicleLocation? = null,
    @SerializedName("dataFrameRef") val dataFrameRef: String? = null,
    @SerializedName("datedVehicleJourneyRef") val datedVehicleJourneyRef: String? = null
)

data class VehicleLocation(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

// Favourite stop (for local storage)
data class FavouriteStop(
    val id: String,
    val name: String,
    val shortCode: String? = null,
    val type: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)

// Departure also needs serviceReference and direction for trip details
// (already covered by vehicle.dataFrameRef / datedVehicleJourneyRef)

// ===== Service Alerts =====
data class SituationsRequest(val idList: List<String>)

data class SituationGroup(
    val id: String? = null,
    val situations: List<Situation>? = null
)

data class Situation(
    val title: String? = null,
    val details: String? = null
)

// ===== Stop Facilities =====
data class StopsAssetsRequest(val idList: List<String>)

data class StopAssetGroup(
    val id: String? = null,
    val assets: List<StopAsset>? = null
)

data class StopAsset(
    val assetType: String? = null
)

// ===== Estimated Timetable (Trip Details) =====
data class EstimatedTimetableRequest(
    val dataFrameRef: String,
    val datedVehicleJourneyRef: String
)

data class TimetableRequest(
    val clientTimeZoneOffsetInMS: Long = 0,
    val timetableDirection: String = "OUTBOUND",
    val timetableId: String,
    val maxColumnsToFetch: Int = 3,
    val dateAndTime: String,
    val includeNonTimingPoints: Boolean = true
)

data class TimetableResponse(
    val rows: List<TimetableRow>? = null,
    val columns: List<TimetableColumn>? = null
)

data class TimetableRow(
    val rowIndex: Int = 0,
    val stopName: String? = null,
    val shortCode: String? = null,
    val stopReference: String? = null,
    val coordinate: Coordinate? = null,
    val type: String? = null
)

data class TimetableColumn(
    val events: Map<String, TimetableEvent>? = null
)

data class TimetableEvent(
    val timeOfEvent: String? = null,
    val realTimeOfEvent: String? = null
)

// ===== Visible Lookup (Map / Nearby) =====
data class VisibleLookupRequest(
    val center: Coordinate,
    val upperRight: Coordinate,
    val lowerLeft: Coordinate,
    val visibleLookupOrigin: String = "LIVE_DEPARTURE",
    val filteringTypes: List<String> = listOf("BUS_STOP", "TRAM_STOP_AREA", "TRAIN_STATION"),
    val language: String = "en"
)

// ===== Vehicle Location =====
data class VehicleLocationRequest(
    val serviceReference: String,
    val direction: String = "OUTBOUND"
)

data class VehicleLocationResponse(
    val vehicleLocations: List<VehiclePositionInfo>? = null
)

data class VehiclePositionInfo(
    val coordinate: Coordinate? = null,
    val bearing: Double? = null
)

// Nearby stop with distance
data class NearbyStop(
    val id: String,
    val name: String,
    val type: String,
    val latitude: Double,
    val longitude: Double,
    val shortCode: String? = null,
    val distanceMeters: Double = 0.0
)
