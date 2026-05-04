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
import com.productivityos.app.domain.model.*
import com.productivityos.app.presentation.ui.components.*
import com.productivityos.app.presentation.ui.theme.*
import com.productivityos.app.presentation.viewmodel.*

@Composable
fun InsightsScreen(viewModel: InsightsViewModel = hiltViewModel()) {
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "AI POWERED",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextTertiary, letterSpacing = 1.8.sp
                    )
                )
                Text(
                    text = stringResource(R.string.insights_title),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = TextPrimary, fontWeight = FontWeight.Bold
                    )
                )
            }
            LivePill()
        }
        Spacer(Modifier.height(20.dp))

        when (val state = uiState) {
            is InsightsUiState.Loading -> {
                Box(
                    Modifier.fillMaxWidth().height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BrandBlue, strokeWidth = 2.dp)
                }
            }
            is InsightsUiState.Error -> {
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodySmall.copy(color = Danger)
                )
            }
            is InsightsUiState.Success -> {
                SectionHeader(text = stringResource(R.string.ai_analysis))
                Spacer(Modifier.height(8.dp))

                if (state.insights.isEmpty()) {
                    EmptyInsightsState()
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.insights.forEach { insight ->
                            InsightCard(insight = insight)
                        }
                    }
                }
            }
        }
    }
}

// ── InsightCard — same data, new tokens ──────────────────────
@Composable
fun InsightCard(insight: Insight) {
    // Map impact → accent color (blue-green system)
    val accentColor = when (insight.impact) {
        InsightImpact.HIGH   -> BrandBlue
        InsightImpact.MEDIUM -> AccentCyan
        InsightImpact.LOW    -> TextSecondary
    }
    val bgColor = when (insight.impact) {
        InsightImpact.HIGH   -> BrandBlueAlpha12
        InsightImpact.MEDIUM -> AccentCyanAlpha15
        InsightImpact.LOW    -> SurfaceHigh
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
    ) {
        // Gradient accent strip on top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(accentColor, accentColor.copy(alpha = 0f))
                    )
                )
        )
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Emoji in tinted box
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(bgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = insight.emoji,
                            fontSize = MaterialTheme.typography.titleMedium.fontSize
                        )
                    }
                    Text(
                        text = insight.title,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
                ImpactBadge(impact = insight.impact)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = insight.description,
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
            )
        }
    }
}

@Composable
private fun EmptyInsightsState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("🧠", fontSize = 36.sp)
        Text(
            text = "No insights yet",
            style = MaterialTheme.typography.headlineSmall.copy(color = TextSecondary)
        )
        Text(
            text = "Use the app for a few days to generate AI insights",
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextTertiary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0F14)
@Composable
fun InsightsScreenPreview() {
    ProductivityOSTheme {
        InsightsScreen()
    }
}