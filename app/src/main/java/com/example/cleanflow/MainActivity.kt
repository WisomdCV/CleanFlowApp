package com.example.cleanflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
                    // For older androids, we assume onboarding handles it or check specific permissions
                    // For simplicity in this demo, strict check:
                    false 
                }
                
                NavHost(navController = navController, startDestination = if (hasPermission) "dashboard" else "onboarding") {
                    
                    composable("onboarding") {
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

                    composable("dashboard") {
                        val viewModel: HomeViewModel = hiltViewModel()
                        
                        DashboardScreen(
                            viewModel = viewModel,
                            onCollectionClick = { collectionId ->
                                // Navigate to Gallery instead of Viewer
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
                    
                    // New: Gallery screen (Grid view)
                    composable(
                        route = "gallery/{collectionId}",
                        arguments = listOf(navArgument("collectionId") { type = NavType.StringType })
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
                    
                    // Updated: Viewer with optional initialIndex
                    composable(
                        route = "viewer/{collectionId}?initialIndex={initialIndex}",
                        arguments = listOf(
                            navArgument("collectionId") { type = NavType.StringType },
                            navArgument("initialIndex") { 
                                type = NavType.IntType
                                defaultValue = 0
                            }
                        )
                    ) {
                        val viewModel: ViewerViewModel = hiltViewModel()
                        
                        MediaViewerScreen(
                            viewModel = viewModel,
                            onBackClick = { navController.popBackStack() }
                        )
                    }

                    composable("settings") {
                        SettingsScreen(
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                    
                    composable("trash") {
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