package com.productivityos.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.productivityos.app.domain.model.*
import com.productivityos.app.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── Schedule ──────────────────────────────────────────────────

sealed class ScheduleUiState {
    object Loading : ScheduleUiState()
    data class Success(
        val blocks: List<ScheduleBlock>,
        val changes: List<ScheduleChange>,
        val applied: Boolean = false
    ) : ScheduleUiState()
    data class Error(val message: String) : ScheduleUiState()
}

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val getSchedule: GetScheduleUseCase,
    private val generateSchedule: GenerateScheduleUseCase,
    private val applySchedule: ApplyScheduleUseCase,
    private val getUserProfile: GetUserProfileUseCase,
    private val getHourlyFocus: GetHourlyFocusUseCase,
    private val getTodayScore: GetTodayScoreUseCase,
    private val scheduleEngine: ScheduleEngine,
    private val hapticHelper: com.productivityos.app.data.local.HapticHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScheduleUiState>(ScheduleUiState.Loading)
    val uiState: StateFlow<ScheduleUiState> = _uiState.asStateFlow()

    init {
        // Generate on first load using ScheduleEngine
        viewModelScope.launch { runScheduleEngine() }

        // Observe stored schedule blocks and keep UI in sync
        viewModelScope.launch {
            getSchedule()
                .catch { _uiState.value = ScheduleUiState.Error(it.message ?: "Error") }
                .collect { blocks ->
                    val current = _uiState.value
                    if (blocks.isNotEmpty() && current is ScheduleUiState.Success) {
                        _uiState.value = current.copy(blocks = blocks)
                    } else if (blocks.isNotEmpty() && current is ScheduleUiState.Loading) {
                        // Blocks loaded from Room before engine ran — show them with empty changes
                        _uiState.value = ScheduleUiState.Success(blocks = blocks, changes = emptyList())
                    }
                }
        }
    }

    // Called by the "Regenerate" / "Apply" button flow
    fun generateNewSchedule() {
        viewModelScope.launch { runScheduleEngine() }
    }

    fun applyNewSchedule() {
        val current = _uiState.value as? ScheduleUiState.Success ?: return
        viewModelScope.launch {
            applySchedule(current.blocks)
            hapticHelper.success()
            _uiState.value = current.copy(applied = true)
        }
    }

    // ── Core: collect all inputs, run ScheduleEngine, persist + emit ──────────

    private suspend fun runScheduleEngine() {
        _uiState.value = ScheduleUiState.Loading

        // Collect the latest snapshot of each input flow
        val hourly  = getHourlyFocus().firstOrNull() ?: emptyList()
        val score   = getTodayScore().firstOrNull()
        val profile = getUserProfile().firstOrNull()

        if (profile == null || score == null) {
            // Not enough data yet — fall back to repository-level generation (uses profile from DB)
            generateSchedule()
            return
        }

        // Run the engine
        val result = scheduleEngine.generate(
            hourlyFocus = hourly,
            score       = score,
            profile     = profile
        )

        // Persist blocks to Room via the repository so the Flow observer picks them up
        applySchedule(result.blocks) // reuses the existing use case which calls repo.applySchedule
        generateSchedule()           // also writes to Room via repo so getTomorrowSchedule() emits

        _uiState.value = ScheduleUiState.Success(
            blocks  = result.blocks,
            changes = result.changes
        )
    }
}

// ── Profile ───────────────────────────────────────────────────

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    data class Success(val profile: UserProfile) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getUserProfile: GetUserProfileUseCase,
    private val userProfileRepository: com.productivityos.app.domain.repository.UserProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getUserProfile()
                .catch { _uiState.value = ProfileUiState.Error(it.message ?: "Error") }
                .collect { profile ->
                    if (profile != null) {
                        _uiState.value = ProfileUiState.Success(profile)
                    }
                }
        }
    }

    fun updateFocusPreference(newPref: String) {
        val current = (_uiState.value as? ProfileUiState.Success)?.profile ?: return
        val updated = current.copy(focusPreference = newPref)
        viewModelScope.launch {
            userProfileRepository.updateProfile(updated)
            // uiState updates automatically via the Room Flow in init
        }
    }

    fun updateWorkCategory(newCategory: String) {
        val current = (_uiState.value as? ProfileUiState.Success)?.profile ?: return
        val updated = current.copy(workCategory = newCategory)
        viewModelScope.launch {
            userProfileRepository.updateProfile(updated)
        }
    }
}

// ── Settings ──────────────────────────────────────────────────

sealed class SettingsUiState {
    object Loading : SettingsUiState()
    data class Success(val settings: AppSettings) : SettingsUiState()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val getSettings: GetSettingsUseCase,
    private val updateSettings: UpdateSettingsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getSettings().collect { settings ->
                _uiState.value = SettingsUiState.Success(settings)
            }
        }
    }

    fun toggleDarkTheme() = updateSetting { it.copy(isDarkTheme = !it.isDarkTheme) }
    fun toggleOnDeviceAi() = updateSetting { it.copy(onDeviceAi = !it.onDeviceAi) }
    fun toggleAutoDelete() = updateSetting { it.copy(autoDeleteAfter7Days = !it.autoDeleteAfter7Days) }
    fun toggleLiveTracker() = updateSetting { it.copy(liveTrackerEnabled = !it.liveTrackerEnabled) }
    fun toggleHapticFeedback() = updateSetting { it.copy(hapticFeedback = !it.hapticFeedback) }
    fun setFontSize(size: FontSize) = updateSetting { it.copy(fontSize = size) }

    private fun updateSetting(transform: (AppSettings) -> AppSettings) {
        val current = (_uiState.value as? SettingsUiState.Success)?.settings ?: return
        val updated = transform(current)
        _uiState.value = SettingsUiState.Success(updated)
        viewModelScope.launch { updateSettings(updated) }
    }
}