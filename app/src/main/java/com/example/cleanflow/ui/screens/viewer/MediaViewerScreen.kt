package com.example.cleanflow.ui.screens.viewer

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.cleanflow.domain.model.ContentScaleMode
import com.example.cleanflow.domain.model.MediaType
import com.example.cleanflow.ui.components.VideoPlayer
import com.example.cleanflow.util.FileSizeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MediaViewerScreen(
    viewModel: ViewerViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val preferences by viewModel.userPreferences.collectAsState(initial = null)
    val navigationEvent by viewModel.navigationEvent.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    
    val context = LocalContext.current
    val files = uiState.files
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Handle Snackbar Trigger
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short
            )
            viewModel.consumeSnackbar()
        }
    }

    if (files.isNotEmpty() && preferences != null) {
        val pagerState = rememberPagerState(
            initialPage = uiState.initialIndex.coerceIn(0, files.size - 1),
            pageCount = { files.size }
        )
        val userPrefs = preferences!!

        // Handle auto-navigation
        LaunchedEffect(navigationEvent) {
            navigationEvent?.let { nextIndex ->
                if (nextIndex < files.size) {
                    pagerState.animateScrollToPage(nextIndex)
                }
                viewModel.resetNavigation()
            }
        }

        // GLOBAL UI STATE (Hoisted)
        var isOverlayVisible by remember { mutableStateOf(true) }
        
        // Hoisted Video State (we only track the 'active' video for the global slider)
        var currentVideoDuration by remember { mutableLongStateOf(0L) }
        var currentVideoPosition by remember { mutableLongStateOf(0L) }
        // We trigger seeks via this shared state. Null means no seek pending.
        var globalSeekRequest by remember { mutableStateOf<Long?>(null) }


        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = Color.Black,
            content = { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(Color.Black)
                ) {
                    VerticalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        // Guard against index OOB if list shrinks rapidly
                        if (page < files.size) {
                            val file = files[page]
                            val isCurrentPage = pagerState.currentPage == page
                            
                            // Local Per-Item State
                            val initialScale = userPrefs.defaultContentScale == ContentScaleMode.FIT
                            var isContentFit by remember(file.id) { mutableStateOf(initialScale) }
                            
                            var isPlaying by remember(file.id) { mutableStateOf(userPrefs.autoPlayVideo) }
                            var playbackSpeed by remember(file.id) { mutableFloatStateOf(1.0f) }
                            
                            // Show/Hide Play icon local state
                            var showPlayPauseIcon by remember { mutableStateOf(false) }
        
                            LaunchedEffect(showPlayPauseIcon) {
                                if (showPlayPauseIcon) {
                                    delay(1000)
                                    showPlayPauseIcon = false
                                }
                            }
        
                            // Elastic Zoom State
                            val scale = remember { Animatable(1f) }
                            val offset = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
                            val scope = rememberCoroutineScope()
        
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()

                                    // Apply Zoom Transformations
                                    .graphicsLayer {
                                        scaleX = scale.value
                                        scaleY = scale.value
                                        translationX = offset.value.x
                                        translationY = offset.value.y
                                    }
                                    // GESTURE: Elastic Zoom
                                    .pointerInput(Unit) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            scope.launch {
                                                scale.snapTo((scale.value * zoom).coerceAtLeast(1f))
                                                val newOffset = offset.value + pan
                                                offset.snapTo(newOffset)
                                            }
                                        }
                                    }
                                    // GESTURE: Snap Back on Release
                                    .pointerInput(Unit) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                if (event.changes.all { !it.pressed }) {
                                                    if (scale.value != 1f || offset.value != Offset.Zero) {
                                                        scope.launch {
                                                            scale.animateTo(1f)
                                                            offset.animateTo(Offset.Zero)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    // GESTURE: Tap & Double Tap
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onDoubleTap = { 
                                                // If zoomed (held?), reset. Logic: Zoom is elastic (snapback), 
                                                // so double tap mainly happens at rest (scale=1).
                                                // Just toggle fit as requested.
                                                isContentFit = !isContentFit
                                            },
                                            onTap = {
                                                if (file.type == MediaType.VIDEO) {
                                                    isPlaying = !isPlaying
                                                    showPlayPauseIcon = true
                                                    isOverlayVisible = true 
                                                } else {
                                                    isOverlayVisible = !isOverlayVisible
                                                }
                                            },
                                            onLongPress = {
                                                if (file.type == MediaType.VIDEO) {
                                                    playbackSpeed = 2.0f
                                                }
                                            }
                                        )
                                    }
                                    .pointerInput(Unit) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                if (file.type == MediaType.VIDEO) {
                                                    val changes = event.changes
                                                    if (changes.all { !it.pressed }) {
                                                        playbackSpeed = 1.0f
                                                    }
                                                }
                                            }
                                        }
                                    }
                            ) {
                                if (file.type == MediaType.VIDEO) {
                                    VideoPlayer(
                                        uri = Uri.parse(file.uri),
                                        playWhenReady = isCurrentPage && isPlaying,
                                        isContentFit = isContentFit,
                                        playbackSpeed = playbackSpeed,
                                        // Pass global seek only if this is the current page
                                        seekToPosition = if (isCurrentPage) globalSeekRequest else null,
                                        onProgress = { pos, dur ->
                                            // Only update global state if this is the current page
                                            if (isCurrentPage) {
                                                currentVideoPosition = pos
                                                currentVideoDuration = dur
                                            }
                                        },
                                        onVideoReady = { dur -> 
                                            if (isCurrentPage) currentVideoDuration = dur 
                                        }
                                    )
                                    
                                    // Icon Overlay (Per video)
                                     AnimatedVisibility(
                                        visible = showPlayPauseIcon,
                                        enter = fadeIn(),
                                        exit = fadeOut(),
                                        modifier = Modifier.align(Alignment.Center)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(80.dp)
                                                .background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.extraLarge),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Default.PlayArrow else Icons.Default.Pause,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(48.dp)
                                            )
                                        }
                                    }
                                    
                                     // 2x Speed Indicator
                                    if (playbackSpeed > 1.0f) {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopCenter)
                                                .padding(top = 80.dp)
                                                .background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small)
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text("2x Speed", color = Color.White, style = MaterialTheme.typography.labelLarge)
                                        }
                                    }
                                } else {
                                    AsyncImage(
                                        model = file.uri,
                                        contentDescription = file.displayName,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = if (isContentFit) ContentScale.Fit else ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
        
                    // --- GLOBAL OVERLAY (Top of Pager) ---
                    // Guard against index OOB
                    if (pagerState.currentPage < files.size) {
                        val currentFile = files[pagerState.currentPage]
                        
                        // Back Button
                        AnimatedVisibility(
                            visible = isOverlayVisible,
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier.align(Alignment.TopStart)
                        ) {
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier
                                    .statusBarsPadding()
                                    .padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                        }
        
                        // Bottom Info & Controls
                        AnimatedVisibility(
                            visible = isOverlayVisible,
                            enter = fadeIn(),
                            exit = fadeOut(),
                            modifier = Modifier.align(Alignment.BottomStart)
                        ) {
                             Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                        )
                                    )
                                    .padding(16.dp)
                                    .padding(bottom = 32.dp)
                            ) {
                                // Global Slider (only if current item is video)
                                if (currentFile.type == MediaType.VIDEO && currentVideoDuration > 0) {
                                     Slider(
                                        value = currentVideoPosition.toFloat(),
                                        onValueChange = { 
                                            currentVideoPosition = it.toLong()
                                        },
                                        onValueChangeFinished = {
                                            globalSeekRequest = currentVideoPosition
                                        },
                                        valueRange = 0f..currentVideoDuration.toFloat().coerceAtLeast(1f),
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.primary,
                                            activeTrackColor = MaterialTheme.colorScheme.primary,
                                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier.height(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
        
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = currentFile.displayName,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = Color.White
                                        )
                                        Text(
                                            text = FileSizeFormatter.format(currentFile.size),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.LightGray
                                        )
                                    }
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Animated Delete Button
                                        val deleteButtonScale by animateFloatAsState(
                                            targetValue = 1f,
                                            animationSpec = androidx.compose.animation.core.spring(
                                                dampingRatio = 0.5f,
                                                stiffness = 300f
                                            ),
                                            label = "deleteScale"
                                        )
                                        
                                        FloatingActionButton(
                                            onClick = { 
                                                 viewModel.moveToTrash(currentFile)
                                            },
                                            containerColor = MaterialTheme.colorScheme.error,
                                            modifier = Modifier
                                                .size(56.dp)
                                                .graphicsLayer {
                                                    scaleX = deleteButtonScale
                                                    scaleY = deleteButtonScale
                                                }
                                        ) {
                                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
                                        }
                                        
                                        Spacer(modifier = Modifier.width(16.dp))
                                        
                                        // Animated Share Button
                                        val shareButtonScale by animateFloatAsState(
                                            targetValue = 1f,
                                            animationSpec = androidx.compose.animation.core.spring(
                                                dampingRatio = 0.5f,
                                                stiffness = 300f
                                            ),
                                            label = "shareScale"
                                        )
                                        
                                        val context = LocalContext.current
                                        FloatingActionButton(
                                            onClick = { 
                                                viewModel.shareFile(context, currentFile) 
                                            },
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .size(56.dp)
                                                .graphicsLayer {
                                                    scaleX = shareButtonScale
                                                    scaleY = shareButtonScale
                                                }
                                        ) {
                                            Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Empty state handled naturally
                    }
                }
            }
        )
    } else {
        // Empty state - all files deleted or loading
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Column(
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No hay más archivos",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Todos los archivos fueron eliminados",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(24.dp))
                FloatingActionButton(
                    onClick = onBackClick,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Volver")
                    }
                }
            }
        }
    }
}
