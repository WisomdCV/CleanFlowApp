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
import com.example.cleanflow.ui.screens.home.DashboardScreen
import com.example.cleanflow.ui.screens.home.HomeViewModel
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
                            }
                        )
                        
                        // We need to pass navigation callback to DashboardScreen or handle it here.
                        // Since DashboardScreen didn't have a callback in previous step, 
                        // I will assume I need to modify DashboardScreen to expose onCollectionClick
                        // OR (cleaner for this step without editing recent file) 
                        // I will modify DashboardScreen in next step OR use a workaround if possible?
                        // No, passing callback is best practice.
                        // But wait, the previous DashboardScreen implementation does NOT have an onClick callback.
                        // I need to update DashboardScreen first? Or I can update it after. 
                        // I will update MainActivity now, but DashboardScreen will technically not trigger navigation yet.
                        // I will update DashboardScreen in the next step to accept the callback.
                    }
                    
                    composable(
                        route = "viewer/{collectionId}",
                        arguments = listOf(navArgument("collectionId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val collectionId = backStackEntry.arguments?.getString("collectionId") ?: ""
                        
                        val viewModel: ViewerViewModel = viewModel(
                            factory = ViewerViewModel.Factory(repository, collectionId)
                        )
                        
                        MediaViewerScreen(
                            viewModel = viewModel,
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}