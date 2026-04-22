package com.execos.daily

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

object DailyFeedbackScheduler {
    private const val UNIQUE_WORK_NAME = "daily_feedback_6pm"

    fun schedule(context: Context) {
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        val target = LocalDateTime.of(LocalDate.now(zone), LocalTime.of(18, 0))
        val nextRun = if (now.isBefore(target)) target else target.plusDays(1)
        val initialDelay = Duration.between(now, nextRun).coerceAtLeast(Duration.ZERO)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val req = PeriodicWorkRequestBuilder<DailyFeedbackWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay(initialDelay)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                req,
            )
    }
}

