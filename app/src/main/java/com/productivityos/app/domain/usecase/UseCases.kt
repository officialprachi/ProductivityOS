package com.productivityos.app.domain.usecase

import com.productivityos.app.domain.model.*
import com.productivityos.app.domain.repository.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTodayScoreUseCase @Inject constructor(
    private val repo: ProductivityRepository
) {
    operator fun invoke(): Flow<ProductivityScore?> = repo.getTodayScore()
}

class GetWeekScoresUseCase @Inject constructor(
    private val repo: ProductivityRepository
) {
    operator fun invoke(): Flow<List<ProductivityScore>> = repo.getWeekScores()
}

class RefreshScoreUseCase @Inject constructor(
    private val repo: ProductivityRepository
) {
    suspend operator fun invoke() = repo.refreshScore()
}

class GetTodayUsageUseCase @Inject constructor(
    private val repo: AppUsageRepository
) {
    operator fun invoke(): Flow<List<AppUsage>> = repo.getTodayUsage()
}

class GetWeekUsageUseCase @Inject constructor(
    private val repo: AppUsageRepository
) {
    operator fun invoke(): Flow<List<AppUsage>> = repo.getWeekUsage()
}

class SyncUsageDataUseCase @Inject constructor(
    private val repo: AppUsageRepository
) {
    suspend operator fun invoke() = repo.syncUsageData()
}

class GetHourlyFocusUseCase @Inject constructor(
    private val repo: HourlyFocusRepository
) {
    operator fun invoke(): Flow<List<HourlyFocus>> = repo.getTodayHourlyFocus()
}

class GetInsightsUseCase @Inject constructor(
    private val repo: InsightRepository
) {
    operator fun invoke(): Flow<List<Insight>> = repo.getTodayInsights()
}

class RefreshInsightsUseCase @Inject constructor(
    private val repo: InsightRepository
) {
    suspend operator fun invoke() = repo.refreshInsights()
}

class GetScheduleUseCase @Inject constructor(
    private val repo: ScheduleRepository
) {
    operator fun invoke(): Flow<List<ScheduleBlock>> = repo.getTomorrowSchedule()
}

class GenerateScheduleUseCase @Inject constructor(
    private val repo: ScheduleRepository
) {
    suspend operator fun invoke() = repo.generateNewSchedule()
}

class ApplyScheduleUseCase @Inject constructor(
    private val repo: ScheduleRepository
) {
    suspend operator fun invoke(blocks: List<ScheduleBlock>) = repo.applySchedule(blocks)
}

class GetUserProfileUseCase @Inject constructor(
    private val repo: UserProfileRepository
) {
    operator fun invoke(): Flow<UserProfile?> = repo.getProfile()
}

class GetSettingsUseCase @Inject constructor(
    private val repo: SettingsRepository
) {
    operator fun invoke(): Flow<AppSettings> = repo.getSettings()
}

class UpdateSettingsUseCase @Inject constructor(
    private val repo: SettingsRepository
) {
    suspend operator fun invoke(settings: AppSettings) = repo.updateSettings(settings)
}
