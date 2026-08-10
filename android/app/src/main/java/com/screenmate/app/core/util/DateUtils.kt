package com.screenmate.app.core.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object DateUtils {
    private val zoneId = ZoneId.systemDefault()
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

    fun todayDate(): String = LocalDate.now(zoneId).format(dateFormatter)
    
    fun yesterdayDate(): String = LocalDate.now(zoneId).minusDays(1).format(dateFormatter)

    fun formatDuration(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    fun formatDurationMinutes(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    fun formatTime(epochMillis: Long): String {
        val instant = Instant.ofEpochMilli(epochMillis)
        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a").withZone(zoneId)
        return timeFormatter.format(instant)
    }

    fun dayStartMillis(dateStr: String): Long {
        return LocalDate.parse(dateStr, dateFormatter).atStartOfDay(zoneId).toInstant().toEpochMilli()
    }

    fun dayEndMillis(dateStr: String): Long {
        return LocalDate.parse(dateStr, dateFormatter).atTime(23, 59, 59, 999).atZone(zoneId).toInstant().toEpochMilli()
    }

    fun daysAgo(n: Int): String {
        return LocalDate.now(zoneId).minusDays(n.toLong()).format(dateFormatter)
    }

    fun dayOfWeek(dateStr: String): String {
        val date = LocalDate.parse(dateStr, dateFormatter)
        return date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
    }

    fun monthYear(dateStr: String): String {
        val date = LocalDate.parse(dateStr, dateFormatter)
        return date.format(monthYearFormatter)
    }

    fun datesBetween(start: String, end: String): List<String> {
        val startDate = LocalDate.parse(start, dateFormatter)
        val endDate = LocalDate.parse(end, dateFormatter)
        val numDays = ChronoUnit.DAYS.between(startDate, endDate).toInt()
        return (0..numDays).map { startDate.plusDays(it.toLong()).format(dateFormatter) }
    }
}
