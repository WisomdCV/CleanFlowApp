package com.example.cleanflow.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.cleanflow.data.repository.ContentScaleMode
import com.example.cleanflow.data.repository.SettingsRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: SettingsRepository,
    onBackClick: () -> Unit
) {
    val preferences by repository.userPreferences.collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            preferences?.let { prefs ->
                // Auto Play Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Auto-play Videos",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Play videos automatically when scrolling",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = prefs.autoPlayVideo,
                        onCheckedChange = { 
                            scope.launch { repository.toggleAutoPlay(prefs.autoPlayVideo) }
                        }
                    )
                }

                HorizontalDivider()

                // Content Scale Selector
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Default View Mode",
                        style = MaterialTheme.typography.titleMedium
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        ContentScaleMode.entries.forEachIndexed { index, mode ->
                            SegmentedButton(
                                selected = prefs.defaultContentScale == mode,
                                onClick = { 
                                    scope.launch { repository.setContentScale(mode) }
                                },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = ContentScaleMode.entries.size
                                )
                            ) {
                                Text(mode.name)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HorizontalDivider() {
    androidx.compose.material3.HorizontalDivider(
        modifier = Modifier.padding(vertical = 8.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}
