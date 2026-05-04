package com.productivityos.app.data.local

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

// ── Entities ────────────────────────────────────────────────

@Entity(tableName = "productivity_scores")
data class ProductivityScoreEntity(
    @PrimaryKey val date: String,          // ISO date string
    val score: Int,
    val focusMinutes: Int,
    val distractMinutes: Int,
    val deepSessions: Int
)

@Entity(
    tableName = "app_usage",
    indices = [Index(value = ["date", "packageName"], unique = true)]
)
data class AppUsageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val packageName: String,
    val appName: String,
    val emoji: String,
    val totalMinutes: Int,
    val scoreImpact: Int,
    val category: String                   // "FOCUS" | "DISTRACT" | "NEUTRAL"
)

@Entity(
    tableName = "hourly_focus",
    indices = [Index(value = ["date", "hour"], unique = true)]
)
data class HourlyFocusEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val hour: Int,
    val level: Int
)

@Entity(tableName = "insights")
data class InsightEntity(
    @PrimaryKey val insightId: String,
    val date: String,
    val title: String,
    val description: String,
    val impact: String,                    // "HIGH" | "MEDIUM" | "LOW"
    val emoji: String
)

@Entity(tableName = "schedule_blocks")
data class ScheduleBlockEntity(
    @PrimaryKey val blockId: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val title: String,
    val blockType: String                  // "DEEP" | "LIGHT" | "BREAK" | "ADMIN"
)

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val email: String,
    val workerType: String,
    val workCategory: String,
    val focusPreference: String,
    val avgScore: Int,
    val bestStreak: Int,
    val currentStreak: Int,
    val primaryDevice: String,
    val lastStreakDate: String = ""   // ISO date "YYYY-MM-DD"
)

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val isDarkTheme: Boolean,
    val fontSize: String,
    val onDeviceAi: Boolean,
    val autoDeleteAfter7Days: Boolean,
    val liveTrackerEnabled: Boolean,
    val hapticFeedback: Boolean,
    val blockedAppsJson: String = "",        // comma-separated package names
    val appUsageLimitsJson: String = ""      // "pkg:minutes,pkg:minutes,..."
)

// ── DAOs ────────────────────────────────────────────────────

@Dao
interface ProductivityScoreDao {
    @Query("SELECT * FROM productivity_scores WHERE date = :date")
    fun getScoreForDate(date: String): Flow<ProductivityScoreEntity?>

    @Query("SELECT * FROM productivity_scores ORDER BY date DESC LIMIT 7")
    fun getLastSevenDays(): Flow<List<ProductivityScoreEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(score: ProductivityScoreEntity)
}

@Dao
interface AppUsageDao {
    @Query("SELECT * FROM app_usage WHERE date = :date ORDER BY totalMinutes DESC")
    fun getUsageForDate(date: String): Flow<List<AppUsageEntity>>

    @Query("SELECT * FROM app_usage WHERE date >= :startDate ORDER BY totalMinutes DESC")
    fun getUsageFromDate(startDate: String): Flow<List<AppUsageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(usage: List<AppUsageEntity>)

    @Query("DELETE FROM app_usage WHERE date < :cutoffDate")
    suspend fun deleteOlderThan(cutoffDate: String)

    @Query("DELETE FROM app_usage WHERE date = :date")
    suspend fun deleteForDate(date: String)
}

@Dao
interface HourlyFocusDao {
    @Query("SELECT * FROM hourly_focus WHERE date = :date ORDER BY hour ASC")
    fun getHourlyFocusForDate(date: String): Flow<List<HourlyFocusEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(data: List<HourlyFocusEntity>)

    @Query("DELETE FROM hourly_focus WHERE date = :date")
    suspend fun deleteForDate(date: String)
}

@Dao
interface InsightDao {
    @Query("SELECT * FROM insights WHERE date = :date ORDER BY impact DESC")
    fun getInsightsForDate(date: String): Flow<List<InsightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(insights: List<InsightEntity>)
}

@Dao
interface ScheduleBlockDao {
    @Query("SELECT * FROM schedule_blocks WHERE date = :date ORDER BY startTime ASC")
    fun getBlocksForDate(date: String): Flow<List<ScheduleBlockEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(blocks: List<ScheduleBlockEntity>)

    @Query("DELETE FROM schedule_blocks WHERE date = :date")
    suspend fun deleteForDate(date: String)
}

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 1")
    fun getProfile(): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfileEntity)
}

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun getSettings(): Flow<AppSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(settings: AppSettingsEntity)
}

// ── Database ─────────────────────────────────────────────────

@Database(
    entities = [
        ProductivityScoreEntity::class,
        AppUsageEntity::class,
        HourlyFocusEntity::class,
        InsightEntity::class,
        ScheduleBlockEntity::class,
        UserProfileEntity::class,
        AppSettingsEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class ProductivityDatabase : RoomDatabase() {
    abstract fun productivityScoreDao(): ProductivityScoreDao
    abstract fun appUsageDao(): AppUsageDao
    abstract fun hourlyFocusDao(): HourlyFocusDao
    abstract fun insightDao(): InsightDao
    abstract fun scheduleBlockDao(): ScheduleBlockDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN blockedAppsJson TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN appUsageLimitsJson TEXT NOT NULL DEFAULT ''")
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Recreate hourly_focus with unique index on (date, hour)
                db.execSQL("DROP TABLE IF EXISTS hourly_focus_old")
                db.execSQL("ALTER TABLE hourly_focus RENAME TO hourly_focus_old")
                db.execSQL("""
                    CREATE TABLE hourly_focus (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date TEXT NOT NULL,
                        hour INTEGER NOT NULL,
                        level INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX index_hourly_focus_date_hour ON hourly_focus (date, hour)")
                // Copy distinct rows (keep latest per date+hour)
                db.execSQL("""
                    INSERT INTO hourly_focus (id, date, hour, level)
                    SELECT id, date, hour, level FROM hourly_focus_old
                    GROUP BY date, hour
                """.trimIndent())
                db.execSQL("DROP TABLE hourly_focus_old")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN lastStreakDate TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Destructive fallback handled by fallbackToDestructiveMigration()
            }
        }
    }
}