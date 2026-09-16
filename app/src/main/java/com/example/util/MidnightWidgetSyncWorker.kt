package com.example.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class MidnightWidgetSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            WidgetUpdater.updateAllWidgets(applicationContext)
        } catch (e: Exception) {
            android.util.Log.e("MidnightWidgetSync", "Error updating widgets at midnight", e)
        }
        return Result.success()
    }
}
