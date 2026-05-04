package com.productivityos.app.data.local

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.productivityos.app.domain.model.AppCategory
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

@HiltWorker
class DailyScoreWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val scoreDao: ProductivityScoreDao,
    private val appUsageDao: AppUsageDao,
    private val notificationHelper: NotificationHelper,
    private val userProfileDao: UserProfileDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val today = LocalDate.now().toString()

        val scoreEntity = scoreDao.getScoreForDate(today).firstOrNull() ?: return Result.success()
        val usageList   = appUsageDao.getUsageForDate(today).firstOrNull() ?: emptyList()

        val focusHours = scoreEntity.focusMinutes / 60
        val topLeak    = usageList
            .filter { it.category == AppCategory.DISTRACT.name }
            .maxByOrNull { it.totalMinutes }
            ?.appName ?: "None"

        notificationHelper.showDailyScore(
            score      = scoreEntity.score,
            focusHours = focusHours,
            topLeakApp = topLeak
        )

        // ── Update streak + avgScore in UserProfile ───────────
        updateProfileStats(today, scoreEntity.score)

        return Result.success()
    }

    private suspend fun updateProfileStats(today: String, todayScore: Int) {
        val profile = userProfileDao.getProfile().firstOrNull() ?: return

        // Avg score: rolling average of last 7 days
        val last7 = (0 until 7).mapNotNull { daysAgo ->
            val date = LocalDate.now().minusDays(daysAgo.toLong()).toString()
            scoreDao.getScoreForDate(date).firstOrNull()?.score
        }
        val newAvg = if (last7.isEmpty()) todayScore else last7.average().toInt()

        // Streak: count consecutive days with score >= 50 going back from today
        var streak = 0
        var date = LocalDate.now()
        while (true) {
            val s = scoreDao.getScoreForDate(date.toString()).firstOrNull()
            if (s != null && s.score >= 50) {
                streak++
                date = date.minusDays(1)
            } else break
        }
        val newBest = maxOf(profile.bestStreak, streak)

        // Persist only if something changed
        if (newAvg != profile.avgScore || newBest != profile.bestStreak || streak != profile.currentStreak) {
            userProfileDao.upsert(
                profile.copy(
                    avgScore       = newAvg,
                    bestStreak     = newBest,
                    currentStreak  = streak
                )
            )
        }
    }

    companion object {
        private const val WORK_NAME = "daily_score_summary"

        fun schedule(context: Context) {
            // Calculate initial delay to first 9 PM trigger
            val now    = LocalDateTime.now()
            val target = now.toLocalDate().atTime(LocalTime.of(21, 0))
            val firstTarget = if (now.isBefore(target)) target else target.plusDays(1)
            val delayMinutes = java.time.Duration.between(now, firstTarget).toMinutes()

            val request = PeriodicWorkRequestBuilder<DailyScoreWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}