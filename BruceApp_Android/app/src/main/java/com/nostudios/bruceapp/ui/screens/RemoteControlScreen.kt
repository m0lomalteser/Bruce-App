package com.nostudios.bruceapp.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nostudios.bruceapp.ui.theme.*
import com.nostudios.bruceapp.viewmodel.BruceViewModel
import kotlinx.coroutines.delay

private data class TFTCommand(
    val type: CommandType
) {
    sealed class CommandType {
        data class FillScreen(val color: Color) : CommandType()
        data class FillRect(val x: Float, val y: Float, val w: Float, val h: Float, val color: Color) : CommandType()
        data class DrawFastHLine(val x: Float, val y: Float, val w: Float, val color: Color) : CommandType()
        data class DrawFastVLine(val x: Float, val y: Float, val h: Float, val color: Color) : CommandType()
        data class DrawCircle(val cx: Float, val cy: Float, val r: Float, val color: Color, val filled: Boolean) : CommandType()
        data class DrawTriangle(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val x3: Float, val y3: Float, val color: Color, val filled: Boolean) : CommandType()
        data class DrawEllipse(val cx: Float, val cy: Float, val rx: Float, val ry: Float, val color: Color, val filled: Boolean) : CommandType()
        data class DrawWideLine(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val w: Float, val color: Color) : CommandType()
        data class DrawSecondaryRoundRect(val x: Float, val y: Float, val w: Float, val h: Float, val r: Float, val color: Color, val filled: Boolean) : CommandType()
        data class DrawArc(val cx: Float, val cy: Float, val r: Float, val sa: Float, val ea: Float, val lineWidth: Float, val color: Color) : CommandType()
        data class DrawString(val text: String, val x: Float, val y: Float, val color: Color, val fontSize: Float, val xOffset: Float, val bgColor: Color?) : CommandType()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteControlScreen(
    viewModel: BruceViewModel
) {
    val rawScreenData by viewModel.bleManager.rawScreenData.collectAsState()
    var drawCommands by remember { mutableStateOf<List<TFTCommand>>(emptyList()) }
    var canvasWidth by remember { mutableStateOf(240f) }
    var canvasHeight by remember { mutableStateOf(135f) }
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp
    val textMeasurer = rememberTextMeasurer()

    LaunchedEffect(Unit) {
        viewModel.bleManager.sendCommand("START_BLE_SCREEN")
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.bleManager.sendCommand("STOP_BLE_SCREEN")
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30)
            parseBinaryStream(rawScreenData)?.let { parsed ->
                drawCommands = parsed
                viewModel.bleManager.sendCommand("SCREEN_KEEP_ALIVE")
            }
        }
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            if (!isLandscape) {
                TopAppBar(
                    title = { Text("Live Control", style = MaterialTheme.typography.bodyMedium) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black, titleContentColor = White)
                )
            }
        }
    ) { padding ->
        if (isLandscape) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    renderCommands(drawCommands, size.width, size.height, canvasWidth, canvasHeight, textMeasurer)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(Color.Black),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.weight(0.5f))

                Box(
                    modifier = Modifier
                        .size(width = 300.dp, height = 170.dp)
                        .padding(8.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        renderCommands(drawCommands, size.width, size.height, canvasWidth, canvasHeight, textMeasurer)
                    }
                }

                Spacer(Modifier.weight(0.5f))

                DPadControl(onCommand = { viewModel.bleManager.sendCommand(it) })

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun DPadControl(onCommand: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp)) {
        ControlButton(icon = "\u25B2", onClick = { onCommand("BTN_UP") })
        Row(horizontalArrangement = Arrangement.spacedBy(40.dp)) {
            ControlButton(icon = "\u25C0", onClick = { onCommand("BTN_LEFT") })
            ControlButton(icon = "\u25CF", isAction = true, onClick = { onCommand("BTN_ACTION") })
            ControlButton(icon = "\u25B6", onClick = { onCommand("BTN_RIGHT") })
        }
        ControlButton(icon = "\u25BC", onClick = { onCommand("BTN_DOWN") })
    }
}

@Composable
private fun ControlButton(icon: String, isAction: Boolean = false, onClick: () -> Unit) {
    val bgColor = if (isAction) Color.White else CardDark
    val contentColor = if (isAction) Color.Black else White
    FilledIconButton(
        onClick = onClick,
        modifier = Modifier.size(80.dp),
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(containerColor = bgColor)
    ) {
        Text(icon, color = contentColor, fontWeight = FontWeight.Bold)
    }
}

private fun parseBinaryStream(data: ByteArray): List<TFTCommand>? {
    if (data.isEmpty()) return null
    val bytes = data.toList()
    val commands = mutableListOf<TFTCommand>()
    var offset = 0
    var w = 240f
    var h = 135f
    var dimsUpdated = false

    while (offset < bytes.size) {
        if (bytes[offset].toInt() and 0xFF != 0xAA) { offset++; continue }
        if (offset + 2 >= bytes.size) break
        val size = bytes[offset + 1].toInt() and 0xFF
        if (size <= 0) { offset++; continue }
        val fn = bytes[offset + 2].toInt() and 0xFF
        if (offset + size > bytes.size) break
        val start = offset + 3

        when (fn) {
            99 -> {
                val rw = readInt16(bytes, start)
                val rh = readInt16(bytes, start + 2)
                if (rw > 0 && rh > 0) {
                    w = rw.toFloat()
                    h = rh.toFloat()
                    dimsUpdated = true
                }
            }
            0 -> {
                val color = color565(readInt16(bytes, start))
                commands.add(TFTCommand(TFTCommand.CommandType.FillScreen(color)))
            }
            1, 2 -> {
                val x = readInt16(bytes, start).toFloat()
                val y = readInt16(bytes, start + 2).toFloat()
                val rw = readInt16(bytes, start + 4).toFloat()
                val rh = readInt16(bytes, start + 6).toFloat()
                val color = color565(readInt16(bytes, start + 8))
                commands.add(TFTCommand(TFTCommand.CommandType.FillRect(x, y, rw, rh, color)))
            }
            3, 4 -> {
                val x = readInt16(bytes, start).toFloat()
                val y = readInt16(bytes, start + 2).toFloat()
                val rw = readInt16(bytes, start + 4).toFloat()
                val rh = readInt16(bytes, start + 6).toFloat()
                val r = readInt16(bytes, start + 8).toFloat()
                val color = color565(readInt16(bytes, start + 10))
                commands.add(TFTCommand(TFTCommand.CommandType.DrawSecondaryRoundRect(x, y, rw, rh, r, color, fn == 4)))
            }
            5, 6 -> {
                val cx = readInt16(bytes, start).toFloat()
                val cy = readInt16(bytes, start + 2).toFloat()
                val r = readInt16(bytes, start + 4).toFloat()
                val color = color565(readInt16(bytes, start + 6))
                commands.add(TFTCommand(TFTCommand.CommandType.DrawCircle(cx, cy, r, color, fn == 6)))
            }
            7, 8 -> {
                val x1 = readInt16(bytes, start).toFloat()
                val y1 = readInt16(bytes, start + 2).toFloat()
                val x2 = readInt16(bytes, start + 4).toFloat()
                val y2 = readInt16(bytes, start + 6).toFloat()
                val x3 = readInt16(bytes, start + 8).toFloat()
                val y3 = readInt16(bytes, start + 10).toFloat()
                val color = color565(readInt16(bytes, start + 12))
                commands.add(TFTCommand(TFTCommand.CommandType.DrawTriangle(x1, y1, x2, y2, x3, y3, color, fn == 8)))
            }
            9, 10 -> {
                val cx = readInt16(bytes, start).toFloat()
                val cy = readInt16(bytes, start + 2).toFloat()
                val rx = readInt16(bytes, start + 4).toFloat()
                val ry = readInt16(bytes, start + 6).toFloat()
                val color = color565(readInt16(bytes, start + 8))
                commands.add(TFTCommand(TFTCommand.CommandType.DrawEllipse(cx, cy, rx, ry, color, fn == 10)))
            }
            11 -> {
                val x1 = readInt16(bytes, start).toFloat()
                val y1 = readInt16(bytes, start + 2).toFloat()
                val x2 = readInt16(bytes, start + 4).toFloat()
                val y2 = readInt16(bytes, start + 6).toFloat()
                val color = color565(readInt16(bytes, start + 8))
                commands.add(TFTCommand(TFTCommand.CommandType.DrawWideLine(x1, y1, x2, y2, 1f, color)))
            }
            12 -> {
                if (start + 15 < bytes.size) {
                    val cx = readInt16(bytes, start).toFloat()
                    val cy = readInt16(bytes, start + 2).toFloat()
                    val r = readInt16(bytes, start + 4).toFloat()
                    val ir = readInt16(bytes, start + 6).toFloat()
                    val sa = readInt16(bytes, start + 8).toFloat()
                    val ea = readInt16(bytes, start + 10).toFloat()
                    val color = color565(readInt16(bytes, start + 12))
                    commands.add(TFTCommand(TFTCommand.CommandType.DrawArc(cx, cy, (r + ir) / 2f, sa + 90f, ea + 90f, maxOf(r - ir, 1f), color)))
                }
            }
            13 -> {
                val x1 = readInt16(bytes, start).toFloat()
                val y1 = readInt16(bytes, start + 2).toFloat()
                val x2 = readInt16(bytes, start + 4).toFloat()
                val y2 = readInt16(bytes, start + 6).toFloat()
                val w = readInt16(bytes, start + 8).toFloat()
                val color = color565(readInt16(bytes, start + 10))
                commands.add(TFTCommand(TFTCommand.CommandType.DrawWideLine(x1, y1, x2, y2, w, color)))
            }
            14, 15, 16, 17 -> {
                val x = readInt16(bytes, start).toFloat()
                val y = readInt16(bytes, start + 2).toFloat()
                val scale = readInt16(bytes, start + 4).toFloat()
                val fgRaw = readInt16(bytes, start + 6)
                val bgRaw = readInt16(bytes, start + 8)
                val fgColor = color565(fgRaw)
                val bgColor = if (bgRaw == fgRaw || bgRaw == 0) null else color565(bgRaw)
                val textLen = size - 13
                if (textLen > 0 && start + 10 + textLen <= bytes.size) {
                    val textBytes = bytes.subList(start + 10, start + 10 + textLen)
                    val filtered = textBytes.filter { it.toInt() in 32..126 }.toByteArray()
                    val text = String(filtered)
                    val fontW: Float = if (scale == 3f) 13.5f else if (scale == 2f) 9f else 4.5f
                    val offX: Float = if (fn == 14) (text.length * fontW) / 2f else if (fn == 15) text.length * fontW else 0f
                    if (bgColor != null) {
                        commands.add(TFTCommand(TFTCommand.CommandType.FillRect(x - offX, y, text.length * fontW, scale * 8f, bgColor)))
                    }
                    commands.add(TFTCommand(TFTCommand.CommandType.DrawString(text, x, y, fgColor, if (scale == 3f) 24f else if (scale == 2f) 16f else 12f, offX, bgColor)))
                }
            }
            19 -> {
                if (start + 5 < bytes.size) {
                    commands.add(TFTCommand(TFTCommand.CommandType.FillRect(readInt16(bytes, start).toFloat(), readInt16(bytes, start + 2).toFloat(), 1f, 1f, color565(readInt16(bytes, start + 4)))))
                }
            }
            20 -> {
                val x = readInt16(bytes, start).toFloat()
                val y = readInt16(bytes, start + 2).toFloat()
                val rh = readInt16(bytes, start + 4).toFloat()
                val color = color565(readInt16(bytes, start + 6))
                commands.add(TFTCommand(TFTCommand.CommandType.DrawFastVLine(x, y, rh, color)))
            }
            21 -> {
                val x = readInt16(bytes, start).toFloat()
                val y = readInt16(bytes, start + 2).toFloat()
                val rw = readInt16(bytes, start + 4).toFloat()
                val color = color565(readInt16(bytes, start + 6))
                commands.add(TFTCommand(TFTCommand.CommandType.DrawFastHLine(x, y, rw, color)))
            }
        }
        offset += size
    }

    return if (dimsUpdated) commands else commands
}

private fun DrawScope.renderCommands(
    commands: List<TFTCommand>,
    canvasW: Float,
    canvasH: Float,
    tftW: Float,
    tftH: Float,
    textMeasurer: androidx.compose.ui.text.TextMeasurer
) {
    val scaleX = canvasW / tftW
    val scaleY = canvasH / tftH

    for (cmd in commands) {
        when (val type = cmd.type) {
            is TFTCommand.CommandType.FillScreen -> {
                drawRect(color = type.color, size = Size(canvasW, canvasH))
            }
            is TFTCommand.CommandType.FillRect -> {
                drawRect(
                    color = type.color,
                    topLeft = Offset(type.x * scaleX, type.y * scaleY),
                    size = Size(type.w * scaleX, type.h * scaleY)
                )
            }
            is TFTCommand.CommandType.DrawFastHLine -> {
                drawLine(type.color, Offset(type.x * scaleX, type.y * scaleY), Offset((type.x + type.w) * scaleX, type.y * scaleY), strokeWidth = 1.5f)
            }
            is TFTCommand.CommandType.DrawFastVLine -> {
                drawLine(type.color, Offset(type.x * scaleX, type.y * scaleY), Offset(type.x * scaleX, (type.y + type.h) * scaleY), strokeWidth = 1.5f)
            }
            is TFTCommand.CommandType.DrawCircle -> {
                if (type.filled) {
                    drawCircle(type.color, type.r * scaleX, Offset(type.cx * scaleX, type.cy * scaleY))
                } else {
                    drawCircle(type.color, type.r * scaleX, Offset(type.cx * scaleX, type.cy * scaleY), style = Stroke(1.5f))
                }
            }
            is TFTCommand.CommandType.DrawTriangle -> {
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(type.x1 * scaleX, type.y1 * scaleY)
                    lineTo(type.x2 * scaleX, type.y2 * scaleY)
                    lineTo(type.x3 * scaleX, type.y3 * scaleY)
                    close()
                }
                if (type.filled) drawPath(path, type.color)
                else drawPath(path, type.color, style = Stroke(1.5f))
            }
            is TFTCommand.CommandType.DrawEllipse -> {
                if (type.filled) {
                    drawOval(type.color, Offset((type.cx - type.rx) * scaleX, (type.cy - type.ry) * scaleY), Size(type.rx * 2 * scaleX, type.ry * 2 * scaleY))
                } else {
                    drawOval(type.color, Offset((type.cx - type.rx) * scaleX, (type.cy - type.ry) * scaleY), Size(type.rx * 2 * scaleX, type.ry * 2 * scaleY), style = Stroke(1.5f))
                }
            }
            is TFTCommand.CommandType.DrawWideLine -> {
                drawLine(type.color, Offset(type.x1 * scaleX, type.y1 * scaleY), Offset(type.x2 * scaleX, type.y2 * scaleY), strokeWidth = type.w * scaleX)
            }
            is TFTCommand.CommandType.DrawSecondaryRoundRect -> {
                val rect = androidx.compose.ui.geometry.Rect(Offset(type.x * scaleX, type.y * scaleY), Size(type.w * scaleX, type.h * scaleY))
                val r = type.r * scaleX
                val path = androidx.compose.ui.graphics.Path().apply {
                    addRoundRect(androidx.compose.ui.geometry.RoundRect(rect, r, r))
                }
                if (type.filled) drawPath(path, type.color)
                else drawPath(path, type.color, style = Stroke(1.5f))
            }
            is TFTCommand.CommandType.DrawArc -> {
                val startAngleDeg = type.sa
                val sweepAngleDeg = type.ea - type.sa
                drawArc(color = type.color, startAngle = startAngleDeg, sweepAngle = sweepAngleDeg, useCenter = false, topLeft = Offset((type.cx - type.r) * scaleX, (type.cy - type.r) * scaleY), size = Size(type.r * 2 * scaleX, type.r * 2 * scaleY), style = Stroke(type.lineWidth * scaleX))
            }
            is TFTCommand.CommandType.DrawString -> {
                val fontSize = type.fontSize * scaleY
                val style = TextStyle(
                    color = type.color,
                    fontSize = fontSize.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                val result = textMeasurer.measure(type.text, style)
                drawText(
                    result,
                    topLeft = Offset((type.x - type.xOffset) * scaleX, type.y * scaleY)
                )
            }
        }
    }
}

private fun readInt16(bytes: List<Byte>, index: Int): Int {
    if (index + 1 >= bytes.size) return 0
    val u16 = ((bytes[index].toInt() and 0xFF) shl 8) or (bytes[index + 1].toInt() and 0xFF)
    return if (u16 and 0x8000 != 0) u16 - 0x10000 else u16
}

private fun color565(c: Int): Color {
    val r = ((c shr 11) and 0x1F) / 31.0f
    val g = ((c shr 5) and 0x3F) / 63.0f
    val b = (c and 0x1F) / 31.0f
    return Color(r, g, b)
}
