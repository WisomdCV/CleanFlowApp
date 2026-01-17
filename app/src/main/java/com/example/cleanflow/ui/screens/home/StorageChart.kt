package com.example.cleanflow.ui.screens.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.cleanflow.domain.model.SmartDashboardStats

@Composable
fun StorageChart(
    stats: SmartDashboardStats,
    modifier: Modifier = Modifier
) {
    val animationProgress = remember { Animatable(0f) }
    
    LaunchedEffect(stats) {
        animationProgress.animateTo(1f, animationSpec = tween(1000))
    }

    val total = stats.totalUsedSize.coerceAtLeast(1)
    val videoRatio = stats.videoSize.toFloat() / total
    val imageRatio = stats.imageSize.toFloat() / total
    
    val videoColor = MaterialTheme.colorScheme.primary
    val imageColor = MaterialTheme.colorScheme.secondary
    
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 24.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val radius = diameter / 2
            val topLeft = Offset(
                x = (size.width - diameter) / 2,
                y = (size.height - diameter) / 2
            )
            val sizeC = Size(diameter, diameter)
            
            // Background track
            drawArc(
                color = Color.LightGray.copy(alpha = 0.2f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth),
                topLeft = topLeft,
                size = sizeC
            )
            
            var startAngle = -90f
            
            // Video Arc
            val videoSweep = 360f * videoRatio * animationProgress.value
            if (videoSweep > 1) {
                drawArc(
                    color = videoColor,
                    startAngle = startAngle,
                    sweepAngle = videoSweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    topLeft = topLeft,
                    size = sizeC
                )
                startAngle += videoSweep
            }
            
            // Image Arc
            val imageSweep = 360f * imageRatio * animationProgress.value
            if (imageSweep > 1) {
                drawArc(
                    color = imageColor,
                    startAngle = startAngle,
                    sweepAngle = imageSweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    topLeft = topLeft,
                    size = sizeC
                )
            }
        }
    }
}
