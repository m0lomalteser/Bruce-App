package com.nostudios.bruceapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nostudios.bruceapp.R
import com.nostudios.bruceapp.data.model.BruceDevice
import com.nostudios.bruceapp.ui.theme.CardDark
import com.nostudios.bruceapp.ui.theme.Green
import com.nostudios.bruceapp.ui.theme.Red
import com.nostudios.bruceapp.ui.theme.WhiteOp70

@Composable
fun DeviceTile(
    device: BruceDevice,
    isConnected: Boolean,
    batteryLevel: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(24.dp))
            .background(CardDark)
            .then(Modifier.padding(4.dp)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            BatteryIcon(
                level = batteryLevel,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            )

            val imageResId = getDeviceImageId(device.imageName)
            Image(
                painter = painterResource(id = imageResId),
                contentDescription = device.name,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                contentScale = ContentScale.Fit
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isConnected) Green else Red)
            )
            Text(
                text = device.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun BatteryIcon(level: Int?, modifier: Modifier = Modifier) {
    if (level != null) {
        val icon = when {
            level > 80 -> Icons.Filled.BatteryFull
            level > 40 -> Icons.Filled.BatteryFull
            else -> Icons.Filled.BatteryFull
        }
        Icon(
            imageVector = icon,
            contentDescription = "Battery $level%",
            tint = WhiteOp70,
            modifier = modifier.size(16.dp)
        )
    } else {
        Box(modifier = modifier) {
            Icon(
                imageVector = Icons.Filled.BatteryFull,
                contentDescription = "Unknown battery",
                tint = WhiteOp70,
                modifier = Modifier.size(16.dp)
            )
            Icon(
                imageVector = Icons.Filled.QuestionMark,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier
                    .size(10.dp)
                    .align(Alignment.Center)
            )
        }
    }
}

private fun getDeviceImageId(imageName: String): Int {
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
