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
                
                NavHost(navController = navController, startDestination = "dashboard") {
                    
                    composable("dashboard") {
                        val viewModel: HomeViewModel = hiltViewModel()
                        
                        DashboardScreen(
                            viewModel = viewModel,
                            onCollectionClick = { collectionId ->
                                navController.navigate("viewer/$collectionId")
                            },
                            onSettingsClick = {
                                navController.navigate("settings")
                            },
                            onTrashClick = {
                                navController.navigate("trash")
                            }
                        )
                    }
                    
                    composable(
                        route = "viewer/{collectionId}",
                        arguments = listOf(navArgument("collectionId") { type = NavType.StringType })
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