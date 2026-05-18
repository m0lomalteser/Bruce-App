package com.nostudios.bruceapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nostudios.bruceapp.ui.theme.CardDark

fun Modifier.liquidGlass(
    tintColor: Color? = null,
    cornerRadius: Int = 16
): Modifier = this
    .padding(4.dp)
    .shadow(10.dp, RoundedCornerShape(cornerRadius.dp), ambientColor = Color.Black.copy(alpha = 0.3f), spotColor = Color.Black.copy(alpha = 0.3f))
    .background(
        brush = Brush.linearGradient(
            colors = listOf(
                CardDark.copy(alpha = 0.85f),
                CardDark.copy(alpha = 0.7f)
            )
        ),
        shape = RoundedCornerShape(cornerRadius.dp)
    )
    .clip(RoundedCornerShape(cornerRadius.dp))

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    tintColor: Color? = null,
    cornerRadius: Int = 16,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .padding(4.dp)
            .shadow(10.dp, RoundedCornerShape(cornerRadius.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        CardDark.copy(alpha = 0.85f),
                        CardDark.copy(alpha = 0.7f)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius.dp)
            )
            .clip(RoundedCornerShape(cornerRadius.dp))
            .then(
                if (tintColor != null) Modifier.background(tintColor.copy(alpha = 0.1f), RoundedCornerShape(cornerRadius.dp))
                else Modifier
            )
    ) {
        content()
    }
}
