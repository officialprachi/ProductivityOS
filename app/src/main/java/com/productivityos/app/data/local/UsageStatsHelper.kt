package com.productivityos.app.data.local

import android.app.usage.UsageStatsManager
import android.content.Context
import com.productivityos.app.domain.model.AppCategory
import com.productivityos.app.domain.model.AppUsage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageStatsHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    // ── Package → category classification ─────────────────────

    private val focusPackages = setOf(
        "com.android.vending", // Play Store
        "com.google.android.apps.docs",
        "com.google.android.apps.sheets",
        "com.google.android.apps.slides",
        "com.microsoft.office.word",
        "com.microsoft.office.excel",
        "com.microsoft.office.powerpoint",
        "com.microsoft.teams",
        "com.slack",
        "com.jetbrains.rider",
        "com.termux",
        "com.github.android",
        "com.visualstudio.code",
        "com.vscode",
        "com.adobe.reader",
        "com.notion.id",
        "com.todoist",
        "com.things3",
        "com.omnifocus",
        "md.obsidian",
        "com.logseq.app",
        "com.roamresearch",
        "com.evernote",
        "com.onenote",
        "org.kiwix.kiwixmobile", // Wikipedia
        "com.google.android.apps.classroom",
        "com.duolingo",
        "com.grammarly.android.keyboard"
    )

    private val focusKeywords = listOf(
        "code", "dev", "editor", "ide", "office", "word", "excel",
        "doc", "sheet", "note", "task", "todo", "jira", "linear",
        "github", "gitlab", "bitbucket", "terminal", "ssh", "git",
        "figma", "sketch", "zeplin", "notion", "obsidian", "roam",
        "productivity", "work", "study", "learn", "course", "book",
        "reader", "pdf", "email", "mail", "calendar", "meet", "zoom",
        "teams", "slack", "classroom"
    )

    private val distractPackages = setOf(
        "com.instagram.android",
        "com.facebook.katana",
        "com.twitter.android",
        "com.zhiliaoapp.musically", // TikTok
        "com.ss.android.ugc.trill",
        "com.snapchat.android",
        "com.reddit.frontpage",
        "com.pinterest",
        "com.tumblr",
        "com.google.android.youtube",
        "com.netflix.mediaclient",
        "com.disney.disneyplus",
        "com.amazon.avod.thirdpartyclient", // Prime Video
        "com.hulu.plus",
        "com.twitch.android.app",
        "com.valvesoftware.android.steam.community",
        "com.roblox.client",
        "com.mojang.minecraftpe",
        "com.king.candycrushsaga",
        "com.supercell.clashofclans",
        "com.zynga.words3"
    )

    private val distractKeywords = listOf(
        "instagram", "facebook", "twitter", "tiktok", "snap", "reddit",
        "pinterest", "tumblr", "youtube", "netflix", "disney", "hulu",
        "twitch", "gaming", "game", "candy", "clash", "pubg", "fortnite",
        "social", "chat", "dating", "tinder", "bumble", "stream", "video"
    )

    // ── Emoji mapping by package / keyword ────────────────────

    private fun emojiFor(packageName: String, appName: String): String {
        val pkg = packageName.lowercase()
        val name = appName.lowercase()
        return when {
            "instagram" in pkg -> "📸"
            "youtube" in pkg -> "🎥"
            "netflix" in pkg -> "🎬"
            "facebook" in pkg -> "👥"
            "twitter" in pkg || "x.com" in pkg -> "🐦"
            "tiktok" in pkg || "musically" in pkg || "ugc.trill" in pkg -> "🎵"
            "snapchat" in pkg -> "👻"
            "reddit" in pkg -> "🤖"
            "slack" in pkg -> "💬"
            "teams" in pkg -> "🤝"
            "zoom" in pkg -> "📹"
            "github" in pkg || "gitlab" in pkg -> "🐙"
            "vscode" in pkg || "visualstudio" in pkg -> "💻"
            "notion" in pkg -> "📝"
            "obsidian" in pkg -> "🔮"
            "spotify" in pkg -> "🎧"
            "gmail" in pkg || "email" in pkg || "mail" in name -> "✉️"
            "calendar" in pkg || "calendar" in name -> "📅"
            "maps" in pkg || "maps" in name -> "🗺️"
            "chrome" in pkg || "firefox" in pkg || "browser" in name -> "🌐"
            "whatsapp" in pkg -> "💬"
            "telegram" in pkg -> "✈️"
            "discord" in pkg -> "🎮"
            "twitch" in pkg -> "🕹️"
            "game" in pkg || "game" in name -> "🎮"
            "music" in name || "spotify" in pkg -> "🎵"
            "photo" in name || "camera" in name -> "📷"
            "news" in pkg || "news" in name -> "📰"
            "docs" in pkg || "doc" in name -> "📄"
            "sheets" in pkg || "excel" in pkg -> "📊"
            "slides" in pkg || "powerpoint" in pkg -> "📊"
            "word" in pkg -> "📝"
            "pdf" in name -> "📋"
            "settings" in pkg -> "⚙️"
            else -> "📱"
        }
    }

    // ── Category classification ────────────────────────────────

    private fun categoryFor(packageName: String, appName: String): AppCategory {
        val pkg = packageName.lowercase()
        val name = appName.lowercase()

        if (packageName in distractPackages) return AppCategory.DISTRACT
        if (packageName in focusPackages) return AppCategory.FOCUS

        if (distractKeywords.any { it in pkg || it in name }) return AppCategory.DISTRACT
        if (focusKeywords.any { it in pkg || it in name }) return AppCategory.FOCUS

        return AppCategory.NEUTRAL
    }

    // ── Score impact calculation ───────────────────────────────

    private fun scoreImpactFor(category: AppCategory, minutes: Int): Int = when (category) {
        AppCategory.FOCUS -> minutes * 2       // +2 pts per focus minute
        AppCategory.DISTRACT -> -(minutes * 1) // -1 pt per distract minute
        AppCategory.NEUTRAL -> 0
    }

    // ── Friendly app name ─────────────────────────────────────

    // Labels that are too generic to be useful as app names
    private val genericLabels = setOf(
        "android", "android system", "android os", "system", "phone",
        "dialer", "launcher", "home", "settings", "package installer",
        "permission controller", "app"
    )

    // Well-known packages whose PM label is unreliable or generic
    private val knownFriendlyNames = mapOf(
        "com.netflix.mediaclient"              to "Netflix",
        "com.google.android.youtube"           to "YouTube",
        "com.instagram.android"               to "Instagram",
        "com.facebook.katana"                 to "Facebook",
        "com.twitter.android"                 to "Twitter",
        "com.zhiliaoapp.musically"            to "TikTok",
        "com.ss.android.ugc.trill"            to "TikTok",
        "com.snapchat.android"                to "Snapchat",
        "com.reddit.frontpage"                to "Reddit",
        "com.pinterest"                       to "Pinterest",
        "com.tumblr"                          to "Tumblr",
        "com.disney.disneyplus"               to "Disney+",
        "com.amazon.avod.thirdpartyclient"    to "Prime Video",
        "com.hulu.plus"                       to "Hulu",
        "com.twitch.android.app"              to "Twitch",
        "com.discord"                         to "Discord",
        "com.whatsapp"                        to "WhatsApp",
        "org.telegram.messenger"              to "Telegram",
        "com.google.android.apps.maps"        to "Google Maps",
        "com.google.android.gm"               to "Gmail",
        "com.google.android.calendar"         to "Google Calendar",
        "com.google.android.apps.docs"        to "Google Docs",
        "com.google.android.apps.sheets"      to "Google Sheets",
        "com.google.android.apps.slides"      to "Google Slides",
        "com.microsoft.office.word"           to "Microsoft Word",
        "com.microsoft.office.excel"          to "Microsoft Excel",
        "com.microsoft.office.powerpoint"     to "PowerPoint",
        "com.microsoft.teams"                 to "Microsoft Teams",
        "com.slack"                           to "Slack",
        "us.zoom.videomeetings"               to "Zoom",
        "com.spotify.music"                   to "Spotify",
        "com.google.android.apps.youtube.music" to "YouTube Music",
        "com.valvesoftware.android.steam.community" to "Steam",
        "com.roblox.client"                   to "Roblox",
        "com.mojang.minecraftpe"              to "Minecraft",
        "com.king.candycrushsaga"             to "Candy Crush",
        "com.supercell.clashofclans"          to "Clash of Clans",
        "md.obsidian"                         to "Obsidian",
        "com.notion.id"                       to "Notion",
        "com.todoist"                         to "Todoist",
        "com.evernote"                        to "Evernote",
        "com.microsoft.launcher"              to "Microsoft Launcher",
        "com.github.android"                  to "GitHub",
        "com.google.android.apps.photos"      to "Google Photos",
        "com.google.android.dialer"           to "Phone",
        "com.android.chrome"                  to "Chrome",
        "com.opera.browser"                   to "Opera",
        "org.mozilla.firefox"                 to "Firefox",
        "com.microsoft.bing"                  to "Bing",
        "com.amazon.mShop.android.shopping"   to "Amazon",
        "com.flipkart.android"                to "Flipkart",
        "com.myntra.android"                  to "Myntra",
        "com.bookmyshow.app"                  to "BookMyShow",
        "com.paytm.app"                       to "Paytm",
        "com.phonepe.app"                     to "PhonePe",
        "net.one97.paytm"                     to "Paytm",
        "com.google.android.apps.nbu.paisa.user" to "Google Pay",
        "com.dreamplug.androidapp"            to "CRED",
        "com.moffice.eng"                     to "MOffice",
        "com.miui.home"                       to "MIUI Launcher",
        "com.android.camera2"                 to "Camera",
        "com.google.android.GoogleCamera"     to "Camera",
        "com.samsung.android.messaging"       to "Messages",
        "com.google.android.apps.messaging"   to "Messages"
    )

    private fun humanizePackageName(packageName: String): String {
        // Use last meaningful segment, but skip trivial suffixes like "android", "app", "client"
        val trivial = setOf("android", "app", "client", "mobile", "lite", "plus", "pro", "free", "new", "eng")
        val segments = packageName.split(".")
        // Find last non-trivial, non-company-prefix segment
        val meaningful = segments
            .drop(2) // skip "com.company"
            .filter { it.lowercase() !in trivial && it.length > 2 }
            .lastOrNull()
            ?: segments.lastOrNull { it.length > 2 }
            ?: packageName.substringAfterLast(".")

        return meaningful
            .replace(Regex("([a-z])([A-Z])"), "$1 $2") // camelCase → words
            .split(Regex("[_\\-]"))
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercaseChar() } }
            .trim()
            .ifBlank { packageName }
    }

    private fun appNameFor(packageName: String): String {
        // 1. Check our known-names map first — most reliable
        knownFriendlyNames[packageName]?.let { return it }

        return try {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            val label = pm.getApplicationLabel(info).toString().trim()

            // 2. If PM label is blank, numeric, or a known generic → humanize from package
            if (label.lowercase() in genericLabels || label.isBlank() || label == packageName) {
                humanizePackageName(packageName)
            } else {
                label
            }
        } catch (e: Exception) {
            // 3. PM failed (app uninstalled / restricted) → humanize from package
            humanizePackageName(packageName)
        }
    }

    // ── System package filter ─────────────────────────────────

    // Allowlist: distract/focus apps that must NEVER be skipped even if they
    // match a system-ish prefix (e.g. YouTube starts with com.google.android.)
    private val neverSkip = distractPackages + focusPackages

    // Labels that indicate a system component masquerading as a user app
    private val systemLabels = setOf(
        "android", "system", "android system", "android os",
        "phone", "dialer", "launcher", "home", "settings",
        "package installer", "permission controller"
    )

    private fun shouldSkip(packageName: String, appName: String): Boolean {
        // Never skip known distract/focus apps regardless of prefix
        if (packageName in neverSkip) return false

        // Skip our own app
        if (packageName == context.packageName) return true

        // Skip blank names or generic system labels
        val nameLower = appName.trim().lowercase()
        if (nameLower.isBlank()) return true
        if (nameLower in systemLabels) return true
        // "Android" as an app label always means a system component
        if (nameLower == "android") return true

        // Skip low-level system prefixes
        // NOTE: neverSkip check at top means YouTube, Maps etc. are safe from this
        val systemPrefixes = listOf(
            "com.android.",
            "com.google.android.",          // catches camera2, tts, connectivity etc.
            "android.",
            "com.samsung.android.server",
            "com.sec.android.",
            "com.qualcomm.",
            "com.miui.",
            "com.huawei.systemmanager",
            "com.oneplus.",
            "com.oppo.",
            "com.realme."
        )
        if (systemPrefixes.any { packageName.startsWith(it) }) return true

        // Skip OS internals by exact package name
        val exactSkip = setOf(
            "android",
            "com.android.systemui",
            "com.android.launcher3",
            "com.android.launcher",
            "com.android.phone",
            "com.android.dialer",
            "com.android.settings",
            "com.android.inputmethod.latin",
            "com.google.android.inputmethod.latin",
            "com.samsung.android.honeyboard",
            "com.android.server.telecom",
            "com.android.keychain",
            "com.android.externalstorage",
            "com.android.providers.media",
            "com.android.providers.downloads",
            "com.android.bluetooth",
            "com.google.android.apps.wellbeing",  // Digital Wellbeing (not useful to show)
            "com.google.android.as"               // Android System Intelligence
        )
        return packageName in exactSkip
    }

    // ── Main query ────────────────────────────────────────────
    // Uses queryEvents (foreground/background events) for accurate real-time
    // same-day tracking. INTERVAL_BEST aggregates are cached by Android and
    // do NOT update live within the current day.

    fun queryTodayUsage(): List<AppUsage> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTime = cal.timeInMillis
        val endTime = System.currentTimeMillis()

        // Primary: calculate per-app foreground time from raw events (live, not cached)
        val foregroundMs = calculateForegroundTimeFromEvents(startTime, endTime)

        // Fallback: if events returned nothing (permission edge case), use INTERVAL_BEST
        val sourceMs: Map<String, Long> = if (foregroundMs.isNotEmpty()) {
            foregroundMs
        } else {
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_BEST, startTime, endTime
            ) ?: return emptyList()
            stats.associate { it.packageName to it.totalTimeInForeground }
        }

        return sourceMs
            .filter { (_, ms) -> ms > 30_000L }
            .mapNotNull { (pkg, ms) ->
                val appName = appNameFor(pkg)
                if (shouldSkip(pkg, appName)) return@mapNotNull null
                val totalMinutes = (ms / 60_000L).toInt()
                val category = categoryFor(pkg, appName)
                AppUsage(
                    packageName = pkg,
                    appName = appName,
                    emoji = emojiFor(pkg, appName),
                    totalMinutes = totalMinutes,
                    scoreImpact = scoreImpactFor(category, totalMinutes),
                    category = category
                )
            }
            .sortedByDescending { it.totalMinutes }
    }

    // ── Calculate foreground time from raw UsageEvents ────────
    // More accurate than INTERVAL_BEST for same-day queries because Android
    // caches INTERVAL_BEST aggregates and does not refresh them in real time.

    private fun calculateForegroundTimeFromEvents(startTime: Long, endTime: Long): Map<String, Long> {
        val events = usageStatsManager.queryEvents(startTime, endTime) ?: return emptyMap()
        val event = android.app.usage.UsageEvents.Event()

        // Track foreground-enter timestamps per package
        val foregroundStart = mutableMapOf<String, Long>()
        val foregroundTotal = mutableMapOf<String, Long>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            when (event.eventType) {
                android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    foregroundStart[pkg] = event.timeStamp
                }
                android.app.usage.UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val start = foregroundStart.remove(pkg) ?: continue
                    val elapsed = event.timeStamp - start
                    if (elapsed > 0) {
                        foregroundTotal[pkg] = (foregroundTotal[pkg] ?: 0L) + elapsed
                    }
                }
            }
        }

        // Any app still in foreground at query time: close its open interval
        val now = endTime
        foregroundStart.forEach { (pkg, start) ->
            val elapsed = now - start
            if (elapsed > 0) {
                foregroundTotal[pkg] = (foregroundTotal[pkg] ?: 0L) + elapsed
            }
        }

        return foregroundTotal
    }

    // ── Hourly focus data ─────────────────────────────────────
    // Shows relative activity level per hour (all apps, weighted by category).
    // Focus apps count fully, neutral apps at 50%, distract apps at 25%
    // so the pattern reflects productive usage skew.

    fun queryHourlyFocus(): List<Pair<Int, Int>> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayStart = cal.timeInMillis
        val now = System.currentTimeMillis()
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        val hourlyFocus = MutableList(24) { 0 }

        for (hour in 0 until 24) {
            val hourStart = dayStart + hour * 3_600_000L
            val hourEnd   = if (hour == currentHour) now else hourStart + 3_600_000L

            if (hourStart > now) break

            // For the current (partial) hour use events for live accuracy;
            // for completed past hours INTERVAL_BEST is fine (data is stable).
            val hourMs: Map<String, Long> = if (hour == currentHour) {
                calculateForegroundTimeFromEvents(hourStart, now)
            } else {
                val stats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_BEST, hourStart, hourEnd
                ) ?: continue
                stats.associate { it.packageName to it.totalTimeInForeground }
            }

            // Weighted minutes: focus=1.0, neutral=0.5, distract=0.25
            var weightedMinutes = 0.0
            for ((pkg, ms) in hourMs) {
                if (ms <= 0) continue
                val appName = appNameFor(pkg)
                if (shouldSkip(pkg, appName)) continue
                val mins = ms / 60_000.0
                val weight = when (categoryFor(pkg, appName)) {
                    AppCategory.FOCUS    -> 1.0
                    AppCategory.NEUTRAL  -> 0.5
                    AppCategory.DISTRACT -> 0.25
                }
                weightedMinutes += mins * weight
            }

            // Scale 0–60 weighted minutes → 0–10
            hourlyFocus[hour] = ((weightedMinutes / 60.0) * 10.0).toInt().coerceIn(0, 10)
        }

        return hourlyFocus.mapIndexed { hour, level -> Pair(hour, level) }
    }

    // ── App switch count (for penalty calc) ───────────────────

    fun queryAppSwitchCount(): Int {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val events = usageStatsManager.queryEvents(cal.timeInMillis, System.currentTimeMillis())
        var switchCount = 0
        val event = android.app.usage.UsageEvents.Event()

        while (events?.hasNextEvent() == true) {
            events.getNextEvent(event)
            if (event.eventType == android.app.usage.UsageEvents.Event.MOVE_TO_FOREGROUND) {
                switchCount++
            }
        }
        return switchCount
    }
}