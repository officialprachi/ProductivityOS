package com.productivityos.app.presentation.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import com.productivityos.app.domain.model.AppSettings
import com.productivityos.app.domain.model.FontSize
import com.productivityos.app.presentation.ui.components.*
import com.productivityos.app.presentation.ui.theme.*
import com.productivityos.app.presentation.viewmodel.*

// ═══════════════════════════════════════════════════════════════
// ENTRY
// ═══════════════════════════════════════════════════════════════

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
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
        Text(
            text = "SETTINGS",
            style = MaterialTheme.typography.labelSmall.copy(
                color = TextTertiary,
                letterSpacing = 1.8.sp
            )
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium.copy(
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(Modifier.height(20.dp))

        when (val state = uiState) {
            is SettingsUiState.Loading -> {
                Box(
                    Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BrandBlue, strokeWidth = 2.dp)
                }
            }
            is SettingsUiState.Success -> {
                SettingsContent(
                    settings           = state.settings,
                    onToggleDark       = { viewModel.toggleDarkTheme() },
                    onToggleLiveTracker= { viewModel.toggleLiveTracker() },
                    onToggleHaptic     = { viewModel.toggleHapticFeedback() },
                    onSetFontSize      = { viewModel.setFontSize(it) }
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// CONTENT
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SettingsContent(
    settings: AppSettings,
    onToggleDark: () -> Unit,
    onToggleLiveTracker: () -> Unit,
    onToggleHaptic: () -> Unit,
    onSetFontSize: (FontSize) -> Unit
) {
    // ── APPEARANCE ────────────────────────────────────────────
    SectionHeader(text = stringResource(R.string.appearance))
    Spacer(Modifier.height(8.dp))

    SettingsGroup {
        // Theme toggle row
        SettingRow(
            icon = if (settings.isDarkTheme) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
            iconTint = AccentCyan,
            title = stringResource(R.string.theme),
            subtitle = if (settings.isDarkTheme)
                stringResource(R.string.dark_mode_active)
            else
                stringResource(R.string.light_mode_active),
            action = {
                ThemeToggle(isDark = settings.isDarkTheme, onToggle = onToggleDark)
            }
        )
        SettingsDivider()
        // Font size row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BrandBlueAlpha12),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.TextFields,
                        contentDescription = null,
                        tint = BrandBlue,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.font_size),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            FontSizeChips(selected = settings.fontSize, onSelect = onSetFontSize)
        }
    }
    Spacer(Modifier.height(20.dp))

    // ── AI & PRIVACY ──────────────────────────────────────────
    SectionHeader(text = stringResource(R.string.ai_privacy))
    Spacer(Modifier.height(8.dp))

    SettingsGroup {
        SettingsDivider()
        SettingsDivider()
        SettingToggleRow(
            icon     = Icons.Rounded.Radar,
            iconTint = AccentCyan,
            title    = stringResource(R.string.live_tracker),
            subtitle = stringResource(R.string.live_tracker_sub),
            checked  = settings.liveTrackerEnabled,
            onToggle = onToggleLiveTracker
        )
        SettingsDivider()
        SettingToggleRow(
            icon     = Icons.Rounded.Vibration,
            iconTint = TextSecondary,
            title    = stringResource(R.string.haptic_feedback),
            subtitle = null,
            checked  = settings.hapticFeedback,
            onToggle = onToggleHaptic
        )
    }
    Spacer(Modifier.height(20.dp))

    // ── DATA POLICY ───────────────────────────────────────────
    SectionHeader(text = stringResource(R.string.data_policy))
    Spacer(Modifier.height(8.dp))
    DataPolicyBox()
    Spacer(Modifier.height(12.dp))

    OutlineButton(text = stringResource(R.string.export_data), onClick = {})
    Spacer(Modifier.height(8.dp))
    OutlineButton(text = stringResource(R.string.reset_ai), onClick = {})
    Spacer(Modifier.height(8.dp))

    // Danger action — red outline
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Danger.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .clickable {}
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.clear_data).uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(
                color = Danger,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// SETTINGS GROUP (card container)
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp)),
        content = content
    )
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = SurfaceBorder,
        thickness = 1.dp,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

// ═══════════════════════════════════════════════════════════════
// SETTING ROW (generic — custom action slot)
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SettingRow(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String?,
    action: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = TextPrimary, fontWeight = FontWeight.Medium
                )
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }
        }
        action()
    }
}

// ═══════════════════════════════════════════════════════════════
// SETTING TOGGLE ROW
// ═══════════════════════════════════════════════════════════════

@Composable
private fun SettingToggleRow(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String?,
    checked: Boolean,
    onToggle: () -> Unit
) {
    SettingRow(
        icon     = icon,
        iconTint = iconTint,
        title    = title,
        subtitle = subtitle,
        action   = { PosToggle(checked = checked, onCheckedChange = { onToggle() }) }
    )
}

// ═══════════════════════════════════════════════════════════════
// THEME TOGGLE — pill style
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ThemeToggle(isDark: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(CardBgHigh)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        listOf("🌙" to true, "☀️" to false).forEach { (emoji, dark) ->
            val isActive = isDark == dark
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isActive) BrandBlueAlpha20 else androidx.compose.ui.graphics.Color.Transparent)
                    .then(
                        if (isActive) Modifier.border(1.dp, BrandBlue.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        else Modifier
                    )
                    .clickable { if (!isActive) onToggle() }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(text = emoji, fontSize = 14.sp)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// FONT SIZE CHIPS
// ═══════════════════════════════════════════════════════════════

@Composable
private fun FontSizeChips(selected: FontSize, onSelect: (FontSize) -> Unit) {
    val options = listOf(
        FontSize.SMALL  to Pair("A", "SM"),
        FontSize.MEDIUM to Pair("A", "MD"),
        FontSize.LARGE  to Pair("A", "LG"),
        FontSize.XLARGE to Pair("A", "XL")
    )
    val fontSizes = listOf(11.sp, 14.sp, 17.sp, 20.sp)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEachIndexed { index, (size, labels) ->
            val isSelected = selected == size
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) BrandBlueAlpha20 else SurfaceHigh)
                    .border(
                        1.dp,
                        if (isSelected) BrandBlue.copy(alpha = 0.4f) else SurfaceBorder,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelect(size) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = labels.first,
                    fontSize = fontSizes[index],
                    color = if (isSelected) BrandBlue else TextTertiary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = labels.second,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = if (isSelected) BrandBlue else TextTertiary,
                        fontSize = 7.sp
                    )
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// DATA POLICY BOX
// ═══════════════════════════════════════════════════════════════

@Composable
private fun DataPolicyBox() {
    val policies = listOf(
        "No keystroke tracking",
        "No content access",
        "No cloud sync by default",
        "No advertising"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BrandGreenAlpha12)
            .border(1.dp, BrandGreen.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        policies.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                row.forEach { policy ->
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(RoundedCornerShape(99.dp))
                                .background(BrandGreenAlpha20),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✓", color = BrandGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = policy,
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PREVIEW
// ═══════════════════════════════════════════════════════════════

@Preview(showBackground = true, backgroundColor = 0xFF0A0F14)
@Composable
fun SettingsScreenPreview() {
    ProductivityOSTheme {
        SettingsScreen()
    }
}