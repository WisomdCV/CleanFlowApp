package com.example.cleanflow.ui.screens.viewer

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.cleanflow.domain.model.MediaType
import com.example.cleanflow.ui.components.VideoPlayer
import com.example.cleanflow.util.FileSizeFormatter

@Composable
fun MediaViewerScreen(
    viewModel: ViewerViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val preferences by viewModel.userPreferences.collectAsState(initial = null)
    
    val files = uiState.files

    if (files.isNotEmpty() && preferences != null) {
        val pagerState = rememberPagerState(pageCount = { files.size })
        val userPrefs = preferences!!

        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val file = files[page]
                val isCurrentPage = pagerState.currentPage == page
                
                // Local state for temporary toggle per item, seeded by preference
                // We use a key to reset it when scrolling to a new page effectively, 
                // OR we can keep it strictly local. 
                // Requirement: "Alterna temporalmente... solo para ese item"
                var isContentFit by remember(file.id) { 
                    mutableStateOf(userPrefs.defaultContentScale == com.example.cleanflow.data.repository.ContentScaleMode.FIT) 
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = {
                                    isContentFit = !isContentFit
                                    println("Double tap detected. New Fit State: $isContentFit")
                                }
                            )
                        }
                ) {
                    if (file.type == MediaType.VIDEO) {
                        VideoPlayer(
                            uri = Uri.parse(file.uri),
                            playWhenReady = isCurrentPage && userPrefs.autoPlayVideo,
                            isContentFit = isContentFit
                        )
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

            // Overlay UI
            ViewerOverlay(
                modifier = Modifier.align(Alignment.BottomStart),
                fileName = files[pagerState.currentPage].displayName,
                fileSize = FileSizeFormatter.format(files[pagerState.currentPage].size),
                onDeleteClick = { println("Delete clicked for ${files[pagerState.currentPage].id}") },
                onSaveClick = { println("Save clicked for ${files[pagerState.currentPage].id}") }
            )

            // Top Bar
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        }
    } else {
        // Empty or loading state could go here
        Box(modifier = Modifier.fillMaxSize().background(Color.Black))
    }
}

@Composable
fun ViewerOverlay(
    modifier: Modifier = Modifier,
    fileName: String,
    fileSize: String,
    onDeleteClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                )
            )
            .padding(16.dp)
            .padding(bottom = 32.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
                Text(
                    text = fileSize,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray
                )
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                FloatingActionButton(
                    onClick = onDeleteClick,
                    containerColor = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
                }
                Spacer(modifier = Modifier.width(16.dp))
                FloatingActionButton(
                    onClick = onSaveClick,
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "Keep")
                }
            }
        }
    }
}
