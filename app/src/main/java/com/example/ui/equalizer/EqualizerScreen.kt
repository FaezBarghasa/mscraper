package com.example.ui.equalizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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

        // Sliders
        val bandFreqs = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")
        Row(
            modifier = Modifier.fillMaxWidth().height(260.dp),
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
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier.weight(1f).width(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Slider(
                            value = currentDb,
                            onValueChange = { newVal ->
                                if (eqEnabled) {
                                    viewModel.setEqualizerBand(index, newVal)
                                }
                            },
                            valueRange = -10f..10f,
                            enabled = eqEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = accentColor,
                                activeTrackColor = accentColor,
                                inactiveTrackColor = HoloBg
                            ),
                            modifier = Modifier
                                .requiredWidth(160.dp)
                                .rotate(-90f)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(freq, style = MaterialTheme.typography.labelSmall, color = TextGray, fontSize = 11.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Bass Boost Knob
        var bassBoost by remember { mutableStateOf(50f) }
        var virtualizer by remember { mutableStateOf(30f) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("BASS AMPLIFIER", style = MaterialTheme.typography.labelSmall, color = TextGray)
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(HoloBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        drawArc(
                            color = HoloBg,
                            startAngle = 135f,
                            sweepAngle = 270f,
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = accentColor,
                            startAngle = 135f,
                            sweepAngle = 270f * (bassBoost / 100f),
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Text("${bassBoost.toInt()}%", style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = bassBoost,
                    onValueChange = { bassBoost = it },
                    valueRange = 0f..100f,
                    modifier = Modifier.width(100.dp),
                    colors = SliderDefaults.colors(thumbColor = accentColor, activeTrackColor = accentColor)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("SURROUND MATRIX", style = MaterialTheme.typography.labelSmall, color = TextGray)
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(HoloBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        drawArc(
                            color = HoloBg,
                            startAngle = 135f,
                            sweepAngle = 270f,
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = CyberCyan,
                            startAngle = 135f,
                            sweepAngle = 270f * (virtualizer / 100f),
                            useCenter = false,
                            style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Text("${virtualizer.toInt()}%", style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = virtualizer,
                    onValueChange = { virtualizer = it },
                    valueRange = 0f..100f,
                    modifier = Modifier.width(100.dp),
                    colors = SliderDefaults.colors(thumbColor = CyberCyan, activeTrackColor = CyberCyan)
                )
            }
        }
    }
}
