package com.productivityos.app.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.firestore.FirebaseFirestore
import com.productivityos.app.data.local.*
import com.productivityos.app.data.repository.*
import com.productivityos.app.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ProductivityDatabase =
        Room.databaseBuilder(
            context,
            ProductivityDatabase::class.java,
            "productivity_db"
        )
            .addMigrations(
                ProductivityDatabase.MIGRATION_1_2,
                ProductivityDatabase.MIGRATION_2_3,
                ProductivityDatabase.MIGRATION_3_4,
                ProductivityDatabase.MIGRATION_4_5,
                ProductivityDatabase.MIGRATION_5_6
            )
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideScoreDao(db: ProductivityDatabase) = db.productivityScoreDao()
    @Provides fun provideAppUsageDao(db: ProductivityDatabase) = db.appUsageDao()
    @Provides fun provideHourlyFocusDao(db: ProductivityDatabase) = db.hourlyFocusDao()
    @Provides fun provideInsightDao(db: ProductivityDatabase) = db.insightDao()
    @Provides fun provideScheduleBlockDao(db: ProductivityDatabase) = db.scheduleBlockDao()
    @Provides fun provideUserProfileDao(db: ProductivityDatabase) = db.userProfileDao()
    @Provides fun provideAppSettingsDao(db: ProductivityDatabase) = db.appSettingsDao()

    @Provides
    @Singleton
    fun provideUsageStatsHelper(@ApplicationContext context: Context) =
        com.productivityos.app.data.local.UsageStatsHelper(context)

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    fun provideFirestoreRepository(firestore: FirebaseFirestore): FirestoreRepository =
        FirestoreRepository(firestore)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindProductivityRepository(impl: ProductivityRepositoryImpl): ProductivityRepository

    @Binds @Singleton
    abstract fun bindAppUsageRepository(impl: AppUsageRepositoryImpl): AppUsageRepository

    @Binds @Singleton
    abstract fun bindHourlyFocusRepository(impl: HourlyFocusRepositoryImpl): HourlyFocusRepository

    @Binds @Singleton
    abstract fun bindInsightRepository(impl: InsightRepositoryImpl): InsightRepository

    @Binds @Singleton
    abstract fun bindScheduleRepository(impl: ScheduleRepositoryImpl): ScheduleRepository

    @Binds @Singleton
    abstract fun bindUserProfileRepository(impl: UserProfileRepositoryImpl): UserProfileRepository

    @Binds @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository
}