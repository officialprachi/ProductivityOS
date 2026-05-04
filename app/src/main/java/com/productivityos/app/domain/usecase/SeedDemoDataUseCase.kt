package com.productivityos.app.domain.usecase

import com.productivityos.app.domain.model.*
import com.productivityos.app.domain.repository.ScheduleRepository
import com.productivityos.app.domain.repository.SettingsRepository
import com.productivityos.app.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

/**
 * Seeds the local database on first launch.
 * Profile is only written once — if one already exists, only name/email are
 * updated so real streak and score data are never overwritten by fake values.
 */
class SeedDemoDataUseCase @Inject constructor(
    private val scheduleRepo: ScheduleRepository,
    private val profileRepo: UserProfileRepository,
    private val settingsRepo: SettingsRepository
) {
    suspend operator fun invoke(
        userName: String = "User",
        userEmail: String = ""
    ) {
        scheduleRepo.generateNewSchedule()
        settingsRepo.updateSettings(AppSettings())

        val existing = profileRepo.getProfile().firstOrNull()
        if (existing == null) {
            // First install — write a clean profile with zero streaks
            profileRepo.updateProfile(
                UserProfile(
                    name = userName,
                    email = userEmail,
                    workerType = "Deep Worker",
                    workCategory = "Developer",
                    focusPreference = "Morning",
                    avgScore = 0,
                    bestStreak = 0,
                    currentStreak = 0,
                    primaryDevice = "Android Device"
                )
            )
        } else {
            // Profile exists — only refresh name/email, never touch streak or avgScore
            profileRepo.updateProfile(
                existing.copy(name = userName, email = userEmail)
            )
        }
    }
}
