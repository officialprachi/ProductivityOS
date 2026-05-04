package com.productivityos.app.domain.model

import java.time.LocalDate
import java.time.LocalTime

// ── Productivity Score ──────────────────────────────────────
data class ProductivityScore(
    val value: Int,               // 0–100
    val focusMinutes: Int,
    val distractMinutes: Int,
    val deepSessions: Int,
    val date: LocalDate = LocalDate.now()
)

// ── App Usage ───────────────────────────────────────────────
data class AppUsage(
    val packageName: String,
    val appName: String,
    val emoji: String,
    val totalMinutes: Int,
    val scoreImpact: Int,         // positive = focus, negative = distraction
    val category: AppCategory
)

enum class AppCategory { FOCUS, DISTRACT, NEUTRAL }

// ── Hour-by-hour focus levels ───────────────────────────────
data class HourlyFocus(
    val hour: Int,                // 0–23
    val level: Int                // 0–10 focus intensity
)

// ── Insight card ────────────────────────────────────────────
data class Insight(
    val id: String,
    val title: String,
    val description: String,
    val impact: InsightImpact,
    val emoji: String
)

enum class InsightImpact { HIGH, MEDIUM, LOW }

// ── Schedule block ──────────────────────────────────────────
data class ScheduleBlock(
    val id: String,
    val startTime: String,        // e.g. "08:00"
    val endTime: String,          // e.g. "10:30"
    val title: String,
    val blockType: BlockType
)

enum class BlockType { DEEP, LIGHT, BREAK, ADMIN }

// ── Schedule change ─────────────────────────────────────────
data class ScheduleChange(
    val description: String,
    val isPositive: Boolean       // true = green dot, false = rose dot
)

// ── User profile ────────────────────────────────────────────
data class UserProfile(
    val name: String,
    val email: String,
    val workerType: String,       // e.g. "Deep Worker"
    val workCategory: String,     // e.g. "Developer"
    val focusPreference: String,  // e.g. "Morning"
    val avgScore: Int,
    val bestStreak: Int,
    val currentStreak: Int,
    val primaryDevice: String,
    val lastStreakDate: String = ""  // ISO date "2025-05-01" — last day streak was active
)

// ── App settings ────────────────────────────────────────────
data class AppSettings(
    val isDarkTheme: Boolean = true,
    val fontSize: FontSize = FontSize.MEDIUM,
    val onDeviceAi: Boolean = false,
    val autoDeleteAfter7Days: Boolean = false,
    val liveTrackerEnabled: Boolean = true,
    val hapticFeedback: Boolean = true,
    val blockedApps: List<String> = listOf(
        "com.instagram.android",
        "com.google.android.youtube",
        "com.whatsapp",
        "com.snapchat.android",
        "com.twitter.android",
        "com.facebook.katana",
        "com.spotify.music",
        "com.netflix.mediaclient"
    ),
    // pkg → daily limit in minutes (0 = no limit)
    val appUsageLimits: Map<String, Int> = emptyMap()
)

enum class FontSize { SMALL, MEDIUM, LARGE, XLARGE }

// ── Focus session ───────────────────────────────────────────
data class FocusSession(
    val durationMinutes: Int,
    val remainingSeconds: Int,
    val isRunning: Boolean,
    val blockedApps: List<String>
)

// ── Weekly data ─────────────────────────────────────────────
data class WeekDay(
    val label: String,            // "Mo", "Tu" etc.
    val focusHours: Float,
    val isToday: Boolean
)

// ── Live tracker alert ──────────────────────────────────────
data class LiveTrackerAlert(
    val appName: String,
    val minutesSpent: Int,
    val plannedActivity: String   // e.g. "deep work"
)

// ── Burnout info ────────────────────────────────────────────
data class BurnoutInfo(
    val workedMinutes: Int,
    val dailyCapacityMinutes: Int = 9 * 60,   // 9 hours
    val zone: BurnoutZone
)

enum class BurnoutZone { SAFE, CAUTION, DANGER }