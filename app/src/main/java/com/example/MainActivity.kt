package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.editor.AskAIScreen
import com.example.ui.screens.AppSplashScreen
import com.example.ui.screens.FocusHubScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NoteEditorScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SubjectDetailScreen
import com.example.ui.test.TestHubScreen
import com.example.ui.theme.PrepOSTheme
import com.example.util.PrepOSFocusNotificationManager
import com.example.viewmodel.PrepOSViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: PrepOSViewModel by viewModels()
    private var pendingIntentState by mutableStateOf<Intent?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingIntentState = intent
        setContent {
            val preferences by viewModel.preferences.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val appThemeMode = preferences?.appThemeMode ?: "SYSTEM"
            val isDark = when (appThemeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> systemDark
            }

            PrepOSTheme(darkTheme = isDark) {
                // Request Notification Permission on Android 13+ (API 33+)
                NotificationPermissionHandler()

                PrepOSApp(
                    viewModel = viewModel,
                    initialIntent = pendingIntentState,
                    onIntentConsumed = { pendingIntentState = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingIntentState = intent
    }
}

@Composable
private fun NotificationPermissionHandler() {
    val context = LocalContext.current
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { /* Granted or dismissed */ }

        LaunchedEffect(Unit) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

@Composable
fun PrepOSApp(
    viewModel: PrepOSViewModel,
    initialIntent: Intent? = null,
    onIntentConsumed: () -> Unit = {}
) {
    val navController = rememberNavController()

    // Continuously track active app usage / study time while user is inside the application
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000L)
            viewModel.recordActiveStudySeconds(1)
        }
    }

    // Smooth, instant tab switching helper without accumulating backstack history
    val navigateToTab: (String) -> Unit = { tab ->
        if (tab == "home" || tab == "study") {
            viewModel.setCurrentHomeTab(tab)
        }
        val targetRoute = when (tab) {
            "home" -> "home?tab=home"
            "study" -> "home?tab=study"
            "ask_ai" -> "ask_ai"
            "plan" -> "focus_hub"
            "practice" -> "test_hub"
            else -> "home?tab=home"
        }
        val startDestinationId = navController.graph.findStartDestination().id
        navController.navigate(targetRoute) {
            popUpTo(startDestinationId) {
                saveState = (tab != "home" && tab != "study")
            }
            launchSingleTop = true
            restoreState = (tab != "home" && tab != "study")
        }
    }

    // Handle deep navigation from system push notifications & notification action buttons
    LaunchedEffect(initialIntent) {
        if (initialIntent != null) {
            val navigateTo = initialIntent.getStringExtra(PrepOSFocusNotificationManager.EXTRA_NAVIGATE_TO)
            val actionType = initialIntent.getStringExtra(PrepOSFocusNotificationManager.EXTRA_ACTION_TYPE)
            val actionPayload = initialIntent.getStringExtra(PrepOSFocusNotificationManager.EXTRA_ACTION_PAYLOAD)

            when {
                navigateTo == "plan" || actionType == "START_FOCUS" -> {
                    navigateToTab("plan")
                }
                navigateTo == "practice" || actionType == "TAKE_TEST" -> {
                    navigateToTab("practice")
                }
                navigateTo == "editor" && !actionPayload.isNullOrBlank() -> {
                    navController.navigate("editor/$actionPayload")
                }
                navigateTo == "ask_ai" -> {
                    navigateToTab("ask_ai")
                }
            }
            onIntentConsumed()
        }
    }

    var showSplashScreen by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.fillMaxSize(),
            enterTransition = { fadeIn(tween(140)) },
            exitTransition = { fadeOut(tween(140)) },
            popEnterTransition = { fadeIn(tween(140)) },
            popExitTransition = { fadeOut(tween(140)) }
        ) {
        composable(
            route = "home?tab={tab}",
            arguments = listOf(
                navArgument("tab") {
                    type = NavType.StringType
                    defaultValue = "home"
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val tab = backStackEntry.arguments?.getString("tab") ?: "home"
            HomeScreen(
                viewModel = viewModel,
                initialTab = tab,
                onOpenSubject = { subjectId, subjectName ->
                    val encodedName = Uri.encode(subjectName)
                    navController.navigate("subject/$subjectId/$encodedName")
                },
                onOpenChapter = { chapterId ->
                    navController.navigate("editor/$chapterId")
                },
                onOpenSettings = {
                    navController.navigate("settings")
                },
                onOpenFocusHub = {
                    navigateToTab("plan")
                },
                onOpenTest = { chapterId ->
                    navController.navigate("test_hub?chapterId=$chapterId")
                },
                onOpenTestHub = {
                    navigateToTab("practice")
                },
                onOpenAskAI = {
                    navigateToTab("ask_ai")
                }
            )
        }

        composable(
            route = "test_hub?chapterId={chapterId}",
            arguments = listOf(
                navArgument("chapterId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val chapterId = backStackEntry.arguments?.getString("chapterId")
            TestHubScreen(
                viewModel = viewModel,
                initialChapterId = chapterId,
                onNavigateTab = { tab ->
                    navigateToTab(tab)
                },
                onOpenAskAI = { prompt ->
                    navigateToTab("ask_ai")
                }
            )
        }

        composable("focus_hub") {
            FocusHubScreen(
                viewModel = viewModel,
                onBack = { navigateToTab("home") },
                onNavigateTab = { tab -> navigateToTab(tab) },
                onOpenAskAI = { navigateToTab("ask_ai") },
                onOpenTest = { chapterId ->
                    navController.navigate("test_hub?chapterId=$chapterId")
                }
            )
        }

        composable("ask_ai") {
            AskAIScreen(
                viewModel = viewModel,
                onDismiss = { navigateToTab("home") },
                onNavigateTab = { tab ->
                    navigateToTab(tab)
                }
            )
        }

        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "subject/{subjectId}/{subjectName}",
            arguments = listOf(
                navArgument("subjectId") { type = NavType.StringType },
                navArgument("subjectName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val subjectId = backStackEntry.arguments?.getString("subjectId") ?: ""
            val rawName = backStackEntry.arguments?.getString("subjectName") ?: "Subject"
            val subjectName = Uri.decode(rawName)

            SubjectDetailScreen(
                subjectId = subjectId,
                subjectName = subjectName,
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenChapter = { chapterId ->
                    navController.navigate("editor/$chapterId")
                },
                onOpenTest = { chapterId ->
                    navController.navigate("test_hub?chapterId=$chapterId")
                }
            )
        }

        composable(
            route = "editor/{chapterId}",
            arguments = listOf(
                navArgument("chapterId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val chapterId = backStackEntry.arguments?.getString("chapterId") ?: ""

            NoteEditorScreen(
                chapterId = chapterId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }

    // App Startup Splash Screen Overlay with smooth fade & scale out
    androidx.compose.animation.AnimatedVisibility(
        visible = showSplashScreen,
        enter = fadeIn(),
        exit = fadeOut(tween(350)) + scaleOut(targetScale = 1.05f, animationSpec = tween(350))
    ) {
        AppSplashScreen(
            onDismiss = { showSplashScreen = false }
        )
    }
}
}
