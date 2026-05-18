package com.nostudios.bruceapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nostudios.bruceapp.data.model.BruceDevice
import com.nostudios.bruceapp.ble.ConnectionState
import com.nostudios.bruceapp.ui.components.DeviceTile
import com.nostudios.bruceapp.ui.theme.*
import com.nostudios.bruceapp.util.chunkedInto
import com.nostudios.bruceapp.viewmodel.BruceViewModel
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyDevicesScreen(
    viewModel: BruceViewModel,
    onDeviceClick: (BruceDevice) -> Unit,
    onAddDeviceClick: () -> Unit
) {
    val savedDevices by viewModel.savedDevices.collectAsState()
    val discoveredDevices by viewModel.bleManager.discoveredDevices.collectAsState()
    val connectionState by viewModel.bleManager.connectionState.collectAsState()

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Dashboard",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background,
                    titleContentColor = White
                ),
                actions = {
                    IconButton(onClick = onAddDeviceClick) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "Add Device",
                            tint = White
                        )
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
        ) {
            if (discoveredDevices.isNotEmpty()) {
                val newDevice = discoveredDevices.first()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable {
                            viewModel.bleManager.connectToDevice(newDevice)
                            onAddDeviceClick()
                        },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF262626))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Memory, contentDescription = null, tint = White)
                            Text(
                                "New Device: ${newDevice.name ?: "Bruce"}",
                                color = White
                            )
                        }
                        Text(
                            "PAIR",
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                    }
                }
            }

            if (savedDevices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No Hardware",
                            style = MaterialTheme.typography.bodyMedium,
                            color = White,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Turn on a Bruce device to pair it.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = WhiteOp70,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                val chunked = savedDevices.chunkedInto(2)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    for (row in chunked) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            for (device in row) {
                                val isConnected = viewModel.bleManager.connectedPeripherals.containsKey(device.id)
                                val batteryLevel = if (isConnected &&
                                    viewModel.bleManager.activePeripheral?.device?.address == device.id
                                ) viewModel.bleManager.batteryLevel.value else null

                                DeviceTile(
                                    device = device,
                                    isConnected = isConnected,
                                    batteryLevel = batteryLevel,
                                    onClick = { onDeviceClick(device) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (row.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}
