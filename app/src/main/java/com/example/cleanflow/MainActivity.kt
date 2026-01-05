package com.example.cleanflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.cleanflow.data.repository.MediaRepositoryImpl
import com.example.cleanflow.data.repository.MediaStoreDataSource
import com.example.cleanflow.data.repository.SettingsRepository
import com.example.cleanflow.ui.screens.home.DashboardScreen
import com.example.cleanflow.ui.screens.home.HomeViewModel
import com.example.cleanflow.ui.screens.settings.SettingsScreen
import com.example.cleanflow.ui.screens.viewer.MediaViewerScreen
import com.example.cleanflow.ui.screens.viewer.ViewerViewModel
import com.example.cleanflow.ui.theme.CleanFlowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Manual Dependency Injection
        val dataSource = MediaStoreDataSource(applicationContext)
        val repository = MediaRepositoryImpl(dataSource)
        val settingsRepository = SettingsRepository(applicationContext)
        
        setContent {
            CleanFlowTheme {
                val navController = rememberNavController()
                
                NavHost(navController = navController, startDestination = "dashboard") {
                    
                    composable("dashboard") {
                        // Using viewModel composable with factory
                        val viewModel: HomeViewModel = viewModel(
                            factory = HomeViewModel.Factory(repository)
                        )
                        
                        DashboardScreen(
                            viewModel = viewModel,
                            onCollectionClick = { collectionId ->
                                navController.navigate("viewer/$collectionId")
                            },
                            onSettingsClick = {
                                navController.navigate("settings")
                            }
                        )
                    }
                    
                    composable(
                        route = "viewer/{collectionId}",
                        arguments = listOf(navArgument("collectionId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val collectionId = backStackEntry.arguments?.getString("collectionId") ?: ""
                        
                        val viewModel: ViewerViewModel = viewModel(
                            factory = ViewerViewModel.Factory(repository, settingsRepository, collectionId)
                        )
                        
                        MediaViewerScreen(
                            viewModel = viewModel,
                            onBackClick = { navController.popBackStack() }
                        )
                    }

                    composable("settings") {
                        SettingsScreen(
                            repository = settingsRepository,
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}