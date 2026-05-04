package com.productivityos.app.presentation.ui.screens

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.productivityos.app.R
import com.productivityos.app.domain.model.ScheduleChange
import com.productivityos.app.presentation.ui.components.*
import com.productivityos.app.presentation.ui.theme.*
import com.productivityos.app.presentation.viewmodel.*

// ═══════════════════════════════════════════════════════════════
// ENTRY
// ═══════════════════════════════════════════════════════════════

@Composable
fun ScheduleScreen(
    onBack: () -> Unit = {},
    viewModel: ScheduleViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 100.dp)
    ) {
        // ── Top bar ───────────────────────────────────────────
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(CardBgHigh)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AI OPTIMISED",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextTertiary, letterSpacing = 1.8.sp
                    )
                )
                Text(
                    text = stringResource(R.string.schedule_title),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = TextPrimary, fontWeight = FontWeight.Bold
                    )
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        when (val state = uiState) {
            is ScheduleUiState.Loading -> {
                Box(
                    Modifier.fillMaxWidth().height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BrandBlue, strokeWidth = 2.dp)
                }
            }
            is ScheduleUiState.Error -> {
                ErrorBox(message = state.message)
            }
            is ScheduleUiState.Success -> {
                ScheduleContent(
                    state = state,
                    onApply       = { viewModel.applyNewSchedule() },
                    onRegenerate  = { viewModel.generateNewSchedule() }
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// SCHEDULE CONTENT
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ScheduleContent(
    state: ScheduleUiState.Success,
    onApply: () -> Unit,
    onRegenerate: () -> Unit
) {
    // ── AI badge card ─────────────────────────────────────────
    AiHeaderCard()
    Spacer(Modifier.height(12.dp))

    // ── Schedule blocks ───────────────────────────────────────
    SectionHeader(text = "TODAY'S BLOCKS")
    Spacer(Modifier.height(8.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
    ) {
        if (state.blocks.isEmpty()) {
            EmptyBlocksState()
        } else {
            state.blocks.forEach { block ->
                ScheduleBlockRow(block = block)
            }
        }
    }
    Spacer(Modifier.height(12.dp))

    // ── Changes made ──────────────────────────────────────────
    SectionHeader(text = stringResource(R.string.changes_made))
    Spacer(Modifier.height(8.dp))
    ChangesCard(changes = state.changes)
    Spacer(Modifier.height(16.dp))

    // ── Applied banner ────────────────────────────────────────
    if (state.applied) {
        AppliedBanner()
        Spacer(Modifier.height(10.dp))
        OutlineButton(text = "Regenerate Schedule", onClick = onRegenerate)
    } else {
        GradientButton(
            text = stringResource(R.string.apply_schedule),
            onClick = onApply
        )
        Spacer(Modifier.height(8.dp))
        OutlineButton(text = "Regenerate Schedule", onClick = onRegenerate)
    }
}

// ═══════════════════════════════════════════════════════════════
// AI HEADER CARD
// ═══════════════════════════════════════════════════════════════

@Composable
private fun AiHeaderCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
    ) {
        // Gradient strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.TopCenter)
                .background(GradientPrimary)
        )
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BrandBlueAlpha12),
                contentAlignment = Alignment.Center
            ) {
                Text("🤖", fontSize = 22.sp)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.ai_optimised),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = TextPrimary, fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    text = stringResource(R.string.based_on_patterns),
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(BrandBlueAlpha12)
                    .border(1.dp, BrandBlue.copy(alpha = 0.25f), RoundedCornerShape(99.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "AI",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = BrandBlue, fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// CHANGES CARD
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ChangesCard(changes: List<ScheduleChange>) {
    if (changes.isEmpty()) {
        BaseCard {
            Text(
                text = "No changes from default schedule",
                style = MaterialTheme.typography.bodySmall.copy(color = TextTertiary)
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
    ) {
        changes.forEachIndexed { index, change ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Dot indicator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (change.isPositive) BrandGreen else Danger)
                )
                Text(
                    text = change.description,
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary),
                    modifier = Modifier.weight(1f)
                )
                // Positive/negative badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(
                            if (change.isPositive) BrandGreenAlpha12 else DangerAlpha
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (change.isPositive) "+" else "−",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (change.isPositive) BrandGreen else Danger,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
            if (index < changes.size - 1) {
                HorizontalDivider(
                    color = SurfaceBorder,
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// APPLIED BANNER
// ═══════════════════════════════════════════════════════════════

@Composable
private fun AppliedBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BrandGreenAlpha12)
            .border(1.dp, BrandGreen.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(BrandGreenAlpha20),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = BrandGreen,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = "Schedule applied to your day!",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = BrandGreen, fontWeight = FontWeight.SemiBold
            )
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// EMPTY / ERROR STATES
// ═══════════════════════════════════════════════════════════════

@Composable
private fun EmptyBlocksState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("📅", fontSize = 32.sp)
        Text(
            text = "No blocks yet",
            style = MaterialTheme.typography.headlineSmall.copy(color = TextSecondary),
            textAlign = TextAlign.Center
        )
        Text(
            text = "Tap Regenerate to build an AI schedule",
            style = MaterialTheme.typography.bodySmall.copy(color = TextTertiary),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorBox(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DangerAlpha)
            .border(1.dp, Danger.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Rounded.Warning,
            contentDescription = null,
            tint = Danger,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall.copy(color = Danger)
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// PREVIEW
// ═══════════════════════════════════════════════════════════════

@Preview(showBackground = true, backgroundColor = 0xFF0A0F14)
@Composable
fun ScheduleScreenPreview() {
    ProductivityOSTheme {
        ScheduleScreen()
    }
}