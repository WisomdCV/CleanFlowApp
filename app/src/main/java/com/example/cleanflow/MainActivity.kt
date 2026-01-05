package com.example.cleanflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cleanflow.data.repository.MediaRepositoryImpl
import com.example.cleanflow.data.repository.MediaStoreDataSource
import com.example.cleanflow.ui.screens.home.DashboardScreen
import com.example.cleanflow.ui.screens.home.HomeViewModel
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
                // Using viewModel composable with factory
                val viewModel: HomeViewModel = viewModel(
                    factory = HomeViewModel.Factory(repository)
                )
                
                DashboardScreen(viewModel = viewModel)
            }
        }
    }
}