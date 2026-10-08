package com.example.util

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Health Connect Availability Status on current device.
 */
enum class HealthConnectAvailability {
    AVAILABLE,
    UPDATE_REQUIRED,
    NOT_SUPPORTED
}

/**
 * Aggregated daily health metrics synced from Health Connect.
 */
data class HealthConnectDailyData(
    val steps: Long = 0,
    val caloriesBurned: Double = 0.0,
    val latestWeightKg: Double? = null,
    val lastSyncTime: Instant? = null
)

/**
 * Central Manager for Google Health Connect integration in FoodLab.
 * Handles availability checking, permission querying, daily steps & calorie read,
 * and weight sync.
 */
class HealthConnectManager(private val context: Context) {

    private val tag = "HealthConnectManager"

    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getWritePermission(WeightRecord::class)
    )

    private val healthConnectClient: HealthConnectClient? by lazy {
        if (checkAvailability() == HealthConnectAvailability.AVAILABLE) {
            HealthConnectClient.getOrCreate(context)
        } else {
            null
        }
    }

    private val _isAuthorized = MutableStateFlow(false)
    val isAuthorized: StateFlow<Boolean> = _isAuthorized.asStateFlow()

    private val _dailyData = MutableStateFlow(HealthConnectDailyData())
    val dailyData: StateFlow<HealthConnectDailyData> = _dailyData.asStateFlow()

    /**
     * Checks whether Health Connect is available on this Android device.
     */
    fun checkAvailability(): HealthConnectAvailability {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
            return HealthConnectAvailability.NOT_SUPPORTED
        }
        return when (HealthConnectClient.getSdkStatus(context)) {
            HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.UPDATE_REQUIRED
            else -> HealthConnectAvailability.NOT_SUPPORTED
        }
    }

    /**
     * Returns true if all required read/write permissions have been granted.
     */
    suspend fun checkPermissionsGranted(): Boolean {
        val client = healthConnectClient ?: return false
        return try {
            val granted = client.permissionController.getGrantedPermissions()
            val hasAll = granted.containsAll(permissions)
            _isAuthorized.value = hasAll
            hasAll
        } catch (e: Exception) {
            Log.e(tag, "Error checking Health Connect permissions", e)
            _isAuthorized.value = false
            false
        }
    }

    /**
     * Syncs health data for a given date (defaulting to today).
     */
    suspend fun syncDailyData(targetDate: LocalDate = LocalDate.now()): Result<HealthConnectDailyData> {
        val client = healthConnectClient
            ?: return Result.failure(IllegalStateException("Health Connect is not available on this device"))

        val startOfDay = targetDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = startOfDay.plus(1, ChronoUnit.DAYS)

        return try {
            val steps = readSteps(client, startOfDay, endOfDay)
            val calories = readCaloriesBurned(client, startOfDay, endOfDay)
            val weight = readLatestWeight(client)

            val updatedData = HealthConnectDailyData(
                steps = steps,
                caloriesBurned = calories,
                latestWeightKg = weight,
                lastSyncTime = Instant.now()
            )
            _dailyData.value = updatedData
            Result.success(updatedData)
        } catch (e: Exception) {
            Log.e(tag, "Failed to sync Health Connect daily data", e)
            Result.failure(e)
        }
    }

    /**
     * Aggregates total steps for the specified time window.
     */
    private suspend fun readSteps(
        client: HealthConnectClient,
        startTime: Instant,
        endTime: Instant
    ): Long {
        return try {
            val response = client.aggregate(
                AggregateRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            response[StepsRecord.COUNT_TOTAL] ?: 0L
        } catch (e: Exception) {
            Log.w(tag, "Error reading steps from Health Connect", e)
            0L
        }
    }

    /**
     * Aggregates total calories burned (active + basal or total) for the specified time window.
     */
    private suspend fun readCaloriesBurned(
        client: HealthConnectClient,
        startTime: Instant,
        endTime: Instant
    ): Double {
        return try {
            val response = client.aggregate(
                AggregateRequest(
                    metrics = setOf(
                        TotalCaloriesBurnedRecord.ENERGY_TOTAL,
                        ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL
                    ),
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            val total = response[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories
            val active = response[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories

            total ?: active ?: 0.0
        } catch (e: Exception) {
            Log.w(tag, "Error reading calories burned from Health Connect", e)
            0.0
        }
    }

    /**
     * Reads the most recent weight recorded in Health Connect.
     */
    private suspend fun readLatestWeight(client: HealthConnectClient): Double? {
        return try {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = WeightRecord::class,
                    timeRangeFilter = TimeRangeFilter.after(Instant.now().minus(30, ChronoUnit.DAYS)),
                    ascendingOrder = false,
                    pageSize = 1
                )
            )
            response.records.firstOrNull()?.weight?.inKilograms
        } catch (e: Exception) {
            Log.w(tag, "Error reading weight from Health Connect", e)
            null
        }
    }

    /**
     * Writes a new weight log to Health Connect.
     */
    suspend fun writeWeight(
        weightKg: Double,
        time: Instant = Instant.now()
    ): Result<Unit> {
        val client = healthConnectClient
            ?: return Result.failure(IllegalStateException("Health Connect is not available on this device"))

        return try {
            val record = WeightRecord(
                weight = Mass.kilograms(weightKg),
                time = time,
                zoneOffset = ZoneId.systemDefault().rules.getOffset(time),
                metadata = Metadata(recordingMethod = Metadata.RECORDING_METHOD_MANUAL_ENTRY)
            )
            client.insertRecords(listOf(record))
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Failed to write weight to Health Connect", e)
            Result.failure(e)
        }
    }
}
