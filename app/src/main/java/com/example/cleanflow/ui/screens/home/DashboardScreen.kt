package com.example.cleanflow.ui.screens.home

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.cleanflow.R
import com.example.cleanflow.domain.model.MediaCollection
import com.example.cleanflow.util.FileSizeFormatter
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun DashboardScreen(
    viewModel: HomeViewModel,
    onCollectionClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onTrashClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = context as? androidx.lifecycle.LifecycleOwner
    
    // -- Permission State Management --
    
    // 1. "All Files Access" for Android 11+ (R)
    val isStorageManagerState = androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(
             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                 android.os.Environment.isExternalStorageManager()
             } else {
                 true // Not required/applicable for < Android 11 in this specific logic block
             }
        )
    }
    val isStorageManager = isStorageManagerState.value
    
    // Refresh "All Files Access" status when resuming (e.g. coming back from Settings)
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                     isStorageManagerState.value = android.os.Environment.isExternalStorageManager()
                 }
            }
        }
        lifecycleOwner?.lifecycle?.addObserver(observer)
        onDispose { lifecycleOwner?.lifecycle?.removeObserver(observer) }
    }

    // 2. Standard Permissions for older Android versions (< R) or if we want to fallback?
    // Actually, for a "Cleaner" app on R+, MANAGE_EXTERNAL_STORAGE is best.
    // We will only use the runtime permissions block if we are BELOW Android 11.
    val useRuntimePermissions = Build.VERSION.SDK_INT < Build.VERSION_CODES.R

    val permissions = if (useRuntimePermissions) {
        listOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    } else {
        emptyList()
    }
    
    val permissionState = rememberMultiplePermissionsState(permissions = permissions)
    
    LaunchedEffect(key1 = useRuntimePermissions) {
        if (useRuntimePermissions && !permissionState.allPermissionsGranted) {
            permissionState.launchMultiplePermissionRequest()
        }
    }

    // -- UI Structure --

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CleanFlow Dashboard") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    androidx.compose.material3.IconButton(onClick = onTrashClick) {
                        androidx.compose.material3.Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Delete,
                            contentDescription = "Papelera"
                        )
                    }
                    androidx.compose.material3.IconButton(onClick = onSettingsClick) {
                        androidx.compose.material3.Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        
        // Check conditions to show content
        val canShowContent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            isStorageManager
        } else {
            permissionState.allPermissionsGranted
        }

        if (canShowContent) {
            DashboardContent(
                uiState = uiState,
                onCollectionClick = onCollectionClick,
                modifier = Modifier.padding(paddingValues)
            )
        } else {
            // Permission Request UI
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) 
                            "Se requiere acceso total a archivos para limpiar media de otras apps."
                        else 
                            "Se requieren permisos de almacenamiento para escanear.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(horizontal = 32.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    
                    androidx.compose.material3.Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                try {
                                    val intent = android.content.Intent(
                                        android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                                    ).apply {
                                        data = android.net.Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    // Fallback if the specific intent fails
                                    val intent = android.content.Intent(
                                        android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                                    )
                                    context.startActivity(intent)
                                }
                            } else {
                                permissionState.launchMultiplePermissionRequest()
                            }
                        }
                    ) {
                        Text("Conceder Permiso")
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardContent(
    uiState: HomeUiState,
    onCollectionClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // storage header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Used Storage",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = FileSizeFormatter.format(uiState.totalSpaceUsed),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(uiState.collections) { collection ->
                    MediaCollectionCard(
                        collection = collection,
                        onClick = { onCollectionClick(collection.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaCollectionCard(
    collection: MediaCollection,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                if (collection.previewUri.isNotEmpty()) {
                    AsyncImage(
                        model = collection.previewUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = collection.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1
                )
                Text(
                    text = "${collection.fileCount} files",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = FileSizeFormatter.format(collection.totalSize),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
