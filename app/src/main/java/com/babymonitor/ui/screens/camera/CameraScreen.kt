package com.babymonitor.ui.screens.camera

import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    onNavigateBack: () -> Unit,
    viewModel: CameraViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    LaunchedEffect(previewView) {
        previewView?.let { view ->
            viewModel.startCamera(lifecycleOwner, view)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopMonitoring()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Camera Mode") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleNightVision() }) {
                        Icon(
                            imageVector = if (uiState.isNightVisionEnabled)
                                Icons.Default.Nightlight else Icons.Default.LightMode,
                            contentDescription = "Night Vision"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Camera Preview
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }.also { previewView = it }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Status Overlay
            StatusOverlay(
                uiState = uiState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            )

            // Alert Banner
            AnimatedVisibility(
                visible = uiState.hasActiveAlert,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                AlertBanner(
                    alertMessage = uiState.currentAlertMessage,
                    onDismiss = { viewModel.dismissAlert() }
                )
            }

            // Room Code Display
            if (uiState.isStreaming) {
                RoomCodeCard(
                    roomCode = uiState.roomCode,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 100.dp)
                )
            }

            // Control Panel
            ControlPanel(
                uiState = uiState,
                onToggleStreaming = { viewModel.toggleStreaming() },
                onToggleMotionDetection = { viewModel.toggleMotionDetection() },
                onToggleCryDetection = { viewModel.toggleCryDetection() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun StatusOverlay(
    uiState: CameraUiState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Streaming status
        StatusIndicator(
            isActive = uiState.isStreaming,
            activeColor = Color.Red,
            label = if (uiState.isStreaming) "LIVE" else "OFF"
        )

        // Motion detection status
        StatusIndicator(
            isActive = uiState.isMotionDetectionEnabled,
            activeColor = if (uiState.motionDetected) Color.Yellow else Color.Green,
            label = "Motion"
        )

        // Cry detection status
        StatusIndicator(
            isActive = uiState.isCryDetectionEnabled,
            activeColor = if (uiState.cryDetected) Color.Red else Color.Green,
            label = "Audio"
        )
    }
}

@Composable
private fun StatusIndicator(
    isActive: Boolean,
    activeColor: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isActive) activeColor else Color.Gray)
        )
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun AlertBanner(
    alertMessage: String?,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = alertMessage ?: "Alert!",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss")
            }
        }
    }
}

@Composable
private fun RoomCodeCard(
    roomCode: String?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Room Code",
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = roomCode ?: "------",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Share this code with viewers",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


@Composable
private fun ControlPanel(
    uiState: CameraUiState,
    onToggleStreaming: () -> Unit,
    onToggleMotionDetection: () -> Unit,
    onToggleCryDetection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main streaming button
            Button(
                onClick = onToggleStreaming,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isStreaming)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = if (uiState.isStreaming)
                        Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (uiState.isStreaming) "Stop Streaming" else "Start Streaming")
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Detection toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FilterChip(
                    selected = uiState.isMotionDetectionEnabled,
                    onClick = onToggleMotionDetection,
                    label = { Text("Motion") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )

                FilterChip(
                    selected = uiState.isCryDetectionEnabled,
                    onClick = onToggleCryDetection,
                    label = { Text("Cry Detection") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }

            // Audio level indicator
            if (uiState.isCryDetectionEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                AudioLevelIndicator(
                    level = uiState.audioLevel,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun AudioLevelIndicator(
    level: Float,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Audio Level",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { level.coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = when {
                level > 0.7f -> MaterialTheme.colorScheme.error
                level > 0.4f -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.primary
            }
        )
    }
}
