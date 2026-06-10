package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.CarState
import com.example.ui.theme.*
import com.example.ui.viewmodel.GameViewModel

@Composable
fun SpaScreen(viewModel: GameViewModel) {
    val playerState by viewModel.playerState.collectAsState()
    val cars by viewModel.allCars.collectAsState()
    val activeCar = cars.find { it.id == playerState?.activeCarId } ?: CarState("m3_e30", "BMW M3", 0, true, "#E31E24")

    val activeCarColor = try {
        Color(android.graphics.Color.parseColor(activeCar.colorHex))
    } catch (e: Exception) {
        NeonCyan
    }

    val currencySymbol = if (playerState?.activeLocation == "Germany") "€" else "$"
    val convertFactor = if (playerState?.activeLocation == "Germany") 0.9f else 1.0f

    // Spa Bath Hot Springs State
    var spaTempSlider by remember { mutableFloatStateOf(38.5f) }
    val isOptimalTemp = spaTempSlider in 37f..41f

    // Detailing Mud Spots
    val mudSpots by viewModel.mudPoints.collectAsState()
    val activeMusSpotsCount = mudSpots.count { it.isActive }
    val totalSpots = mudSpots.size
    val cleanPercentage = if (totalSpots == 0) 100 else ((totalSpots - activeMusSpotsCount).toFloat() / totalSpots * 100).toInt()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCarbon)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Module 1: Hot Springs Spa
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "♨️",
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "🧼 ONSEN SPA BATHHOUSE",
                        color = NeonCyan,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Breathe and recoup deep physical energy. Optimal human temperature zone is 37°C–41°C. Regulate slider precisely for double energy recovery!",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Thermostat Regulator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Spa Temp: ${String.format("%.1f", spaTempSlider)}°C",
                        color = if (isOptimalTemp) GrassGreen else MRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isOptimalTemp) {
                        Badge(containerColor = GrassGreen) {
                            Text("OPTIMAL (+45 Energy)", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                        }
                    } else {
                        Badge(containerColor = Color.DarkGray) {
                            Text("NORMAL (+20 Energy)", color = Color.White, fontSize = 9.sp, modifier = Modifier.padding(4.dp))
                        }
                    }
                }

                Slider(
                    value = spaTempSlider,
                    onValueChange = { spaTempSlider = it },
                    valueRange = 30f..50f,
                    colors = SliderDefaults.colors(
                        thumbColor = if (isOptimalTemp) GrassGreen else MRed,
                        activeTrackColor = if (isOptimalTemp) GrassGreen.copy(alpha = 0.4f) else MRed.copy(alpha = 0.4f)
                    )
                )

                Button(
                    onClick = { viewModel.takeBath(spaTempSlider, (18 * convertFactor).toInt()) },
                    enabled = (playerState?.money ?: 0) >= (18 * convertFactor).toInt(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "BATHE NOW ($currencySymbol${(18 * convertFactor).toInt()})",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Module 2: BMW Detailing & Polish Canvas Box
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, activeCarColor.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "✨",
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "BMW SHOWROOM DETAIL CELL",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Clean: $cleanPercentage%",
                        color = if (cleanPercentage == 100) GrassGreen else BrightGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Swipe off dark road mud spots to restore paint. Complete detailing repairs body damage and awards a $currencySymbol${(150 * convertFactor).toInt()} bonus!",
                    color = Color.Gray,
                    fontSize = 10.sp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Detailing Swipe canvas box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)), RoundedCornerShape(12.dp))
                        // Capture touch swipes
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                viewModel.scrubMudAt(change.position.x, change.position.y)
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                viewModel.scrubMudAt(offset.x, offset.y)
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // Draw glossy reflection background glow
                        drawCircle(
                            color = activeCarColor.copy(alpha = 0.15f),
                            radius = w * 0.4f,
                            center = Offset(w * 0.5f, h * 0.5f)
                        )

                        // Draw Car silhouette stencil profile (Top down car)
                        val shellPath = androidx.compose.ui.graphics.Path().apply {
                            val cx = w * 0.5f
                            val cy = h * 0.5f
                            val carW = w * 0.35f
                            val carH = h * 0.75f

                            moveTo(cx - carW * 0.2f, cy - carH * 0.5f) // top hood nose
                            quadraticTo(cx, cy - carH * 0.53f, cx + carW * 0.2f, cy - carH * 0.5f)
                            lineTo(cx + carW * 0.45f, cy - carH * 0.28f) // front fender right
                            lineTo(cx + carW * 0.45f, cy - carH * 0.05f) // cabin right side
                            lineTo(cx + carW * 0.52f, cy + carH * 0.10f) // rear fender right
                            lineTo(cx + carW * 0.45f, cy + carH * 0.38f) // trunk line right
                            quadraticTo(cx, cy + carH * 0.48f, cx - carW * 0.45f, cy + carH * 0.38f) // rear lip spoiler
                            lineTo(cx - carW * 0.52f, cy + carH * 0.10f) // rear fender left
                            lineTo(cx - carW * 0.45f, cy - carH * 0.05f) // cabin left side
                            lineTo(cx - carW * 0.45f, cy - carH * 0.28f) // front fender left
                            close()
                        }

                        // Outline of BMW Top down model
                        drawPath(
                            path = shellPath,
                            color = activeCarColor.copy(alpha = 0.85f),
                            style = Stroke(width = 4.dp.toPx())
                        )

                        // Draw headlights
                        val cx = w * 0.5f
                        val cy = h * 0.5f
                        val carW = w * 0.35f
                        val carH = h * 0.75f

                        drawCircle(color = NeonCyan, radius = 8f, center = Offset(cx - carW * 0.25f, cy - carH * 0.48f))
                        drawCircle(color = NeonCyan, radius = 8f, center = Offset(cx + carW * 0.25f, cy - carH * 0.48f))

                        // Draw windshield cabin lines
                        drawRect(
                            color = Color.DarkGray,
                            topLeft = Offset(cx - carW * 0.3f, cy - carH * 0.2f),
                            size = androidx.compose.ui.geometry.Size(carW * 0.6f, carH * 0.22f)
                        )

                        // Draw active dirty spots!
                        mudSpots.forEach { spot ->
                            if (spot.isActive) {
                                // Draw splattered brown mud patch
                                drawCircle(
                                    color = Color(0xFF5C4033).copy(alpha = 0.88f),
                                    radius = 22f,
                                    center = Offset(spot.x, spot.y)
                                )
                                drawCircle(
                                    color = Color(0xFF3D2B1F).copy(alpha = 0.9f),
                                    radius = 12f,
                                    center = Offset(spot.x + 4f, spot.y - 4f)
                                )
                            }
                        }
                    }

                    // Water splash effects
                    if (activeMusSpotsCount == 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(GrassGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "✨ SHOWROOM POLISH COMPLETE! (+150 Cash)",
                                color = GrassGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Active Vehicle: ${activeCar.name}",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                    TextButton(onClick = { viewModel.resetDetailingMud() }) {
                        Text("RESET MUD", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
