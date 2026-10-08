package com.example.util

import android.content.Context
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HealthConnectManagerTest {

    private lateinit var context: Context
    private lateinit var manager: HealthConnectManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        manager = HealthConnectManager(context)
    }

    @Test
    fun permissions_containsAllRequiredReadAndWritePermissions() {
        val permissions = manager.permissions

        assertEquals(5, permissions.size)
        assertTrue(permissions.contains(HealthPermission.getReadPermission(StepsRecord::class)))
        assertTrue(permissions.contains(HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)))
        assertTrue(permissions.contains(HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)))
        assertTrue(permissions.contains(HealthPermission.getReadPermission(WeightRecord::class)))
        assertTrue(permissions.contains(HealthPermission.getWritePermission(WeightRecord::class)))
    }

    @Test
    fun dailyData_defaultStateIsEmpty() {
        val initialData = manager.dailyData.value

        assertEquals(0L, initialData.steps)
        assertEquals(0.0, initialData.caloriesBurned, 0.001)
        assertEquals(null, initialData.latestWeightKg)
        assertEquals(null, initialData.lastSyncTime)
    }

    @Test
    fun dailyData_copyUpdatesValuesCorrectly() {
        val now = Instant.now()
        val updated = HealthConnectDailyData(
            steps = 8450L,
            caloriesBurned = 420.5,
            latestWeightKg = 73.2,
            lastSyncTime = now
        )

        assertEquals(8450L, updated.steps)
        assertEquals(420.5, updated.caloriesBurned, 0.001)
        assertEquals(73.2, updated.latestWeightKg!!, 0.001)
        assertEquals(now, updated.lastSyncTime)
    }

    @Test
    fun checkAvailability_returnsValidEnumWithoutCrashing() {
        val availability = manager.checkAvailability()
        assertNotNull(availability)
        assertTrue(
            availability == HealthConnectAvailability.AVAILABLE ||
            availability == HealthConnectAvailability.UPDATE_REQUIRED ||
            availability == HealthConnectAvailability.NOT_SUPPORTED
        )
    }
}
