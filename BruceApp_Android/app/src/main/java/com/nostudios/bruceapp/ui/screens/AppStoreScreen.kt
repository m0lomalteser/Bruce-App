package com.nostudios.bruceapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nostudios.bruceapp.data.model.AppScript
import com.nostudios.bruceapp.data.model.StoreCategory
import com.nostudios.bruceapp.store.StoreViewModel
import com.nostudios.bruceapp.ui.theme.*
import com.nostudios.bruceapp.viewmodel.BruceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppStoreScreen(
    viewModel: BruceViewModel,
    storeViewModel: StoreViewModel
) {
    val categories by storeViewModel.categories.collectAsState()
    val isLoadingCategories by storeViewModel.isLoadingCategories.collectAsState()
    val popupMessage by storeViewModel.popupMessage.collectAsState()
    val isInstalling by storeViewModel.isInstalling.collectAsState()
    val installationProgress by storeViewModel.installationProgress.collectAsState()

    LaunchedEffect(Unit) {
        storeViewModel.fetchCategories()
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text("Bruce App Store", style = MaterialTheme.typography.bodyMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black, titleContentColor = White)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoadingCategories) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Loading Categories...", color = White)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    items(categories) { category ->
                        var showScripts by remember { mutableStateOf(false) }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showScripts = true },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardDark)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        category.displayName,
                                        color = if (category.displayName == "Updates") Orange else White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "${category.displayCount} App(s)",
                                        color = WhiteOp70
                                    )
                                }
                                Icon(Icons.Filled.ChevronRight, null, tint = WhiteOp50)
                            }
                        }

                        if (showScripts) {
                            ScriptListView(
                                category = category,
                                storeViewModel = storeViewModel,
                                viewModel = viewModel,
                                onDismiss = { showScripts = false }
                            )
                        }
                    }
                }
            }

            popupMessage?.let { message ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = CardDark),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(message, color = White)
                            if (isInstalling) {
                                LinearProgressIndicator(
                                    progress = { installationProgress.toFloat() },
                                    modifier = Modifier.width(180.dp),
                                    color = White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScriptListView(
    category: StoreCategory,
    storeViewModel: StoreViewModel,
    viewModel: BruceViewModel,
    onDismiss: () -> Unit
) {
    val availableScripts by storeViewModel.availableScripts.collectAsState()
    val isLoadingScripts by storeViewModel.isLoadingScripts.collectAsState()
    var selectedScript by remember { mutableStateOf<AppScript?>(null) }

    LaunchedEffect(category.slug) {
        storeViewModel.fetchAppsAndScanStorage(category, viewModel.bleManager)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(category.displayName, color = White) },
        text = {
            if (isLoadingScripts) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Scaning SD-Card...", color = White)
                }
            } else if (availableScripts.isEmpty()) {
                Text("No Apps in this Categorie.", color = White)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(availableScripts) { script ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { selectedScript = script },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardDark)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(script.name, color = White, fontWeight = FontWeight.Bold)
                                    ScriptStatusBadge(script)
                                }
                                Text(script.description, color = Gray, maxLines = 3, overflow = TextOverflow.Ellipsis)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Version: ${script.version}", color = Gray)
                                    if (script.installedVersion != null) {
                                        Text("Installed", color = Green)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = White) }
        },
        containerColor = Background
    )

    selectedScript?.let { script ->
        AlertDialog(
            onDismissRequest = { selectedScript = null },
            title = { Text(script.name, color = White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (script.installedVersion != null) {
                        Button(
                            onClick = {
                                storeViewModel.installApp(script, category.slug ?: "generic", viewModel.bleManager)
                                selectedScript = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = White, contentColor = Color.Black)
                        ) { Text("Update / Reinstall") }
                        Button(
                            onClick = {
                                storeViewModel.deleteApp(script, category.slug ?: "generic", viewModel.bleManager)
                                selectedScript = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Red, contentColor = White)
                        ) { Text("Delete") }
                    } else {
                        Button(
                            onClick = {
                                storeViewModel.installApp(script, category.slug ?: "generic", viewModel.bleManager)
                                selectedScript = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = White, contentColor = Color.Black)
                        ) { Text("Install") }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { selectedScript = null }) { Text("Cancel", color = White) } },
            containerColor = Background
        )
    }
}

@Composable
private fun ScriptStatusBadge(script: AppScript) {
    val installed = script.installedVersion
    if (installed != null) {
        if (installed != script.version) {
            Text(
                "UPDATE",
                color = White,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(Orange.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        } else {
            Icon(Icons.Filled.CheckCircle, null, tint = Green)
        }
    } else {
        Icon(Icons.Filled.CloudDownload, null, tint = White)
    }
}
