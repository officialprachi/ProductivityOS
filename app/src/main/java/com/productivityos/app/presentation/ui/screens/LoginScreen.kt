package com.productivityos.app.presentation.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.productivityos.app.presentation.ui.theme.*
import com.productivityos.app.presentation.viewmodel.AuthUiState
import com.productivityos.app.presentation.viewmodel.LoginViewModel

// ── Unchanged data ────────────────────────────────────────────
private val WORK_TYPES = listOf(
    "💻" to "Developer",
    "🎨" to "Designer",
    "📊" to "Analyst",
    "✍️" to "Writer",
    "📋" to "Manager",
    "🔬" to "Researcher",
    "🎓" to "Student",
    "🚀" to "Founder"
)

private val FOCUS_PREFS = listOf(
    "🌅" to "Morning",
    "☀️" to "Midday",
    "🌆" to "Afternoon",
    "🌙" to "Evening",
    "🦉" to "Night Owl"
)

@Composable
fun LoginScreen(
    onAuthSuccess: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    // ── Unchanged state ───────────────────────────────────────
    var isSignUp        by remember { mutableStateOf(false) }
    var step            by remember { mutableStateOf(1) }
    var name            by remember { mutableStateOf("") }
    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedWorkType  by remember { mutableStateOf("") }
    var selectedFocusPref by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) onAuthSuccess()
    }

    val isLoading    = uiState is AuthUiState.Loading
    val errorMessage = (uiState as? AuthUiState.Error)?.message

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .imePadding()
    ) {
        // ── Decorative gradient blobs ─────────────────────────
        Box(
            modifier = Modifier
                .size(280.dp)
                .offset(x = 140.dp, y = (-80).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(listOf(BrandBlue.copy(alpha = 0.15f), Color.Transparent))
                )
        )
        Box(
            modifier = Modifier
                .size(220.dp)
                .offset(x = (-60).dp, y = 620.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(listOf(BrandGreen.copy(alpha = 0.10f), Color.Transparent))
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 72.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Brand logo ────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(GradientPrimary),
                contentAlignment = Alignment.Center
            ) {
                Text("⚡", fontSize = 28.sp)
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "ProductivityOS",
                style = MaterialTheme.typography.displayMedium.copy(
                    color = TextPrimary, fontWeight = FontWeight.ExtraBold
                )
            )
            Text(
                "Your personal focus operating system",
                style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondary)
            )
            Spacer(Modifier.height(32.dp))

            // ── Auth card ─────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(CardBg)
                    .border(1.dp, SurfaceBorder, RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                // ── Unchanged AnimatedContent transition ──────
                AnimatedContent(
                    targetState = isSignUp to step,
                    transitionSpec = {
                        (fadeIn(tween(220)) + slideInHorizontally { it / 4 })
                            .togetherWith(fadeOut(tween(150)) + slideOutHorizontally { -it / 4 })
                    },
                    label = "auth_step"
                ) { (signUp, currentStep) ->

                    if (!signUp || currentStep == 1) {
                        // ── STEP 1: Credentials ───────────────
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {

                            TabToggle(isSignUp = isSignUp, onToggle = { mode ->
                                isSignUp = mode; step = 1; viewModel.clearError()
                                name = ""; email = ""; password = ""
                                selectedWorkType = ""; selectedFocusPref = ""
                            })
                            Spacer(Modifier.height(22.dp))

                            // Name — sign up only — unchanged logic
                            AnimatedVisibility(
                                visible = signUp,
                                enter   = fadeIn() + expandVertically(),
                                exit    = fadeOut() + shrinkVertically()
                            ) {
                                Column {
                                    AuthField(
                                        value = name, onValueChange = { name = it },
                                        label = "Full Name", placeholder = "Ada Lovelace",
                                        icon = Icons.Default.Person,
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                                    )
                                    Spacer(Modifier.height(14.dp))
                                }
                            }

                            AuthField(
                                value = email, onValueChange = { email = it; viewModel.clearError() },
                                label = "Email", placeholder = "you@example.com",
                                icon = Icons.Default.Email,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                            )
                            Spacer(Modifier.height(14.dp))
                            AuthField(
                                value = password, onValueChange = { password = it; viewModel.clearError() },
                                label = "Password",
                                placeholder = if (signUp) "Min. 6 characters" else "••••••••",
                                icon = Icons.Default.Lock,
                                isPassword = true, passwordVisible = passwordVisible,
                                onTogglePassword = { passwordVisible = !passwordVisible },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    focusManager.clearFocus()
                                    if (!signUp) viewModel.signIn(email, password)
                                })
                            )

                            AnimatedVisibility(visible = errorMessage != null) {
                                errorMessage?.let { ErrorBanner(it) }
                            }
                            Spacer(Modifier.height(20.dp))

                            // CTA — gradient instead of flat Plum
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (!isLoading) GradientPrimary else Brush.linearGradient(listOf(SurfaceBorder, SurfaceBorder)))
                                    .clickable(enabled = !isLoading) {
                                        focusManager.clearFocus()
                                        if (signUp) {
                                            when {
                                                name.isBlank() || email.isBlank() || password.isBlank() ->
                                                    viewModel.setError("Please fill in all fields")
                                                password.length < 6 ->
                                                    viewModel.setError("Password must be at least 6 characters")
                                                else -> { viewModel.clearError(); step = 2 }
                                            }
                                        } else {
                                            viewModel.signIn(email, password)
                                        }
                                    }
                                    .height(54.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.5.dp, color = White
                                    )
                                } else {
                                    Text(
                                        if (signUp) "Next →" else "Sign In",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            color = White, fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }

                    } else {
                        // ── STEP 2: Work type + Focus pref ────
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(CardBgHigh)
                                        .clickable { step = 1 },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("←", style = MaterialTheme.typography.bodyLarge.copy(color = TextSecondary))
                                }
                                Column {
                                    Text(
                                        "Almost there!",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            color = TextPrimary, fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        "Tell us how you work",
                                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                                    )
                                }
                            }
                            Spacer(Modifier.height(22.dp))

                            // Step indicator dots — gradient for active
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f).height(3.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(BrandBlue)
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f).height(3.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(GradientPrimary)
                                )
                            }
                            Spacer(Modifier.height(22.dp))

                            SectionLabel(icon = "💼", title = "Work Type", subtitle = "What best describes your role?")
                            Spacer(Modifier.height(10.dp))
                            ChipGrid(items = WORK_TYPES, selected = selectedWorkType, onSelect = { selectedWorkType = it })
                            Spacer(Modifier.height(20.dp))

                            SectionLabel(icon = "⏰", title = "Peak Focus Time", subtitle = "When do you do your best work?")
                            Spacer(Modifier.height(10.dp))
                            ChipGrid(items = FOCUS_PREFS, selected = selectedFocusPref, onSelect = { selectedFocusPref = it })

                            AnimatedVisibility(visible = errorMessage != null) {
                                errorMessage?.let { ErrorBanner(it) }
                            }
                            Spacer(Modifier.height(22.dp))

                            // Create account CTA — same logic
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (!isLoading) GradientPrimary else Brush.linearGradient(listOf(SurfaceBorder, SurfaceBorder)))
                                    .clickable(enabled = !isLoading) {
                                        when {
                                            selectedWorkType.isBlank() ->
                                                viewModel.setError("Please select your work type")
                                            selectedFocusPref.isBlank() ->
                                                viewModel.setError("Please select your peak focus time")
                                            else ->
                                                viewModel.signUp(
                                                    email, password, name,
                                                    workType  = selectedWorkType,
                                                    focusPref = selectedFocusPref
                                                )
                                        }
                                    }
                                    .height(54.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp, color = White)
                                } else {
                                    Text(
                                        "Create Account ✓",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            color = White, fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "By continuing, you agree to our Terms of Service\nand Privacy Policy.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextTertiary, textAlign = TextAlign.Center
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════
// SUB-COMPOSABLES — same structure, new tokens only
// ═══════════════════════════════════════════════════════════════

@Composable
private fun TabToggle(isSignUp: Boolean, onToggle: (Boolean) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBgHigh)
            .padding(4.dp)
    ) {
        Row {
            listOf(false to "Sign In", true to "Create Account").forEach { (mode, label) ->
                val selected = isSignUp == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (selected) GradientPrimary
                            else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                        )
                        .clickable { onToggle(mode) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = if (selected) White else TextSecondary,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(icon: String, title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(BrandBlueAlpha12),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 17.sp)
        }
        Column {
            Text(title, style = MaterialTheme.typography.headlineSmall.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
            Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
        }
    }
}

@Composable
private fun ChipGrid(
    items: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(2).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                row.forEach { (emoji, label) ->
                    val isSelected = selected == label
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) BrandBlueAlpha20 else SurfaceHigh)
                            .border(
                                1.dp,
                                if (isSelected) BrandBlue.copy(alpha = 0.4f) else SurfaceBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onSelect(label) }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
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

@Composable
private fun ErrorBanner(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(DangerAlpha)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(Icons.Default.Warning, null, tint = Danger, modifier = Modifier.size(15.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodySmall.copy(color = Danger),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AuthField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePassword: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary, letterSpacing = 1.sp),
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = value, onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(placeholder, style = MaterialTheme.typography.bodyMedium.copy(color = TextTertiary))
            },
            leadingIcon  = { Icon(icon, null, tint = TextSecondary, modifier = Modifier.size(18.dp)) },
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = { onTogglePassword?.invoke() }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            null, tint = TextSecondary, modifier = Modifier.size(18.dp)
                        )
                    }
                }
            } else null,
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor      = TextPrimary,
                unfocusedTextColor    = TextPrimary,
                focusedBorderColor    = BrandBlue,
                unfocusedBorderColor  = SurfaceBorder,
                cursorColor           = BrandBlue,
                focusedContainerColor = SurfaceHigh,
                unfocusedContainerColor = SurfaceHigh,
                focusedLeadingIconColor   = BrandBlue,
                unfocusedLeadingIconColor = TextSecondary
            ),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary)
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0F14)
@Composable
private fun LoginScreenPreview() {
    ProductivityOSTheme { LoginScreen(onAuthSuccess = {}) }
}