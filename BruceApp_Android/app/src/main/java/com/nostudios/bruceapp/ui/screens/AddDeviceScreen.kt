package com.nostudios.bruceapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nostudios.bruceapp.ble.ConnectionState
import com.nostudios.bruceapp.ui.theme.*
import com.nostudios.bruceapp.viewmodel.BruceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceScreen(
    viewModel: BruceViewModel,
    onDismiss: () -> Unit
) {
    val connectionState by viewModel.bleManager.connectionState.collectAsState()
    val discoveredDevices by viewModel.bleManager.discoveredDevices.collectAsState()
    val hardwareName by viewModel.bleManager.hardwareName.collectAsState()
    var pinInput by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.bleManager.startExplicitScan()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.bleManager.stopExplicitScan()
        }
    }

    LaunchedEffect(connectionState) {
        if (connectionState == ConnectionState.Paired) {
            val active = viewModel.bleManager.activePeripheral?.device
            if (active != null) {
                viewModel.saveDevice(active, hardwareName, pinInput.ifBlank { null })
                onDismiss()
            }
        }
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("New Device", style = MaterialTheme.typography.bodyMedium) },
                navigationIcon = {
                    TextButton(onClick = {
                        viewModel.bleManager.stopExplicitScan()
                        onDismiss()
                    }) { Text("Cancel", color = White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                connectionState == ConnectionState.NeedsPin || connectionState == ConnectionState.Authenticating -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Spacer(Modifier.weight(1f))
                        Icon(
                            Icons.Filled.Lock,
                            contentDescription = null,
                            tint = White,
                            modifier = Modifier.size(60.dp)
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 20.dp)
                        ) {
                            Text("PIN-Pairing successful", color = White)
                            Text(
                                "Your Bruce Device now shows a PIN enter it here:",
                                color = Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = { pinInput = it },
                            label = { Text("PIN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = White
                            ),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = White,
                                unfocusedBorderColor = Gray
                            ),
                            modifier = Modifier.width(200.dp).padding(vertical = 16.dp)
                        )
                        if (connectionState == ConnectionState.Authenticating) {
                            CircularProgressIndicator(color = White, modifier = Modifier.size(24.dp))
                            Text("Checking PIN...", color = Gray)
                        } else {
                            Button(
                                onClick = { viewModel.bleManager.submitPin(pinInput) },
                                enabled = pinInput.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(containerColor = White, contentColor = Color.Black)
                            ) {
                                Text("Pair", fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.weight(1f))
                    }
                }

                discoveredDevices.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = White, modifier = Modifier.size(32.dp))
                        Text(
                            "Searching for Bruce Hardware...",
                            color = Gray,
                            modifier = Modifier.padding(top = 20.dp)
                        )
                    }
                }

                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        for (device in discoveredDevices) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.bleManager.connectToDevice(device) },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = CardDark)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Icon(Icons.Filled.Memory, contentDescription = null, tint = White)
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            device.name ?: "Unknown Board",
                                            color = White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text("Ready for Pairing", color = Green)
                                    }
                                    Icon(Icons.Filled.Add, contentDescription = null, tint = White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
