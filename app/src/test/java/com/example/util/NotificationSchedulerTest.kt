package com.example.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NotificationSchedulerTest {

    @Test
    fun testChannelId() {
        assertEquals("foodlab_reminders", MealReminderWorker.CHANNEL_ID)
    }

    @Test
    fun testScheduleAndCancelReminders() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        NotificationScheduler.scheduleReminders(context, enabled = true)
        NotificationScheduler.scheduleReminders(context, enabled = false)
    }

    @Test
    fun testCalculateInitialDelay_futureTimeToday() {
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 8)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val delay = NotificationScheduler.calculateInitialDelay(13, 0, calendar.timeInMillis)
        assertEquals(5 * 60 * 60 * 1000L, delay)
    }

    @Test
    fun testCalculateInitialDelay_pastTimeSchedulesNextDay() {
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 14)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val delay = NotificationScheduler.calculateInitialDelay(8, 30, calendar.timeInMillis)
        assertEquals((18 * 60 + 30) * 60 * 1000L, delay)
    }
}
