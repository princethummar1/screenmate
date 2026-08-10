package com.screenmate.app.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
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
    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
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

        val events = try {
            usageStatsManager.queryEvents(startMillis, endMillis)
        } catch (e: SecurityException) {
            return null
        }

        var unlockCount = 0
        var firstUsageAt: Long? = null
        var lastUsageAt: Long? = null
        
        val appUsageMap = mutableMapOf<String, Long>()
        val appOpenCountMap = mutableMapOf<String, Int>()
        
        var currentForegroundApp: String? = null
        var currentAppStartTime = 0L
        var lastPauseTime = 0L
        var lastPausedApp: String? = null
        
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            
            when (event.eventType) {
                UsageEvents.Event.KEYGUARD_HIDDEN -> {
                    unlockCount++
                    if (firstUsageAt == null) firstUsageAt = event.timeStamp
                }
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    val pkg = event.packageName
                    if (firstUsageAt == null) firstUsageAt = event.timeStamp
                    
                    if (currentForegroundApp != pkg) {
                        // If a different app was previously considered foreground but no PAUSED event
                        // was received, close that session now using this resume timestamp.
                        if (currentForegroundApp != null) {
                            val prev = currentForegroundApp!!
                            val sessionDuration = event.timeStamp - currentAppStartTime
                            if (sessionDuration > 0) {
                                appUsageMap[prev] = (appUsageMap[prev] ?: 0L) + sessionDuration
                            }
                        }

                        currentForegroundApp = pkg
                        currentAppStartTime = event.timeStamp

                        // Check for session continuation (2000ms threshold)
                        val isContinuation = (pkg == lastPausedApp) && (event.timeStamp - lastPauseTime < 2000)
                        if (!isContinuation) {
                            appOpenCountMap[pkg] = (appOpenCountMap[pkg] ?: 0) + 1
                        }
                    }
                }
                UsageEvents.Event.ACTIVITY_PAUSED -> {
                    lastUsageAt = event.timeStamp
                    val pkg = event.packageName
                    if (currentForegroundApp == pkg) {
                        val sessionDuration = event.timeStamp - currentAppStartTime
                        if (sessionDuration > 0) {
                            appUsageMap[pkg] = (appUsageMap[pkg] ?: 0L) + sessionDuration
                        }
                        lastPausedApp = pkg
                        lastPauseTime = event.timeStamp
                        currentForegroundApp = null
                    }
                }
            }
        }

        // Handle case where app is still open at the end of the queried time range
        if (currentForegroundApp != null) {
            val sessionDuration = endMillis - currentAppStartTime
            if (sessionDuration > 0) {
                appUsageMap[currentForegroundApp!!] = (appUsageMap[currentForegroundApp!!] ?: 0L) + sessionDuration
            }
        }

        val apps = appUsageMap.mapNotNull { (pkg, durationMillis) ->
            if (durationMillis < 1000) return@mapNotNull null
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
                usageSeconds = durationMillis / 1000,
                openCount = appOpenCountMap[pkg] ?: 1
            )
        }.sortedByDescending { it.usageSeconds }

        val totalScreenTimeSeconds = apps.sumOf { it.usageSeconds }
        val totalOpenCount = apps.sumOf { it.openCount }

        return DailyUsageData(
            totalScreenTimeSeconds = totalScreenTimeSeconds,
            unlockCount = unlockCount,
            appOpenCount = totalOpenCount,
            firstUsageAt = firstUsageAt,
            lastUsageAt = lastUsageAt,
            apps = apps
        )
    }
}
