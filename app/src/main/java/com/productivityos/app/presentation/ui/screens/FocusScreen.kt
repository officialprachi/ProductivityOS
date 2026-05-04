package com.productivityos.app.presentation.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.productivityos.app.R
import com.productivityos.app.presentation.ui.components.*
import com.productivityos.app.presentation.ui.theme.*
import com.productivityos.app.presentation.viewmodel.FocusViewModel
import com.productivityos.app.presentation.viewmodel.knownAppNames

// ═══════════════════════════════════════════════════════════════
// ENTRY
// ═══════════════════════════════════════════════════════════════

@Composable
fun FocusScreen(viewModel: FocusViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Usage limit dialog
    uiState.limitDialogPkg?.let { pkg ->
        UsageLimitDialog(
            appName      = viewModel.friendlyName(pkg),
            currentLimit = uiState.appUsageLimits[pkg] ?: 0,
            usedMinutes  = uiState.todayUsageMinutes[pkg] ?: 0,
            onConfirm    = { mins -> viewModel.setUsageLimit(pkg, mins); viewModel.closeLimitDialog() },
            onDismiss    = { viewModel.closeLimitDialog() }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 100.dp)
    ) {
        // ── Top bar ───────────────────────────────────────────
        Spacer(Modifier.height(16.dp))
        Text(
            text = "FOCUS MODE",
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextTertiary, letterSpacing = 1.8.sp
            )
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.focus_title),
            style = MaterialTheme.typography.headlineMedium.copy(
                color = TextPrimary, fontWeight = FontWeight.Bold
            )
        )
        Spacer(Modifier.height(20.dp))

        // ── Timer hero ────────────────────────────────────────
        TimerDisplay(
            remainingSeconds = uiState.remainingSeconds,
            isRunning        = uiState.isRunning
        )
        Spacer(Modifier.height(12.dp))

        // ── Duration chips ────────────────────────────────────
        SectionHeader(text = stringResource(R.string.select_duration))
        Spacer(Modifier.height(8.dp))
        DurationChips(
            selectedDuration = uiState.selectedDuration,
            onSelect         = { viewModel.selectDuration(it) }
        )
        Spacer(Modifier.height(12.dp))

        // ── CTA button ────────────────────────────────────────
        if (uiState.isRunning) {
            // Stop — red outline
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Danger.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .clickable { viewModel.toggleSession() }
                    .padding(vertical = 15.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Stop,
                        contentDescription = null,
                        tint = Danger,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.end_session).uppercase(),
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = Danger, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp
                        )
                    )
                }
            }
        } else {
            GradientButton(
                text = stringResource(R.string.start_session),
                onClick = { viewModel.toggleSession() }
            )
        }
        Spacer(Modifier.height(20.dp))

        // ── Blocked apps ──────────────────────────────────────
        SectionHeader(text = stringResource(R.string.blocked_apps))
        Spacer(Modifier.height(8.dp))
        AppPickerGrid(
            allApps      = knownAppNames.keys.toList(),
            blockedApps  = uiState.blockedApps,
            enabled      = !uiState.isRunning,
            friendlyName = { viewModel.friendlyName(it) },
            onToggle     = { viewModel.toggleBlockedApp(it) }
        )
        Spacer(Modifier.height(20.dp))

        // ── Usage limits ──────────────────────────────────────
        SectionHeader(text = stringResource(R.string.usage_limits))
        Spacer(Modifier.height(8.dp))
        AppUsageLimitsGrid(
            allApps      = knownAppNames.keys.toList(),
            limits       = uiState.appUsageLimits,
            usedMinutes  = uiState.todayUsageMinutes,
            friendlyName = { viewModel.friendlyName(it) },
            onSetLimit   = { pkg -> viewModel.openLimitDialog(pkg) }
        )
        Spacer(Modifier.height(20.dp))

        // ── Burnout meter ─────────────────────────────────────
        SectionHeader(text = stringResource(R.string.burnout_meter))
        Spacer(Modifier.height(8.dp))
        BurnoutMeter(
            workedMinutes   = uiState.burnoutWorkedMinutes,
            capacityMinutes = uiState.burnoutCapacityMinutes,
            zone            = uiState.burnoutZone
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// TIMER DISPLAY — gradient ring hero
// ═══════════════════════════════════════════════════════════════

@Composable
private fun TimerDisplay(remainingSeconds: Int, isRunning: Boolean) {
    // Pulse animation when running
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f,
        targetValue  = if (isRunning) 1.03f else 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ring_pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .border(
                1.dp,
                if (isRunning) BrandBlue.copy(alpha = 0.35f) else SurfaceBorder,
                RoundedCornerShape(20.dp)
            )
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        // Subtle background glow when running
        if (isRunning) {
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(BrandBlue.copy(alpha = 0.08f), androidx.compose.ui.graphics.Color.Transparent)
                        )
                    )
            )
        }

        // Ring circle
        Box(
            modifier = Modifier
                .size((180 * pulse).dp)
                .clip(CircleShape)
                .background(
                    if (isRunning) BrandBlueAlpha12
                    else CardBgHigh
                )
                .then(
                    if (isRunning) Modifier.border(1.5.dp, BrandBlue.copy(alpha = 0.25f), CircleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = formatSeconds(remainingSeconds),
                    style = if (isRunning)
                        MaterialTheme.typography.headlineMedium.copy(
                            brush = GradientAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 34.sp,
                            fontFamily = MonoFamily
                        )
                    else
                        MaterialTheme.typography.headlineMedium.copy(
                            color = TextTertiary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 34.sp,
                            fontFamily = MonoFamily
                        ),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = if (isRunning) "IN PROGRESS" else "READY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isRunning) BrandBlue else TextTertiary,
                        letterSpacing = 1.4.sp
                    )
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// DURATION CHIPS
// ═══════════════════════════════════════════════════════════════

@Composable
private fun DurationChips(selectedDuration: Int, onSelect: (Int) -> Unit) {
    val durations = listOf(25 to "25m", 45 to "45m", 60 to "60m", 90 to "90m")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        durations.forEach { (mins, label) ->
            val isSelected = selectedDuration == mins
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) BrandBlueAlpha20 else CardBg)
                    .border(
                        1.dp,
                        if (isSelected) BrandBlue.copy(alpha = 0.4f) else SurfaceBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelect(mins) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = if (isSelected) BrandBlue else TextTertiary,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// APP PICKER GRID
// ═══════════════════════════════════════════════════════════════

@Suppress("SpellCheckingInspection")
private val appEmojis = mapOf(
    "com.instagram.android"      to "📸",
    "com.google.android.youtube" to "🎥",
    "com.whatsapp"               to "💬",
    "com.snapchat.android"       to "👻",
    "com.twitter.android"        to "🐦",
    "com.facebook.katana"        to "👥",
    "com.spotify.music"          to "🎵",
    "com.netflix.mediaclient"    to "🎬"
)

@Composable
private fun AppPickerGrid(
    allApps: List<String>,
    blockedApps: List<String>,
    enabled: Boolean,
    friendlyName: (String) -> String,
    onToggle: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        allApps.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { pkg ->
                    val isBlocked = pkg in blockedApps
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isBlocked) DangerAlpha else CardBg)
                            .border(
                                1.dp,
                                if (isBlocked) Danger.copy(alpha = 0.4f) else SurfaceBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .then(if (enabled) Modifier.clickable { onToggle(pkg) } else Modifier)
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = appEmojis[pkg] ?: "📱", fontSize = 16.sp)
                        Text(
                            text = friendlyName(pkg),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = if (isBlocked) Danger else TextSecondary,
                                fontWeight = FontWeight.SemiBold
                            ),
                            maxLines = 1
                        )
                        // Checkbox indicator
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(if (isBlocked) DangerAlpha else CardBgHigh)
                                .border(
                                    1.dp,
                                    if (isBlocked) Danger.copy(alpha = 0.5f) else SurfaceBorder,
                                    RoundedCornerShape(5.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isBlocked) {
                                Text("✓", fontSize = 10.sp, color = Danger, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
    if (!enabled) {
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Stop session to edit blocked apps",
            style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary)
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// USAGE LIMITS GRID
// ═══════════════════════════════════════════════════════════════

@Composable
private fun AppUsageLimitsGrid(
    allApps: List<String>,
    limits: Map<String, Int>,
    usedMinutes: Map<String, Int>,
    friendlyName: (String) -> String,
    onSetLimit: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        allApps.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { pkg ->
                    val limitMins = limits[pkg] ?: 0
                    val usedMins  = usedMinutes[pkg] ?: 0
                    val hasLimit  = limitMins > 0
                    val exceeded  = hasLimit && usedMins >= limitMins
                    val progress  = if (hasLimit) (usedMins.toFloat() / limitMins).coerceIn(0f, 1f) else 0f

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (exceeded) DangerAlpha else CardBg)
                            .border(
                                1.dp,
                                when {
                                    exceeded -> Danger.copy(alpha = 0.4f)
                                    hasLimit -> BrandBlue.copy(alpha = 0.3f)
                                    else     -> SurfaceBorder
                                },
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onSetLimit(pkg) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(text = appEmojis[pkg] ?: "📱", fontSize = 15.sp)
                            Text(
                                text = friendlyName(pkg),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (exceeded) Danger else TextSecondary,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                maxLines = 1
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        if (hasLimit) {
                            GradientProgressBar(
                                fraction = progress,
                                height   = 3.dp
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = if (exceeded) "Limit reached · ${limitMins}m"
                                else "${usedMins}m / ${limitMins}m",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (exceeded) Danger else TextTertiary,
                                    fontSize = 10.sp
                                )
                            )
                        } else {
                            Text(
                                text = "Tap to set limit",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = TextTertiary, fontSize = 10.sp
                                )
                            )
                        }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// USAGE LIMIT DIALOG
// ═══════════════════════════════════════════════════════════════

@Composable
private fun UsageLimitDialog(
    appName: String,
    currentLimit: Int,
    usedMinutes: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val presets = listOf(0, 15, 30, 45, 60, 90, 120)
    var selected by remember { mutableIntStateOf(currentLimit) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = CardBg,
        shape            = RoundedCornerShape(20.dp),
        title = {
            Text(
                text  = "Daily Limit · $appName",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = TextPrimary, fontWeight = FontWeight.Bold
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (usedMinutes > 0) {
                    Text(
                        text  = "Used today: ${usedMinutes}m",
                        style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary)
                    )
                }
                presets.chunked(4).forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        row.forEach { mins ->
                            val label = if (mins == 0) "None" else "${mins}m"
                            val isSelected = selected == mins
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) BrandBlueAlpha20 else CardBgHigh)
                                    .border(
                                        1.dp,
                                        if (isSelected) BrandBlue.copy(alpha = 0.4f) else SurfaceBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selected = mins }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text  = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isSelected) BrandBlue else TextTertiary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
                if (selected > 0) {
                    Text(
                        text  = "App blocked after ${selected}m of daily use",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = BrandBlue.copy(alpha = 0.8f), fontSize = 11.sp
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selected) }) {
                Text(
                    "Save",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = BrandBlue, fontWeight = FontWeight.Bold
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Cancel",
                    style = MaterialTheme.typography.labelLarge.copy(color = TextTertiary)
                )
            }
        }
    )
}

// ═══════════════════════════════════════════════════════════════
// PREVIEW
// ═══════════════════════════════════════════════════════════════

@Preview(showBackground = true, backgroundColor = 0xFF0A0F14)
@Composable
fun FocusScreenPreview() {
    ProductivityOSTheme {
        FocusScreen()
    }
}