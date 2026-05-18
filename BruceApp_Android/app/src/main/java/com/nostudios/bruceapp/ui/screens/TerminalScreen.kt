package com.nostudios.bruceapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nostudios.bruceapp.ui.theme.*
import com.nostudios.bruceapp.viewmodel.BruceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    viewModel: BruceViewModel
) {
    val terminalOutput by viewModel.bleManager.terminalOutput.collectAsState()
    var command by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(terminalOutput.size) {
        if (terminalOutput.isNotEmpty()) {
            listState.animateScrollToItem(terminalOutput.size - 1)
        }
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text("Terminal", style = MaterialTheme.typography.bodyMedium) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black, titleContentColor = White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(terminalOutput) { _, line ->
                    Text(
                        text = line,
                        color = if (line.startsWith(">")) Gray else White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(UltraThinMaterial)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(">", color = Gray)
                TextField(
                    value = command,
                    onValueChange = { command = it },
                    placeholder = { Text("COMMAND", color = Gray) },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = White),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        cursorColor = White
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                IconButton(onClick = {
                    if (command.isNotBlank()) {
                        viewModel.bleManager.sendCommand(command)
                        command = ""
                    }
                }) {
                    Icon(Icons.Filled.KeyboardReturn, "Send", tint = White)
                }
            }
        }
    }
}
