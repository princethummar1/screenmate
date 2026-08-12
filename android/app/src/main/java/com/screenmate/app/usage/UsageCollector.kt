package com.screenmate.app.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Process
import com.screenmate.app.core.util.DateUtils

data class AppUsageData(
    val packageName: String,
    val appLabel: String,
    val usageSeconds: Long,
    val openCount: Int
)

data class DailyUsageData(
    val totalScreenTimeSeconds: Long,
    val unlockCount: Int,
    val appOpenCount: Int,
    val firstUsageAt: Long?,
    val lastUsageAt: Long?,
    val apps: List<AppUsageData>
)

object UsageCollector {
    private fun shouldCountPackage(context: Context, packageName: String): Boolean {
        if (packageName == context.packageName) return false

        val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent == null) return false

        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val isUpdatedSystemApp = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
            !isSystemApp || isUpdatedSystemApp
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (android.os.Build.VERSION.SDK_INT >= 29) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun getDailyUsage(context: Context, date: String): DailyUsageData? {
        if (!hasUsageAccess(context)) return null

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val pm = context.packageManager

        val startMillis = DateUtils.dayStartMillis(date)
        val endMillis = DateUtils.dayEndMillis(date)

        // METHOD 1: Use queryUsageStats for accurate total screen time
        // This gives us the real total time each app was in foreground
        var totalScreenTimeFromStats = 0L
        val appTimeMap = mutableMapOf<String, Long>()
        val appOpenFromStats = mutableMapOf<String, Int>()

        try {
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startMillis,
                endMillis
            )
            if (stats != null) {
                for (usageStat in stats) {
                    val pkg = usageStat.packageName
                    if (!shouldCountPackage(context, pkg)) continue
                    val timeInForeground = usageStat.totalTimeInForeground
                    if (timeInForeground > 0) {
                        appTimeMap[pkg] = timeInForeground
                        totalScreenTimeFromStats += timeInForeground
                    }
                }
            }
        } catch (_: Exception) { }

        // METHOD 2: Use queryEvents for unlock count and app open counts
        var unlockCount = 0
        var firstUsageAt: Long? = null
        var lastUsageAt: Long? = null
        val appOpenCountMap = mutableMapOf<String, Int>()

        try {
            val events = usageStatsManager.queryEvents(startMillis, endMillis)
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                when (event.eventType) {
                    UsageEvents.Event.KEYGUARD_HIDDEN -> {
                        unlockCount++
                        if (firstUsageAt == null) firstUsageAt = event.timeStamp
                    }
                    UsageEvents.Event.ACTIVITY_RESUMED -> {
                        if (firstUsageAt == null) firstUsageAt = event.timeStamp
                        val pkg = event.packageName
                        if (shouldCountPackage(context, pkg)) {
                            appOpenCountMap[pkg] = (appOpenCountMap[pkg] ?: 0) + 1
                        }
                    }
                    UsageEvents.Event.ACTIVITY_PAUSED -> {
                        lastUsageAt = event.timeStamp
                    }
                }
            }
        } catch (_: Exception) { }

        // Use the MORE ACCURATE total from queryUsageStats
        // Fall back to event-based calculation if stats returned 0
        val finalTotalSeconds = if (totalScreenTimeFromStats > 0) {
            totalScreenTimeFromStats / 1000
        } else {
            // Fallback: sum from app time map (already in ms from queryEvents)
            appTimeMap.values.sum() / 1000
        }

        if (finalTotalSeconds == 0L && unlockCount == 0) return null

        val apps = appTimeMap.mapNotNull { (pkg, durationMs) ->
            val durationSec = durationMs / 1000
            if (durationSec < 1) return@mapNotNull null
            if (pkg == context.packageName) return@mapNotNull null

            val label = try {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: PackageManager.NameNotFoundException) {
                pkg
            }

            AppUsageData(
                packageName = pkg,
                appLabel = label,
                usageSeconds = durationSec,
                openCount = appOpenCountMap[pkg] ?: 1
            )
        }.sortedByDescending { it.usageSeconds }

        val totalOpenCount = appOpenCountMap.values.sum()

        return DailyUsageData(
            totalScreenTimeSeconds = finalTotalSeconds,
            unlockCount = unlockCount,
            appOpenCount = totalOpenCount,
            firstUsageAt = firstUsageAt,
            lastUsageAt = lastUsageAt,
            apps = apps
        )
    }
}
