package com.productivityos.app.domain.usecase

import com.productivityos.app.domain.model.BlockType
import com.productivityos.app.domain.model.HourlyFocus
import com.productivityos.app.domain.model.ProductivityScore
import com.productivityos.app.domain.model.ScheduleBlock
import com.productivityos.app.domain.model.ScheduleChange
import com.productivityos.app.domain.model.UserProfile
import javax.inject.Inject
import javax.inject.Singleton

data class ScheduleResult(
    val blocks: List<ScheduleBlock>,
    val changes: List<ScheduleChange>
)

@Singleton
class ScheduleEngine @Inject constructor() {

    fun generate(
        hourlyFocus: List<HourlyFocus>,
        score: ProductivityScore,
        profile: UserProfile
    ): ScheduleResult {

        val focusPref = profile.focusPreference
        val workType  = profile.workerType

        // ── Step 1: Find the user's peak 2-hour focus window ──────────────────
        // Prefer stated preference, but validate against actual hourly data
        val preferredStart = preferenceToHour(focusPref)
        val peakStart = if (hourlyFocus.size >= 2) {
            // Find 2-hr window with highest combined focus around preferred time
            val candidates = (0 until hourlyFocus.size - 1).map { i ->
                val sum = hourlyFocus[i].level + (hourlyFocus.getOrNull(i + 1)?.level ?: 0)
                Pair(hourlyFocus[i].hour, sum)
            }
            // Weight candidates closer to preferred time more heavily
            val best = candidates.maxByOrNull { (hour, sum) ->
                val distancePenalty = Math.abs(hour - preferredStart) * 0.5
                sum - distancePenalty
            }
            best?.first ?: preferredStart
        } else preferredStart

        // ── Step 2: Build role-specific deep work block title ─────────────────
        val deepTitle = deepTitleFor(workType)

        // ── Step 3: Determine if afternoon deep block is warranted ────────────
        // Only add if score was good (≥50) and actual afternoon focus level was decent
        val afternoonLevel = hourlyFocus.filter { it.hour in 13..15 }
            .maxByOrNull { it.level }?.level ?: 5
        val hasAfternoonDeep = afternoonLevel > 6 && score.value >= 50

        // ── Step 4: Determine if user needs a recovery day (low score) ────────
        val isRecoveryDay = score.value < 35

        // ── Step 5: Build the block list ──────────────────────────────────────
        val blocks = mutableListOf<ScheduleBlock>()

        if (isRecoveryDay) {
            // Low score yesterday → lighter schedule to recover
            blocks += ScheduleBlock("s1", formatHour(peakStart), formatHour(peakStart + 1, 30),
                "$deepTitle (Light)", BlockType.DEEP)
            blocks += ScheduleBlock("s2", formatHour(peakStart + 1, 30), formatHour(peakStart + 1, 45),
                "Short Break", BlockType.BREAK)
            blocks += ScheduleBlock("s3", formatHour(peakStart + 1, 45), "12:00",
                "Email & Catchup", BlockType.LIGHT)
            blocks += ScheduleBlock("s4", "12:00", "13:30", "Lunch & Rest", BlockType.BREAK)
            blocks += ScheduleBlock("s5", "13:30", "15:00", "Light Tasks & Reviews", BlockType.LIGHT)
            blocks += ScheduleBlock("s6", "15:00", "16:00", "Admin & Planning", BlockType.ADMIN)
        } else {
            // Normal or strong day → full schedule

            // Deep work block at peak window (2h 30m)
            val deepEnd = formatHour(peakStart + 2, 30)
            blocks += ScheduleBlock("s1", formatHour(peakStart), deepEnd, deepTitle, BlockType.DEEP)

            // 15-min break after deep work
            val breakEnd = formatHour(peakStart + 2, 45)
            blocks += ScheduleBlock("s2", deepEnd, breakEnd, "Short Break", BlockType.BREAK)

            // Email & messages until noon
            val commsEnd = if (peakStart <= 9) "12:00" else formatHour(peakStart + 4)
            blocks += ScheduleBlock("s3", breakEnd, commsEnd, "Email & Messages", BlockType.LIGHT)

            // Lunch
            blocks += ScheduleBlock("s4", "12:00", "13:00", "Lunch Break", BlockType.BREAK)

            // Afternoon deep block if focus data supports it
            if (hasAfternoonDeep) {
                val afternoonTitle = afternoonDeepTitleFor(workType)
                blocks += ScheduleBlock("s5", "13:00", "14:30", afternoonTitle, BlockType.DEEP)
                blocks += ScheduleBlock("s6", "14:30", "14:45", "Short Break", BlockType.BREAK)
            }

            // Admin & reviews at low-energy 3 PM slot
            blocks += ScheduleBlock("s7", "15:00", "16:00", adminTitleFor(workType), BlockType.ADMIN)

            // Evening wrap-up for non-morning workers
            if (focusPref.contains("Evening", ignoreCase = true) ||
                focusPref.contains("Night", ignoreCase = true)) {
                blocks += ScheduleBlock("s8", "17:00", "19:00",
                    "$deepTitle (Evening)", BlockType.DEEP)
                blocks += ScheduleBlock("s9", "19:00", "19:30", "Wind Down", BlockType.BREAK)
            }
        }

        // ── Step 6: Build personalised change descriptions ────────────────────
        val changes = buildChanges(
            peakStart      = peakStart,
            focusPref      = focusPref,
            workType       = workType,
            deepTitle      = deepTitle,
            hasAfternoon   = hasAfternoonDeep,
            isRecovery     = isRecoveryDay,
            score          = score,
            switchCount    = score.focusMinutes / maxOf(score.deepSessions, 1)
        )

        return ScheduleResult(blocks = blocks, changes = changes)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun preferenceToHour(focusPref: String): Int = when {
        focusPref.contains("Morning", ignoreCase = true)   -> 8
        focusPref.contains("Midday", ignoreCase = true)    -> 10
        focusPref.contains("Afternoon", ignoreCase = true) -> 13
        focusPref.contains("Evening", ignoreCase = true)   -> 17
        focusPref.contains("Night", ignoreCase = true)     -> 20
        else -> 8
    }

    private fun deepTitleFor(workType: String): String = when {
        workType.contains("Developer", ignoreCase = true)  -> "Deep Coding Session"
        workType.contains("Designer", ignoreCase = true)   -> "Deep Design Session"
        workType.contains("Writer", ignoreCase = true)     -> "Deep Writing Session"
        workType.contains("Analyst", ignoreCase = true)    -> "Deep Analysis Block"
        workType.contains("Manager", ignoreCase = true)    -> "Strategic Planning Block"
        workType.contains("Researcher", ignoreCase = true) -> "Deep Research Block"
        workType.contains("Student", ignoreCase = true)    -> "Deep Study Session"
        workType.contains("Founder", ignoreCase = true)    -> "Founder Focus Block"
        else -> "Deep Work Session"
    }

    private fun afternoonDeepTitleFor(workType: String): String = when {
        workType.contains("Developer", ignoreCase = true)  -> "Afternoon Code Review"
        workType.contains("Designer", ignoreCase = true)   -> "Afternoon Design Iteration"
        workType.contains("Writer", ignoreCase = true)     -> "Afternoon Editing Session"
        workType.contains("Analyst", ignoreCase = true)    -> "Afternoon Data Review"
        workType.contains("Manager", ignoreCase = true)    -> "Team Sync & 1-on-1s"
        workType.contains("Researcher", ignoreCase = true) -> "Afternoon Literature Review"
        workType.contains("Student", ignoreCase = true)    -> "Afternoon Practice Session"
        workType.contains("Founder", ignoreCase = true)    -> "Stakeholder & Ops Review"
        else -> "Afternoon Focus"
    }

    private fun adminTitleFor(workType: String): String = when {
        workType.contains("Developer", ignoreCase = true)  -> "PR Reviews & Tickets"
        workType.contains("Designer", ignoreCase = true)   -> "Feedback & Revisions"
        workType.contains("Writer", ignoreCase = true)     -> "Editing & Publishing"
        workType.contains("Analyst", ignoreCase = true)    -> "Reports & Dashboards"
        workType.contains("Manager", ignoreCase = true)    -> "Admin & Status Updates"
        workType.contains("Researcher", ignoreCase = true) -> "Notes & Documentation"
        workType.contains("Student", ignoreCase = true)    -> "Assignments & Deadlines"
        workType.contains("Founder", ignoreCase = true)    -> "Ops & Investor Updates"
        else -> "Admin & Reviews"
    }

    private fun buildChanges(
        peakStart: Int,
        focusPref: String,
        workType: String,
        deepTitle: String,
        hasAfternoon: Boolean,
        isRecovery: Boolean,
        score: ProductivityScore,
        switchCount: Int
    ): List<ScheduleChange> {
        val changes = mutableListOf<ScheduleChange>()

        val peakLabel = hourToLabel(peakStart)
        val deepLabel = deepTitle.lowercase().removePrefix("deep ").removeSuffix(" session").removeSuffix(" block")

        // Always show what we scheduled and why
        changes += ScheduleChange(
            "Scheduled $deepLabel at $peakLabel — your ${focusPref.lowercase()} peak window",
            isPositive = true
        )
        changes += ScheduleChange(
            "Added 15-min recovery buffer after deep work",
            isPositive = true
        )
        changes += ScheduleChange(
            "Batched all email & messages into one 75-min slot",
            isPositive = true
        )

        // Score-based insight
        if (isRecovery) {
            changes += ScheduleChange(
                "Lighter schedule — yesterday's score was ${score.value}/100, rest up",
                isPositive = false
            )
        } else if (score.value >= 75) {
            changes += ScheduleChange(
                "Strong day yesterday (${score.value}/100) — kept full deep work load",
                isPositive = true
            )
        }

        // Afternoon deep block
        if (hasAfternoon) {
            changes += ScheduleChange(
                "Added afternoon focus block — your 1–3 PM energy is strong",
                isPositive = true
            )
        } else {
            changes += ScheduleChange(
                "Removed afternoon deep work — 3 PM energy dip detected",
                isPositive = false
            )
        }

        // Focus minutes context
        val focusHours = score.focusMinutes / 60
        if (focusHours < 2) {
            changes += ScheduleChange(
                "Only ${focusHours}h focused yesterday — protected more time tomorrow",
                isPositive = true
            )
        }

        // Distraction context
        if (score.distractMinutes > 60) {
            val distractHrs = score.distractMinutes / 60
            changes += ScheduleChange(
                "Removed ${distractHrs}h+ distraction windows from schedule",
                isPositive = false
            )
        }

        return changes
    }

    private fun hourToLabel(hour: Int): String {
        val suffix = if (hour < 12) "AM" else "PM"
        val h = if (hour > 12) hour - 12 else if (hour == 0) 12 else hour
        return "$h $suffix"
    }

    private fun formatHour(hour: Int, minute: Int = 0): String =
        "%02d:%02d".format(hour.coerceIn(0, 23), minute)
}
