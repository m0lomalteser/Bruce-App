package com.nostudios.bruceapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nostudios.bruceapp.R
import com.nostudios.bruceapp.ble.ConnectionState
import com.nostudios.bruceapp.data.model.BruceDevice
import com.nostudios.bruceapp.ui.components.ActionRow
import com.nostudios.bruceapp.ui.components.PinEntryOverlay
import com.nostudios.bruceapp.ui.components.StatItem
import com.nostudios.bruceapp.ui.theme.*
import com.nostudios.bruceapp.viewmodel.BruceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(
    device: BruceDevice,
    viewModel: BruceViewModel,
    onNavigateToTerminal: () -> Unit,
    onNavigateToRemoteControl: () -> Unit,
    onNavigateToQuickActions: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val connectionState by viewModel.bleManager.connectionState.collectAsState()
    val batteryLevel by viewModel.bleManager.batteryLevel.collectAsState()
    val hardwareName by viewModel.bleManager.hardwareName.collectAsState()

    val isConnected = connectionState == ConnectionState.Paired

    val showPin = connectionState == ConnectionState.NeedsPin || connectionState == ConnectionState.Authenticating

    LaunchedEffect(device.id) {
        viewModel.bleManager.selectActiveDevice(device.id)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Background,
            topBar = {
                TopAppBar(
                    title = { Text("", style = MaterialTheme.typography.bodyMedium) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .then(if (showPin) Modifier.blur(10.dp) else Modifier),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(top = 20.dp)
                ) {
                    val imageResId = getImageId(device.imageName)
                    Image(
                        painter = painterResource(id = imageResId),
                        contentDescription = device.name,
                        modifier = Modifier
                            .size(150.dp)
                            .clip(CircleShape)
                            .background(CardDark)
                            .padding(20.dp),
                        contentScale = ContentScale.Fit
                    )

                    Text(
                        text = device.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Black,
                        color = White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(CardDark, RoundedCornerShape(20.dp))
                        .padding(vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(
                        icon = Icons.Filled.BatteryFull,
                        label = "PWR",
                        value = if (isConnected) "${batteryLevel ?: "--"}%" else "--%"
                    )
                    StatItem(
                        icon = if (isConnected) Icons.Filled.Link else Icons.Filled.LinkOff,
                        label = "STATUS",
                        value = if (isConnected) "SECURE" else "N/A"
                    )
                    StatItem(
                        icon = Icons.Filled.Bluetooth,
                        label = "BLE",
                        value = if (isConnected) "ON" else "OFF"
                    )
                }

                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ActionRow(
                        icon = Icons.Filled.Smartphone,
                        title = "Navigation",
                        subtitle = "Live Screen & Control",
                        onClick = { if (isConnected) onNavigateToRemoteControl() }
                    )

                    ActionRow(
                        icon = Icons.Filled.Terminal,
                        title = "Terminal",
                        subtitle = "Serial Command Session",
                        onClick = { if (isConnected) onNavigateToTerminal() },
                        modifier = if (!isConnected) Modifier else Modifier
                    )

                    ActionRow(
                        icon = Icons.Filled.Bolt,
                        title = "Quick Actions",
                        subtitle = "BadUSB, BLE Spam, etc.",
                        onClick = { if (isConnected) onNavigateToQuickActions() },
                        modifier = if (!isConnected) Modifier else Modifier
                    )
                }

                Button(
                    onClick = {
                        viewModel.deleteDevice(device)
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        "Remove Device",
                        color = Red,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (showPin) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                PinEntryOverlay(bleManager = viewModel.bleManager)
            }
        }
    }
}

private fun getImageId(imageName: String): Int {
    return when (imageName) {
        "m5stack_core" -> R.drawable.m5stack_core
        "m5stack_core2" -> R.drawable.m5stack_core2
        "m5stack_cardputer" -> R.drawable.m5stack_cardputer
        "m5stick_sticks3" -> R.drawable.m5stick_sticks3
        "m5stack_cplus2" -> R.drawable.m5stack_cplus2
        "lilygo_t_deck" -> R.drawable.lilygo_t_deck
        "lilygo_t_embed" -> R.drawable.lilygo_t_embed
        "lilygo_t_embed_cc1101" -> R.drawable.lilygo_t_embed_cc1101
        "lilygo_t_lora_pager" -> R.drawable.lilygo_t_lora_pager
        else -> R.drawable.bruce
    }
}
