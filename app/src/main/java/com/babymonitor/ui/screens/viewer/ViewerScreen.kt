package com.babymonitor.ui.screens.viewer

import android.view.ViewGroup
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import org.webrtc.SurfaceViewRenderer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    onNavigateBack: () -> Unit,
    viewModel: ViewerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var surfaceViewRenderer by remember { mutableStateOf<SurfaceViewRenderer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.disconnect()
            surfaceViewRenderer?.release()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Viewer Mode") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (uiState.isConnected) {
                        IconButton(onClick = { viewModel.toggleMute() }) {
                            Icon(
                                imageVector = if (uiState.isMuted)
                                    Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = "Toggle Mute"
                            )
                        }
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
            when {
                !uiState.isConnected && !uiState.isConnecting -> {
                    // Room code entry
                    RoomCodeEntry(
                        roomCode = uiState.enteredRoomCode,
                        onRoomCodeChange = { viewModel.updateRoomCode(it) },
                        onConnect = { viewModel.connect(surfaceViewRenderer) },
                        isValid = uiState.isRoomCodeValid,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    )
                }

                uiState.isConnecting -> {
                    // Connecting state
                    ConnectingView(
                        roomCode = uiState.enteredRoomCode,
                        onCancel = { viewModel.disconnect() },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                uiState.isConnected -> {
                    // Video view
                    AndroidView(
                        factory = { ctx ->
                            SurfaceViewRenderer(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                setEnableHardwareScaler(true)
                                viewModel.initRenderer(this)
                            }.also { surfaceViewRenderer = it }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Connection status overlay
                    ConnectionStatusOverlay(
                        uiState = uiState,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(16.dp)
                    )
                }
            }

            // Alert banner
            AnimatedVisibility(
                visible = uiState.hasAlert,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 80.dp)
            ) {
                AlertBanner(
                    alertMessage = uiState.alertMessage,
                    alertType = uiState.alertType,
                    onDismiss = { viewModel.dismissAlert() }
                )
            }

            // Error message
            if (uiState.errorMessage != null) {
                ErrorView(
                    message = uiState.errorMessage!!,
                    onRetry = { viewModel.connect(surfaceViewRenderer) },
                    onDismiss = { viewModel.clearError() },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp)
                )
            }
        }
    }
}

@Composable
private fun RoomCodeEntry(
    roomCode: String,
    onRoomCodeChange: (String) -> Unit,
    onConnect: () -> Unit,
    isValid: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.QrCodeScanner,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Enter Room Code",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Enter the 6-character code from the camera device",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = roomCode,
            onValueChange = { if (it.length <= 6) onRoomCodeChange(it.uppercase()) },
            label = { Text("Room Code") },
            placeholder = { Text("ABCD12") },
            singleLine = true,
            textStyle = MaterialTheme.typography.headlineMedium.copy(
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            ),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { if (isValid) onConnect() }
            ),
            modifier = Modifier.widthIn(max = 200.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onConnect,
            enabled = isValid,
            modifier = Modifier.fillMaxWidth(0.6f)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Connect")
        }
    }
}

@Composable
private fun ConnectingView(
    roomCode: String,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Connecting to $roomCode...",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Establishing secure connection",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(onClick = onCancel) {
            Text("Cancel")
        }
    }
}

@Composable
private fun ConnectionStatusOverlay(
    uiState: ViewerUiState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Live indicator
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color.Red)
        )
        Text(
            text = "LIVE",
            color = Color.White,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )

        Divider(
            modifier = Modifier
                .height(16.dp)
                .width(1.dp),
            color = Color.White.copy(alpha = 0.5f)
        )

        Text(
            text = uiState.enteredRoomCode,
            color = Color.White,
            style = MaterialTheme.typography.labelMedium
        )

        if (uiState.isMuted) {
            Icon(
                imageVector = Icons.Default.VolumeOff,
                contentDescription = "Muted",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun AlertBanner(
    alertMessage: String?,
    alertType: String?,
    onDismiss: () -> Unit
) {
    val backgroundColor = when (alertType) {
        "cry" -> MaterialTheme.colorScheme.errorContainer
        "motion" -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }

    val icon = when (alertType) {
        "cry" -> Icons.Default.VolumeUp
        "motion" -> Icons.Default.Visibility
        else -> Icons.Default.Warning
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = alertMessage ?: "Alert received",
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
private fun ErrorView(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Connection Error",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Button(onClick = onRetry) {
                    Text("Retry")
                }
            }
        }
    }
}
