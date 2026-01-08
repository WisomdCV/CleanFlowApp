package com.example.cleanflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
                
                NavHost(
                    navController = navController, 
                    startDestination = if (hasPermission) "dashboard" else "onboarding"
                ) {
                    
                    // Onboarding - Fade transition
                    composable(
                        route = "onboarding",
                        enterTransition = { fadeIn(tween(500)) },
                        exitTransition = { fadeOut(tween(300)) }
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

                    // Dashboard - Fade in, slide out
                    composable(
                        route = "dashboard",
                        enterTransition = { fadeIn(tween(300)) },
                        exitTransition = { slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(200)) },
                        popEnterTransition = { slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(200)) },
                        popExitTransition = { fadeOut(tween(300)) }
                    ) {
                        val viewModel: HomeViewModel = hiltViewModel()
                        
                        DashboardScreen(
                            viewModel = viewModel,
                            onCollectionClick = { collectionId ->
                                navController.navigate("gallery/$collectionId")
                            },
                            onSettingsClick = {
                                navController.navigate("settings")
                            },
                            onTrashClick = {
                                navController.navigate("trash")
                            }
                        )
                    }
                    
                    // Gallery - Slide horizontal
                    composable(
                        route = "gallery/{collectionId}",
                        arguments = listOf(navArgument("collectionId") { type = NavType.StringType }),
                        enterTransition = { slideInHorizontally(tween(300)) { it } },
                        exitTransition = { slideOutHorizontally(tween(300)) { -it / 4 } + fadeOut(tween(200)) },
                        popEnterTransition = { slideInHorizontally(tween(300)) { -it / 4 } + fadeIn(tween(200)) },
                        popExitTransition = { slideOutHorizontally(tween(300)) { it } }
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
                    
                    // Viewer - Fade + Scale (premium feel)
                    composable(
                        route = "viewer/{collectionId}?initialIndex={initialIndex}",
                        arguments = listOf(
                            navArgument("collectionId") { type = NavType.StringType },
                            navArgument("initialIndex") { 
                                type = NavType.IntType
                                defaultValue = 0
                            }
                        ),
                        enterTransition = { fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.92f) },
                        exitTransition = { fadeOut(tween(200)) },
                        popEnterTransition = { fadeIn(tween(200)) },
                        popExitTransition = { fadeOut(tween(250)) + scaleOut(tween(250), targetScale = 0.92f) }
                    ) {
                        val viewModel: ViewerViewModel = hiltViewModel()
                        
                        MediaViewerScreen(
                            viewModel = viewModel,
                            onBackClick = { navController.popBackStack() }
                        )
                    }

                    // Settings - Slide from bottom
                    composable(
                        route = "settings",
                        enterTransition = { slideInVertically(tween(350)) { it } },
                        exitTransition = { fadeOut(tween(200)) },
                        popEnterTransition = { fadeIn(tween(200)) },
                        popExitTransition = { slideOutVertically(tween(300)) { it } }
                    ) {
                        SettingsScreen(
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    
                    // Trash - Slide from bottom
                    composable(
                        route = "trash",
                        enterTransition = { slideInVertically(tween(350)) { it } },
                        exitTransition = { fadeOut(tween(200)) },
                        popEnterTransition = { fadeIn(tween(200)) },
                        popExitTransition = { slideOutVertically(tween(300)) { it } }
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