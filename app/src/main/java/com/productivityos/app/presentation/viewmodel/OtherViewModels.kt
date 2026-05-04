package com.productivityos.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.productivityos.app.domain.model.*
import com.productivityos.app.domain.usecase.*
import com.productivityos.app.data.local.FocusBlockerService
import com.productivityos.app.data.local.FocusBlockerService.Companion.KEY_SESSION_END_MS
import com.productivityos.app.data.local.FocusBlockerService.Companion.KEY_SESSION_START_MS
import com.productivityos.app.data.local.FocusBlockerService.Companion.KEY_SESSION_COMMITTED
import com.productivityos.app.data.local.FocusBlockerService.Companion.KEY_TOTAL_FOCUS_MINUTES
import com.productivityos.app.data.local.FocusBlockerService.Companion.PREFS_NAME
import com.productivityos.app.data.local.HapticHelper
import com.productivityos.app.data.local.NotificationHelper
import com.productivityos.app.domain.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.time.LocalDate
import java.time.temporal.ChronoField

// ── Insights ─────────────────────────────────────────────────

sealed class InsightsUiState {
    object Loading : InsightsUiState()
    data class Success(val insights: List<Insight>) : InsightsUiState()
    data class Error(val message: String) : InsightsUiState()
}

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val getInsights: GetInsightsUseCase,
    private val refreshInsights: RefreshInsightsUseCase,
    private val getTodayUsage: GetTodayUsageUseCase,
    private val getTodayScore: GetTodayScoreUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<InsightsUiState>(InsightsUiState.Loading)
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        // Step 1: wait for usage + score data to be present, then run engine
        viewModelScope.launch {
            combine(
                getTodayUsage(),
                getTodayScore()
            ) { usage, score -> usage to score }
                .collect { (usage, score) ->
                    // Only trigger engine when we have real data
                    if (usage.isNotEmpty() && score != null) {
                        refreshInsights()
                    } else {
                        // No data yet — still seed demo insights so UI isn't empty
                        refreshInsights()
                    }
                }
        }

        // Step 2: observe insights from Room and push to UI
        viewModelScope.launch {
            getInsights()
                .catch { _uiState.value = InsightsUiState.Error(it.message ?: "Error") }
                .collect { insights ->
                    _uiState.value = if (insights.isEmpty()) InsightsUiState.Loading
                    else InsightsUiState.Success(insights)
                }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching { refreshInsights() }
                .onFailure { _uiState.value = InsightsUiState.Error(it.message ?: "Refresh failed") }
        }
    }
}

// ── Analytics ─────────────────────────────────────────────────

data class WeeklyData(
    val days: List<WeekDay>,
    val totalTracked: String,
    val scoreImpact: String,
    val focusTime: String,
    val distractTime: String
)

sealed class AnalyticsUiState {
    object Loading : AnalyticsUiState()
    data class Success(
        val todayUsage: List<AppUsage>,
        val weeklyData: WeeklyData,
        val focusSessionMinutes: Int = 0,
        val todayScore: ProductivityScore? = null
    ) : AnalyticsUiState()
    data class Error(val message: String) : AnalyticsUiState()
}

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val getTodayUsage: GetTodayUsageUseCase,
    private val getWeekScores: GetWeekScoresUseCase,
    private val syncUsageData: SyncUsageDataUseCase,
    private val refreshScore: RefreshScoreUseCase,
    private val getTodayScore: GetTodayScoreUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<AnalyticsUiState>(AnalyticsUiState.Loading)
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    private val dayLabels = listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su")

    init {
        // Observe Room — UI updates automatically whenever DB rows change
        viewModelScope.launch {
            combine(getTodayUsage(), getWeekScores(), getTodayScore()) { usage, weekScores, score ->
                Triple(usage, weekScores, score)
            }
                .catch { _uiState.value = AnalyticsUiState.Error(it.message ?: "Error") }
                .collect { (usage, weekScores, score) ->
                    buildState(usage, weekScores, score)
                }
        }
        // Initial sync on open
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) { syncAndRefresh() }
    }

    /** Called from screen every 60 s via LaunchedEffect ticker */
    fun refreshLiveData() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) { syncAndRefresh() }
    }

    private suspend fun syncAndRefresh() {
        runCatching { syncUsageData() }
        runCatching { refreshScore() }
    }

    private fun buildState(usage: List<AppUsage>, weekScores: List<ProductivityScore>, score: ProductivityScore?) {
        val sortedUsage = usage.sortedByDescending { it.totalMinutes }
        val total = usage.sumOf { it.totalMinutes }
        val hrs = total / 60
        val mins = total % 60
        val impact = usage.sumOf { it.scoreImpact }.coerceIn(-999, 999)
        val focusMinutes = usage
            .filter { it.category == AppCategory.FOCUS }
            .sumOf { it.totalMinutes }
        val distractMinutes = usage
            .filter { it.category == AppCategory.DISTRACT }
            .sumOf { it.totalMinutes }

        val scoreByDay = weekScores.associateBy { it.date }
        val today = LocalDate.now()
        val monday = today.with(ChronoField.DAY_OF_WEEK, 1)
        val weekDays = (0..6).map { offset ->
            val date = monday.plusDays(offset.toLong())
            val label = dayLabels[offset]
            val score = scoreByDay[date]
            val focusHours = (score?.focusMinutes ?: 0) / 60f
            WeekDay(label, focusHours, date == today)
        }

        // Derive net score impact: focus gains minus distraction penalty
        val focusPoints      = ((focusMinutes / 15) * 10).coerceAtMost(70)
        val distractPenalty  = ((distractMinutes / 15) * 8).coerceAtMost(40)
        val netImpact        = focusPoints - distractPenalty
        val liveScore        = (30 + netImpact).coerceIn(0, 100)

        val prefs = context.getSharedPreferences(
            PREFS_NAME,
            android.content.Context.MODE_PRIVATE
        )
        val focusSessionMins = prefs.getInt(KEY_TOTAL_FOCUS_MINUTES, 0) / 60

        _uiState.value = AnalyticsUiState.Success(
            todayUsage = sortedUsage,
            focusSessionMinutes = focusSessionMins,
            todayScore = score,
            weeklyData = WeeklyData(
                days = weekDays,
                totalTracked = "${hrs}h ${mins}m",
                scoreImpact = if (netImpact >= 0) "+${netImpact} pts" else "${netImpact} pts",
                focusTime = "${focusMinutes / 60}h ${focusMinutes % 60}m",
                distractTime = "${distractMinutes / 60}h ${distractMinutes % 60}m"
            )
        )
    }
}

// ── Focus ─────────────────────────────────────────────────────

// Maps known distracting package names to friendly labels
internal val knownAppNames = mapOf(
    "com.instagram.android"        to "Instagram",
    "com.google.android.youtube"   to "YouTube",
    "com.whatsapp"                 to "WhatsApp",
    "com.snapchat.android"         to "Snapchat",
    "com.twitter.android"          to "Twitter / X",
    "com.facebook.katana"          to "Facebook",
    "com.spotify.music"            to "Spotify",
    "com.netflix.mediaclient"      to "Netflix"
)

data class FocusUiState(
    val selectedDuration: Int = 90,               // minutes
    val remainingSeconds: Int = 90 * 60,
    val totalSeconds: Int = 90 * 60,
    val isRunning: Boolean = false,
    val totalFocusSessionMinutes: Int = 0,        // accumulated focus session time today
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
    val burnoutWorkedMinutes: Int = 0,
    val burnoutCapacityMinutes: Int = 540,
    val burnoutZone: BurnoutZone = BurnoutZone.SAFE,
    // pkg → daily limit in minutes (0 or absent = no limit)
    val appUsageLimits: Map<String, Int> = emptyMap(),
    // today's actual usage for limit progress display
    val todayUsageMinutes: Map<String, Int> = emptyMap(),
    // which app's limit dialog is open (null = closed)
    val limitDialogPkg: String? = null
)

@HiltViewModel
class FocusViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val notificationHelper: NotificationHelper,
    private val hapticHelper: HapticHelper,
    private val getTodayUsage: GetTodayUsageUseCase,
    private val getSettings: GetSettingsUseCase,
    private val updateSettings: UpdateSettingsUseCase,
    private val profileRepository: UserProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FocusUiState())
    val uiState: StateFlow<FocusUiState> = _uiState.asStateFlow()

    private var timerJob: kotlinx.coroutines.Job? = null

    init {
        observeBurnout()
        observeSettings()
        observeTodayUsage()
        resumeTimerFromWallClock()   // re-sync timer if session was running before app switch
        loadTotalFocusMinutes()      // load today's accumulated focus session time
    }

    // ── Load accumulated focus session minutes from SharedPrefs ──

    private fun loadTotalFocusMinutes() {
        // Reset focus-session seconds if it's a new day (in case HomeViewModel hasn't run yet)
        com.productivityos.app.data.local.FocusBlockerService.resetIfNewDay(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        // KEY_TOTAL_FOCUS_MINUTES now stores seconds. If old value was in minutes it would be
        // a small number (≤ 1440) which is also a valid seconds value (≤ 24min) — acceptable drift.
        // Reset key if suspiciously large (> 86400s = 24h) to clear any corrupted state.
        val stored = prefs.getInt(KEY_TOTAL_FOCUS_MINUTES, 0)
        if (stored > 86400) {
            prefs.edit().putInt(KEY_TOTAL_FOCUS_MINUTES, 0).apply()
            _uiState.update { it.copy(totalFocusSessionMinutes = 0) }
        } else {
            _uiState.update { it.copy(totalFocusSessionMinutes = stored / 60) }
        }
    }

    // Adds elapsed seconds — stored as seconds in SharedPrefs, displayed as minutes.
    // `commitSession` = true means this is the final commit for a session (natural complete
    // or manual stop). Uses KEY_SESSION_COMMITTED flag to prevent double-counting if
    // ViewModel is recreated while a session is mid-flight.
    private fun addFocusSeconds(seconds: Int, commitSession: Boolean = false) {
        if (seconds <= 0) return
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        if (commitSession) {
            // Guard: skip if this session was already committed
            if (prefs.getBoolean(KEY_SESSION_COMMITTED, false)) return
            prefs.edit().putBoolean(KEY_SESSION_COMMITTED, true).apply()
        }
        val current = prefs.getInt(KEY_TOTAL_FOCUS_MINUTES, 0)   // stored as seconds now
        val updated = current + seconds
        prefs.edit().putInt(KEY_TOTAL_FOCUS_MINUTES, updated).apply()
        _uiState.update { it.copy(totalFocusSessionMinutes = updated / 60) }
    }

    // ── Resume timer from wall clock if session was running ───
    // Uses KEY_SESSION_END_MS stored by FocusBlockerService so timer
    // survives app switching, backgrounding, or ViewModel recreation.

    private fun resumeTimerFromWallClock() {
        val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val endMs = prefs.getLong(KEY_SESSION_END_MS, 0L)
        if (endMs <= 0L) return                                  // no active session

        val remaining = ((endMs - System.currentTimeMillis()) / 1000).toInt()
        if (remaining <= 0) {
            // Session already expired while app was away — commit actual elapsed seconds
            val startMs = prefs.getLong(KEY_SESSION_START_MS, 0L)
            if (startMs > 0L) {
                val elapsedSeconds = ((endMs - startMs) / 1000).toInt().coerceAtLeast(0)
                addFocusSeconds(elapsedSeconds, commitSession = true)
            } else {
                // Fallback: use stored duration if start time is unavailable
                val duration = prefs.getInt(FocusBlockerService.Companion.KEY_SESSION_DURATION_MINS, 0)
                addFocusSeconds(duration * 60, commitSession = true)
            }
            prefs.edit().remove(KEY_SESSION_END_MS).apply()
            return
        }

        // Restore running state with correct remaining time
        _uiState.update { it.copy(isRunning = true, remainingSeconds = remaining) }

        // Re-attach countdown coroutine driven by wall clock
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(500)
                val left = ((endMs - System.currentTimeMillis()) / 1000).toInt()
                if (left <= 0) {
                    // Use actual elapsed (start → end) for accuracy
                    val startMs = prefs.getLong(KEY_SESSION_START_MS, 0L)
                    val elapsedSeconds = if (startMs > 0L)
                        ((endMs - startMs) / 1000).toInt().coerceAtLeast(0)
                    else
                        _uiState.value.selectedDuration * 60
                    addFocusSeconds(elapsedSeconds, commitSession = true)
                    _uiState.update { it.copy(isRunning = false, remainingSeconds = 0) }
                    FocusBlockerService.stopBlocking(context)
                    notificationHelper.showFocusComplete(_uiState.value.selectedDuration)
                    hapticHelper.success()
                    val profile = profileRepository.getProfile().firstOrNull()
                    if (profile != null) {
                        val newStreak = profile.currentStreak + 1
                        profileRepository.updateProfile(
                            profile.copy(
                                currentStreak = newStreak,
                                bestStreak    = maxOf(profile.bestStreak, newStreak)
                            )
                        )
                    }
                    break
                }
                if (!_uiState.value.isRunning) break
                _uiState.update { it.copy(remainingSeconds = left) }
            }
        }
    }

    // ── Load blocked apps + limits from settings (single collector to avoid race) ──

    private fun observeSettings() {
        viewModelScope.launch {
            getSettings().collect { settings ->
                _uiState.update {
                    it.copy(
                        blockedApps    = settings.blockedApps,
                        appUsageLimits = settings.appUsageLimits
                    )
                }
            }
        }
    }

    // ── Track today's real usage minutes per app ──────────────

    private fun observeTodayUsage() {
        viewModelScope.launch {
            getTodayUsage().collect { usageList ->
                val minuteMap = usageList.associate { it.packageName to it.totalMinutes }
                _uiState.update { it.copy(todayUsageMinutes = minuteMap) }
            }
        }
    }

    // ── Set daily time limit for an app (0 = remove limit) ───

    fun setUsageLimit(pkg: String, limitMinutes: Int) {
        viewModelScope.launch {
            val settings = getSettings().first()
            val updatedLimits = if (limitMinutes <= 0)
                settings.appUsageLimits - pkg
            else
                settings.appUsageLimits + (pkg to limitMinutes)
            updateSettings(settings.copy(appUsageLimits = updatedLimits))
        }
    }

    fun openLimitDialog(pkg: String) {
        _uiState.update { it.copy(limitDialogPkg = pkg) }
    }

    fun closeLimitDialog() {
        _uiState.update { it.copy(limitDialogPkg = null) }
    }

    // ── Toggle a single app in/out of blocked list ────────────

    fun toggleBlockedApp(pkg: String) {
        viewModelScope.launch {
            val settings = getSettings().first()
            val updated = if (pkg in settings.blockedApps)
                settings.blockedApps - pkg
            else
                settings.blockedApps + pkg
            updateSettings(settings.copy(blockedApps = updated))
        }
    }

    // ── Burnout: derive from real today usage ─────────────────

    private fun observeBurnout() {
        viewModelScope.launch {
            getTodayUsage().collect { usageList ->
                val totalMinutes = usageList.sumOf { it.totalMinutes }
                val capacityMinutes = 540 // 9h max
                val zone = when {
                    totalMinutes >= capacityMinutes * 0.9 -> BurnoutZone.DANGER
                    totalMinutes >= capacityMinutes * 0.7 -> BurnoutZone.CAUTION
                    else                                  -> BurnoutZone.SAFE
                }
                _uiState.update {
                    it.copy(
                        burnoutWorkedMinutes  = totalMinutes,
                        burnoutCapacityMinutes = capacityMinutes,
                        burnoutZone           = zone
                    )
                }
            }
        }
    }

    fun selectDuration(minutes: Int) {
        _uiState.update {
            it.copy(
                selectedDuration = minutes,
                remainingSeconds = minutes * 60,
                totalSeconds = minutes * 60,
                isRunning = false
            )
        }
        timerJob?.cancel()
    }

    fun toggleSession() {
        val current = _uiState.value
        if (current.isRunning) {
            // ── Stop session ──────────────────────────────────
            timerJob?.cancel()
            // Elapsed seconds = total - remaining (both in seconds)
            val elapsedSeconds = current.selectedDuration * 60 - current.remainingSeconds
            addFocusSeconds(elapsedSeconds, commitSession = true)
            _uiState.update { it.copy(isRunning = false) }
            FocusBlockerService.stopBlocking(context)
            hapticHelper.warning()
        } else {
            // ── Start session ─────────────────────────────────
            _uiState.update { it.copy(isRunning = true) }
            hapticHelper.focusStart()

            // Start the blocker service with current blocked apps + duration
            FocusBlockerService.startBlocking(
                context        = context,
                blockedApps    = current.blockedApps,
                durationMinutes = current.selectedDuration
            )

            // Countdown timer — wall-clock based so it survives app switches
            val endMs = context
                .getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
                .getLong(KEY_SESSION_END_MS, 0L)
            val startMs = context
                .getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
                .getLong(KEY_SESSION_START_MS, 0L)

            timerJob = viewModelScope.launch {
                while (true) {
                    delay(500)
                    val left = ((endMs - System.currentTimeMillis()) / 1000).toInt()
                    if (left <= 0 || !_uiState.value.isRunning) {
                        // Timer hit zero — stop naturally
                        if (left <= 0 && _uiState.value.isRunning) {
                            val duration = _uiState.value.selectedDuration
                            // Use actual elapsed from wall clock for accuracy
                            val elapsedSeconds = if (startMs > 0L)
                                ((endMs - startMs) / 1000).toInt().coerceAtLeast(0)
                            else
                                duration * 60
                            addFocusSeconds(elapsedSeconds, commitSession = true)
                            _uiState.update { it.copy(isRunning = false, remainingSeconds = 0) }
                            FocusBlockerService.stopBlocking(context)
                            notificationHelper.showFocusComplete(duration)
                            hapticHelper.success()
                            val profile = profileRepository.getProfile().firstOrNull()
                            if (profile != null) {
                                val newStreak = profile.currentStreak + 1
                                profileRepository.updateProfile(
                                    profile.copy(
                                        currentStreak = newStreak,
                                        bestStreak    = maxOf(profile.bestStreak, newStreak)
                                    )
                                )
                            }
                        }
                        break
                    }
                    _uiState.update { it.copy(remainingSeconds = left) }
                }
            }
        }
    }

    /** Friendly label for a blocked package name. */
    fun friendlyName(packageName: String): String =
        knownAppNames[packageName]
            ?: packageName.substringAfterLast(".").replaceFirstChar { it.uppercaseChar() }

    override fun onCleared() {
        super.onCleared()
        // Safety: clean up if ViewModel is destroyed mid-session
        if (_uiState.value.isRunning) {
            FocusBlockerService.stopBlocking(context)
        }
    }
}