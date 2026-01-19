package com.example.cleanflow.ui.screens.home

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.GridView
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
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.util.lerp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import com.example.cleanflow.R
import com.example.cleanflow.domain.model.MediaCollection
import com.example.cleanflow.domain.model.SmartDashboardStats
import com.example.cleanflow.util.FileSizeFormatter
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun DashboardScreen(
    viewModel: HomeViewModel,
    onCollectionClick: (String) -> Unit,
    onSmartFilterClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onTrashClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = context as? androidx.lifecycle.LifecycleOwner
    
    // Tech Mode State
    var isTechMode by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    
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
                    androidx.compose.material3.IconButton(onClick = { isTechMode = !isTechMode }) {
                         androidx.compose.material3.Icon(
                             imageVector = if (isTechMode) androidx.compose.material.icons.Icons.Default.GridView 
                                           else androidx.compose.material.icons.Icons.Default.ViewCarousel,
                             contentDescription = "Switch View"
                         )
                    }
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
        
        Column(
            modifier = androidx.compose.ui.Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Dashboard content directly here (remove canShowContent check)
            DashboardContent(
                uiState = uiState,
                onCollectionClick = onCollectionClick,
                onSmartFilterClick = onSmartFilterClick,
                isTechMode = isTechMode
            )
    }
    }
}

@Composable
fun DashboardContent(
    uiState: HomeUiState,
    onCollectionClick: (String) -> Unit,
    onSmartFilterClick: (String) -> Unit,
    isTechMode: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // --- Storage Header with Chart ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StorageChart(
                stats = uiState.stats,
                modifier = Modifier.size(120.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(text = "Almacenamiento", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = FileSizeFormatter.format(uiState.totalSize),
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Video (${FileSizeFormatter.format(uiState.stats.videoSize)})", style = MaterialTheme.typography.labelSmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.secondary, androidx.compose.foundation.shape.CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Imágenes (${FileSizeFormatter.format(uiState.stats.imageSize)})", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        
        // Smart Actions
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
             SmartActionChip(
                 label = "Grandes", 
                 count = uiState.stats.largeFilesCount,
                 onClick = { onSmartFilterClick("LARGE_FILES") }
             )
             SmartActionChip(
                 label = "Viejos", 
                 count = uiState.stats.oldFilesCount,
                 onClick = { onSmartFilterClick("OLD_FILES") }
             )
             SmartActionChip(
                 label = "Duplicados", 
                 count = uiState.stats.duplicateGroupsCount,
                 isWarning = true,
                 onClick = { onSmartFilterClick("DUPLICATES") }
             )
        }

        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.collections.isEmpty()) {
            // Empty state - no media collections found
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    androidx.compose.material3.Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No hay colecciones",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No se encontraron imágenes o videos",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            androidx.compose.animation.Crossfade(targetState = isTechMode, label = "viewMode") { tech ->
                if (tech) {
                    TechDashboardCarousel(
                        collections = uiState.collections,
                        onCollectionClick = onCollectionClick
                    )
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

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TechDashboardCarousel(
    collections: List<MediaCollection>,
    onCollectionClick: (String) -> Unit
) {
    val startPage = if (collections.isNotEmpty()) collections.size / 2 else 0
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = startPage,
        pageCount = { collections.size }
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {
        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 64.dp),
            modifier = Modifier.height(400.dp) // Taller for immersion
        ) { page ->
            val collection = collections[page]
            
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        // Calculate the absolute offset for the current page from the
                        // scroll position. We use the absolute value which allows us to mirror
                        // any effects for both directions
                        val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                        val absOffset = Math.abs(pageOffset)

                        // Scale: 1f at center, down to 0.85f
                        val scale = lerp(
                            start = 0.85f,
                            stop = 1f,
                            fraction = 1f - absOffset.coerceIn(0f, 1f)
                        )
                        scaleX = scale
                        scaleY = scale

                        // Alpha: Fade out side items
                        alpha = lerp(
                            start = 0.5f,
                            stop = 1f,
                            fraction = 1f - absOffset.coerceIn(0f, 1f)
                        )
                        
                        // Rotation Y for 3D effect (Cover Flow)
                        // Negative rotation for left items, positive for right
                        rotationY = pageOffset * -30f 
                    }
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                TechCollectionCard(
                    collection = collection,
                    onClick = { onCollectionClick(collection.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TechCollectionCard(
    collection: MediaCollection,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(350.dp), // Large card
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Full background image
            if (collection.previewUri.isNotEmpty()) {
                AsyncImage(
                    model = collection.previewUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.4f),
                                Color.Black.copy(alpha = 0.9f)
                            )
                        )
                    )
            )
            
            // Content
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(24.dp)
            ) {
                Text(
                    text = collection.displayName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${collection.fileCount} archivos • ${FileSizeFormatter.format(collection.totalSize)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                // Decorative "Open" pill
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primary, 
                            MaterialTheme.shapes.extraLarge
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "EXPLORAR",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun SmartActionChip(
    label: String,
    count: Int,
    isWarning: Boolean = false,
    onClick: () -> Unit
) {
    val containerColor = if (isWarning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isWarning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
    
    androidx.compose.material3.Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = containerColor,
        modifier = Modifier.height(32.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = contentColor)
            if (count > 0) {
                Spacer(modifier = Modifier.width(4.dp))
                // Badge circle
                Box(modifier = Modifier
                    .size(16.dp)
                    .background(contentColor.copy(alpha = 0.2f), androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = contentColor
                    )
                }
            }
        }
    }
}


// Extension helper for sign if needed or just use built-in logic


