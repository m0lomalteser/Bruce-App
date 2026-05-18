package com.nostudios.bruceapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nostudios.bruceapp.R

val DotoFontFamily = FontFamily(
    Font(R.font.doto, FontWeight.Normal)
)

val BruceTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = DotoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = DotoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    titleLarge = TextStyle(
        fontFamily = DotoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    ),
    titleMedium = TextStyle(
        fontFamily = DotoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
    ),
    labelLarge = TextStyle(
        fontFamily = DotoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
    ),
    labelMedium = TextStyle(
        fontFamily = DotoFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    )
)
