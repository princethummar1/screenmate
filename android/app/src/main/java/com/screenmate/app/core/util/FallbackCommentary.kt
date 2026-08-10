package com.screenmate.app.core.util

import kotlin.math.absoluteValue

object FallbackCommentary {
    fun generateFallback(totalMinutes: Int, topApp: String?, unlockCount: Int): String {
        val timeStr = DateUtils.formatDurationMinutes(totalMinutes)
        val app = topApp ?: "your phone"
        
        val date = DateUtils.todayDate()
        val hash = (date.hashCode() + totalMinutes).absoluteValue
        
        val lowTemplates = listOf(
            "Only $timeStr today? Look at you being all productive \uD83D\uDC40",
            "Barely on your phone ($timeStr)! Touch some grass, oh wait, you already did \uD83C\uDF3F",
            "$timeStr... Are you okay? Did you lose your charger? \uD83D\uDE31",
            "A modest $timeStr. The digital minimalism is strong with this one \u2728"
        )
        
        val medTemplates = listOf(
            "$app got a lot of your attention today. Could be worse \uD83E\uDD37",
            "$timeStr total, with $unlockCount unlocks. Average, but we can do better \uD83D\uDCC8",
            "Not bad, not great. $timeStr is perfectly balanced, as all things should be \u2696\uFE0F",
            "You unlocked your phone $unlockCount times. Looking for something specific? \uD83D\uDD0D"
        )
        
        val highTemplates = listOf(
            "$timeStr of screen time... your phone needs a vacation too \uD83D\uDC80",
            "Wow. $timeStr? Did you even blink today? \uD83D\uDC40",
            "Your thumb must have abs by now after $timeStr on $app \uD83D\uDCAA",
            "$timeStr screen time. The algorithm has you right where it wants you \uD83D\uDD78\uFE0F"
        )
        
        return when {
            totalMinutes < 120 -> lowTemplates[hash % lowTemplates.size]
            totalMinutes < 300 -> medTemplates[hash % medTemplates.size]
            else -> highTemplates[hash % highTemplates.size]
        }
    }
}
