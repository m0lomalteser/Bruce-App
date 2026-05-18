package com.nostudios.bruceapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nostudios.bruceapp.ble.BruceBLEManager
import com.nostudios.bruceapp.ble.ConnectionState
import com.nostudios.bruceapp.ui.theme.CardDark
import com.nostudios.bruceapp.ui.theme.White
import com.nostudios.bruceapp.ui.theme.WhiteOp70

@Composable
fun PinEntryOverlay(
    bleManager: BruceBLEManager,
    modifier: Modifier = Modifier
) {
    var pinInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = null,
            tint = White,
            modifier = Modifier.size(50.dp)
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Lock active", style = MaterialTheme.typography.bodyMedium, color = White)
            Text(
                "The Device shows a PIN for unlock.",
                style = MaterialTheme.typography.bodyMedium,
                color = WhiteOp70,
                textAlign = TextAlign.Center
            )
        }

        OutlinedTextField(
            value = pinInput,
            onValueChange = { pinInput = it },
            label = { Text("PIN") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Done),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = White
            ),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = White,
                unfocusedBorderColor = WhiteOp70,
                cursorColor = White
            ),
            modifier = Modifier.width(200.dp)
        )

        val isAuthenticating = bleManager.connectionState.value == ConnectionState.Authenticating
        Button(
            onClick = {
                if (pinInput.isNotEmpty()) bleManager.submitPin(pinInput)
            },
            enabled = pinInput.isNotEmpty() && !isAuthenticating,
            colors = ButtonDefaults.buttonColors(containerColor = White, contentColor = Color.Black),
            modifier = Modifier.width(160.dp)
        ) {
            Text(
                if (isAuthenticating) "CHECKING..." else "UNLOCK",
                fontWeight = FontWeight.Bold
            )
        }
    }
}
