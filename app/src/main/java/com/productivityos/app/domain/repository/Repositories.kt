package com.productivityos.app.domain.repository

import com.productivityos.app.domain.model.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface ProductivityRepository {
    fun getTodayScore(): Flow<ProductivityScore?>
    fun getWeekScores(): Flow<List<ProductivityScore>>
    suspend fun refreshScore()
}

interface AppUsageRepository {
    fun getTodayUsage(): Flow<List<AppUsage>>
    fun getWeekUsage(): Flow<List<AppUsage>>
    suspend fun syncUsageData()
}

interface HourlyFocusRepository {
    fun getTodayHourlyFocus(): Flow<List<HourlyFocus>>
}

interface InsightRepository {
    fun getTodayInsights(): Flow<List<Insight>>
    suspend fun refreshInsights()
}

interface ScheduleRepository {
    fun getTomorrowSchedule(): Flow<List<ScheduleBlock>>
    suspend fun applySchedule(blocks: List<ScheduleBlock>)
    suspend fun generateNewSchedule()
}

interface UserProfileRepository {
    fun getProfile(): Flow<UserProfile?>
    suspend fun updateProfile(profile: UserProfile)
}

interface SettingsRepository {
    fun getSettings(): Flow<AppSettings>
    suspend fun updateSettings(settings: AppSettings)
}
