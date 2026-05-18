package com.nostudios.bruceapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nostudios.bruceapp.ui.theme.*
import com.nostudios.bruceapp.viewmodel.BruceViewModel

private data class ActionItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val command: String
)

private data class CategoryItem(
    val name: String,
    val icon: ImageVector,
    val actions: List<ActionItem>
)

private val categories = listOf(
    CategoryItem("Wi-Fi", Icons.Filled.Wifi, listOf(
        ActionItem("Network Scan", "Scan for local networks", Icons.Filled.Search, "WIFI_SCAN"),
        ActionItem("Status Check", "View current connection info", Icons.Filled.Info, "WIFI_STATUS"),
        ActionItem("Deauth Attack", "Perform deauth attack", Icons.Filled.Bolt, "WIFI_DEAUTH")
    )),
    CategoryItem("Bluetooth", Icons.Filled.Bluetooth, listOf(
        ActionItem("Device Scan", "Scan for BLE advertisements", Icons.Filled.Sensors, "BLE_SCAN"),
        ActionItem("Stop Advertising", "Halt active broadcasts", Icons.Filled.StopCircle, "BLE_STOP"),
        ActionItem("Apple", "BLESPAM", Icons.Filled.Bolt, "BLESPAM_APPLE_CONTINUITY"),
        ActionItem("Samsung", "BLESPAM", Icons.Filled.Bolt, "BLESPAM_SAMSUNG"),
        ActionItem("Android", "BLESPAM", Icons.Filled.Bolt, "BLESPAM_ANDROID"),
        ActionItem("Windows", "BLESPAM", Icons.Filled.Bolt, "BLESPAM_WINDOWS")
    )),
    CategoryItem("IR (Infrared)", Icons.Filled.Visibility, listOf(
        ActionItem("Record Signal", "Listen for incoming IR codes", Icons.Filled.FiberManualRecord, "IR_REC"),
        ActionItem("Send Saved", "Transmit saved remote code", Icons.Filled.Send, "IR_TX")
    )),
    CategoryItem("RF (Radio)", Icons.Filled.Waves, listOf(
        ActionItem("Frequency Analyzer", "Monitor local sub-GHz bands", Icons.Filled.BarChart, "RF_ANALYZE"),
        ActionItem("Listen Mode", "Receive signal packets", Icons.Filled.Hearing, "RF_RX"),
        ActionItem("Record", "Records signal packets", Icons.Filled.Bolt, "RF_RECORD"),
        ActionItem("Play Recording", "Plays recorded signal packets", Icons.Filled.Bolt, "RF_PLAY")
    )),
    CategoryItem("RFID", Icons.Filled.CreditCard, listOf(
        ActionItem("Read Card", "Scan high/low frequency tags", Icons.Filled.Sensors, "RFID_READ"),
        ActionItem("Emulate Tag", "Broadcast saved UID", Icons.Filled.Waves, "RFID_EMU")
    ))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionsScreen(
    viewModel: BruceViewModel
) {
    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text("Quick Actions", style = MaterialTheme.typography.bodyMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black, titleContentColor = White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            for (category in categories) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Icon(category.icon, contentDescription = null, tint = White)
                        Text(
                            category.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = White
                        )
                    }

                    for (action in category.actions) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.bleManager.sendCommand(action.command) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = CardDark)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Icon(action.icon, contentDescription = null, tint = White, modifier = Modifier.size(24.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(action.title, color = White, fontWeight = FontWeight.Bold)
                                    Text(action.subtitle, color = Gray, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
