package com.execos.data.usage

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

class DeviceUsageRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val zone: ZoneId = ZoneId.systemDefault()

    private val trackedPkgs = setOf(
        // WhatsApp
        "com.whatsapp",
        "com.whatsapp.w4b",
        // Meta
        "com.facebook.katana",
        "com.facebook.orca",
        "com.instagram.android",
        // TikTok
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill",
        // X / Twitter
        "com.twitter.android",
        // Snapchat
        "com.snapchat.android",
        // YouTube
        "com.google.android.youtube",
        // Reddit
        "com.reddit.frontpage",
        // Discord
        "com.discord",
        // LinkedIn
        "com.linkedin.android",
        // Telegram
        "org.telegram.messenger",
        // Signal
        "org.thoughtcrime.securesms",
        // Browsers (browsing time)
        "com.android.chrome",
        "com.chrome.beta",
        "com.chrome.dev",
        "com.chrome.canary",
        "com.sec.android.app.sbrowser", // Samsung Internet
        "org.mozilla.firefox",
        "org.mozilla.firefox_beta",
        "org.mozilla.fenix", // Firefox
        "com.microsoft.emmx", // Edge
        "com.brave.browser",
        "com.opera.browser",
        "com.opera.browser.beta",
        "com.opera.mini.native",
        "com.duckduckgo.mobile.android",
    )

    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getSocialAndBrowserUsage(): Result<List<AppUsageItem>> = runCatching {
        val usage = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val pm = context.packageManager

        val now = System.currentTimeMillis()
        val startToday = LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli()
        val startWeek = LocalDate.now()
            .with(DayOfWeek.MONDAY)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()

        val todayByPkg = usage.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startToday, now)
            .orEmpty()
            .groupBy { it.packageName }
            .mapValues { (_, v) -> v.sumOf { it.totalTimeInForeground } }

        val weekByPkg = usage.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startWeek, now)
            .orEmpty()
            .groupBy { it.packageName }
            .mapValues { (_, v) -> v.sumOf { it.totalTimeInForeground } }

        val items = trackedPkgs.mapNotNull { pkg ->
            val todayMs = todayByPkg[pkg] ?: 0L
            val weekMs = weekByPkg[pkg] ?: 0L
            if (todayMs == 0L && weekMs == 0L) return@mapNotNull null

            val label = resolveAppLabel(pm, pkg) ?: return@mapNotNull null
            AppUsageItem(
                packageName = pkg,
                appLabel = label,
                minutesToday = (todayMs / 60_000L).toInt(),
                minutesThisWeek = (weekMs / 60_000L).toInt(),
            )
        }.sortedWith(compareByDescending<AppUsageItem> { it.minutesToday }.thenByDescending { it.minutesThisWeek })

        items
    }

    private fun resolveAppLabel(pm: PackageManager, pkg: String): String? =
        runCatching {
            val appInfo = pm.getApplicationInfo(pkg, 0)
            pm.getApplicationLabel(appInfo).toString()
        }.getOrNull()
}

