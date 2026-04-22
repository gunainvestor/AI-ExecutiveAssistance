package com.execos

import android.app.Application
import com.execos.daily.DailyFeedbackScheduler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ExecOsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DailyFeedbackScheduler.schedule(this)
    }
}
