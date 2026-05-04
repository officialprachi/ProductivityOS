package com.productivityos.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.productivityos.app.data.local.FocusBlockerService
import com.productivityos.app.data.local.LiveTrackerManager
import com.productivityos.app.domain.model.*
import com.productivityos.app.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import java.time.LocalDate
import javax.inject.Inject

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val score: ProductivityScore,
        val totalUsedMinutes: Int,
        val topLeakApp: AppUsage?,
        val hourlyFocus: List<HourlyFocus>,
        val streak: Int,
        val bestStreak: Int,
        val userName: String = "",
        val totalFocusSessionMinutes: Int = 0,
        val showLiveTrackerAlert: Boolean = false,
        val liveTrackerAlert: LiveTrackerAlert? = null
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val getTodayScore: GetTodayScoreUseCase,
    private val getTodayUsage: GetTodayUsageUseCase,
    private val getHourlyFocus: GetHourlyFocusUseCase,
    private val getUserProfile: GetUserProfileUseCase,
    private val getSettings: GetSettingsUseCase,
    private val refreshScore: RefreshScoreUseCase,
    private val syncUsage: SyncUsageDataUseCase,
    private val liveTrackerManager: LiveTrackerManager,
    private val userProfileRepository: com.productivityos.app.domain.repository.UserProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        performDailyResetIfNeeded()
        seedDataIfEmpty()
        observeData()
        startLiveTrackerIfEnabled()
        collectAlerts()
        pollFocusSessionMinutes()
        startPeriodicScoreRefresh()
        checkDailyStreak()
    }

    /**
     * Resets focus-session seconds and triggers a full data refresh when the
     * calendar date has changed since the last reset. This ensures score, streak,
     * deep-session count, and focus-time shown on the Home screen always reflect
     * the current day and start from zero at midnight.
     */
    private fun performDailyResetIfNeeded() {
        val wasReset = FocusBlockerService.resetIfNewDay(context)
        if (wasReset) {
            // New day: immediately re-sync usage + score so stale yesterday data
            // is not shown while the coroutines below boot up.
            viewModelScope.launch(Dispatchers.IO) {
                runCatching { syncUsage() }
                runCatching { refreshScore() }
                pushLatestScore()
            }
        }
    }

    private fun seedDataIfEmpty() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            runCatching { syncUsage() }
            runCatching { refreshScore() }
            pushLatestScore()
        }
    }

    /** Called from HomeScreen every 60 s via LaunchedEffect ticker */
    fun refreshLiveData() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            runCatching { syncUsage() }
            runCatching { refreshScore() }
            // Force-push latest score from Room to UI immediately after write
            pushLatestScore()
        }
    }

    /** Called on app open — maintains daily streak based on calendar days */
    private fun checkDailyStreak() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val profile = userProfileRepository.getProfile().firstOrNull() ?: return@runCatching
                val today     = LocalDate.now()
                val todayStr  = today.toString()

                // Already processed today — no change needed
                if (profile.lastStreakDate == todayStr) return@runCatching

                val lastDate = if (profile.lastStreakDate.isNotBlank())
                    runCatching { LocalDate.parse(profile.lastStreakDate) }.getOrNull()
                else null

                val newStreak = when {
                    lastDate == null -> 1                          // first time ever
                    lastDate == today.minusDays(1) -> profile.currentStreak + 1   // consecutive day
                    lastDate == today -> profile.currentStreak     // same day (shouldn't reach here)
                    else -> 1                                      // gap → reset streak
                }

                userProfileRepository.updateProfile(
                    profile.copy(
                        currentStreak  = newStreak,
                        bestStreak     = maxOf(profile.bestStreak, newStreak),
                        lastStreakDate  = todayStr
                    )
                )
            }
        }
    }

    /** Background coroutine: re-sync usage + score every 60 s so score stays live */
    private fun startPeriodicScoreRefresh() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            while (true) {
                kotlinx.coroutines.delay(60_000L)
                runCatching { syncUsage() }
                runCatching { refreshScore() }
                pushLatestScore()
            }
        }
    }

    /** Read latest score + usage directly from use cases and update _uiState immediately */
    private suspend fun pushLatestScore() {
        try {
            val score   = getTodayScore().firstOrNull() ?: return
            val usage   = getTodayUsage().firstOrNull() ?: emptyList()
            val hourly  = getHourlyFocus().firstOrNull() ?: emptyList()
            val profile = getUserProfile().firstOrNull()
            val prefs   = context.getSharedPreferences(
                FocusBlockerService.PREFS_NAME, android.content.Context.MODE_PRIVATE
            )
            val focusSessionMins = prefs.getInt(FocusBlockerService.KEY_TOTAL_FOCUS_MINUTES, 0) / 60
            val topLeak = usage.filter { it.category == AppCategory.DISTRACT }
                .maxByOrNull { it.totalMinutes }

            val newState = HomeUiState.Success(
                score                  = score,
                totalUsedMinutes       = usage.sumOf { it.totalMinutes },
                topLeakApp             = topLeak,
                hourlyFocus            = hourly,
                streak                 = profile?.currentStreak ?: 0,
                bestStreak             = profile?.bestStreak ?: 0,
                userName               = profile?.name ?: "",
                totalFocusSessionMinutes = focusSessionMins
            )
            // Preserve any live tracker alert that may be showing
            val existing = _uiState.value as? HomeUiState.Success
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                _uiState.value = if (existing?.liveTrackerAlert != null) {
                    newState.copy(
                        liveTrackerAlert    = existing.liveTrackerAlert,
                        showLiveTrackerAlert = existing.showLiveTrackerAlert
                    )
                } else newState
            }
        } catch (_: Exception) {}
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                getTodayScore(),
                getTodayUsage(),
                getHourlyFocus(),
                getUserProfile()
            ) { score, usage, hourly, profile ->
                if (score == null) return@combine HomeUiState.Loading
                val topLeak = usage.filter { it.category == AppCategory.DISTRACT }
                    .maxByOrNull { it.totalMinutes }
                val prefs = context.getSharedPreferences(
                    FocusBlockerService.PREFS_NAME, android.content.Context.MODE_PRIVATE
                )
                val focusSessionMins = prefs.getInt(FocusBlockerService.KEY_TOTAL_FOCUS_MINUTES, 0) / 60
                HomeUiState.Success(
                    score = score,
                    totalUsedMinutes = usage.sumOf { it.totalMinutes },
                    topLeakApp = topLeak,
                    hourlyFocus = hourly,
                    streak = profile?.currentStreak ?: 0,
                    bestStreak = profile?.bestStreak ?: 0,
                    userName = profile?.name ?: "",
                    totalFocusSessionMinutes = focusSessionMins
                )
            }.catch { e ->
                _uiState.value = HomeUiState.Error(e.message ?: "Unknown error")
            }.collect { state ->
                // Preserve any active alert when data refreshes
                val existingAlert = (_uiState.value as? HomeUiState.Success)?.liveTrackerAlert
                val showAlert     = (_uiState.value as? HomeUiState.Success)?.showLiveTrackerAlert ?: false
                _uiState.value = if (state is HomeUiState.Success && existingAlert != null) {
                    state.copy(liveTrackerAlert = existingAlert, showLiveTrackerAlert = showAlert)
                } else state            }
        }
    }

    // ── Live Tracker ──────────────────────────────────────────

    private fun startLiveTrackerIfEnabled() {
        viewModelScope.launch {
            getSettings().collect { settings ->
                if (settings.liveTrackerEnabled) {
                    liveTrackerManager.start(liveTrackerEnabled = true)
                } else {
                    liveTrackerManager.stop()
                }
            }
        }
    }

    private fun collectAlerts() {
        viewModelScope.launch {
            liveTrackerManager.alertFlow.collect { alert ->
                val current = _uiState.value as? HomeUiState.Success ?: return@collect
                if (alert != null) {
                    _uiState.value = current.copy(
                        showLiveTrackerAlert = true,
                        liveTrackerAlert = alert
                    )
                } else {
                    _uiState.value = current.copy(showLiveTrackerAlert = false)
                }
            }
        }
    }

    // ── Poll focus session minutes every 30s so UI stays live ──

    private fun pollFocusSessionMinutes() {
        viewModelScope.launch {
            while (true) {
                val prefs = context.getSharedPreferences(
                    FocusBlockerService.PREFS_NAME, android.content.Context.MODE_PRIVATE
                )
                val mins = prefs.getInt(FocusBlockerService.KEY_TOTAL_FOCUS_MINUTES, 0) / 60
                val current = _uiState.value as? HomeUiState.Success
                if (current != null && current.totalFocusSessionMinutes != mins) {
                    _uiState.value = current.copy(totalFocusSessionMinutes = mins)
                }
                kotlinx.coroutines.delay(30_000L)
            }
        }
    }

    fun showLiveTrackerAlert() {
        val current = _uiState.value
        if (current is HomeUiState.Success) {
            _uiState.value = current.copy(
                showLiveTrackerAlert = true,
                liveTrackerAlert = LiveTrackerAlert(
                    appName = "YouTube",
                    minutesSpent = 18,
                    plannedActivity = "deep work"
                )
            )
        }
    }

    fun dismissLiveTrackerAlert() {
        val current = _uiState.value
        if (current is HomeUiState.Success) {
            _uiState.value = current.copy(showLiveTrackerAlert = false)
            liveTrackerManager.clearAlert()
        }
    }

    fun switchToFocusMode() {
        dismissLiveTrackerAlert()
    }
}