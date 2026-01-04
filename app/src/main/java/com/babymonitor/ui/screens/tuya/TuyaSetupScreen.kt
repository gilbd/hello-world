package com.babymonitor.ui.screens.tuya

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.babymonitor.tuya.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TuyaSetupScreen(
    onBack: () -> Unit,
    viewModel: TuyaSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val deviceState by viewModel.deviceState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tuya SmartLife Setup") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            StatusCard(deviceState, connectionState)

            when (uiState) {
                is TuyaSetupUiState.NotConfigured -> {
                    ConfigurationCard(
                        onConfigure = { accessId, accessSecret, productId, productKey, region ->
                            viewModel.configure(accessId, accessSecret, productId, productKey, region)
                        }
                    )
                }

                is TuyaSetupUiState.Configured -> {
                    when (deviceState) {
                        is TuyaDeviceState.Unregistered -> {
                            PairingOptionsCard(
                                onQRPairing = { viewModel.startQRPairing() },
                                onAPPairing = { viewModel.startAPPairing() }
                            )
                        }

                        is TuyaDeviceState.Pairing -> {
                            val state = deviceState as TuyaDeviceState.Pairing
                            when (state.mode) {
                                PairingMode.QR_CODE -> {
                                    QRCodePairingCard(
                                        qrData = viewModel.getQRCodeData(),
                                        onCancel = { viewModel.cancelPairing() }
                                    )
                                }
                                PairingMode.AP -> {
                                    APPairingCard(
                                        onCancel = { viewModel.cancelPairing() }
                                    )
                                }
                                else -> {}
                            }
                        }

                        is TuyaDeviceState.Registering -> {
                            RegisteringCard()
                        }

                        is TuyaDeviceState.Registered -> {
                            RegisteredCard(
                                deviceId = (deviceState as TuyaDeviceState.Registered).deviceId,
                                connectionState = connectionState,
                                onDisconnect = { viewModel.disconnect() },
                                onUnregister = { viewModel.unregister() }
                            )
                        }

                        is TuyaDeviceState.Error -> {
                            ErrorCard(
                                message = (deviceState as TuyaDeviceState.Error).message,
                                onRetry = { viewModel.retry() }
                            )
                        }
                    }
                }

                is TuyaSetupUiState.Error -> {
                    ErrorCard(
                        message = (uiState as TuyaSetupUiState.Error).message,
                        onRetry = { viewModel.retry() }
                    )
                }
            }

            // Help Section
            HelpCard()
        }
    }
}

@Composable
private fun StatusCard(
    deviceState: TuyaDeviceState,
    connectionState: TuyaConnectionState
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                connectionState is TuyaConnectionState.Connected -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                deviceState is TuyaDeviceState.Error -> MaterialTheme.colorScheme.errorContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Device Status",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    getDeviceStatusText(deviceState),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "Connection",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    getConnectionStatusText(connectionState),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = when (connectionState) {
                        is TuyaConnectionState.Connected -> Color(0xFF4CAF50)
                        is TuyaConnectionState.Error -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

@Composable
private fun ConfigurationCard(
    onConfigure: (String, String, String, String, TuyaRegion) -> Unit
) {
    var accessId by remember { mutableStateOf("") }
    var accessSecret by remember { mutableStateOf("") }
    var productId by remember { mutableStateOf("") }
    var productKey by remember { mutableStateOf("") }
    var selectedRegion by remember { mutableStateOf(TuyaRegion.US) }
    var showSecret by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Tuya Developer Credentials",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Enter your credentials from the Tuya IoT Platform",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = accessId,
                onValueChange = { accessId = it },
                label = { Text("Access ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = accessSecret,
                onValueChange = { accessSecret = it },
                label = { Text("Access Secret") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showSecret = !showSecret }) {
                        Icon(
                            if (showSecret) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showSecret) "Hide" else "Show"
                        )
                    }
                }
            )

            OutlinedTextField(
                value = productId,
                onValueChange = { productId = it },
                label = { Text("Product ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = productKey,
                onValueChange = { productKey = it },
                label = { Text("Product Key (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Region Dropdown
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedRegion.name,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Region") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    TuyaRegion.entries.forEach { region ->
                        DropdownMenuItem(
                            text = { Text(region.name) },
                            onClick = {
                                selectedRegion = region
                                expanded = false
                            }
                        )
                    }
                }
            }

            Button(
                onClick = {
                    onConfigure(accessId, accessSecret, productId, productKey, selectedRegion)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = accessId.isNotBlank() && accessSecret.isNotBlank() && productId.isNotBlank()
            ) {
                Text("Save Configuration")
            }
        }
    }
}

@Composable
private fun PairingOptionsCard(
    onQRPairing: () -> Unit,
    onAPPairing: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Add to SmartLife",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Choose how to pair this device with your SmartLife app",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = onQRPairing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.QrCode, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("QR Code Pairing")
            }

            OutlinedButton(
                onClick = onAPPairing,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Wifi, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("AP Mode Pairing")
            }
        }
    }
}

@Composable
private fun QRCodePairingCard(
    qrData: String,
    onCancel: () -> Unit
) {
    val qrBitmap = remember(qrData) { generateQRCode(qrData) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Scan with SmartLife App",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Open SmartLife app → Add Device → Scan QR Code",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            qrBitmap?.let { bitmap ->
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Pairing QR Code",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Text(
                "Waiting for SmartLife to connect...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )

            CircularProgressIndicator(modifier = Modifier.size(24.dp))

            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun APPairingCard(onCancel: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "AP Mode Pairing",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Icon(
                Icons.Default.Wifi,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                "1. Open SmartLife app\n2. Add Device → Camera\n3. Select 'AP Mode'\n4. Connect to 'BabyMonitor_AP' WiFi\n5. Follow the in-app instructions",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            CircularProgressIndicator(modifier = Modifier.size(24.dp))

            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun RegisteringCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                "Registering with Tuya Cloud...",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun RegisteredCard(
    deviceId: String,
    connectionState: TuyaConnectionState,
    onDisconnect: () -> Unit,
    onUnregister: () -> Unit
) {
    var showUnregisterDialog by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50)
                )
                Text(
                    "Device Registered",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Column {
                Text(
                    "Device ID",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    deviceId,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (connectionState is TuyaConnectionState.Connected) {
                Text(
                    "✓ Connected to Tuya Cloud",
                    color = Color(0xFF4CAF50)
                )
                Text(
                    "This device is now visible in your SmartLife app as a smart camera.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDisconnect,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Disconnect")
                }

                Button(
                    onClick = { showUnregisterDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Unregister")
                }
            }
        }
    }

    if (showUnregisterDialog) {
        AlertDialog(
            onDismissRequest = { showUnregisterDialog = false },
            title = { Text("Unregister Device?") },
            text = {
                Text("This will remove this device from Tuya/SmartLife. You'll need to pair again to use it.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnregisterDialog = false
                        onUnregister()
                    }
                ) {
                    Text("Unregister", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnregisterDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )

            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer
            )

            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun HelpCard() {
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "How to get Tuya credentials",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(
                        if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand"
                    )
                }
            }

            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Text(
                    """
                    1. Go to developer.tuya.com
                    2. Create a Cloud Project
                    3. Under "Product Development", create an IPC product
                    4. Select "Security & Video Surveillance" → "Smart Camera"
                    5. Get Product ID from product details
                    6. Get Access ID and Secret from Cloud Project settings

                    For detailed instructions, see the TUYA_SETUP.md guide.
                    """.trimIndent(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun getDeviceStatusText(state: TuyaDeviceState): String {
    return when (state) {
        is TuyaDeviceState.Unregistered -> "Not Registered"
        is TuyaDeviceState.Registering -> "Registering..."
        is TuyaDeviceState.Pairing -> "Pairing..."
        is TuyaDeviceState.Registered -> "Registered"
        is TuyaDeviceState.Error -> "Error"
    }
}

private fun getConnectionStatusText(state: TuyaConnectionState): String {
    return when (state) {
        is TuyaConnectionState.Disconnected -> "Disconnected"
        is TuyaConnectionState.Connecting -> "Connecting..."
        is TuyaConnectionState.Connected -> "Connected"
        is TuyaConnectionState.Error -> "Error"
    }
}

private fun generateQRCode(data: String): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(
                    x, y,
                    if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                )
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}
