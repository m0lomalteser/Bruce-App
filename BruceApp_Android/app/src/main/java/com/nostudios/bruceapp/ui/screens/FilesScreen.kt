package com.nostudios.bruceapp.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nostudios.bruceapp.ble.BruceFileItem
import com.nostudios.bruceapp.data.model.BruceDevice
import com.nostudios.bruceapp.ui.theme.*
import com.nostudios.bruceapp.viewmodel.BruceViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    viewModel: BruceViewModel,
    savedDevices: List<BruceDevice>
) {
    val remoteFiles by viewModel.bleManager.remoteFiles.collectAsState()
    val isLoading by viewModel.bleManager.isFileFolderLoading.collectAsState()
    val isDownloading by viewModel.bleManager.isDownloading.collectAsState()
    val downloadingFileName by viewModel.bleManager.downloadingFileName.collectAsState()
    val lastDownloadedFilePath by viewModel.bleManager.lastDownloadedFilePath.collectAsState()

    var currentPath by remember { mutableStateOf("/") }
    var selectedDeviceId by remember { mutableStateOf<String?>(null) }
    var showCreateModal by remember { mutableStateOf(false) }
    var isCreatingFolder by remember { mutableStateOf(true) }
    var newItemName by remember { mutableStateOf("") }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { uploadLocalFile(it, currentPath, viewModel, context) }
    }

    LaunchedEffect(savedDevices) {
        if (selectedDeviceId == null && savedDevices.isNotEmpty()) {
            selectedDeviceId = savedDevices.first().id
            viewModel.bleManager.selectActiveDevice(savedDevices.first().id)
            refreshDirectory(currentPath, viewModel)
        }
    }

    val activeDeviceName = savedDevices.firstOrNull { it.id == selectedDeviceId }?.name ?: "SELECT HW"

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text("File Manager", style = MaterialTheme.typography.bodyMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black, titleContentColor = White)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    var menuExpanded by remember { mutableStateOf(false) }

                    Box {
                        Button(
                            onClick = { menuExpanded = true },
                            colors = ButtonDefaults.buttonColors(containerColor = White, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(activeDeviceName, fontWeight = FontWeight.Bold)
                            Icon(Icons.Filled.ArrowDropDown, "Select", modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            savedDevices.forEach { device ->
                                DropdownMenuItem(
                                    text = {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(device.name)
                                            if (selectedDeviceId == device.id) Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp))
                                        }
                                    },
                                    onClick = {
                                        selectedDeviceId = device.id
                                        viewModel.bleManager.selectActiveDevice(device.id)
                                        currentPath = "/"
                                        refreshDirectory(currentPath, viewModel)
                                        menuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Text(":", color = Gray)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Filled.Folder, null, tint = WhiteOp70, modifier = Modifier.size(14.dp))
                        Text(currentPath, color = WhiteOp70, maxLines = 1)
                    }

                    Spacer(Modifier.weight(1f))

                    if (currentPath != "/") {
                        IconButton(onClick = {
                            currentPath = goUpOneDirectory(currentPath)
                            refreshDirectory(currentPath, viewModel)
                        }) {
                            Icon(Icons.Filled.ArrowBack, "Up", tint = White)
                        }
                    }
                }

                // File list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (remoteFiles.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(color = White)
                                    Text("Reading storage over BLE...", color = Gray)
                                } else {
                                    Icon(Icons.Filled.Inbox, null, tint = Gray, modifier = Modifier.size(40.dp))
                                    Text("Directory empty or offline", color = Gray)
                                }
                            }
                        }
                    } else {
                        items(remoteFiles) { item ->
                            FileItemRow(
                                item = item,
                                onClick = {
                                    if (item.isDirectory) {
                                        currentPath = if (currentPath == "/") "/${item.name}" else "$currentPath/${item.name}"
                                        refreshDirectory(currentPath, viewModel)
                                    }
                                },
                                onDownload = {
                                    val targetPath = if (currentPath == "/") "/${item.name}" else "$currentPath/${item.name}"
                                    viewModel.bleManager.sendCommand("FS_DOWNLOAD_START $targetPath")
                                },
                                onDelete = {
                                    val targetPath = if (currentPath == "/") "/${item.name}" else "$currentPath/${item.name}"
                                    viewModel.bleManager.sendCommand("FS_REMOVE $targetPath")
                                    refreshDirectory(currentPath, viewModel)
                                }
                            )
                        }
                    }
                }

                // Action bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { isCreatingFolder = true; newItemName = ""; showCreateModal = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CardDark)
                    ) {
                        Icon(Icons.Filled.CreateNewFolder, null)
                        Text("FOLDER", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { filePickerLauncher.launch("*/*") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = White, contentColor = Color.Black)
                    ) {
                        Icon(Icons.Filled.Upload, null)
                        Text("UPLOAD", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Download overlay
            if (isDownloading) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = CardDark),
                        modifier = Modifier.padding(30.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            CircularProgressIndicator(color = White)
                            Text("DOWNLOADING...", color = White, fontWeight = FontWeight.Bold)
                            Text(downloadingFileName, color = Gray)
                        }
                    }
                }
            }

            // Create folder/file modal
            if (showCreateModal) {
                Box(
                    modifier = Modifier.fillMaxSize().background(BlackOp60),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = CardDark),
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OutlinedTextField(
                                value = newItemName,
                                onValueChange = { newItemName = it },
                                label = { Text(if (isCreatingFolder) "FOLDER NAME" else "FILE NAME") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = White,
                                    unfocusedBorderColor = WhiteOp30,
                                    focusedTextColor = White,
                                    unfocusedTextColor = White
                                ),
                                singleLine = true
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TextButton(onClick = { showCreateModal = false }) { Text("CANCEL", color = Gray) }
                                Button(
                                    onClick = {
                                        if (newItemName.isNotBlank()) {
                                            val fullPath = if (currentPath == "/") "/$newItemName" else "$currentPath/$newItemName"
                                            viewModel.bleManager.sendCommand(
                                                if (isCreatingFolder) "FS_MKDIR $fullPath" else "FS_CREATE $fullPath"
                                            )
                                            newItemName = ""
                                            showCreateModal = false
                                            refreshDirectory(currentPath, viewModel)
                                        }
                                    },
                                    enabled = newItemName.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = White, contentColor = Color.Black)
                                ) {
                                    Text("CREATE", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileItemRow(
    item: BruceFileItem,
    onClick: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                if (item.isDirectory) Icons.Filled.Folder else Icons.Filled.Description,
                null,
                tint = White
            )
            Column(Modifier.weight(1f)) {
                Text(item.name, color = White, fontWeight = FontWeight.SemiBold, maxLines = 1)
                item.size?.let { Text(it, color = Gray) }
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Filled.MoreVert, "Options", tint = White)
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    if (!item.isDirectory) {
                        DropdownMenuItem(text = { Text("Download") }, onClick = { showMenu = false; onDownload() })
                    }
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { showMenu = false; onDelete() })
                }
            }
        }
    }
}

private fun refreshDirectory(path: String, viewModel: BruceViewModel) {
    viewModel.bleManager.let { manager ->
        manager.sendCommand("FS_LIST $path")
    }
}

private fun goUpOneDirectory(path: String): String {
    val components = path.split("/").filter { it.isNotEmpty() }
    return if (components.size <= 1) "/"
    else "/${components.dropLast(1).joinToString("/")}"
}

private fun uploadLocalFile(uri: Uri, currentPath: String, viewModel: BruceViewModel, context: android.content.Context) {
    val inputStream = try {
        context.contentResolver?.openInputStream(uri)
    } catch (_: Exception) { null }
    if (inputStream == null) return
    val fileData = inputStream.readBytes()
    inputStream.close()
    val fileName = uri.lastPathSegment ?: "unknown"
    val targetPath = if (currentPath == "/") "/$fileName" else "$currentPath/$fileName"
    val hexString = fileData.joinToString("") { "%02x".format(it) }
    viewModel.bleManager.sendCommand("FS_UPLOAD_START $targetPath ${fileData.size}")
    val chunkSize = 400
    var index = 0
    while (index < hexString.length) {
        val end = minOf(index + chunkSize, hexString.length)
        viewModel.bleManager.sendCommand("FS_UPLOAD_CHUNK ${hexString.substring(index, end)}")
        index += chunkSize
    }
    viewModel.bleManager.sendCommand("FS_UPLOAD_END")
}
