package com.example.cleanflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.cleanflow.ui.screens.gallery.GalleryScreen
import com.example.cleanflow.ui.screens.gallery.GalleryViewModel
import com.example.cleanflow.ui.screens.home.DashboardScreen
import com.example.cleanflow.ui.screens.home.HomeViewModel
import com.example.cleanflow.ui.screens.settings.SettingsScreen
import com.example.cleanflow.ui.screens.trash.TrashScreen
import com.example.cleanflow.ui.screens.trash.TrashViewModel
import com.example.cleanflow.ui.screens.viewer.MediaViewerScreen
import com.example.cleanflow.ui.screens.viewer.ViewerViewModel
import com.example.cleanflow.ui.theme.CleanFlowTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            CleanFlowTheme {
                val navController = rememberNavController()
                
                // Determine start destination
                val hasPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    android.os.Environment.isExternalStorageManager()
                } else {
                    false 
                }
                
                // Optimized animation specs
                val fastTween = tween<Float>(220, easing = FastOutSlowInEasing)
                val mediumTween = tween<Float>(180, easing = FastOutSlowInEasing)
                
                NavHost(
                    navController = navController, 
                    startDestination = if (hasPermission) "dashboard" else "onboarding"
                ) {
                    
                    // Onboarding - Fade transition
                    composable(
                        route = "onboarding",
                        enterTransition = { fadeIn(tween(400)) },
                        exitTransition = { fadeOut(tween(200)) }
                    ) {
                        val viewModel: com.example.cleanflow.ui.screens.onboarding.OnboardingViewModel = hiltViewModel()
                        com.example.cleanflow.ui.screens.onboarding.OnboardingScreen(
                            viewModel = viewModel,
                            onPermissionGranted = {
                                navController.navigate("dashboard") {
                                    popUpTo("onboarding") { inclusive = true }
                                }
                            }
                        )
                    }

                    // Dashboard - Clean slide transitions
                    composable(
                        route = "dashboard",
                        enterTransition = { fadeIn(tween(200, easing = FastOutSlowInEasing)) },
                        exitTransition = { slideOutHorizontally(tween(220, easing = FastOutSlowInEasing)) { -it / 3 } },
                        popEnterTransition = { slideInHorizontally(tween(220, easing = FastOutSlowInEasing)) { -it / 3 } },
                        popExitTransition = { fadeOut(tween(180)) }
                    ) {
                        val viewModel: HomeViewModel = hiltViewModel()
                        
                        DashboardScreen(
                            viewModel = viewModel,
                            onCollectionClick = { collectionId ->
                                navController.navigate("gallery/$collectionId")
                            },
                            onSmartFilterClick = { filterType ->
                                navController.navigate("gallery/_smart?filterType=$filterType")
                            },
                            onSettingsClick = {
                                navController.navigate("settings")
                            },
                            onTrashClick = {
                                navController.navigate("trash")
                            }
                        )
                    }
                    
                    // Gallery - Fast horizontal slide (supports both collection and filter modes)
                    composable(
                        route = "gallery/{collectionId}?filterType={filterType}",
                        arguments = listOf(
                            navArgument("collectionId") { type = NavType.StringType },
                            navArgument("filterType") { 
                                type = NavType.StringType
                                defaultValue = "ALL"
                            }
                        ),
                        enterTransition = { slideInHorizontally(tween(220, easing = FastOutSlowInEasing)) { it } },
                        exitTransition = { slideOutHorizontally(tween(200, easing = FastOutSlowInEasing)) { -it / 4 } },
                        popEnterTransition = { slideInHorizontally(tween(200, easing = FastOutSlowInEasing)) { -it / 4 } },
                        popExitTransition = { slideOutHorizontally(tween(220, easing = FastOutSlowInEasing)) { it } }
                    ) {
                        val viewModel: GalleryViewModel = hiltViewModel()
                        
                        GalleryScreen(
                            viewModel = viewModel,
                            onFileClick = { index ->
                                val collectionId = it.arguments?.getString("collectionId") ?: ""
                                navController.navigate("viewer/$collectionId?initialIndex=$index")
                            },
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    
                    // Viewer - Quick fade + subtle scale (premium feel)
                    composable(
                        route = "viewer/{collectionId}?initialIndex={initialIndex}",
                        arguments = listOf(
                            navArgument("collectionId") { type = NavType.StringType },
                            navArgument("initialIndex") { 
                                type = NavType.IntType
                                defaultValue = 0
                            }
                        ),
                        enterTransition = { fadeIn(tween(200)) + scaleIn(tween(200), initialScale = 0.95f) },
                        exitTransition = { fadeOut(tween(150)) },
                        popEnterTransition = { fadeIn(tween(150)) },
                        popExitTransition = { fadeOut(tween(180)) + scaleOut(tween(180), targetScale = 0.95f) }
                    ) {
                        val viewModel: ViewerViewModel = hiltViewModel()
                        
                        MediaViewerScreen(
                            viewModel = viewModel,
                            onBackClick = { navController.popBackStack() }
                        )
                    }

                    // Settings - Fast vertical slide
                    composable(
                        route = "settings",
                        enterTransition = { slideInVertically(tween(250, easing = FastOutSlowInEasing)) { it } },
                        exitTransition = { fadeOut(tween(150)) },
                        popEnterTransition = { fadeIn(tween(150)) },
                        popExitTransition = { slideOutVertically(tween(220, easing = FastOutSlowInEasing)) { it } }
                    ) {
                        SettingsScreen(
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    
                    // Trash - Fast vertical slide
                    composable(
                        route = "trash",
                        enterTransition = { slideInVertically(tween(250, easing = FastOutSlowInEasing)) { it } },
                        exitTransition = { fadeOut(tween(150)) },
                        popEnterTransition = { fadeIn(tween(150)) },
                        popExitTransition = { slideOutVertically(tween(220, easing = FastOutSlowInEasing)) { it } }
                    ) {
                        val viewModel: TrashViewModel = hiltViewModel()
                        
                        TrashScreen(
                            viewModel = viewModel,
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}