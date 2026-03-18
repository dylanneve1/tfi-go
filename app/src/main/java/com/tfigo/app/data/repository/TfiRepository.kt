package com.tfigo.app.data.repository

import com.tfigo.app.data.api.TfiApi
import com.tfigo.app.data.api.ApiClient
import com.tfigo.app.data.model.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val exception: Exception? = null) : ApiResult<Nothing>()
}

class TfiRepository(private val api: TfiApi = ApiClient.api) {

    suspend fun searchStops(query: String): ApiResult<List<LocationResult>> {
        return try {
            val results = api.searchLocations(query)
            ApiResult.Success(results)
        } catch (e: Exception) {
            ApiResult.Error("Failed to search: ${e.localizedMessage}", e)
        }
    }

    suspend fun getDepartures(stop: LocationResult): ApiResult<List<Departure>> {
        return try {
            val now = Date()
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)
            sdf.timeZone = TimeZone.getDefault()
            val timeStr = sdf.format(now)

            val request = DepartureRequest(
                clientTimeZoneOffsetInMS = TimeZone.getDefault().rawOffset.toLong(),
                departureDate = timeStr,
                departureTime = timeStr,
                stopIds = listOf(stop.id),
                stopType = stop.type,
                stopName = stop.name,
                requestTime = timeStr
            )
            val response = api.getDepartures(request)
            if (response.errorMessage != null) {
                ApiResult.Error(response.errorMessage)
            } else {
                ApiResult.Success(response.stopDepartures ?: emptyList())
            }
        } catch (e: Exception) {
            ApiResult.Error("Failed to load departures: ${e.localizedMessage}", e)
        }
    }

    suspend fun getDeparturesForFavourite(fav: FavouriteStop): ApiResult<List<Departure>> {
        val location = LocationResult(
            id = fav.id,
            name = fav.name,
            type = fav.type,
            shortCode = fav.shortCode
        )
        return getDepartures(location)
    }

    suspend fun getAlerts(stopId: String): ApiResult<List<String>> {
        return try {
            val response = api.getStopAlerts(SituationsRequest(listOf(stopId)))
            val alerts = response.flatMap { group ->
                group.situations?.map { it.title ?: it.details ?: "Service disruption" } ?: emptyList()
            }
            ApiResult.Success(alerts)
        } catch (e: Exception) {
            ApiResult.Success(emptyList()) // Silently fail like HTML version
        }
    }

    suspend fun getFacilities(stopId: String): ApiResult<List<String>> {
        return try {
            val response = api.getStopAssets(StopsAssetsRequest(listOf(stopId)))
            val facilities = response.flatMap { group ->
                group.assets?.mapNotNull { it.assetType?.uppercase() } ?: emptyList()
            }
            ApiResult.Success(facilities)
        } catch (e: Exception) {
            ApiResult.Success(emptyList())
        }
    }

    suspend fun getTripDetails(departure: Departure): ApiResult<TimetableResponse> {
        val vehicle = departure.vehicle
        // Try real-time estimated timetable first
        if (vehicle?.dataFrameRef != null && vehicle.datedVehicleJourneyRef != null) {
            try {
                val response = api.getEstimatedTimetable(
                    EstimatedTimetableRequest(
                        dataFrameRef = vehicle.dataFrameRef,
                        datedVehicleJourneyRef = vehicle.datedVehicleJourneyRef
                    )
                )
                if (!response.rows.isNullOrEmpty()) {
                    return ApiResult.Success(response)
                }
            } catch (_: Exception) {}
        }

        // Fallback to scheduled timetable
        val serviceRef = vehicle?.reference
        if (serviceRef != null) {
            try {
                val now = Date()
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US)
                sdf.timeZone = TimeZone.getDefault()

                val response = api.getTimetable(
                    TimetableRequest(
                        timetableId = serviceRef,
                        timetableDirection = departure.serviceDirection ?: "OUTBOUND",
                        dateAndTime = sdf.format(now)
                    )
                )
                if (!response.rows.isNullOrEmpty()) {
                    return ApiResult.Success(response)
                }
            } catch (_: Exception) {}
        }

        return ApiResult.Error("Trip details unavailable")
    }

    suspend fun getNearbyStops(latitude: Double, longitude: Double): ApiResult<List<NearbyStop>> {
        return try {
            val delta = 0.005 // ~500m
            val request = VisibleLookupRequest(
                center = Coordinate(latitude, longitude),
                upperRight = Coordinate(latitude + delta, longitude + delta),
                lowerLeft = Coordinate(latitude - delta, longitude - delta)
            )
            val results = api.getVisibleStops(request)
            val stops = results
                .filter { it.coordinate != null }
                .map { loc ->
                    NearbyStop(
                        id = loc.id,
                        name = loc.name,
                        type = loc.type,
                        latitude = loc.coordinate!!.latitude,
                        longitude = loc.coordinate.longitude,
                        shortCode = loc.shortCode,
                        distanceMeters = haversine(
                            latitude, longitude,
                            loc.coordinate.latitude, loc.coordinate.longitude
                        )
                    )
                }
                .sortedBy { it.distanceMeters }
                .take(8)
            ApiResult.Success(stops)
        } catch (e: Exception) {
            ApiResult.Error("Failed to load nearby stops: ${e.localizedMessage}", e)
        }
    }

    suspend fun getMapStops(
        south: Double, west: Double, north: Double, east: Double
    ): ApiResult<List<LocationResult>> {
        return try {
            val centerLat = (south + north) / 2
            val centerLon = (west + east) / 2
            val request = VisibleLookupRequest(
                center = Coordinate(centerLat, centerLon),
                upperRight = Coordinate(north, east),
                lowerLeft = Coordinate(south, west)
            )
            val results = api.getVisibleStops(request)
            ApiResult.Success(results.filter { it.coordinate != null })
        } catch (e: Exception) {
            ApiResult.Error("Failed to load map stops: ${e.localizedMessage}", e)
        }
    }

    companion object {
        fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
            val r = 6371000.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2).pow(2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(dLon / 2).pow(2)
            return r * 2 * atan2(sqrt(a), sqrt(1 - a))
        }
    }
}
