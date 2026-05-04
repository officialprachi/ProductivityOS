package com.productivityos.app.presentation.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.productivityos.app.R
import com.productivityos.app.domain.model.UserProfile
import com.productivityos.app.presentation.ui.components.*
import com.productivityos.app.presentation.ui.theme.*
import com.productivityos.app.presentation.viewmodel.*

// ── Unchanged data ────────────────────────────────────────────
private val FOCUS_OPTIONS = listOf(
    "🌅" to "Morning",
    "☀️" to "Midday",
    "🌆" to "Afternoon",
    "🌙" to "Evening",
    "🦉" to "Night Owl"
)

private val WORK_TYPES_OPTIONS = listOf(
    "💻" to "Developer",
    "🎨" to "Designer",
    "📊" to "Analyst",
    "✍️" to "Writer",
    "📋" to "Manager",
    "🔬" to "Researcher",
    "🎓" to "Student",
    "🚀" to "Founder"
)

@Composable
fun ProfileScreen(
    onNavigateSettings: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // ── Unchanged state ───────────────────────────────────────
    var showFocusEditor by remember { mutableStateOf(false) }
    var showWorkEditor  by remember { mutableStateOf(false) }

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
                    text = "MY PROFILE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextTertiary, letterSpacing = 1.8.sp
                    )
                )
                Text(
                    text = stringResource(R.string.profile_title),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        color = TextPrimary, fontWeight = FontWeight.Bold
                    )
                )
            }
            // Edit toggle — tapping cycles: none → focus → work → none
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .border(1.dp, BrandBlue.copy(alpha = 0.4f), RoundedCornerShape(99.dp))
                    .clickable {
                        when {
                            !showFocusEditor && !showWorkEditor -> showFocusEditor = true
                            showFocusEditor -> { showFocusEditor = false; showWorkEditor = true }
                            else -> showWorkEditor = false
                        }
                    }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = when {
                        showFocusEditor -> "EDITING FOCUS ›"
                        showWorkEditor  -> "EDITING WORK ›"
                        else            -> stringResource(R.string.edit)
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = BrandBlue, fontWeight = FontWeight.Bold
                    )
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        when (val state = uiState) {
            is ProfileUiState.Loading -> {
                Box(
                    Modifier.fillMaxWidth().height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = BrandBlue, strokeWidth = 2.dp)
                }
            }
            is ProfileUiState.Error -> {
                Text(text = state.message, style = MaterialTheme.typography.bodySmall.copy(color = Danger))
            }
            is ProfileUiState.Success -> {
                ProfileContent(
                    profile            = state.profile,
                    showFocusEditor    = showFocusEditor,
                    onFocusSaved       = { showFocusEditor = false },
                    showWorkEditor     = showWorkEditor,
                    onWorkSaved        = { showWorkEditor = false },
                    onNavigateSettings = onNavigateSettings,
                    viewModel          = viewModel
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// PROFILE CONTENT — same structure, new tokens
// ═══════════════════════════════════════════════════════════════

@Composable
private fun ProfileContent(
    profile: UserProfile,
    showFocusEditor: Boolean,
    onFocusSaved: () -> Unit,
    showWorkEditor: Boolean,
    onWorkSaved: () -> Unit,
    onNavigateSettings: () -> Unit,
    viewModel: ProfileViewModel
) {
    // ── Unchanged state ───────────────────────────────────────
    var pendingFocusPref by remember(profile.focusPreference) {
        mutableStateOf(profile.focusPreference)
    }

    // ── Profile hero card ─────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
    ) {
        // Gradient strip top
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.TopCenter)
                .background(GradientPrimary)
        )
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Avatar
            Box(modifier = Modifier.size(58.dp)) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(BrandBlueAlpha20)
                        .border(1.5.dp, BrandBlue.copy(alpha = 0.3f), RoundedCornerShape(18.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profile.name.take(1).uppercase(),
                        style = MaterialTheme.typography.displayMedium.copy(
                            color = BrandBlue, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp
                        )
                    )
                }
                // Verified dot
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(BrandGreen)
                        .border(2.dp, CardBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = White,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.displayMedium.copy(
                        color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp
                    )
                )
                Text(
                    text = profile.email,
                    style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary)
                )
                Spacer(Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(BrandBlueAlpha12)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "🧠 ${profile.workerType}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = BrandBlue, fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(10.dp))

    // ── Stat grid ─────────────────────────────────────────────
    StatGrid(profile = profile)
    Spacer(Modifier.height(10.dp))

    // ── Focus editor (unchanged logic) ────────────────────────
    AnimatedVisibility(
        visible = showFocusEditor,
        enter   = fadeIn() + expandVertically(),
        exit    = fadeOut() + shrinkVertically()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBg)
                .border(1.dp, BrandBlue.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Peak Focus Time",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = TextPrimary, fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        "When do you do your best work?",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }
                // Save button — same onClick
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(GradientPrimary)
                        .clickable {
                            viewModel.updateFocusPreference(pendingFocusPref)
                            onFocusSaved()
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        "Save",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = White, fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            // Chip picker — same logic
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FOCUS_OPTIONS.chunked(2).forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        row.forEach { (emoji, label) ->
                            val isSelected = pendingFocusPref == label
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) BrandBlueAlpha20 else CardBgHigh)
                                    .border(
                                        1.dp,
                                        if (isSelected) BrandBlue.copy(alpha = 0.4f) else CardBorder,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { pendingFocusPref = label }
                                    .padding(horizontal = 12.dp, vertical = 11.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(emoji, fontSize = 15.sp)
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = if (isSelected) BrandBlue else TextSecondary,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
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
        Spacer(Modifier.height(10.dp))
    }

    // ── Work category editor ──────────────────────────────────
    var pendingWorkCategory by remember(profile.workCategory) { mutableStateOf(profile.workCategory) }

    AnimatedVisibility(
        visible = showWorkEditor,
        enter   = fadeIn() + expandVertically(),
        exit    = fadeOut() + shrinkVertically()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBg)
                .border(1.dp, BrandGreen.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Work Type",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = TextPrimary, fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        "What best describes your role?",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(99.dp))
                        .background(GradientPrimary)
                        .clickable {
                            viewModel.updateWorkCategory(pendingWorkCategory)
                            onWorkSaved()
                        }
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                ) {
                    Text(
                        "Save",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = White, fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                WORK_TYPES_OPTIONS.chunked(2).forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        row.forEach { (emoji, label) ->
                            val isSelected = pendingWorkCategory == label
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) BrandGreenAlpha12 else CardBgHigh)
                                    .border(
                                        1.dp,
                                        if (isSelected) BrandGreen.copy(alpha = 0.4f) else CardBorder,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { pendingWorkCategory = label }
                                    .padding(horizontal = 12.dp, vertical = 11.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(emoji, fontSize = 15.sp)
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = if (isSelected) BrandGreen else TextSecondary,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
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
        Spacer(Modifier.height(10.dp))
    }

    // ── Devices ───────────────────────────────────────────────
    SectionHeader(text = stringResource(R.string.my_devices))
    Spacer(Modifier.height(8.dp))
    DeviceRow(deviceName = profile.primaryDevice)
    Spacer(Modifier.height(16.dp))

    // ── Account menu ──────────────────────────────────────────
    SectionHeader(text = stringResource(R.string.account))
    Spacer(Modifier.height(8.dp))
    MenuList(onNavigateSettings = onNavigateSettings)
}

// ═══════════════════════════════════════════════════════════════
// STAT GRID
// ═══════════════════════════════════════════════════════════════

@Composable
private fun StatGrid(profile: UserProfile) {
    // Same data, new accent colors
    val stats = listOf(
        Triple(stringResource(R.string.avg_score),  "${profile.avgScore} / 100",    BrandBlue),
        Triple(stringResource(R.string.best_streak),"${profile.bestStreak} days 🔥", BrandGreen),
        Triple(stringResource(R.string.work_type),   profile.workCategory,           TextPrimary),
        Triple(stringResource(R.string.focus_pref),  profile.focusPreference,        AccentCyan)
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        stats.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { (label, value, color) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(CardBg)
                            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            label.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary)
                        )
                        Text(
                            value,
                            style = MaterialTheme.typography.headlineSmall.copy(
                                color = color, fontWeight = FontWeight.Bold, fontSize = 14.sp
                            )
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// DEVICE ROW
// ═══════════════════════════════════════════════════════════════

@Composable
private fun DeviceRow(deviceName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(BrandBlueAlpha12),
                contentAlignment = Alignment.Center
            ) {
                Text("📱", fontSize = 16.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    deviceName,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = TextPrimary, fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    "Primary · Android",
                    style = MaterialTheme.typography.labelSmall.copy(color = TextTertiary)
                )
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(99.dp))
                .background(BrandGreenAlpha12)
                .padding(horizontal = 9.dp, vertical = 3.dp)
        ) {
            Text(
                "ACTIVE",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = BrandGreen, fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// MENU LIST
// ═══════════════════════════════════════════════════════════════

@Composable
private fun MenuList(onNavigateSettings: () -> Unit) {
    var showPremiumDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    val items = listOf(
        Triple("🚀", stringResource(R.string.go_premium),     { showPremiumDialog = true }),
        Triple("⚙️", stringResource(R.string.settings_menu), onNavigateSettings),
        Triple("🔒", stringResource(R.string.privacy_data),  { showPrivacyDialog = true })
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
    ) {
        items.forEachIndexed { index, (emoji, label, action) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { action() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CardBgHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = 15.sp)
                }
                Text(
                    label,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = TextPrimary, fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "›",
                    style = MaterialTheme.typography.headlineMedium.copy(color = TextTertiary)
                )
            }
            if (index < items.size - 1) {
                HorizontalDivider(
                    color = CardBorder,
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }

    // ── Premium Dialog ────────────────────────────────────────
    if (showPremiumDialog) {
        AlertDialog(
            onDismissRequest = { showPremiumDialog = false },
            containerColor   = CardBg,
            shape            = RoundedCornerShape(20.dp),
            title = {
                Text(
                    "Upgrade to Pro 🚀",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = TextPrimary, fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Price hero
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(BrandBlueAlpha12)
                            .border(1.dp, BrandBlue.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "₹299 / month",
                                style = MaterialTheme.typography.displayMedium.copy(
                                    brush = GradientPrimary,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            )
                            Text(
                                "or ₹2,499 / year  •  Save 30%",
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                            )
                        }
                    }
                    // Feature list
                    val features = listOf(
                        "♾️  Unlimited AI schedule rebuilds",
                        "📊  Advanced analytics & trends",
                        "🔒  Smart app blocking profiles",
                        "🧠  Deep AI insights & coaching",
                        "🎯  Custom focus goals"
                    )
                    features.forEach { feature ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(BrandGreen)
                            )
                            Text(
                                feature,
                                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(GradientPrimary)
                        .clickable { showPremiumDialog = false }
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "START FREE TRIAL",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = White, fontWeight = FontWeight.Bold, letterSpacing = 1.sp
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showPremiumDialog = false }) {
                    Text(
                        "Maybe later",
                        style = MaterialTheme.typography.labelMedium.copy(color = TextTertiary)
                    )
                }
            }
        )
    }

    // ── Privacy Dialog ────────────────────────────────────────
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            containerColor   = CardBg,
            shape            = RoundedCornerShape(20.dp),
            title = {
                Text(
                    "Privacy & Data 🔒",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = TextPrimary, fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    val policies = listOf(
                        Triple("🚫", "No Keystroke Tracking",
                            "We never record what you type — only which apps are open and for how long."),
                        Triple("📵", "No Content Access",
                            "ProductivityOS cannot read your messages, emails, or files."),
                        Triple("☁️", "No Cloud Sync by Default",
                            "All data stays on-device unless you explicitly enable backup."),
                        Triple("📢", "No Advertising",
                            "We don't sell your data or show ads. Ever.")
                    )
                    policies.forEach { (emoji, title, desc) ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(BrandGreenAlpha12),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 16.sp)
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = TextPrimary, fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Text(
                                    desc,
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text(
                        "Got it",
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = BrandBlue, fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0F14)
@Composable
fun ProfileScreenPreview() {
    ProductivityOSTheme { ProfileScreen() }
}