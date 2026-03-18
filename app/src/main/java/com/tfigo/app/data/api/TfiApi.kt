package com.tfigo.app.data.api

import com.tfigo.app.data.model.*
import retrofit2.http.*

interface TfiApi {

    @GET("locationLookup")
    suspend fun searchLocations(
        @Query("query") query: String,
        @Query("allowedTypes") allowedTypes: String = "BUS_STOP,TRAIN_STATION,TRAM_STOP,TRAM_STOP_AREA,COACH_STOP,FERRY_PORT",
        @Query("language") language: String = "en"
    ): List<LocationResult>

    @POST("departures")
    suspend fun getDepartures(
        @Body request: DepartureRequest
    ): DepartureResponse

    @POST("situations/stops")
    suspend fun getStopAlerts(
        @Body request: SituationsRequest
    ): List<SituationGroup>

    @POST("stopsAssets")
    suspend fun getStopAssets(
        @Body request: StopsAssetsRequest
    ): List<StopAssetGroup>

    @POST("estimatedTimetable")
    suspend fun getEstimatedTimetable(
        @Body request: EstimatedTimetableRequest
    ): TimetableResponse

    @POST("timetable")
    suspend fun getTimetable(
        @Body request: TimetableRequest
    ): TimetableResponse

    @POST("visibleLookupRequest")
    suspend fun getVisibleStops(
        @Body request: VisibleLookupRequest
    ): List<LocationResult>

    @POST("vehicleLocation")
    suspend fun getVehicleLocations(
        @Body request: VehicleLocationRequest
    ): VehicleLocationResponse
}
