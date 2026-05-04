package com.productivityos.app.presentation.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.productivityos.app.R
import com.productivityos.app.presentation.ui.components.*
import com.productivityos.app.presentation.ui.theme.*
import com.productivityos.app.presentation.viewmodel.*
import kotlinx.coroutines.delay

@Composable
fun AnalyticsScreen(
    onNavigateFocus: () -> Unit = {},
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // ── Unchanged logic: refresh every 60s ───────────────────
    // Refresh immediately on screen enter, then every 30 s
    LaunchedEffect(Unit) {
        viewModel.refreshLiveData()
        while (true) {
            delay(30_000L)
            viewModel.refreshLiveData()
        }
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "OVERVIEW",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextTertiary, letterSpacing = 1.8.sp
                    )
                )
                Text(
                    text = stringResource(R.string.analytics_title),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = TextPrimary, fontWeight = FontWeight.Bold
                    )
                )
            }
            LivePill()
        }
        Spacer(Modifier.height(20.dp))

        when (val state = uiState) {
            is AnalyticsUiState.Loading -> {
                Box(
                    Modifier.fillMaxWidth().height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BrandBlue, strokeWidth = 2.dp)
                }
            }
            is AnalyticsUiState.Error -> {
                // unchanged: just re-skinned error text
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodySmall.copy(color = Danger)
                )
            }
            is AnalyticsUiState.Success -> {
                // ── Summary stat pills (2×2 grid) ─────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryPill(
                        value      = state.weeklyData.totalTracked,
                        label      = stringResource(R.string.total_tracked),
                        valueColor = BrandBlue,
                        modifier   = Modifier.weight(1f)
                    )
                    SummaryPill(
                        value      = state.weeklyData.scoreImpact,
                        label      = stringResource(R.string.score_impact),
                        valueColor = Danger,
                        modifier   = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SummaryPill(
                        value      = if (state.focusSessionMinutes > 0)
                            "${state.focusSessionMinutes / 60}h ${state.focusSessionMinutes % 60}m"
                        else
                            state.weeklyData.focusTime,
                        label      = stringResource(R.string.focus_time),
                        valueColor = BrandGreen,
                        modifier   = Modifier.weight(1f)
                    )
                    SummaryPill(
                        value      = state.weeklyData.distractTime,
                        label      = stringResource(R.string.distract_time),
                        valueColor = Warning,
                        modifier   = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(12.dp))

                // ── Week chart ────────────────────────────────
                WeekChart(days = state.weeklyData.days)
                Spacer(Modifier.height(12.dp))

                // ── App list ──────────────────────────────────
                SectionHeader(text = stringResource(R.string.apps_used_today))
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBg)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
                ) {
                    if (state.todayUsage.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No app usage recorded today",
                                style = MaterialTheme.typography.bodySmall.copy(color = TextTertiary)
                            )
                        }
                    } else {
                        state.todayUsage.forEach { app ->
                            AppUsageRow(app = app)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))

                OutlineButton(
                    text = stringResource(R.string.set_usage_limits),
                    onClick = { onNavigateFocus() }
                )
            }
        }
    }
}

// ── SummaryPill — re-skinned, same data ───────────────────────
@Composable
fun SummaryPill(
    value: String,
    label: String,
    valueColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.displayMedium.copy(
                color = valueColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0F14)
@Composable
fun AnalyticsScreenPreview() {
    ProductivityOSTheme {
        AnalyticsScreen()
    }
}