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
import com.example.cleanflow.data.local.AppDatabase
import com.example.cleanflow.data.repository.MediaRepositoryImpl
import com.example.cleanflow.data.repository.MediaStoreDataSource
import com.example.cleanflow.data.repository.SettingsRepository
import com.example.cleanflow.data.repository.TrashRepository
import com.example.cleanflow.ui.screens.home.DashboardScreen
import com.example.cleanflow.ui.screens.home.HomeViewModel
import com.example.cleanflow.ui.screens.settings.SettingsScreen
import com.example.cleanflow.ui.screens.trash.TrashScreen
import com.example.cleanflow.ui.screens.trash.TrashViewModel
import com.example.cleanflow.ui.screens.viewer.MediaViewerScreen
import com.example.cleanflow.ui.screens.viewer.ViewerViewModel
import com.example.cleanflow.ui.theme.CleanFlowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Manual Dependency Injection
        val database = AppDatabase.getInstance(applicationContext)
        val trashDao = database.trashDao()
        val trashRepository = TrashRepository(trashDao)
        
        val dataSource = MediaStoreDataSource(applicationContext)
        val repository = MediaRepositoryImpl(dataSource, trashRepository)
        val settingsRepository = SettingsRepository(applicationContext)
        
        setContent {
            CleanFlowTheme {
                val navController = rememberNavController()
                
                NavHost(navController = navController, startDestination = "dashboard") {
                    
                    composable("dashboard") {
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
                            },
                            onTrashClick = {
                                navController.navigate("trash")
                            }
                        )
                    }
                    
                    composable(
                        route = "viewer/{collectionId}",
                        arguments = listOf(navArgument("collectionId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val collectionId = backStackEntry.arguments?.getString("collectionId") ?: ""
                        
                        val viewModel: ViewerViewModel = viewModel(
                            factory = ViewerViewModel.Factory(
                                repository, 
                                settingsRepository, 
                                trashRepository,
                                collectionId
                            )
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
                    
                    composable("trash") {
                        val viewModel: TrashViewModel = viewModel(
                            factory = TrashViewModel.Factory(trashRepository, repository)
                        )
                        
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