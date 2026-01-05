package com.example.cleanflow.ui.components

import android.net.Uri
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    uri: Uri,
    playWhenReady: Boolean,
    isContentFit: Boolean,
    playbackSpeed: Float = 1.0f,
    seekToPosition: Long? = null,
    onProgress: (Long, Long) -> Unit = { _, _ -> },
    onVideoReady: (Long) -> Unit = {}
) {
    val context = LocalContext.current

    // We keep track of the player in a state to release it properly
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    
    // Manage polling for progress
    LaunchedEffect(player, playWhenReady) {
        val p = player ?: return@LaunchedEffect
        while (true) {
            if (p.isPlaying) {
                onProgress(p.currentPosition, p.duration)
            }
            kotlinx.coroutines.delay(500) // Poll every 500ms
        }
    }

    DisposableEffect(uri, context) {
        val exoPlayer = ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(uri))
            prepare()
            repeatMode = Player.REPEAT_MODE_ONE
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        onVideoReady(duration)
                    }
                }
            })
        }
        player = exoPlayer

        onDispose {
            exoPlayer.release()
            player = null
        }
    }

    // React to playWhenReady changes
    DisposableEffect(playWhenReady, player) {
        player?.playWhenReady = playWhenReady
        onDispose {}
    }
    
    // React to Speed changes
    LaunchedEffect(playbackSpeed, player) {
        player?.setPlaybackSpeed(playbackSpeed)
    }
    
    // React to Seek
    LaunchedEffect(seekToPosition, player) {
        if (seekToPosition != null && seekToPosition >= 0) {
            player?.seekTo(seekToPosition)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    this.player = player
                    layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                    useController = false
                    resizeMode = if (isContentFit) AspectRatioFrameLayout.RESIZE_MODE_FIT else AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            },
            update = { playerView ->
                playerView.player = player
                playerView.resizeMode = if (isContentFit) AspectRatioFrameLayout.RESIZE_MODE_FIT else AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
