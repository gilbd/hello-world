package com.babymonitor.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Motion Detection Settings
            SettingsSection(title = "Motion Detection") {
                SwitchSettingItem(
                    title = "Enable Motion Detection",
                    description = "Detect movement in camera view",
                    icon = Icons.Default.Visibility,
                    checked = uiState.motionDetectionEnabled,
                    onCheckedChange = { viewModel.setMotionDetectionEnabled(it) }
                )

                SliderSettingItem(
                    title = "Sensitivity",
                    description = "Higher values detect smaller movements",
                    value = uiState.motionSensitivity,
                    onValueChange = { viewModel.setMotionSensitivity(it) },
                    valueRange = 0f..1f,
                    enabled = uiState.motionDetectionEnabled
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Cry Detection Settings
            SettingsSection(title = "Cry Detection") {
                SwitchSettingItem(
                    title = "Enable Cry Detection",
                    description = "Detect baby crying sounds",
                    icon = Icons.Default.Mic,
                    checked = uiState.cryDetectionEnabled,
                    onCheckedChange = { viewModel.setCryDetectionEnabled(it) }
                )

                SliderSettingItem(
                    title = "Sensitivity",
                    description = "Higher values detect quieter sounds",
                    value = uiState.crySensitivity,
                    onValueChange = { viewModel.setCrySensitivity(it) },
                    valueRange = 0f..1f,
                    enabled = uiState.cryDetectionEnabled
                )

                SliderSettingItem(
                    title = "Volume Threshold",
                    description = "Minimum volume to trigger detection",
                    value = uiState.volumeThreshold,
                    onValueChange = { viewModel.setVolumeThreshold(it) },
                    valueRange = 0.05f..0.5f,
                    enabled = uiState.cryDetectionEnabled
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Notification Settings
            SettingsSection(title = "Notifications") {
                SwitchSettingItem(
                    title = "Enable Notifications",
                    description = "Receive alerts when events are detected",
                    icon = Icons.Default.Notifications,
                    checked = uiState.notificationsEnabled,
                    onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                )

                SwitchSettingItem(
                    title = "Vibration",
                    description = "Vibrate when alerts are triggered",
                    icon = Icons.Default.Vibration,
                    checked = uiState.vibrationEnabled,
                    onCheckedChange = { viewModel.setVibrationEnabled(it) },
                    enabled = uiState.notificationsEnabled
                )

                SwitchSettingItem(
                    title = "Sound",
                    description = "Play sound when alerts are triggered",
                    icon = Icons.Default.VolumeUp,
                    checked = uiState.soundEnabled,
                    onCheckedChange = { viewModel.setSoundEnabled(it) },
                    enabled = uiState.notificationsEnabled
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Video Settings
            SettingsSection(title = "Video Quality") {
                DropdownSettingItem(
                    title = "Resolution",
                    description = "Higher resolution uses more bandwidth",
                    icon = Icons.Default.HighQuality,
                    selectedValue = uiState.videoResolution,
                    options = listOf("480p", "720p", "1080p"),
                    onValueChange = { viewModel.setVideoResolution(it) }
                )

                DropdownSettingItem(
                    title = "Frame Rate",
                    description = "Higher frame rate for smoother video",
                    icon = Icons.Default.Speed,
                    selectedValue = uiState.frameRate,
                    options = listOf("15 fps", "24 fps", "30 fps"),
                    onValueChange = { viewModel.setFrameRate(it) }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // About Section
            SettingsSection(title = "About") {
                SettingItem(
                    title = "Version",
                    description = "1.0.0",
                    icon = Icons.Default.Info
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}

@Composable
private fun SettingItem(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SwitchSettingItem(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled)
                MaterialTheme.colorScheme.onSurfaceVariant
            else
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled)
                    MaterialTheme.colorScheme.onSurfaceVariant
                else
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
private fun SliderSettingItem(
    title: String,
    description: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                text = "${(value * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled)
                    MaterialTheme.colorScheme.primary
                else
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = if (enabled)
                MaterialTheme.colorScheme.onSurfaceVariant
            else
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownSettingItem(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selectedValue: String,
    options: List<String>,
    onValueChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedValue,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .width(120.dp),
                textStyle = MaterialTheme.typography.bodyMedium
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
