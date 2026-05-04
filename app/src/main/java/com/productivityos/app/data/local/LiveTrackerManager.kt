package com.productivityos.app.data.local

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import com.productivityos.app.data.local.ScheduleBlockDao
import com.productivityos.app.domain.model.AppCategory
import com.productivityos.app.domain.model.BlockType
import com.productivityos.app.domain.model.LiveTrackerAlert
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LiveTrackerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scheduleBlockDao: ScheduleBlockDao,
    private val usageStatsHelper: UsageStatsHelper
) {
    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollingJob: Job? = null

    private val _alertFlow = MutableSharedFlow<LiveTrackerAlert?>(replay = 1)
    val alertFlow: Flow<LiveTrackerAlert?> = _alertFlow.asSharedFlow()

    // Track how long the current distraction app has been in foreground
    private var distractionStartMs: Long = 0L
    private var lastDistractPackage: String = ""

    // ── Start / stop ──────────────────────────────────────────

    fun start(liveTrackerEnabled: Boolean) {
        if (!liveTrackerEnabled) return
        if (pollingJob?.isActive == true) return
        pollingJob = scope.launch { pollLoop() }
    }

    fun stop() {
        pollingJob?.cancel()
        pollingJob = null
    }

    // ── Poll loop — every 60 seconds ──────────────────────────

    private suspend fun pollLoop() {
        while (true) {
            runCatching { checkForegroundApp() }
            delay(60_000L)
        }
    }

    private suspend fun checkForegroundApp() {
        val now = System.currentTimeMillis()
        val windowStart = now - 5_000L // last 5 seconds window

        val events = usageStatsManager.queryEvents(windowStart, now)
        val event = UsageEvents.Event()
        var foregroundPkg: String? = null

        while (events?.hasNextEvent() == true) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                foregroundPkg = event.packageName
            }
        }

        if (foregroundPkg == null || foregroundPkg == context.packageName) return

        val appName = usageStatsHelper.resolveAppName(foregroundPkg)
        val category = usageStatsHelper.resolveCategory(foregroundPkg, appName)

        if (category != AppCategory.DISTRACT) {
            // Reset tracking when user moves to non-distract app
            lastDistractPackage = ""
            distractionStartMs = 0L
            return
        }

        // Track how long this distraction has been running
        if (foregroundPkg != lastDistractPackage) {
            lastDistractPackage = foregroundPkg
            distractionStartMs = now
        }
        val minutesSpent = ((now - distractionStartMs) / 60_000L).toInt().coerceAtLeast(1)

        // Check if current time falls inside a DEEP block for tomorrow's schedule
        val deepBlock = currentDeepBlock() ?: return

        _alertFlow.emit(
            LiveTrackerAlert(
                appName       = appName,
                minutesSpent  = minutesSpent,
                plannedActivity = deepBlock
            )
        )
    }

    // ── Check if right now is inside a scheduled DEEP block ───

    private suspend fun currentDeepBlock(): String? {
        val tomorrow = LocalDate.now().plusDays(1).toString()
        val today    = LocalDate.now().toString()
        val fmt      = DateTimeFormatter.ofPattern("HH:mm")
        val nowTime  = LocalTime.now()

        // Check both today and tomorrow's schedule
        for (date in listOf(today, tomorrow)) {
            val blocks = scheduleBlockDao.getBlocksForDate(date).firstOrNull() ?: continue
            val deepBlock = blocks.firstOrNull { block ->
                block.blockType == BlockType.DEEP.name &&
                runCatching {
                    val start = LocalTime.parse(block.startTime, fmt)
                    val end   = LocalTime.parse(block.endTime, fmt)
                    nowTime.isAfter(start) && nowTime.isBefore(end)
                }.getOrDefault(false)
            }
            if (deepBlock != null) return deepBlock.title
        }
        return null
    }

    fun clearAlert() {
        scope.launch { _alertFlow.emit(null) }
    }
}

// Extension helpers so LiveTrackerManager can resolve app name/category
// without duplicating the logic from UsageStatsHelper

fun UsageStatsHelper.resolveAppName(packageName: String): String =
    queryTodayUsage().firstOrNull { it.packageName == packageName }?.appName
        ?: packageName.substringAfterLast(".").replaceFirstChar { it.uppercaseChar() }

fun UsageStatsHelper.resolveCategory(packageName: String, appName: String): AppCategory =
    queryTodayUsage().firstOrNull { it.packageName == packageName }?.category
        ?: AppCategory.NEUTRAL
