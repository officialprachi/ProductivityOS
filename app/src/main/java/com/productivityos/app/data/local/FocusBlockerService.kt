package com.productivityos.app.data.local

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import com.productivityos.app.MainActivity
import com.productivityos.app.R
import com.productivityos.app.data.local.NotificationHelper.Companion.CHANNEL_FOCUS
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class FocusBlockerService : Service() {

    @Inject lateinit var appSettingsDao: AppSettingsDao
    @Inject lateinit var notificationHelper: NotificationHelper

    private val usageStatsManager by lazy {
        getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
    }
    private val prefs: SharedPreferences by lazy {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pollingJob: Job? = null
    private var overlayView: FrameLayout? = null
    private val windowManager by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }

    private val limitNotifiedToday = mutableMapOf<String, String>()

    companion object {
        const val PREFS_NAME                = "focus_blocker_prefs"
        const val KEY_BLOCKED_APPS          = "blocked_apps"
        const val KEY_SESSION_END_MS        = "session_end_ms"
        const val KEY_SESSION_START_MS      = "session_start_ms"
        const val KEY_SESSION_DURATION_MINS = "session_duration_mins"
        const val KEY_SESSION_COMMITTED     = "session_committed"
        const val KEY_LIMITS_ACTIVE         = "limits_active"
        const val KEY_TOTAL_FOCUS_MINUTES   = "total_focus_minutes_today"
        const val KEY_LAST_RESET_DATE       = "last_reset_date"
        const val NOTIF_ID                  = 2001

        fun resetIfNewDay(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val today = LocalDate.now().toString()
            val lastReset = prefs.getString(KEY_LAST_RESET_DATE, "") ?: ""
            if (lastReset == today) return false
            prefs.edit {
                putInt(KEY_TOTAL_FOCUS_MINUTES, 0)
                putString(KEY_LAST_RESET_DATE, today)
            }
            return true
        }

        fun startBlocking(context: Context, blockedApps: List<String>, durationMinutes: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val nowMs = System.currentTimeMillis()
            prefs.edit {
                putStringSet(KEY_BLOCKED_APPS, blockedApps.toSet())
                putLong(KEY_SESSION_END_MS, nowMs + durationMinutes * 60_000L)
                putLong(KEY_SESSION_START_MS, nowMs)
                putInt(KEY_SESSION_DURATION_MINS, durationMinutes)
                putBoolean(KEY_SESSION_COMMITTED, false)
            }
            context.startService(Intent(context, FocusBlockerService::class.java))
        }

        fun stopBlocking(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            prefs.edit {
                remove(KEY_BLOCKED_APPS)
                remove(KEY_SESSION_END_MS)
            }
            if (!prefs.getBoolean(KEY_LIMITS_ACTIVE, false)) {
                context.stopService(Intent(context, FocusBlockerService::class.java))
            }
        }

        fun startLimitEnforcement(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            prefs.edit { putBoolean(KEY_LIMITS_ACTIVE, true) }
            context.startService(Intent(context, FocusBlockerService::class.java))
        }

        fun stopLimitEnforcement(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            prefs.edit { putBoolean(KEY_LIMITS_ACTIVE, false) }
            val sessionActive = prefs.getLong(KEY_SESSION_END_MS, 0L) > System.currentTimeMillis()
            if (!sessionActive) {
                context.stopService(Intent(context, FocusBlockerService::class.java))
            }
        }

        @Suppress("unused")
        fun isSessionActive(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            val endMs = prefs.getLong(KEY_SESSION_END_MS, 0L)
            return endMs > System.currentTimeMillis()
        }
    }

    override fun onCreate() {
        super.onCreate()
        // Ensure the notification channel exists before startForeground(),
        // otherwise Android 8+ throws CannotPostForegroundServiceNotificationException
        // and the process is killed instantly. createChannels() is idempotent.
        notificationHelper.createChannels()          // ← ADDED THIS LINE
        startForeground(NOTIF_ID, buildForegroundNotification())
        startPolling()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        removeBlockOverlay()
        scope.cancel()
    }

    private fun startPolling() {
        pollingJob = scope.launch {
            while (true) {
                checkAndBlock()
                delay(5_000L)
            }
        }
    }

    private suspend fun checkAndBlock() {
        val sessionEndMs = prefs.getLong(KEY_SESSION_END_MS, 0L)
        val sessionActive = sessionEndMs > 0L && System.currentTimeMillis() <= sessionEndMs

        if (sessionEndMs > 0L && !sessionActive) {
            prefs.edit {
                remove(KEY_BLOCKED_APPS)
                remove(KEY_SESSION_END_MS)
            }
        }

        val foregroundPkg = getForegroundApp() ?: return

        if (sessionActive) {
            val blockedApps = prefs.getStringSet(KEY_BLOCKED_APPS, emptySet()) ?: emptySet()
            if (foregroundPkg in blockedApps) {
                val label = resolveAppLabel(foregroundPkg)
                withContext(Dispatchers.Main) {
                    showBlockOverlay(label, reason = "Focus session active")
                }
                return
            } else {
                withContext(Dispatchers.Main) { removeBlockOverlay() }
                return
            }
        }

        val limitsActive = prefs.getBoolean(KEY_LIMITS_ACTIVE, false)
        if (!limitsActive) {
            withContext(Dispatchers.Main) { removeBlockOverlay() }
            return
        }

        val settings = appSettingsDao.getSettings().firstOrNull()
        val limitsJson = settings?.appUsageLimitsJson ?: ""
        if (limitsJson.isBlank()) {
            withContext(Dispatchers.Main) { removeBlockOverlay() }
            return
        }

        val limits = limitsJson.split(",").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                val pkg  = parts[0].trim()
                val mins = parts[1].trim().toIntOrNull() ?: return@mapNotNull null
                if (pkg.isNotBlank() && mins > 0) pkg to mins else null
            } else null
        }.toMap()

        val limitMins = limits[foregroundPkg]
        if (limitMins == null) {
            withContext(Dispatchers.Main) { removeBlockOverlay() }
            return
        }

        val midnightMs = run {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        }
        val todayStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST,
            midnightMs,
            System.currentTimeMillis()
        )
        val usedMins = todayStats
            ?.firstOrNull { it.packageName == foregroundPkg }
            ?.totalTimeInForeground
            ?.div(60_000L)
            ?.toInt() ?: 0

        if (usedMins >= limitMins) {
            val label = resolveAppLabel(foregroundPkg)
            val today = LocalDate.now().toString()
            if (limitNotifiedToday[foregroundPkg] != today) {
                limitNotifiedToday[foregroundPkg] = today
                notificationHelper.showUsageLimitReached(label, limitMins, foregroundPkg)
            }
            withContext(Dispatchers.Main) {
                showBlockOverlay(label, reason = "Daily limit reached · ${usedMins}m / ${limitMins}m")
            }
        } else {
            withContext(Dispatchers.Main) { removeBlockOverlay() }
        }
    }

    private fun resolveAppLabel(pkg: String): String = try {
        val pm   = packageManager
        val info = pm.getApplicationInfo(pkg, 0)
        pm.getApplicationLabel(info).toString()
    } catch (_: Exception) {
        pkg.substringAfterLast(".")
    }

    @SuppressLint("SetTextI18n")
    private fun showBlockOverlay(appLabel: String, reason: String) {
        removeBlockOverlay()

        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            Toast.makeText(this, "$appLabel blocked · $reason", Toast.LENGTH_SHORT).show()
            return
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }

        val overlay = FrameLayout(this).apply {
            setBackgroundColor(Color.argb(230, 20, 10, 30))
        }

        val title = TextView(this).apply {
            text      = "🔒 Blocked"
            textSize  = 22f
            setTextColor(Color.WHITE)
            gravity   = Gravity.CENTER
        }

        val sub = TextView(this).apply {
            text      = "$appLabel is blocked.\n$reason\n\nTap to return to ProductivityOS."
            textSize  = 15f
            setTextColor(Color.argb(200, 255, 255, 255))
            gravity   = Gravity.CENTER
            setPadding(48, 16, 48, 0)
        }

        val inner = FrameLayout(this)
        inner.addView(title, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER_HORIZONTAL or Gravity.TOP
        ).also { it.topMargin = 360 })
        inner.addView(sub, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER_HORIZONTAL or Gravity.TOP
        ).also { it.topMargin = 420 })
        overlay.addView(inner)

        overlay.setOnClickListener {
            removeBlockOverlay()
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
        }

        overlayView = overlay
        windowManager.addView(overlay, params)
    }

    private fun removeBlockOverlay() {
        overlayView?.let {
            runCatching { windowManager.removeView(it) }
            overlayView = null
        }
    }

    private fun getForegroundApp(): String? {
        val now   = System.currentTimeMillis()
        val start = now - 10_000L

        val events = usageStatsManager.queryEvents(start, now)
        val event  = UsageEvents.Event()
        var lastFromEvents: String? = null
        while (events?.hasNextEvent() == true) {
            events.getNextEvent(event)
            @Suppress("DEPRECATION")
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                lastFromEvents = event.packageName
            }
        }
        if (lastFromEvents != null) return lastFromEvents

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_BEST, start, now
        ) ?: return null
        return stats
            .filter { it.lastTimeUsed >= start && it.packageName != packageName }
            .maxByOrNull { it.lastTimeUsed }
            ?.packageName
    }

    private fun buildForegroundNotification() =
        NotificationCompat.Builder(this, CHANNEL_FOCUS)
            .setContentTitle("Focus Mode Active")
            .setContentText("Distraction apps are blocked")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()
}
