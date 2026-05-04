package com.productivityos.app.presentation.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.productivityos.app.R
import com.productivityos.app.domain.model.*
import com.productivityos.app.presentation.ui.components.*
import com.productivityos.app.presentation.ui.theme.*
import com.productivityos.app.presentation.viewmodel.*
import java.time.LocalTime
import kotlinx.coroutines.delay

// ═══════════════════════════════════════════════════════════════
// ENTRY
// ═══════════════════════════════════════════════════════════════

@Composable
fun HomeScreen(
    onNavigateFocus: () -> Unit,
    onNavigateInsights: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Refresh score + usage every 60 s so Home stays in sync with real app activity
    // Refresh immediately on screen enter, then every 30 s
    LaunchedEffect(Unit) {
        viewModel.refreshLiveData()
        while (true) {
            delay(30_000L)
            viewModel.refreshLiveData()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)   // ONE outer horizontal padding
                .padding(bottom = 100.dp)       // nav bar clearance
        ) {
            val userName = (uiState as? HomeUiState.Success)?.userName ?: ""

            HomeTopBar(
                userName = userName,
                onLiveClick = { viewModel.showLiveTrackerAlert() }
            )

            when (val state = uiState) {
                is HomeUiState.Loading -> LoadingContent()
                is HomeUiState.Error   -> ErrorContent(state.message)
                is HomeUiState.Success -> HomeContent(
                    state = state,
                    onViewInsights = onNavigateInsights,
                    onStartFocus = onNavigateFocus
                )
            }
        }

        // Live Tracker Bottom Sheet
        val state = uiState as? HomeUiState.Success
        if (state?.showLiveTrackerAlert == true && state.liveTrackerAlert != null) {
            LiveTrackerBottomSheet(
                alert = state.liveTrackerAlert,
                onSwitchToFocus = {
                    viewModel.switchToFocusMode()
                    onNavigateFocus()
                },
                onIgnore = { viewModel.dismissLiveTrackerAlert() }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// TOP BAR
// ═══════════════════════════════════════════════════════════════

@Composable
private fun HomeTopBar(userName: String, onLiveClick: () -> Unit) {
    val hour = LocalTime.now().hour
    val greeting = when {
        hour < 12 -> "Good Morning"
        hour < 17 -> "Good Afternoon"
        else      -> "Good Evening"
    }
    val displayName = userName.trim().split(" ").firstOrNull()
        ?.takeIf { it.isNotBlank() } ?: "there"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = greeting.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextTertiary,
                    letterSpacing = 1.6.sp
                )
            )
            Text(
                text = "$displayName 👋",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                text = "Here's your focus summary",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LivePill()
            ProfileAvatar(name = displayName, onClick = onLiveClick)
        }
    }
}

@Composable
private fun ProfileAvatar(name: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(BrandBlueAlpha20)
            .border(1.5.dp, BrandBlue.copy(alpha = 0.4f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.take(1).uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(
                color = BrandBlue,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// MAIN CONTENT
// ═══════════════════════════════════════════════════════════════

@Composable
private fun HomeContent(
    state: HomeUiState.Success,
    onViewInsights: () -> Unit,
    onStartFocus: () -> Unit
) {
    // ── Hero score card ───────────────────────────────────────
    ScoreBanner(
        score = state.score,
        totalUsedMinutes = state.totalUsedMinutes,
        focusSessionMinutes = state.totalFocusSessionMinutes
    )
    Spacer(Modifier.height(12.dp))

    // ── Secondary 2-col stats grid ────────────────────────────
    SectionHeader(text = "At a Glance")
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            title = "Streak",
            value = "${state.streak}d",
            icon = Icons.Rounded.Whatshot,
            subtitle = "Best: ${state.bestStreak}d",
            accentColor = BrandGreen,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Deep sessions",
            value = "${state.score.deepSessions}/3",
            icon = Icons.Rounded.Lightbulb,
            subtitle = if (state.score.deepSessions >= 3) "✅ Target hit!" else "${3 - state.score.deepSessions} more to go",
            accentColor = if (state.score.deepSessions >= 3) BrandGreen else BrandBlue,
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(Modifier.height(10.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            title = "Focus",
            value = if (state.totalFocusSessionMinutes > 0)
                formatMinutes(state.totalFocusSessionMinutes)
            else
                formatMinutes(state.score.focusMinutes),
            icon = Icons.Rounded.Timer,
            subtitle = if (state.totalFocusSessionMinutes > 0)
                "Session time today"
            else
                "vs ${formatMinutes(state.score.distractMinutes)} lost",
            accentColor = AccentCyan,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "Daily Target",
            value = "${state.score.value}%",
            icon = Icons.Rounded.Flag,
            subtitle = if (state.score.value >= 80) "🎯 Goal reached!" else "${80 - state.score.value}% to reach 80",
            accentColor = when {
                state.score.value >= 80 -> BrandGreen
                state.score.value >= 50 -> BrandTeal
                else -> Warning
            },
            modifier = Modifier.weight(1f)
        )
    }
    Spacer(Modifier.height(12.dp))

    // ── Leak banner (conditional) ─────────────────────────────
    if (state.topLeakApp != null) {
        LeakBanner(app = state.topLeakApp)
        Spacer(Modifier.height(12.dp))
    }

    // ── Quick actions ─────────────────────────────────────────
    SectionHeader(text = "Quick Actions")
    Spacer(Modifier.height(8.dp))
    QuickActionsRow(
        onStartFocus = onStartFocus,
        onInsights = onViewInsights
    )
    Spacer(Modifier.height(12.dp))

    // ── Today's pattern card ──────────────────────────────────
    TodayPatternCard(hourlyFocus = state.hourlyFocus)
    Spacer(Modifier.height(12.dp))

    // ── Streak card ───────────────────────────────────────────
    StreakRow(streak = state.streak, bestStreak = state.bestStreak)
    Spacer(Modifier.height(20.dp))

    // ── CTA buttons ───────────────────────────────────────────
    GradientButton(
        text = stringResource(R.string.start_session),
        onClick = onStartFocus
    )
    Spacer(Modifier.height(8.dp))
    OutlineButton(
        text = stringResource(R.string.view_insights),
        onClick = onViewInsights
    )
}

// ═══════════════════════════════════════════════════════════════
// QUICK ACTIONS ROW
// ═══════════════════════════════════════════════════════════════

private data class QuickAction(val icon: ImageVector, val label: String, val onClick: () -> Unit)

@Composable
private fun QuickActionsRow(
    onStartFocus: () -> Unit,
    onInsights: () -> Unit
) {
    val actions = listOf(
        QuickAction(Icons.Rounded.PlayArrow, "Focus",      onStartFocus),
        QuickAction(Icons.Rounded.BarChart,  "Insights",   onInsights),
        QuickAction(Icons.Rounded.Block,     "Block Apps", onStartFocus)
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        actions.forEach { action ->
            QuickActionChip(action)
        }
    }
}

@Composable
private fun QuickActionChip(action: QuickAction) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = action.onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(BrandBlueAlpha12),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = action.icon,
                contentDescription = action.label,
                tint = BrandBlue,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            text = action.label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextSecondary,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
            )
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// TODAY'S PATTERN CARD
// ═══════════════════════════════════════════════════════════════

@Composable
private fun TodayPatternCard(hourlyFocus: List<HourlyFocus>) {
    val data = if (hourlyFocus.isEmpty()) {
        listOf(1,1,2,2,3,5,8,10,9,8,7,9,10,8,5,4,6,7,5,3,2,2,1,1)
            .mapIndexed { h, l -> HourlyFocus(hour = h, level = l) }
    } else {
        // fill missing hours with 0
        val map = hourlyFocus.associateBy { it.hour }
        (0..23).map { h -> map[h] ?: HourlyFocus(h, 0) }
    }

    val peakHour   = data.maxByOrNull { it.level }?.hour ?: 9
    val peakLabel  = when {
        peakHour < 12 -> "%02d:00 AM".format(peakHour)
        peakHour == 12 -> "12:00 PM"
        else -> "%02d:00 PM".format(peakHour - 12)
    }
    val totalFocusHours = data.filter { it.level >= 7 }.size  // hours with high focus
    val qualityLabel = when {
        totalFocusHours >= 6  -> "Excellent" to BrandGreen
        totalFocusHours >= 4  -> "Good"      to AccentCyan
        totalFocusHours >= 2  -> "Fair"      to Warning
        else                  -> "Low"       to Danger
    }

    // Segment data into 3 periods
    val morning   = data.filter { it.hour in 5..11 }
    val afternoon = data.filter { it.hour in 12..17 }
    val evening   = data.filter { it.hour in 18..23 }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
    ) {
        // Top accent strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.TopCenter)
                .background(GradientPrimary)
        )

        Column(modifier = Modifier.padding(16.dp)) {

            // ── Header row ────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "TODAY'S PATTERN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextTertiary, letterSpacing = 1.6.sp
                        )
                    )
                    Text(
                        text = "Focus rhythm",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = TextPrimary, fontWeight = FontWeight.SemiBold
                        )
                    )
                }
                // Quality badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(qualityLabel.second.copy(alpha = 0.15f))
                        .border(1.dp, qualityLabel.second.copy(alpha = 0.35f), RoundedCornerShape(99.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = qualityLabel.first,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = qualityLabel.second,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ── Full 24h bar chart ─────────────────────────────
            val currentHour = java.time.LocalTime.now().hour
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                data.forEach { hf ->
                    val fraction = (hf.level / 10f).coerceAtLeast(0.05f)
                    val isNow    = hf.hour == currentHour
                    val isFuture = hf.hour > currentHour
                    val barColor = when {
                        isFuture       -> SurfaceBorder
                        hf.level >= 7  -> BrandBlue
                        hf.level >= 4  -> AccentCyan.copy(alpha = 0.7f)
                        else           -> Danger.copy(alpha = 0.45f)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(fraction)
                            .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                            .background(if (isNow) BrandGreen else barColor)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("12a", "6a", "12p", "6p", "11p").forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextTertiary, fontSize = 9.sp
                        )
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Divider(color = SurfaceBorder, thickness = 0.5.dp)
            Spacer(Modifier.height(12.dp))

            // ── 3-segment summary row ─────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PatternSegment(
                    label    = "Morning",
                    emoji    = "🌅",
                    segments = morning,
                    modifier = Modifier.weight(1f)
                )
                PatternSegment(
                    label    = "Afternoon",
                    emoji    = "☀️",
                    segments = afternoon,
                    modifier = Modifier.weight(1f)
                )
                PatternSegment(
                    label    = "Evening",
                    emoji    = "🌙",
                    segments = evening,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))
            Divider(color = SurfaceBorder, thickness = 0.5.dp)
            Spacer(Modifier.height(10.dp))

            // ── Peak hour + today dot legend ──────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Whatshot,
                        contentDescription = null,
                        tint = BrandGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "Peak at $peakLabel",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary, fontSize = 11.sp
                        )
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(BrandGreen)
                    )
                    Text(
                        text = "Now",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextTertiary, fontSize = 10.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun PatternSegment(
    label: String,
    emoji: String,
    segments: List<HourlyFocus>,
    modifier: Modifier = Modifier
) {
    val avg = if (segments.isEmpty()) 0f else segments.map { it.level }.average().toFloat()
    val pct = (avg / 10f).coerceIn(0f, 1f)
    val color = when {
        avg >= 7 -> BrandBlue
        avg >= 4 -> AccentCyan
        else     -> Danger.copy(alpha = 0.7f)
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceBorder.copy(alpha = 0.3f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = "$emoji $label",
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextSecondary, fontSize = 10.sp
            )
        )
        // Thin progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(CardBgHigh)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(pct)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(99.dp))
                    .background(color)
            )
        }
        Text(
            text = "%.0f%%".format(pct * 100),
            style = MaterialTheme.typography.labelSmall.copy(
                color = color, fontWeight = FontWeight.Bold, fontSize = 11.sp
            )
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// LOADING / ERROR
// ═══════════════════════════════════════════════════════════════

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = BrandBlue,
            strokeWidth = 2.dp
        )
    }
}

@Composable
private fun ErrorContent(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(DangerAlpha),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Warning,
                contentDescription = null,
                tint = Danger,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = TextSecondary
            )
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// PREVIEW
// ═══════════════════════════════════════════════════════════════

@Preview(showBackground = true, backgroundColor = 0xFF0A0F14)
@Composable
fun HomeScreenPreview() {
    ProductivityOSTheme {
        HomeScreen(onNavigateFocus = {}, onNavigateInsights = {})
    }
}