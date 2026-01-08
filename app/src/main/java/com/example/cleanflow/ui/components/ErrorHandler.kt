package com.example.cleanflow.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cleanflow.domain.util.AppException

/**
 * A reusable error snackbar component.
 */
@Composable
fun ErrorSnackbarHost(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = modifier.padding(16.dp),
        snackbar = { data ->
            Snackbar(
                snackbarData = data,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                actionColor = MaterialTheme.colorScheme.error
            )
        }
    )
}

/**
 * Shows an error snackbar when the exception changes.
 */
@Composable
fun HandleError(
    error: AppException?,
    snackbarHostState: SnackbarHostState,
    onErrorConsumed: () -> Unit
) {
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(
                message = it.userMessage,
                actionLabel = "OK"
            )
            onErrorConsumed()
        }
    }
}

/**
 * Shows a success snackbar with optional action.
 */
@Composable
fun HandleSuccess(
    message: String?,
    snackbarHostState: SnackbarHostState,
    onMessageConsumed: () -> Unit
) {
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(message = it)
            onMessageConsumed()
        }
    }
}
