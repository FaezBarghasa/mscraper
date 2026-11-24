package com.example.ui.equalizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.GlassCard
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.DeepVoid
import com.example.ui.theme.HoloBg
import com.example.ui.theme.TextGray
import com.example.viewmodel.MusicViewModel

@Composable
fun EqualizerScreen(viewModel: MusicViewModel = viewModel()) {
    val accentColor by viewModel.themePrimary.collectAsState()
    val secColor by viewModel.themeSecondary.collectAsState()
    val eqEnabled by viewModel.equalizerEnabled.collectAsState()
    val eqPreset by viewModel.equalizerPreset.collectAsState()
    val eqBands by viewModel.equalizerBands.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepVoid)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("DSP EQUALIZER", style = MaterialTheme.typography.headlineMedium, color = accentColor, fontWeight = FontWeight.Bold)
            Switch(
                checked = eqEnabled,
                onCheckedChange = { viewModel.setEqualizerEnabled(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = accentColor,
                    checkedTrackColor = accentColor.copy(alpha = 0.5f),
                    uncheckedThumbColor = TextGray,
                    uncheckedTrackColor = HoloBg
                )
            )
        }
        Text("Modify audio output signals across 5 custom neon bands", style = MaterialTheme.typography.labelSmall, color = TextGray)
        
        Spacer(modifier = Modifier.height(24.dp))

        // Interactive Bezier Curve Graph
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                val gridAlpha = 0.15f
                val count = 5
                val w = size.width
                val h = size.height
                
                for (i in 0 until count) {
                    val x = w * i / (count - 1)
                    drawLine(
                        color = Color.White.copy(alpha = gridAlpha),
                        start = Offset(x, 0f),
                        end = Offset(x, h),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                
                drawLine(
                    color = Color.White.copy(alpha = 0.3f),
                    start = Offset(0f, h / 2),
                    end = Offset(w, h / 2),
                    strokeWidth = 1.dp.toPx()
                )

                if (eqEnabled) {
                    val points = eqBands.mapIndexed { index, dbValue ->
                        val x = w * index / (count - 1)
                        val fraction = (dbValue + 10f) / 20f
                        val y = h - (fraction * h)
                        Offset(x, y)
                    }

                    val path = Path().apply {
                        if (points.isNotEmpty()) {
                            moveTo(points[0].x, points[0].y)
                            for (i in 0 until points.size - 1) {
                                val p0 = points[i]
                                val p1 = points[i+1]
                                val controlX = (p0.x + p1.x) / 2
                                cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                            }
                        }
                    }

                    val fillPath = Path().apply {
                        addPath(path)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(accentColor.copy(alpha = 0.3f), Color.Transparent)
                        )
                    )

                    drawPath(
                        path = path,
                        color = accentColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    points.forEach { pt ->
                        drawCircle(
                            color = secColor,
                            radius = 6.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Preset Chips
        Text("PRESETS", style = MaterialTheme.typography.labelSmall, color = TextGray)
        Spacer(modifier = Modifier.height(8.dp))
        val presets = listOf("Flat", "Bass Boost", "Cyberpunk", "Vocal Focus", "Treble+")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { preset ->
                val isSelected = eqPreset.equals(preset, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .background(
                            if (isSelected) accentColor else HoloBg,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { viewModel.setEqualizerPreset(preset) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        preset,
                        color = if (isSelected) Color.Black else Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Native Vertical EQ Faders
        val bandFreqs = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")
        Row(
            modifier = Modifier.fillMaxWidth().height(240.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bandFreqs.forEachIndexed { index, freq ->
                val currentDb = eqBands.getOrElse(index) { 0f }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                ) {
                    Text(
                        text = if (currentDb > 0) "+${currentDb.toInt()}dB" else "${currentDb.toInt()}dB",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (eqEnabled) accentColor else TextGray,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    VerticalEqFader(
                        value = currentDb,
                        onValueChange = { newVal ->
                            if (eqEnabled) {
                                viewModel.setEqualizerBand(index, newVal)
                            }
                        },
                        enabled = eqEnabled,
                        accentColor = accentColor,
                        modifier = Modifier
                            .weight(1f)
                            .width(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(freq, style = MaterialTheme.typography.labelSmall, color = TextGray, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Interactive Rotary Knobs
        var bassBoost by remember { mutableFloatStateOf(50f) }
        var virtualizer by remember { mutableFloatStateOf(30f) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            InteractiveRotaryKnob(
                label = "BASS AMPLIFIER",
                value = bassBoost,
                onValueChange = { bassBoost = it },
                accentColor = accentColor
            )
            InteractiveRotaryKnob(
                label = "SURROUND MATRIX",
                value = virtualizer,
                onValueChange = { virtualizer = it },
                accentColor = CyberCyan
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun VerticalEqFader(
    value: Float,
    onValueChange: (Float) -> Unit,
    enabled: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val clamped = value.coerceIn(-10f, 10f)

    Box(
        modifier = modifier
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    val fraction = (1f - (offset.y / size.height.toFloat())).coerceIn(0f, 1f)
                    val newVal = -10f + fraction * 20f
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onValueChange(newVal)
                }
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectVerticalDragGestures { change, _ ->
                    change.consume()
                    val fraction = (1f - (change.position.y / size.height.toFloat())).coerceIn(0f, 1f)
                    val newVal = -10f + fraction * 20f
                    onValueChange(newVal)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(vertical = 8.dp)) {
            val trackWidth = 6.dp.toPx()
            val centerX = size.width / 2f
            val trackHeight = size.height
            val midY = trackHeight / 2f

            // Background rail
            drawRoundRect(
                color = HoloBg,
                topLeft = Offset(centerX - trackWidth / 2f, 0f),
                size = Size(trackWidth, trackHeight),
                cornerRadius = CornerRadius(trackWidth / 2f, trackWidth / 2f)
            )

            // Normalized [0..1] where 0 is bottom (-10dB) and 1 is top (+10dB)
            val norm = (clamped + 10f) / 20f
            val thumbY = trackHeight * (1f - norm)

            val activeColor = if (enabled) accentColor else TextGray.copy(alpha = 0.4f)
            val barTop = minOf(midY, thumbY)
            val barHeight = kotlin.math.abs(thumbY - midY)

            drawRoundRect(
                color = activeColor.copy(alpha = 0.85f),
                topLeft = Offset(centerX - trackWidth / 2f, barTop),
                size = Size(trackWidth, barHeight),
                cornerRadius = CornerRadius(trackWidth / 2f, trackWidth / 2f)
            )

            // Center 0dB tick
            drawLine(
                color = Color.White.copy(alpha = 0.35f),
                start = Offset(centerX - 10.dp.toPx(), midY),
                end = Offset(centerX + 10.dp.toPx(), midY),
                strokeWidth = 1.5.dp.toPx()
            )

            // Thumb cap
            drawCircle(
                color = activeColor,
                radius = 10.dp.toPx(),
                center = Offset(centerX, thumbY)
            )
            drawCircle(
                color = DeepVoid,
                radius = 4.dp.toPx(),
                center = Offset(centerX, thumbY)
            )
        }
    }
}

@Composable
private fun InteractiveRotaryKnob(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    accentColor: Color
) {
    val haptic = LocalHapticFeedback.current
    var currentVal by remember(value) { mutableFloatStateOf(value) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextGray)
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(HoloBg, CircleShape)
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragStart = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        currentVal = (currentVal - dragAmount * 0.4f).coerceIn(0f, 100f)
                        onValueChange(currentVal)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                drawArc(
                    color = Color.White.copy(alpha = 0.1f),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = accentColor,
                    startAngle = 135f,
                    sweepAngle = 270f * (value / 100f),
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Text(
                "${value.toInt()}%",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text("DRAG VERTICALLY", style = MaterialTheme.typography.labelSmall, color = TextGray.copy(alpha = 0.6f), fontSize = 9.sp)
    }
}
