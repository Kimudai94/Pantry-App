package com.example.pantrypure

import android.app.Application
import androidx.room.Room
import androidx.work.*
import com.example.pantrypure.data.database.PantryDatabase
import com.example.pantrypure.data.repository.PantryRepository
import com.example.pantrypure.util.NotificationHelper
import com.example.pantrypure.worker.ExpiryCheckWorker
import java.util.concurrent.TimeUnit

class PantryPureApplication : Application() {
    private val database by lazy {
        Room.databaseBuilder(this, PantryDatabase::class.java, "pantry_database")
            .fallbackToDestructiveMigration()
            .build()
    }
    val repository by lazy {
        PantryRepository(database.pantryDao(), database.consumptionDao())
    }

    override fun onCreate() {
        super.onCreate()
        
        val notificationHelper = NotificationHelper(this)
        notificationHelper.createNotificationChannel()
        
        scheduleExpiryCheck()
    }

    private fun scheduleExpiryCheck() {
        val expiryCheckRequest = PeriodicWorkRequestBuilder<ExpiryCheckWorker>(
            1, TimeUnit.DAYS
        ).setConstraints(
            Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ExpiryCheckWork",
            ExistingPeriodicWorkPolicy.KEEP,
            expiryCheckRequest
        )
    }
}
