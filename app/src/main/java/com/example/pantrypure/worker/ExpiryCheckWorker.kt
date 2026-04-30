package com.example.pantrypure.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.pantrypure.PantryPureApplication
import com.example.pantrypure.util.NotificationHelper
import kotlinx.coroutines.flow.first

class ExpiryCheckWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val repository = (applicationContext as PantryPureApplication).repository
        val items = repository.getAllItems().first()
        
        val now = System.currentTimeMillis()
        val expiringSoonOrOverdueCount = items.count { item ->
            if (item.expiryDate == null) return@count false
            val thresholdMillis = item.expiryThresholdDays * 24 * 60 * 60 * 1000L
            item.expiryDate < (now + thresholdMillis)
        }

        if (expiringSoonOrOverdueCount > 0) {
            val notificationHelper = NotificationHelper(applicationContext)
            notificationHelper.showExpiryNotification(expiringSoonOrOverdueCount)
        }

        return Result.success()
    }
}
