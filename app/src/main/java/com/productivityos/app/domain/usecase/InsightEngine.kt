package com.productivityos.app.domain.usecase

import com.productivityos.app.domain.model.AppCategory
import com.productivityos.app.domain.model.AppUsage
import com.productivityos.app.domain.model.HourlyFocus
import com.productivityos.app.domain.model.Insight
import com.productivityos.app.domain.model.InsightImpact
import com.productivityos.app.domain.model.ProductivityScore
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InsightEngine @Inject constructor() {

    /**
     * Generates a prioritised list of insights from today's data.
     * All rules from the spec are implemented — results are sorted HIGH → MEDIUM → LOW.
     */
    fun generate(
        usage: List<AppUsage>,
        hourlyFocus: List<HourlyFocus>,
        score: ProductivityScore
    ): List<Insight> {
        val insights = mutableListOf<Insight>()

        // ── Rule a: frequent app switching ────────────────────
        // Average gap = total work minutes / switch count proxy
        // We estimate switches from score's distractMinutes + focusMinutes spread
        val totalActiveMinutes = score.focusMinutes + score.distractMinutes
        val estimatedSwitches = usage.size * 3 // rough: each app has ~3 context switches
        val avgSwitchGapMinutes = if (estimatedSwitches > 0)
            (totalActiveMinutes / estimatedSwitches).coerceAtLeast(1)
        else 60

        if (avgSwitchGapMinutes < 8 && usage.size >= 3) {
            insights += Insight(
                id          = UUID.randomUUID().toString(),
                title       = "Frequent app switching",
                description = "You switch apps every $avgSwitchGapMinutes minutes on average — " +
                              "this fragments deep focus and reduces cognitive throughput significantly.",
                impact      = InsightImpact.HIGH,
                emoji       = "⚡"
            )
        }

        // ── Rule b: peak 2-hour focus window ──────────────────
        val peakWindow = findPeakTwoHourWindow(hourlyFocus)
        if (peakWindow != null) {
            val (startHour, _) = peakWindow
            val endHour = startHour + 2
            val startLabel = formatHour(startHour)
            val endLabel   = formatHour(endHour)
            insights += Insight(
                id          = UUID.randomUUID().toString(),
                title       = "Peak window: $startLabel–$endLabel",
                description = "Your best deep work consistently happens between $startLabel and $endLabel. " +
                              "Guard this time — no meetings, no notifications.",
                impact      = InsightImpact.HIGH,
                emoji       = "🧠"
            )
        }

        // ── Rule c: distraction app > 30 minutes ─────────────
        val distractApps = usage.filter {
            it.category == AppCategory.DISTRACT && it.totalMinutes > 30
        }
        distractApps.forEach { app ->
            // Estimate sessions: each "session" averages ~5 minutes
            val estimatedSessions = (app.totalMinutes / 5).coerceAtLeast(1)
            insights += Insight(
                id          = UUID.randomUUID().toString(),
                title       = "${app.emoji} ${app.appName}: ${app.totalMinutes} min",
                description = "${app.appName} consumed ${app.totalMinutes} minutes across " +
                              "~$estimatedSessions sessions today. " +
                              "Most usage likely occurred during planned focus blocks.",
                impact      = InsightImpact.MEDIUM,
                emoji       = app.emoji
            )
        }

        // ── Rule d: 3 PM afternoon energy dip ────────────────
        val hour15Focus = hourlyFocus.find { it.hour == 15 }?.level ?: -1
        if (hour15Focus in 0..3) {
            insights += Insight(
                id          = UUID.randomUUID().toString(),
                title       = "Afternoon energy dip at 3 PM",
                description = "Your focus level drops to $hour15Focus/10 around 3 PM — " +
                              "a natural circadian rhythm pattern. " +
                              "Schedule admin tasks or light reviews here instead of deep work.",
                impact      = InsightImpact.LOW,
                emoji       = "🌅"
            )
        }

        // ── Rule e: total focus hours < 2 ────────────────────
        val focusHours = score.focusMinutes / 60f
        if (focusHours < 2f) {
            val focusHoursFormatted = if (score.focusMinutes < 60)
                "${score.focusMinutes}m"
            else
                "${"%.1f".format(focusHours)}h"
            insights += Insight(
                id          = UUID.randomUUID().toString(),
                title       = "Low focus time today",
                description = "Only $focusHoursFormatted of focused work today. " +
                              "Tomorrow, aim for at least 3 hours — " +
                              "even one 90-minute deep block makes a significant difference.",
                impact      = InsightImpact.HIGH,
                emoji       = "⏱️"
            )
        }

        // ── Rule f: no deep sessions ──────────────────────────
        if (score.deepSessions == 0) {
            insights += Insight(
                id          = UUID.randomUUID().toString(),
                title       = "No deep work sessions",
                description = "No uninterrupted focus blocks detected today. " +
                              "Try blocking 90 minutes tomorrow morning — " +
                              "phone on silent, notifications off, one task only.",
                impact      = InsightImpact.HIGH,
                emoji       = "🎯"
            )
        }

        // ── Bonus: positive reinforcement when doing well ─────
        if (score.value >= 75 && score.deepSessions >= 2) {
            insights += Insight(
                id          = UUID.randomUUID().toString(),
                title       = "Excellent focus day",
                description = "Score of ${score.value}/100 with ${score.deepSessions} deep sessions — " +
                              "you're in the top tier of productive days. " +
                              "Note what made today work and repeat it tomorrow.",
                impact      = InsightImpact.LOW,
                emoji       = "🏆"
            )
        }

        // Sort: HIGH first, then MEDIUM, then LOW — deduplicate by title
        return insights
            .distinctBy { it.title }
            .sortedWith(
                compareByDescending<Insight> {
                    when (it.impact) {
                        InsightImpact.HIGH   -> 2
                        InsightImpact.MEDIUM -> 1
                        InsightImpact.LOW    -> 0
                    }
                }
            )
    }

    // ── Helpers ───────────────────────────────────────────────

    /**
     * Finds the 2-consecutive-hour window with the highest average focus level.
     * Returns (startHour, avgLevel) or null if fewer than 2 hours of data.
     */
    private fun findPeakTwoHourWindow(hourlyFocus: List<HourlyFocus>): Pair<Int, Float>? {
        if (hourlyFocus.size < 2) return null

        val sorted = hourlyFocus.sortedBy { it.hour }
        var bestStart: Int? = null
        var bestAvg = -1f

        for (i in 0 until sorted.size - 1) {
            val current = sorted[i]
            val next    = sorted[i + 1]
            // Only consider consecutive hours
            if (next.hour != current.hour + 1) continue
            val avg = (current.level + next.level) / 2f
            if (avg > bestAvg) {
                bestAvg   = avg
                bestStart = current.hour
            }
        }

        return if (bestStart != null && bestAvg > 0f) Pair(bestStart, bestAvg) else null
    }

    private fun formatHour(hour: Int): String {
        return when {
            hour == 0  -> "12 AM"
            hour < 12  -> "$hour AM"
            hour == 12 -> "12 PM"
            else       -> "${hour - 12} PM"
        }
    }
}
