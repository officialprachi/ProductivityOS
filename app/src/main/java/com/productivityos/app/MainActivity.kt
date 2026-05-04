package com.productivityos.app

import android.app.AppOpsManager
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.productivityos.app.data.local.AppUsageService
import com.productivityos.app.data.local.DailyScoreWorker
import com.productivityos.app.data.local.FocusBlockerService
import com.productivityos.app.domain.model.AppSettings
import com.productivityos.app.domain.repository.SettingsRepository
import com.productivityos.app.domain.usecase.SeedDemoDataUseCase
import com.productivityos.app.presentation.ui.*
import com.productivityos.app.presentation.ui.screens.LoginScreen
import com.productivityos.app.presentation.ui.theme.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var firebaseAuth: FirebaseAuth
    @Inject lateinit var seedDemoData: SeedDemoDataUseCase
    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (hasUsageStatsPermission()) {
            AppUsageService.start(this)
            CoroutineScope(Dispatchers.IO).launch {
                val settings = settingsRepository.getSettings().first()
                if (settings.appUsageLimits.isNotEmpty()) {
                    FocusBlockerService.startLimitEnforcement(this@MainActivity)
                }
            }
        } else {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        // Request overlay permission for FocusBlockerService overlay window
        if (!Settings.canDrawOverlays(this)) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    "package:$packageName".toUri()
                )
            )
        }

        DailyScoreWorker.schedule(this)

        setContent {
            val settings by settingsRepository.getSettings()
                .collectAsStateWithLifecycle(initialValue = AppSettings())

            ProductivityOSTheme(
                darkTheme = settings.isDarkTheme,
                fontSize  = settings.fontSize
            ) {
                var isLoggedIn by remember { mutableStateOf(firebaseAuth.currentUser != null) }
                var lastSeededUid by remember { mutableStateOf<String?>(null) }

                DisposableEffect(Unit) {
                    val listener = FirebaseAuth.AuthStateListener { auth ->
                        val user: FirebaseUser? = auth.currentUser
                        isLoggedIn = user != null
                        if (user != null && user.uid != lastSeededUid) {
                            lastSeededUid = user.uid
                            val name  = user.displayName?.takeIf { it.isNotBlank() }
                                ?: user.email?.substringBefore("@") ?: "User"
                            val email = user.email ?: ""
                            CoroutineScope(Dispatchers.IO).launch {
                                seedDemoData(userName = name, userEmail = email)
                            }
                            if (hasUsageStatsPermission()) AppUsageService.start(this@MainActivity)
                        }
                    }
                    firebaseAuth.addAuthStateListener(listener)
                    onDispose { firebaseAuth.removeAuthStateListener(listener) }
                }

                if (!isLoggedIn) {
                    LoginScreen(onAuthSuccess = { isLoggedIn = true })
                } else {
                    val navController = rememberNavController()
                    Scaffold(
                        modifier = Modifier.fillMaxSize().background(Ink),
                        containerColor = Ink,
                        bottomBar = { ProductivityBottomBar(navController = navController) }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .background(Ink)
                        ) {
                            ProductivityNavHost(navController = navController)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (hasUsageStatsPermission()) AppUsageService.start(this)
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(APP_OPS_SERVICE) as AppOpsManager
        @Suppress("DEPRECATION")
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}