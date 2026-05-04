package com.productivityos.app.presentation.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import com.productivityos.app.domain.model.*
import com.productivityos.app.presentation.ui.theme.*

// ═══════════════════════════════════════════════════════════════
// FOUNDATION COMPONENTS
// ═══════════════════════════════════════════════════════════════

// ── BaseCard ─────────────────────────────────────────────────
// All cards use this. No nested padding — one outer padding only.

@Composable
fun BaseCard(
    modifier: Modifier = Modifier,
    showGradientStrip: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
    ) {
        // Optional gradient identity strip along top edge
        if (showGradientStrip) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .align(Alignment.TopCenter)
                    .background(GradientPrimary)
            )
        }
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

// ── GradientButton ────────────────────────────────────────────
// Primary CTA — gradient background, rounded 12dp, scale on press.

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "btn_scale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) GradientPrimary else Brush.linearGradient(listOf(CardBorder, CardBorder)))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(
                color = if (enabled) White else TextTertiary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
        )
    }
}

// ── OutlineButton ─────────────────────────────────────────────
// Secondary CTA — outlined with border tinted from brand colors.

@Composable
fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, CardBorder),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
        contentPadding = PaddingValues(vertical = 14.dp)
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(
                color = TextSecondary,
                letterSpacing = 1.2.sp
            )
        )
    }
}


// ── StatCard ──────────────────────────────────────────────────
// Compact 2-column grid card: icon highlight + title + big value.

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    accentColor: Color = BrandBlue
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "stat_scale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {}
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Icon with gradient tinted background
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextTertiary,
                    letterSpacing = 1.2.sp
                )
            )
            Text(
                text = value,
                style = MaterialTheme.typography.displayMedium.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp
                )
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ── TaskItem ──────────────────────────────────────────────────
// Clean row: left = task info, right = priority indicator.

@Suppress("unused")
@Composable
fun TaskItem(
    title: String,
    subtitle: String,
    priority: InsightImpact,
    modifier: Modifier = Modifier,
    isDone: Boolean = false,
    onClick: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "task_scale"
    )
    val priorityColor = when (priority) {
        InsightImpact.HIGH   -> BrandBlue
        InsightImpact.MEDIUM -> AccentCyan
        InsightImpact.LOW    -> TextSecondary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Priority accent strip on the left edge
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(priorityColor)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = if (isDone) TextTertiary else TextPrimary,
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        // Status indicator
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isDone) BrandGreenAlpha20 else CardBgHigh)
                .border(1.dp, if (isDone) BrandGreen else CardBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = BrandGreen,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// SECTION HEADER
// ═══════════════════════════════════════════════════════════════

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(
            color = TextTertiary,
            letterSpacing = 1.8.sp
        ),
        modifier = modifier
    )
}

// ═══════════════════════════════════════════════════════════════
// SCORE BANNER (Hero Card)
// ═══════════════════════════════════════════════════════════════

@Composable
fun ScoreBanner(
    score: ProductivityScore,
    totalUsedMinutes: Int? = null,
    focusSessionMinutes: Int = 0,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
    ) {
        // Gradient strip across top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.TopCenter)
                .background(GradientPrimary)
        )
        // Subtle radial glow in top-right corner
        Box(
            modifier = Modifier
                .size(160.dp)
                .align(Alignment.TopEnd)
                .offset(x = 40.dp, y = (-40).dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(BrandBlue.copy(alpha = 0.12f), Color.Transparent)
                    )
                )
        )

        Column(modifier = Modifier.padding(20.dp)) {
            // Label
            Text(
                text = "TODAY'S OVERVIEW",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextTertiary,
                    letterSpacing = 1.8.sp
                )
            )
            Spacer(Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Big score number
                Column {
                    Text(
                        text = "${score.value}",
                        style = MaterialTheme.typography.displayLarge.copy(
                            brush = GradientAccent,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                    Text(
                        text = "SCORE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextTertiary,
                            letterSpacing = 1.4.sp
                        )
                    )
                }

                // Right: bar + stats
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Gradient progress bar
                    GradientProgressBar(fraction = score.value / 100f)

                    // 3 micro stats
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        HeroStat(
                            value = if (focusSessionMinutes > 0)
                                formatMinutes(focusSessionMinutes)
                            else
                                formatMinutes(score.focusMinutes),
                            label = "FOCUS",
                            color = BrandBlue
                        )
                        HeroStat(
                            value = formatMinutes(score.distractMinutes),
                            label = "LOST",
                            color = Danger
                        )
                        HeroStat(
                            value = formatMinutes(
                                totalUsedMinutes ?: (score.focusMinutes + score.distractMinutes)
                            ),
                            label = "TOTAL",
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroStat(value: String, label: String, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = color, letterSpacing = 1.sp)
        )
    }
}

// ── GradientProgressBar ───────────────────────────────────────

@Composable
fun GradientProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(99.dp))
            .background(CardBgHigh)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(99.dp))
                .background(GradientPrimary)
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// STREAK ROW
// ═══════════════════════════════════════════════════════════════

@Composable
fun StreakRow(streak: Int, bestStreak: Int, modifier: Modifier = Modifier) {
    BaseCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon box
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BrandGreenAlpha20),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🔥", fontSize = 22.sp)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "FOCUS STREAK",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextTertiary,
                        letterSpacing = 1.4.sp
                    )
                )
                Text(
                    text = "Best run: $bestStreak days",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }
            Text(
                text = "$streak",
                style = MaterialTheme.typography.displayMedium.copy(
                    brush = GradientAccent,
                    fontWeight = FontWeight.ExtraBold
                )
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// LEAK BANNER
// ═══════════════════════════════════════════════════════════════

@Composable
fun LeakBanner(app: AppUsage?, modifier: Modifier = Modifier) {
    BaseCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DangerAlpha),
                contentAlignment = Alignment.Center
            ) {
                Text(text = app?.emoji ?: "📱", fontSize = 22.sp)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "BIGGEST LEAK",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextTertiary,
                        letterSpacing = 1.4.sp
                    )
                )
                Text(
                    text = app?.appName ?: "Instagram",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = "${app?.totalMinutes ?: 42} minutes lost",
                    style = MaterialTheme.typography.bodySmall.copy(color = Danger)
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(DangerAlpha)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "−${app?.scoreImpact?.let { kotlin.math.abs(it) } ?: 12}",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Danger,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// HOUR CHART
// ═══════════════════════════════════════════════════════════════

@Composable
fun HourChart(hourlyFocus: List<HourlyFocus>, modifier: Modifier = Modifier) {
    var showDialog by remember { mutableStateOf(false) }

    val data = if (hourlyFocus.isEmpty()) {
        listOf(2, 2, 4, 5, 8, 9, 10, 8, 7, 9, 10, 8, 6, 4, 3, 5, 7, 8, 6, 4, 3, 2, 1, 1)
            .mapIndexed { h, l -> HourlyFocus(hour = h, level = l) }
    } else hourlyFocus

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .clickable { showDialog = true }
    ) {
        // gradient strip top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.TopCenter)
                .background(GradientPrimary)
        )
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TODAY'S PATTERN",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextTertiary, letterSpacing = 1.6.sp
                    )
                )
                // Tap hint
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(BrandBlueAlpha12)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "TAP TO EXPAND",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = BrandBlue, fontSize = 7.sp, letterSpacing = 1.sp
                        )
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            // Mini bar chart (preview)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                data.forEach { hf ->
                    val fraction = hf.level / 10f
                    val barMod = Modifier
                        .weight(1f)
                        .fillMaxHeight(fraction.coerceAtLeast(0.06f))
                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    when {
                        hf.level > 6 -> Box(modifier = barMod.background(GradientVertical))
                        hf.level > 3 -> Box(modifier = barMod.background(CardBgHigh))
                        else         -> Box(modifier = barMod.background(Danger.copy(alpha = 0.5f)))
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("12am", "6am", "12pm", "6pm", "11pm").forEach { label ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary)
                    )
                }
            }
        }
    }

    // ── Full-screen pattern dialog ────────────────────────────
    if (showDialog) {
        PatternDialog(data = data, onDismiss = { showDialog = false })
    }
}

// ── PatternDialog ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PatternDialog(data: List<HourlyFocus>, onDismiss: () -> Unit) {
    var selectedHour by remember { mutableStateOf<HourlyFocus?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = CardBg,
        tonalElevation   = 0.dp,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "TODAY'S PATTERN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextTertiary, letterSpacing = 1.8.sp
                        )
                    )
                    Text(
                        text = "Phone usage by hour",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            color = TextPrimary, fontWeight = FontWeight.Bold
                        )
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CardBgHigh)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("✕", style = MaterialTheme.typography.bodyLarge.copy(color = TextSecondary))
                }
            }
            Spacer(Modifier.height(20.dp))

            // Selected hour tooltip
            val selected = selectedHour
            androidx.compose.animation.AnimatedVisibility(
                visible = selected != null,
                enter   = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
                exit    = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
            ) {
                selected?.let { hf ->
                    val hourLabel = formatHour(hf.hour)
                    val levelLabel = when {
                        hf.level >= 8 -> "High focus 🔥"
                        hf.level >= 5 -> "Moderate use"
                        hf.level >= 2 -> "Low activity"
                        else          -> "Mostly idle 💤"
                    }
                    val barColor = when {
                        hf.level > 6 -> BrandBlue
                        hf.level > 3 -> TextSecondary
                        else         -> Danger
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(barColor.copy(alpha = 0.12f))
                            .border(1.dp, barColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = hourLabel,
                            style = MaterialTheme.typography.displayMedium.copy(
                                color = barColor, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp
                            )
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = levelLabel,
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    color = TextPrimary, fontWeight = FontWeight.SemiBold
                                )
                            )
                            Text(
                                text = "Focus intensity: ${hf.level * 10}%",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                        // Mini bar indicator
                        Column(
                            modifier = Modifier.width(6.dp).height(48.dp),
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(6.dp)
                                    .fillMaxHeight(hf.level / 10f)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(barColor)
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }
            }

            // Legend row
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(
                    Triple(BrandBlue,   "High focus", "7–10"),
                    Triple(TextSecondary,"Moderate",  "4–6"),
                    Triple(Danger,      "Low / idle", "0–3")
                ).forEach { (color, label, range) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Text(
                            text = "$label ($range)",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary)
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // Tall bar chart — all 24 hours, tappable
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                data.forEach { hf ->
                    val fraction = (hf.level / 10f).coerceAtLeast(0.04f)
                    val isSelected = selectedHour?.hour == hf.hour
                    val barColor = when {
                        hf.level > 6 -> null   // use gradient
                        hf.level > 3 -> CardBgHigh
                        else         -> Danger.copy(alpha = 0.6f)
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Selection ring
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(BrandBlue)
                            )
                            Spacer(Modifier.height(3.dp))
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .fillMaxWidth()
                                .fillMaxHeight(fraction)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .then(
                                    if (barColor != null)
                                        Modifier.background(
                                            if (isSelected) barColor.copy(alpha = 1f) else barColor
                                        )
                                    else
                                        Modifier.background(GradientVertical)
                                )
                                .border(
                                    width = if (isSelected) 1.dp else 0.dp,
                                    color = if (isSelected) BrandBlue else androidx.compose.ui.graphics.Color.Transparent,
                                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                )
                                .clickable { selectedHour = if (selectedHour?.hour == hf.hour) null else hf }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Hour labels — show every 3 hours
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                (0..23 step 4).forEach { h ->
                    Text(
                        text = formatHour(h),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextTertiary, fontSize = 8.sp
                        )
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Summary stats row
            val peakHour = data.maxByOrNull { it.level }
            val focusHours = data.count { it.level > 6 }
            val idleHours  = data.count { it.level <= 2 }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple("⏰", "Peak hour",  peakHour?.let { formatHour(it.hour) } ?: "—"),
                    Triple("🔥", "Focus hrs",  "${focusHours}h"),
                    Triple("💤", "Idle hrs",   "${idleHours}h")
                ).forEach { (emoji, label, value) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardBgHigh)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(emoji, fontSize = 18.sp)
                        Text(
                            value,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp
                            )
                        )
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextTertiary, fontSize = 9.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun formatHour(hour: Int): String {
    return when {
        hour == 0  -> "12am"
        hour < 12  -> "${hour}am"
        hour == 12 -> "12pm"
        else       -> "${hour - 12}pm"
    }
}

// ═══════════════════════════════════════════════════════════════
// WEEK CHART
// ═══════════════════════════════════════════════════════════════

@Composable
fun WeekChart(days: List<WeekDay>, modifier: Modifier = Modifier) {
    val maxFocus = days.maxOfOrNull { it.focusHours } ?: 1f
    val denominator = (maxFocus * 1.1f).coerceAtLeast(1f)

    BaseCard(modifier = modifier, showGradientStrip = true) {
        Text(
            text = "WEEKLY FOCUS",
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextTertiary,
                letterSpacing = 1.6.sp
            )
        )
        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            days.forEach { day ->
                val fraction = (day.focusHours / denominator).coerceIn(0.04f, 1f)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(0.6f)
                            .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                            .then(
                                if (day.isToday)
                                    Modifier.background(GradientVertical)
                                else
                                    Modifier.background(CardBgHigh)
                            )
                            .fillMaxHeight(fraction)
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            days.forEach { day ->
                Text(
                    text = day.label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (day.isToday) BrandCyan else TextTertiary,
                        fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// BURNOUT METER
// ═══════════════════════════════════════════════════════════════

@Composable
fun BurnoutMeter(
    workedMinutes: Int,
    capacityMinutes: Int,
    zone: BurnoutZone,
    modifier: Modifier = Modifier
) {
    val fraction = (workedMinutes.toFloat() / capacityMinutes).coerceIn(0f, 1f)
    val (zoneLabel, zoneColor) = when (zone) {
        BurnoutZone.SAFE    -> "SAFE ZONE" to BrandGreen
        BurnoutZone.CAUTION -> "CAUTION"   to Warning
        BurnoutZone.DANGER  -> "DANGER"    to Danger
    }

    BaseCard(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "CAPACITY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextTertiary, letterSpacing = 1.4.sp
                    )
                )
                Text(
                    text = formatMinutes(workedMinutes),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = TextPrimary, fontWeight = FontWeight.Bold
                    )
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(zoneColor.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    text = zoneLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = zoneColor, fontWeight = FontWeight.Bold
                    )
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        GradientProgressBar(fraction = fraction, height = 8.dp)
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("0h", "3h", "6h", "7.2h", "9h").forEach { tick ->
                Text(
                    text = tick,
                    style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// APP USAGE ROW
// ═══════════════════════════════════════════════════════════════

@Composable
fun AppUsageRow(app: AppUsage) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(CardBgHigh),
            contentAlignment = Alignment.Center
        ) {
            Text(text = app.emoji, fontSize = 18.sp)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = app.appName,
                style = MaterialTheme.typography.headlineSmall.copy(color = TextPrimary)
            )
            Text(
                text = formatMinutes(app.totalMinutes),
                style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary)
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val (bgColor, fgColor) = when (app.category) {
                AppCategory.FOCUS    -> BrandBlueAlpha20 to BrandBlue
                AppCategory.DISTRACT -> DangerAlpha      to Danger
                AppCategory.NEUTRAL  -> CardBgHigh      to TextSecondary
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(bgColor)
                    .padding(horizontal = 9.dp, vertical = 3.dp)
            ) {
                val prefix = if (app.scoreImpact > 0) "+" else ""
                Text(
                    text = "$prefix${app.scoreImpact}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = fgColor, fontWeight = FontWeight.Bold
                    )
                )
            }
            val fraction = (app.totalMinutes.toFloat() / 200).coerceIn(0f, 1f)
            val barColor = when (app.category) {
                AppCategory.FOCUS    -> BrandBlue
                AppCategory.DISTRACT -> Danger
                AppCategory.NEUTRAL  -> TextSecondary
            }
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(CardBgHigh)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction)
                        .clip(RoundedCornerShape(99.dp))
                        .background(barColor)
                )
            }
        }
    }
    HorizontalDivider(color = CardBorder, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
}

// ═══════════════════════════════════════════════════════════════
// SCHEDULE BLOCK ROW
// ═══════════════════════════════════════════════════════════════

@Composable
fun ScheduleBlockRow(block: ScheduleBlock) {
    val (accentColor, tagBg, tagFg, tagLabel) = when (block.blockType) {
        BlockType.DEEP  -> listOf(BrandBlue,    BrandBlueAlpha20,  BrandBlue,    "DEEP")
        BlockType.LIGHT -> listOf(AccentCyan,   AccentCyanAlpha15, AccentCyan,   "LIGHT")
        BlockType.BREAK -> listOf(TextTertiary, CardBgHigh,       TextSecondary,"BREAK")
        BlockType.ADMIN -> listOf(Warning,      WarningAlpha,      Warning,      "ADMIN")
    }
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(IntrinsicSize.Min)
                .background(accentColor as Color)
        )
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "${block.startTime}\n${block.endTime}",
                style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary),
                lineHeight = 14.sp,
                modifier = Modifier.width(48.dp)
            )
            Text(
                text = block.title,
                style = MaterialTheme.typography.headlineSmall.copy(color = TextPrimary),
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(tagBg as Color)
                    .padding(horizontal = 9.dp, vertical = 3.dp)
            ) {
                Text(
                    text = tagLabel as String,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = tagFg as Color, fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
    HorizontalDivider(color = CardBorder, thickness = 1.dp)
}

// ═══════════════════════════════════════════════════════════════
// IMPACT BADGE
// ═══════════════════════════════════════════════════════════════

@Composable
fun ImpactBadge(impact: InsightImpact) {
    val (bg, fg, label) = when (impact) {
        InsightImpact.HIGH   -> Triple(BrandBlueAlpha20,  BrandBlue,    "HIGH")
        InsightImpact.MEDIUM -> Triple(AccentCyanAlpha15, AccentCyan,   "MED")
        InsightImpact.LOW    -> Triple(CardBgHigh,       TextSecondary,"LOW")
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(bg)
            .padding(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = fg, fontWeight = FontWeight.Bold)
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// LIVE PILL
// ═══════════════════════════════════════════════════════════════

@Composable
fun LivePill() {
    val alpha by rememberInfiniteTransition(label = "live").animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot"
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(BrandBlueAlpha20)
            .border(1.dp, BrandBlue.copy(alpha = 0.3f), RoundedCornerShape(99.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(BrandBlue.copy(alpha = alpha))
        )
        Text(
            text = "LIVE",
            style = MaterialTheme.typography.labelSmall.copy(
                color = BrandBlue,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// TOGGLE
// ═══════════════════════════════════════════════════════════════

@Composable
fun PosToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        colors = SwitchDefaults.colors(
            checkedThumbColor   = White,
            checkedTrackColor   = BrandBlue,
            uncheckedThumbColor = White,
            uncheckedTrackColor = CardBorder
        )
    )
}

// ═══════════════════════════════════════════════════════════════
// LIVE TRACKER BOTTOM SHEET
// ═══════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTrackerBottomSheet(
    alert: LiveTrackerAlert,
    onSwitchToFocus: () -> Unit,
    onIgnore: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onIgnore,
        containerColor = CardBg,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // Alert badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(DangerAlpha)
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "⚠ LIVE TRACKER ALERT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Danger, fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                    )
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = alert.appName,
                style = MaterialTheme.typography.displayMedium.copy(
                    color = TextPrimary, fontWeight = FontWeight.ExtraBold
                )
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${alert.minutesSpent} minutes spent · You planned ${alert.plannedActivity} right now",
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )
            Spacer(Modifier.height(16.dp))
            // Info box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BrandBlueAlpha12)
                    .border(1.dp, BrandBlue.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "You have deep work planned for this time block.",
                    style = MaterialTheme.typography.bodySmall.copy(color = BrandBlue)
                )
            }
            Spacer(Modifier.height(20.dp))
            GradientButton(text = "Switch to Focus Mode", onClick = onSwitchToFocus)
            Spacer(Modifier.height(8.dp))
            OutlineButton(text = "Ignore for Now", onClick = onIgnore)
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// HELPERS
// ═══════════════════════════════════════════════════════════════

fun formatMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

fun formatSeconds(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return String.format(java.util.Locale.US, "%02d:%02d:%02d", h, m, s)
}