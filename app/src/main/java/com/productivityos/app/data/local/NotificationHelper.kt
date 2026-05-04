package com.productivityos.app.data.local

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.productivityos.app.MainActivity
import com.productivityos.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        // Channel IDs — must match AppUsageService.CHANNEL_* values exactly
        const val CHANNEL_TRACKER  = "tracker"
        const val CHANNEL_BURNOUT  = "burnout"
        const val CHANNEL_FOCUS    = "focus"
        const val CHANNEL_INSIGHTS = "insights"
        const val CHANNEL_LIMITS   = "usage_limits"

        // Notification IDs
        const val NOTIF_BURNOUT_ID        = 1002   // matches AppUsageService.NOTIF_BURNOUT_ID
        const val NOTIF_FOCUS_COMPLETE_ID = 2002
        const val NOTIF_DAILY_SCORE_ID    = 2003
        // usage limit notifications: base + pkg.hashCode() for per-app dedup
        const val NOTIF_LIMIT_BASE_ID     = 3000
    }

    // ── Create all channels (call once on app start) ──────────

    fun createChannels() {
        listOf(
            NotificationChannel(
                CHANNEL_TRACKER, "Usage Tracker",
                NotificationManager.IMPORTANCE_MIN
            ).apply { setShowBadge(false) },

            NotificationChannel(
                CHANNEL_BURNOUT, "Burnout Alert",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Fires when daily work limit is reached" },

            NotificationChannel(
                CHANNEL_FOCUS, "Focus Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ),

            NotificationChannel(
                CHANNEL_INSIGHTS, "Daily Insights",
                NotificationManager.IMPORTANCE_DEFAULT
            ),

            NotificationChannel(
                CHANNEL_LIMITS, "Usage Limits",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Fires when a daily app time limit is reached" }
        ).forEach { nm.createNotificationChannel(it) }
    }

    // ── 1. Burnout alert ──────────────────────────────────────

    fun showBurnoutAlert(workedMinutes: Int) {
        val hours   = workedMinutes / 60
        val minutes = workedMinutes % 60
        val intent  = appIntent()

        nm.notify(
            NOTIF_BURNOUT_ID,
            NotificationCompat.Builder(context, CHANNEL_BURNOUT)
                .setContentTitle("⚠ Burnout Warning")
                .setContentText("You've worked ${hours}h ${minutes}m today. Time for a proper break.")
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .addAction(0, "Take a Break", intent)
                .build()
        )
    }

    // ── 2. Focus session complete ─────────────────────────────

    fun showFocusComplete(durationMinutes: Int) {
        nm.notify(
            NOTIF_FOCUS_COMPLETE_ID,
            NotificationCompat.Builder(context, CHANNEL_FOCUS)
                .setContentTitle("✅ Focus Session Complete!")
                .setContentText("$durationMinutes minutes of deep work done. Great job.")
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(appIntent())
                .build()
        )
    }

    // ── 3. Daily usage limit reached ─────────────────────────

    fun showUsageLimitReached(appLabel: String, limitMinutes: Int, packageName: String) {
        val notifId = NOTIF_LIMIT_BASE_ID + (packageName.hashCode() and 0x0FFF)
        nm.notify(
            notifId,
            NotificationCompat.Builder(context, CHANNEL_LIMITS)
                .setContentTitle("⏱ Daily limit reached · $appLabel")
                .setContentText("You've used ${limitMinutes}m of $appLabel today. App is now blocked.")
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(appIntent())
                .build()
        )
    }

    // ── 4. Daily score summary (called by DailyScoreWorker) ───

    fun showDailyScore(score: Int, focusHours: Int, topLeakApp: String) {
        nm.notify(
            NOTIF_DAILY_SCORE_ID,
            NotificationCompat.Builder(context, CHANNEL_INSIGHTS)
                .setContentTitle("Today's Score: $score/100")
                .setContentText("${focusHours}h focus · $topLeakApp was your biggest distraction")
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(appIntent())
                .build()
        )
    }

    // ── Helpers ───────────────────────────────────────────────

    private fun appIntent(): PendingIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )
}