package com.productivityos.app.data.repository

import android.content.Context
import com.productivityos.app.data.local.*
import com.productivityos.app.domain.model.*
import com.productivityos.app.domain.repository.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

// ── Mappers ────────────────────────────────────────────────

private fun ProductivityScoreEntity.toDomain() = ProductivityScore(
    value = score,
    focusMinutes = focusMinutes,
    distractMinutes = distractMinutes,
    deepSessions = deepSessions,
    date = LocalDate.parse(date)
)

private fun AppUsageEntity.toDomain() = AppUsage(
    packageName = packageName,
    appName = appName,
    emoji = emoji,
    totalMinutes = totalMinutes,
    scoreImpact = scoreImpact,
    category = AppCategory.valueOf(category)
)

private fun HourlyFocusEntity.toDomain() = HourlyFocus(hour = hour, level = level)

private fun InsightEntity.toDomain() = Insight(
    id = insightId,
    title = title,
    description = description,
    impact = InsightImpact.valueOf(impact),
    emoji = emoji
)

private fun ScheduleBlockEntity.toDomain() = ScheduleBlock(
    id = blockId,
    startTime = startTime,
    endTime = endTime,
    title = title,
    blockType = BlockType.valueOf(blockType)
)

private fun UserProfileEntity.toDomain() = UserProfile(
    name = name,
    email = email,
    workerType = workerType,
    workCategory = workCategory,
    focusPreference = focusPreference,
    avgScore = avgScore,
    bestStreak = bestStreak,
    currentStreak = currentStreak,
    primaryDevice = primaryDevice,
    lastStreakDate = lastStreakDate
)

private fun AppSettingsEntity.toDomain() = AppSettings(
    isDarkTheme = isDarkTheme,
    fontSize = FontSize.valueOf(fontSize),
    onDeviceAi = onDeviceAi,
    autoDeleteAfter7Days = autoDeleteAfter7Days,
    liveTrackerEnabled = liveTrackerEnabled,
    hapticFeedback = hapticFeedback,
    blockedApps = if (blockedAppsJson.isBlank()) AppSettings().blockedApps
    else blockedAppsJson.split(",").filter { it.isNotBlank() },
    appUsageLimits = if (appUsageLimitsJson.isBlank()) emptyMap()
    else appUsageLimitsJson.split(",")
        .mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                val pkg = parts[0].trim()
                val mins = parts[1].trim().toIntOrNull() ?: return@mapNotNull null
                if (pkg.isNotBlank() && mins > 0) pkg to mins else null
            } else null
        }.toMap()
)

// ── Sample seed data ────────────────────────────────────────

private fun sampleScoreEntity() = ProductivityScoreEntity(
    date = LocalDate.now().toString(),
    score = 73,
    focusMinutes = 260,
    distractMinutes = 95,
    deepSessions = 2
)

private fun sampleUsageEntities() = listOf(
    AppUsageEntity(0, LocalDate.now().toString(), "com.vscode", "VS Code", "💻", 192, 18, "FOCUS"),
    AppUsageEntity(0, LocalDate.now().toString(), "com.instagram", "Instagram", "📱", 42, -14, "DISTRACT"),
    AppUsageEntity(0, LocalDate.now().toString(), "com.youtube", "YouTube", "🎥", 28, -8, "DISTRACT"),
    AppUsageEntity(0, LocalDate.now().toString(), "com.slack", "Slack", "💬", 64, -5, "NEUTRAL")
)

private fun sampleHourlyData(): List<HourlyFocusEntity> {
    val levels = listOf(2,2,4,5,8,9,10,8,7,9,10,8,6,4,3,5,7,8,6,4,3,2,1,1)
    return levels.mapIndexed { hour, level ->
        HourlyFocusEntity(0, LocalDate.now().toString(), hour, level)
    }
}

private fun sampleInsights() = listOf(
    InsightEntity("1", LocalDate.now().toString(),
        "Frequent app switching detected",
        "You switch apps every 6 minutes on average, which fragments your focus and reduces deep work capacity significantly.",
        "HIGH", "⚡"),
    InsightEntity("2", LocalDate.now().toString(),
        "Peak window: 9–11 AM",
        "Your best deep work consistently happens between 9 and 11 in the morning. Guard this time fiercely.",
        "HIGH", "🧠"),
    InsightEntity("3", LocalDate.now().toString(),
        "Instagram: 42 min distraction",
        "Instagram consumed 42 minutes today across 8 sessions. Most usage occurred during planned focus blocks.",
        "MEDIUM", "📱"),
    InsightEntity("4", LocalDate.now().toString(),
        "Afternoon energy dip at 3 PM",
        "Your focus level drops significantly around 3 PM. This is a natural circadian rhythm pattern.",
        "LOW", "🌅")
)

private fun sampleScheduleBlocks(): List<ScheduleBlockEntity> {
    val tomorrow = LocalDate.now().plusDays(1).toString()
    return listOf(
        ScheduleBlockEntity("s1", tomorrow, "08:00", "10:30", "Morning Deep Work", "DEEP"),
        ScheduleBlockEntity("s2", tomorrow, "10:30", "10:45", "Short Break", "BREAK"),
        ScheduleBlockEntity("s3", tomorrow, "10:45", "12:00", "Email & Messages", "LIGHT"),
        ScheduleBlockEntity("s4", tomorrow, "12:00", "13:00", "Lunch Break", "BREAK"),
        ScheduleBlockEntity("s5", tomorrow, "13:00", "14:30", "Afternoon Focus", "DEEP"),
        ScheduleBlockEntity("s6", tomorrow, "15:00", "16:00", "Admin & Reviews", "ADMIN")
    )
}

private fun sampleProfile() = UserProfileEntity(
    id = 1,
    name = "Maeve",
    email = "maeve@productivityos.app",
    workerType = "Deep Worker",
    workCategory = "Developer",
    focusPreference = "Morning",
    avgScore = 78,
    bestStreak = 12,
    currentStreak = 7,
    primaryDevice = "Pixel 9 Pro"
)

private fun sampleSettings() = AppSettingsEntity(
    id = 1,
    isDarkTheme = true,
    fontSize = "MEDIUM",
    onDeviceAi = false,
    autoDeleteAfter7Days = false,
    liveTrackerEnabled = true,
    hapticFeedback = true
)

// ── Repository Implementations ──────────────────────────────

@Singleton
class ProductivityRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: ProductivityScoreDao,
    private val appUsageDao: AppUsageDao,
    private val settingsDao: AppSettingsDao,
    private val firestoreRepository: FirestoreRepository,
    private val auth: com.google.firebase.auth.FirebaseAuth,
    private val usageStatsHelper: com.productivityos.app.data.local.UsageStatsHelper
) : ProductivityRepository {

    override fun getTodayScore(): Flow<ProductivityScore?> =
        dao.getScoreForDate(LocalDate.now().toString()).map { it?.toDomain() }

    override fun getWeekScores(): Flow<List<ProductivityScore>> =
        dao.getLastSevenDays().map { list -> list.map { it.toDomain() } }

    override suspend fun refreshScore() {
        val today = LocalDate.now().toString()

        // Query live usage directly from UsageStatsManager for freshest data.
        val liveUsage = usageStatsHelper.queryTodayUsage()
        val usageSource: List<com.productivityos.app.data.local.AppUsageEntity> = if (liveUsage.isNotEmpty()) {
            appUsageDao.deleteForDate(today)
            val entities = liveUsage.map { usage ->
                com.productivityos.app.data.local.AppUsageEntity(
                    date = today,
                    packageName = usage.packageName,
                    appName = usage.appName,
                    emoji = usage.emoji,
                    totalMinutes = usage.totalMinutes,
                    scoreImpact = usage.scoreImpact,
                    category = usage.category.name
                )
            }
            appUsageDao.insertAll(entities)
            entities
        } else {
            appUsageDao.getUsageForDate(today).firstOrNull() ?: emptyList()
        }

        // Include focus session minutes from the Focus timer (stored in SharedPrefs as seconds)
        val prefs = context.getSharedPreferences(
            FocusBlockerService.PREFS_NAME, Context.MODE_PRIVATE
        )
        val focusSessionMinutes = (prefs.getInt(FocusBlockerService.KEY_TOTAL_FOCUS_MINUTES, 0) / 60)
            .coerceIn(0, 480) // cap at 8h to prevent abuse

        val usageFocusMinutes    = usageSource.filter { it.category == "FOCUS" }.sumOf { it.totalMinutes }
        val distractMinutes      = usageSource.filter { it.category == "DISTRACT" }.sumOf { it.totalMinutes }

        // Combined focus = tracked focus apps + manual focus sessions
        val totalFocusMinutes = usageFocusMinutes + focusSessionMinutes

        val focusPoints      = ((totalFocusMinutes / 15) * 10).coerceAtMost(70)
        val distractPenalty  = ((distractMinutes / 15) * 8).coerceAtMost(40)
        val score            = (30 + focusPoints - distractPenalty).coerceIn(0, 100)
        val deepSessions     = (totalFocusMinutes / 25).coerceAtLeast(0)

        val entity = ProductivityScoreEntity(
            date            = today,
            score           = score,
            focusMinutes    = totalFocusMinutes,
            distractMinutes = distractMinutes,
            deepSessions    = deepSessions
        )

        dao.upsert(entity)

        val settings = settingsDao.getSettings().firstOrNull()
        val onDeviceAi = settings?.onDeviceAi ?: true
        val userId = auth.currentUser?.uid
        if (!onDeviceAi && userId != null) {
            runCatching { firestoreRepository.syncScoreToFirestore(userId, entity.toDomain()) }
        }
    }
}

@Singleton
class AppUsageRepositoryImpl @Inject constructor(
    private val dao: AppUsageDao,
    private val settingsDao: AppSettingsDao,
    private val firestoreRepository: FirestoreRepository,
    private val auth: com.google.firebase.auth.FirebaseAuth,
    private val usageStatsHelper: com.productivityos.app.data.local.UsageStatsHelper
) : AppUsageRepository {

    override fun getTodayUsage(): Flow<List<AppUsage>> =
        dao.getUsageForDate(LocalDate.now().toString()).map { list -> list.map { it.toDomain() } }

    override fun getWeekUsage(): Flow<List<AppUsage>> =
        dao.getUsageFromDate(LocalDate.now().minusDays(7).toString()).map { list -> list.map { it.toDomain() } }

    override suspend fun syncUsageData() {
        val today = LocalDate.now().toString()
        val realUsage = usageStatsHelper.queryTodayUsage()

        // Only write to Room if we got real data — never insert fake fallback
        if (realUsage.isEmpty()) return

        // Clear stale rows first — then insert fresh snapshot
        dao.deleteForDate(today)
        dao.insertAll(realUsage.map { usage ->
            com.productivityos.app.data.local.AppUsageEntity(
                date = today,
                packageName = usage.packageName,
                appName = usage.appName,
                emoji = usage.emoji,
                totalMinutes = usage.totalMinutes,
                scoreImpact = usage.scoreImpact,
                category = usage.category.name
            )
        })

        // Sync to Firestore if onDeviceAi is off
        val settings = settingsDao.getSettings().firstOrNull()
        val onDeviceAi = settings?.onDeviceAi ?: true
        val userId = auth.currentUser?.uid
        if (!onDeviceAi && userId != null) {
            runCatching { firestoreRepository.syncUsageToFirestore(userId, realUsage) }
        }
    }
}

@Singleton
class HourlyFocusRepositoryImpl @Inject constructor(
    private val dao: HourlyFocusDao
) : HourlyFocusRepository {
    override fun getTodayHourlyFocus(): Flow<List<HourlyFocus>> =
        dao.getHourlyFocusForDate(LocalDate.now().toString()).map { list -> list.map { it.toDomain() } }
}

@Singleton
class InsightRepositoryImpl @Inject constructor(
    private val dao: InsightDao,
    private val appUsageDao: AppUsageDao,
    private val hourlyFocusDao: HourlyFocusDao,
    private val scoreDao: ProductivityScoreDao,
    private val settingsDao: AppSettingsDao,
    private val firestoreRepository: FirestoreRepository,
    private val insightEngine: com.productivityos.app.domain.usecase.InsightEngine,
    private val auth: com.google.firebase.auth.FirebaseAuth
) : InsightRepository {

    override fun getTodayInsights(): Flow<List<Insight>> =
        dao.getInsightsForDate(LocalDate.now().toString()).map { list -> list.map { it.toDomain() } }

    override suspend fun refreshInsights() {
        val today = LocalDate.now().toString()
        val settings = settingsDao.getSettings().firstOrNull()
        val onDeviceAi = settings?.onDeviceAi ?: true
        val userId = auth.currentUser?.uid

        if (!onDeviceAi && userId != null) {
            // Cloud path: fetch insights Firestore generated server-side
            val cloudInsights = runCatching {
                firestoreRepository.fetchInsightsFromFirestore(userId)
            }.getOrNull()

            if (!cloudInsights.isNullOrEmpty()) {
                dao.insertAll(cloudInsights.map { it.toEntity(today) })
                return
            }
        }

        // On-device path: run InsightEngine locally
        val usageEntities   = appUsageDao.getUsageForDate(today).firstOrNull() ?: emptyList()
        val hourlyEntities  = hourlyFocusDao.getHourlyFocusForDate(today).firstOrNull() ?: emptyList()
        val scoreEntity     = scoreDao.getScoreForDate(today).firstOrNull()

        val usage      = usageEntities.map { it.toDomain() }
        val hourly     = hourlyEntities.map { it.toDomain() }
        val score      = scoreEntity?.toDomain()

        val insights = if (usage.isNotEmpty() && score != null) {
            insightEngine.generate(usage, hourly, score)
        } else {
            // No real data yet — fall back to demo insights
            sampleInsights().map { it.toDomain() }
        }

        val insightEntities = insights.map { it.toEntity(today) }
        dao.insertAll(insightEntities)

        // Push generated insights to Firestore when cloud sync is enabled
        if (!onDeviceAi && userId != null) {
            runCatching { firestoreRepository.pushInsightsToFirestore(userId, insights) }
        }
    }

    // ── Mappers ───────────────────────────────────────────────

    private fun Insight.toEntity(date: String) = InsightEntity(
        insightId   = id,
        date        = date,
        title       = title,
        description = description,
        impact      = impact.name,
        emoji       = emoji
    )

    private fun InsightEntity.toDomain() = Insight(
        id          = insightId,
        title       = title,
        description = description,
        impact      = InsightImpact.valueOf(impact),
        emoji       = emoji
    )

    private fun AppUsageEntity.toDomain() = AppUsage(
        packageName = packageName,
        appName     = appName,
        emoji       = emoji,
        totalMinutes = totalMinutes,
        scoreImpact = scoreImpact,
        category    = AppCategory.valueOf(category)
    )

    private fun HourlyFocusEntity.toDomain() = HourlyFocus(hour = hour, level = level)

    private fun ProductivityScoreEntity.toDomain() = ProductivityScore(
        value           = score,
        focusMinutes    = focusMinutes,
        distractMinutes = distractMinutes,
        deepSessions    = deepSessions,
        date            = LocalDate.parse(date)
    )
}

@Singleton
class ScheduleRepositoryImpl @Inject constructor(
    private val dao: ScheduleBlockDao,
    private val profileDao: UserProfileDao,
    private val hourlyFocusDao: HourlyFocusDao
) : ScheduleRepository {

    override fun getTomorrowSchedule(): Flow<List<ScheduleBlock>> {
        val tomorrow = LocalDate.now().plusDays(1).toString()
        return dao.getBlocksForDate(tomorrow).map { list -> list.map { it.toDomain() } }
    }

    override suspend fun applySchedule(blocks: List<ScheduleBlock>) {
        val tomorrow = LocalDate.now().plusDays(1).toString()
        dao.deleteForDate(tomorrow)
        dao.insertAll(blocks.map { block ->
            ScheduleBlockEntity(
                blockId   = block.id,
                date      = tomorrow,
                startTime = block.startTime,
                endTime   = block.endTime,
                title     = block.title,
                blockType = block.blockType.name
            )
        })
    }

    override suspend fun generateNewSchedule() {
        val tomorrow = LocalDate.now().plusDays(1).toString()
        dao.deleteForDate(tomorrow)

        val profile = profileDao.getProfile().firstOrNull()
        val focusPref = profile?.focusPreference ?: "Morning"
        val workType  = profile?.workerType ?: "Developer"

        // Read today's hourly focus to find peak window
        val today = LocalDate.now().toString()
        val hourly = hourlyFocusDao.getHourlyFocusForDate(today).firstOrNull() ?: emptyList()

        val blocks = buildSchedule(focusPref, workType, hourly)
        dao.insertAll(blocks.map { block ->
            ScheduleBlockEntity(
                blockId   = block.id,
                date      = tomorrow,
                startTime = block.startTime,
                endTime   = block.endTime,
                title     = block.title,
                blockType = block.blockType.name
            )
        })
    }

    // ── Core scheduling algorithm ─────────────────────────────

    private fun buildSchedule(
        focusPref: String,
        workType: String,
        hourly: List<HourlyFocusEntity>
    ): List<ScheduleBlock> {

        // 1. Determine deep work start from focus preference
        val deepWorkStart = when {
            focusPref.contains("Morning", ignoreCase = true)   -> 8
            focusPref.contains("Midday", ignoreCase = true)    -> 10
            focusPref.contains("Afternoon", ignoreCase = true) -> 13
            focusPref.contains("Evening", ignoreCase = true)   -> 17
            focusPref.contains("Night", ignoreCase = true)     -> 20
            else -> {
                // Try to find peak from actual hourly data (2-hr window with best focus)
                if (hourly.size >= 2) {
                    var bestHour = 8
                    var bestSum  = 0
                    for (i in 0 until hourly.size - 1) {
                        val sum = hourly[i].level + hourly[i + 1].level
                        if (sum > bestSum) { bestSum = sum; bestHour = hourly[i].hour }
                    }
                    bestHour
                } else 8
            }
        }

        // 2. Build deep work title from work type
        val deepTitle = when {
            workType.contains("Developer", ignoreCase = true)  -> "Deep Coding Session"
            workType.contains("Designer", ignoreCase = true)   -> "Deep Design Session"
            workType.contains("Writer", ignoreCase = true)     -> "Deep Writing Session"
            workType.contains("Analyst", ignoreCase = true)    -> "Deep Analysis Block"
            workType.contains("Manager", ignoreCase = true)    -> "Strategic Planning Block"
            workType.contains("Researcher", ignoreCase = true) -> "Deep Research Block"
            workType.contains("Student", ignoreCase = true)    -> "Deep Study Session"
            workType.contains("Founder", ignoreCase = true)    -> "Deep Strategy Session"
            else -> "Morning Deep Work"
        }

        // 3. Check if afternoon focus is strong enough for a second deep block
        val afternoonFocusLevel = hourly.firstOrNull { it.hour in 13..14 }?.level ?: 5
        val hasAfternoonDeep = afternoonFocusLevel > 5

        val blocks = mutableListOf<ScheduleBlock>()

        val deepStart = formatHour(deepWorkStart)
        val deepEnd   = formatHour(deepWorkStart + 2, 30) // 2h 30m deep block

        blocks += ScheduleBlock("s1", deepStart, deepEnd, deepTitle, BlockType.DEEP)
        blocks += ScheduleBlock("s2", deepEnd, formatHour(deepWorkStart + 2, 45), "Short Break", BlockType.BREAK)
        blocks += ScheduleBlock("s3", formatHour(deepWorkStart + 2, 45), formatHour(deepWorkStart + 4), "Email & Messages", BlockType.LIGHT)
        blocks += ScheduleBlock("s4", "12:00", "13:00", "Lunch Break", BlockType.BREAK)

        if (hasAfternoonDeep) {
            blocks += ScheduleBlock("s5", "13:00", "14:30", "Afternoon Focus", BlockType.DEEP)
        }

        blocks += ScheduleBlock("s6", "15:00", "16:00", "Admin & Reviews", BlockType.ADMIN)

        // 4. Evening wind-down for night owl / evening workers
        if (focusPref.contains("Evening", ignoreCase = true) || focusPref.contains("Night", ignoreCase = true)) {
            blocks += ScheduleBlock("s7", "17:00", "18:00", "Planning Tomorrow", BlockType.LIGHT)
        }

        return blocks
    }

    private fun formatHour(hour: Int, minute: Int = 0): String =
        "%02d:%02d".format(hour.coerceIn(0, 23), minute)

    private fun ScheduleBlockEntity.toDomain() = ScheduleBlock(
        id        = blockId,
        startTime = startTime,
        endTime   = endTime,
        title     = title,
        blockType = BlockType.valueOf(blockType)
    )
}

@Singleton
class UserProfileRepositoryImpl @Inject constructor(
    private val dao: UserProfileDao
) : UserProfileRepository {
    override fun getProfile(): Flow<UserProfile?> =
        dao.getProfile().map { it?.toDomain() }

    override suspend fun updateProfile(profile: UserProfile) {
        dao.upsert(UserProfileEntity(
            name = profile.name,
            email = profile.email,
            workerType = profile.workerType,
            workCategory = profile.workCategory,
            focusPreference = profile.focusPreference,
            avgScore = profile.avgScore,
            bestStreak = profile.bestStreak,
            currentStreak = profile.currentStreak,
            primaryDevice = profile.primaryDevice,
            lastStreakDate = profile.lastStreakDate
        ))
    }
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dao: AppSettingsDao
) : SettingsRepository {
    override fun getSettings(): Flow<AppSettings> =
        dao.getSettings().map { it?.toDomain() ?: AppSettings() }

    override suspend fun updateSettings(settings: AppSettings) {
        val limitsJson = settings.appUsageLimits.entries
            .filter { it.value > 0 }
            .joinToString(",") { "${it.key}:${it.value}" }
        dao.upsert(AppSettingsEntity(
            isDarkTheme = settings.isDarkTheme,
            fontSize = settings.fontSize.name,
            onDeviceAi = settings.onDeviceAi,
            autoDeleteAfter7Days = settings.autoDeleteAfter7Days,
            liveTrackerEnabled = settings.liveTrackerEnabled,
            hapticFeedback = settings.hapticFeedback,
            blockedAppsJson = settings.blockedApps.joinToString(","),
            appUsageLimitsJson = limitsJson
        ))
    }
}