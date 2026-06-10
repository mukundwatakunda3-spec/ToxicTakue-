package com.example.ui.screens

import android.view.KeyEvent
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.CarState
import com.example.utils.SoundSynth
import com.example.ui.theme.*
import com.example.ui.viewmodel.GameViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class RaceState {
    IDLE, PLAYING, CRASHED, FINISHED
}

data class RacingEntity(
    val id: Long,
    var xPercent: Float, // 0.15f to 0.85f
    var yPercent: Float, // 0f to 1f
    val speed: Float,
    val type: EntityType,
    val lane: Int,
    val color: Color = Color.Yellow
)

enum class EntityType {
    TRAFFIC_CAR, COIN, GAS_CAN
}

@Composable
fun RacingScreen(viewModel: GameViewModel) {
    val playerState by viewModel.playerState.collectAsState()
    val cars by viewModel.allCars.collectAsState()
    val activeCar = cars.find { it.id == playerState?.activeCarId } ?: CarState("m3_e30", "BMW M3 E30", 0, true, "#E31E24")

    val activeCarColor = try {
        Color(android.graphics.Color.parseColor(activeCar.colorHex))
    } catch (e: Exception) {
        NeonCyan
    }

    val currencySymbol = if (playerState?.activeLocation == "Germany") "€" else "$"

    // Game Variables
    var raceState by remember { mutableStateOf(RaceState.IDLE) }
    var score by remember { mutableStateOf(0) }
    var cashEarned by remember { mutableStateOf(0) }
    var milesTraveled by remember { mutableStateOf(0) }
    var gasolineReserve by remember { mutableStateOf(100f) }

    // Player position
    var playerXPercent by remember { mutableStateOf(0.5f) } // target middle lane
    var isNitroActive by remember { mutableStateOf(false) }

    // Screen Shake Offset (X, Y)
    var shakeX by remember { mutableStateOf(0f) }
    var shakeY by remember { mutableStateOf(0f) }

    // Entites (Oncoming cars, coins, fuel)
    val entities = remember { mutableStateListOf<RacingEntity>() }
    var entityIdCounter by remember { mutableLongStateOf(0L) }

    // Lanes lines scrolling offset
    var roadScrollOffset by remember { mutableStateOf(0f) }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // Request keyboard focus when playing
    LaunchedEffect(raceState) {
        if (raceState == RaceState.PLAYING) {
            focusRequester.requestFocus()
        }
    }

    // Engine sound synthesizer tick loop
    LaunchedEffect(raceState, playerXPercent, isNitroActive) {
        while (raceState == RaceState.PLAYING) {
            val baseFreq = if (isNitroActive) 350f else 180f + (playerXPercent * 120f)
            SoundSynth.playEngine(baseFreq, durationMs = 150)
            delay(140)
        }
    }

    // Main Game Update Tick Loop
    LaunchedEffect(raceState, isNitroActive, activeCar) {
        if (raceState == RaceState.PLAYING) {
            gasolineReserve = 100f
            score = 0
            cashEarned = 0
            milesTraveled = 0
            entities.clear()
            playerXPercent = 0.5f

            var loops = 0
            while (raceState == RaceState.PLAYING) {
                delay(20) // ~50 fps
                loops++

                // Update road scroll
                val speedMultiplier = if (isNitroActive) 3.2f else 1.5f + (activeCar.turboLevel * 0.3f)
                roadScrollOffset = (roadScrollOffset + 12f * speedMultiplier) % 100f

                // Consume fuel
                val fuelExpense = if (isNitroActive) 0.8f else 0.25f - (activeCar.turboLevel * 0.02f)
                gasolineReserve = (gasolineReserve - fuelExpense).coerceAtLeast(0f)

                if (gasolineReserve <= 0f) {
                    raceState = RaceState.FINISHED
                    viewModel.earnMoneyAndDrive(milesTraveled, cashEarned, 0)
                    break
                }

                // Increment distance
                if (loops % 15 == 0) {
                    milesTraveled += if (isNitroActive) 2 else 1
                    score += if (isNitroActive) 20 else 10
                    cashEarned += if (isNitroActive) 35 else 15
                }

                // Spawn Entities
                if (loops % 45 == 0) {
                    val lane = (0..2).random()
                    val laneXPercent = 0.25f + lane * 0.25f
                    val entityType = if (Math.random() < 0.6) {
                        EntityType.TRAFFIC_CAR
                    } else if (Math.random() < 0.75) {
                        EntityType.COIN
                    } else {
                        EntityType.GAS_CAN
                    }

                    val color = when (entityType) {
                        EntityType.TRAFFIC_CAR -> listOf(Color.Red, Color.White, Color.Blue, Color.Gray).random()
                        EntityType.COIN -> BrightGold
                        EntityType.GAS_CAN -> MRed
                    }

                    entities.add(
                        RacingEntity(
                            id = entityIdCounter++,
                            xPercent = laneXPercent,
                            yPercent = -0.1f,
                            speed = 0.015f + (0.003f * activeCar.turboLevel.toFloat()),
                            type = entityType,
                            lane = lane,
                            color = color
                        )
                    )
                }

                // Move Entities & Collision check
                val iterator = entities.iterator()
                while (iterator.hasNext()) {
                    val entity = iterator.next()
                    val entitySpeedModifier = if (isNitroActive) 2.5f else 1.2f
                    entity.yPercent += entity.speed * entitySpeedModifier

                    // Collision bounds check (player is around yPercent = 0.80f)
                    val isNearY = entity.yPercent in 0.75f..0.85f
                    val isNearX = kotlin.math.abs(entity.xPercent - playerXPercent) < 0.12f

                    if (isNearY && isNearX) {
                        // Process interaction
                        when (entity.type) {
                            EntityType.TRAFFIC_CAR -> {
                                // Screen shake trigger!
                                SoundSynth.playCrash()
                                shakeX = (-15..15).random().toFloat()
                                shakeY = (-15..15).random().toFloat()
                                delay(40)
                                shakeX = 0f
                                shakeY = 0f

                                // Take dynamic car damage, reduce by tyres/handling level!
                                val damageVal = (25 - activeCar.tyresLevel * 3).coerceIn(8, 25)
                                raceState = RaceState.CRASHED
                                viewModel.earnMoneyAndDrive(milesTraveled, cashEarned, damageVal)
                                iterator.remove()
                                break
                            }
                            EntityType.COIN -> {
                                SoundSynth.playCoin()
                                cashEarned += 120
                                score += 100
                                iterator.remove()
                            }
                            EntityType.GAS_CAN -> {
                                SoundSynth.playSplash()
                                gasolineReserve = (gasolineReserve + 25f).coerceAtMost(100f)
                                iterator.remove()
                            }
                        }
                    } else if (entity.yPercent > 1.1f) {
                        iterator.remove() // cleanup off-screen elements
                    }
                }
            }
        }
    }

    // Outer Layout Frame
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCarbon)
            .padding(12.dp)
            .focusable()
            .focusRequester(focusRequester)
            .onKeyEvent { keyEvent ->
                if (raceState == RaceState.PLAYING) {
                    when (keyEvent.nativeKeyEvent.keyCode) {
                        KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_DPAD_LEFT -> {
                            playerXPercent = (playerXPercent - 0.06f).coerceIn(0.20f, 0.80f)
                            true
                        }
                        KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            playerXPercent = (playerXPercent + 0.06f).coerceIn(0.20f, 0.80f)
                            true
                        }
                        KeyEvent.KEYCODE_SPACE -> {
                            isNitroActive = true
                            SoundSynth.playNitro()
                            true
                        }
                        else -> false
                    }
                } else false
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Telemetry Panel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "🚦 ${playerState?.activeLocation?.uppercase()} STREET HIGHWAY",
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "ACTIVE: ${activeCar.name}",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            // Real-time currency balance
            Text(
                text = "$currencySymbol${playerState?.money ?: 0}",
                color = BrightGold,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // Live stats panel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(DarkSurface)
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            StatTelemetry(label = "MILES", valStr = "$milesTraveled mi")
            StatTelemetry(label = "REWARD", valStr = "$currencySymbol$cashEarned")
            StatTelemetry(label = "SCORE", valStr = "$score pts")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Highway Canvas Window Frame
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .offset(x = shakeX.dp, y = shakeY.dp) // screen shake crash transformation!
                .background(Color(0xFF101317))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasW = size.width
                val canvasH = size.height

                // 1. Draw highway tarmac lanes (3 lanes structure)
                val leftBoundaryX = canvasW * 0.15f
                val rightBoundaryX = canvasW * 0.85f
                val laneWidth = (rightBoundaryX - leftBoundaryX) / 3f

                // Draw tarmac asphalt
                drawRect(
                    color = Color(0xFF1A1F26),
                    size = size
                )

                // Draw boundaries
                drawLine(
                    color = Color.White.copy(alpha = 0.6f),
                    start = Offset(leftBoundaryX, 0f),
                    end = Offset(leftBoundaryX, canvasH),
                    strokeWidth = 6f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.6f),
                    start = Offset(rightBoundaryX, 0f),
                    end = Offset(rightBoundaryX, canvasH),
                    strokeWidth = 6f
                )

                // Draw dashed lane strips (moving)
                val segmentLength = 40f
                val gapLength = 30f
                val totalCycle = segmentLength + gapLength
                var currentY = roadScrollOffset - totalCycle

                while (currentY < canvasH) {
                    // Line separator 1
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.5f),
                        start = Offset(leftBoundaryX + laneWidth, currentY),
                        end = Offset(leftBoundaryX + laneWidth, currentY + segmentLength),
                        strokeWidth = 3f
                    )
                    // Line separator 2
                    drawLine(
                        color = Color.LightGray.copy(alpha = 0.5f),
                        start = Offset(leftBoundaryX + 2f * laneWidth, currentY),
                        end = Offset(leftBoundaryX + 2f * laneWidth, currentY + segmentLength),
                        strokeWidth = 3f
                    )
                    currentY += totalCycle
                }

                // 2. Draw Entities (Traffic, Coins, fuel)
                entities.forEach { entity ->
                    val ex = leftBoundaryX + (entity.xPercent - 0.25f) / 0.50f * (rightBoundaryX - leftBoundaryX)
                    val ey = entity.yPercent * canvasH

                    when (entity.type) {
                        EntityType.TRAFFIC_CAR -> {
                            // Oncoming traffic car silhouette
                            val carW = laneWidth * 0.5f
                            val carH = carW * 1.5f
                            drawRoundRect(
                                color = entity.color,
                                topLeft = Offset(ex - carW / 2, ey - carH / 2),
                                size = androidx.compose.ui.geometry.Size(carW, carH),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                            )
                            // Windshields
                            drawRect(
                                color = Color.Black.copy(alpha = 0.7f),
                                topLeft = Offset(ex - carW * 0.35f, ey - carH * 0.2f),
                                size = androidx.compose.ui.geometry.Size(carW * 0.7f, carH * 0.15f)
                            )
                            // Headlights
                            drawCircle(
                                color = Color.White,
                                radius = 6f,
                                center = Offset(ex - carW * 0.3f, ey + carH * 0.4f)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 6f,
                                center = Offset(ex + carW * 0.3f, ey + carH * 0.4f)
                            )
                        }
                        EntityType.COIN -> {
                            // Gold coins with dynamic rotating lines
                            val coinRadius = 16f
                            drawCircle(
                                color = BrightGold,
                                radius = coinRadius,
                                center = Offset(ex, ey)
                            )
                            drawCircle(
                                color = Color.Black,
                                radius = coinRadius * 0.7f,
                                center = Offset(ex, ey),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                        EntityType.GAS_CAN -> {
                            // Fuel Can
                            val fillWidth = 24f
                            val fillHeight = 32f
                            drawRect(
                                color = MRed,
                                topLeft = Offset(ex - fillWidth / 2, ey - fillHeight / 2),
                                size = androidx.compose.ui.geometry.Size(fillWidth, fillHeight)
                            )
                            // Handle
                            drawLine(
                                color = Color.White,
                                start = Offset(ex - 6f, ey - fillHeight / 2),
                                end = Offset(ex - 6f, ey - fillHeight / 2 - 8f),
                                strokeWidth = 3f
                            )
                            drawLine(
                                color = Color.White,
                                start = Offset(ex - 6f, ey - fillHeight / 2 - 8f),
                                end = Offset(ex + 6f, ey - fillHeight / 2 - 8f),
                                strokeWidth = 3f
                            )
                            drawLine(
                                color = Color.White,
                                start = Offset(ex + 6f, ey - fillHeight / 2 - 8f),
                                end = Offset(ex + 6f, ey - fillHeight / 2),
                                strokeWidth = 3f
                            )
                            // Detail lightning bolt mark
                            drawLine(
                                color = BrightGold,
                                start = Offset(ex, ey - 6f),
                                end = Offset(ex - 4f, ey + 2f),
                                strokeWidth = 3f
                            )
                            drawLine(
                                color = BrightGold,
                                start = Offset(ex - 4f, ey + 2f),
                                end = Offset(ex + 4f, ey + 6f),
                                strokeWidth = 3f
                            )
                        }
                    }
                }

                // 3. Draw active Player Car
                val px = leftBoundaryX + (playerXPercent - 0.25f) / 0.50f * (rightBoundaryX - leftBoundaryX)
                val py = canvasH * 0.8f
                val playerW = laneWidth * 0.55f
                val playerH = playerW * 1.6f

                // Underglow neon shadow
                drawCircle(
                    color = activeCarColor.copy(alpha = 0.5f),
                    radius = playerW * 0.8f,
                    center = Offset(px, py + 10f)
                )

                // Exhaust fire flame trail if NOS active
                if (isNitroActive) {
                    val scaleOffset = (0..10).random()
                    // Draw booster flames on canvas
                    val leftTail = Offset(px - playerW * 0.3f, py + playerH * 0.5f)
                    val rightTail = Offset(px + playerW * 0.3f, py + playerH * 0.5f)

                    drawCircle(color = MRed, radius = 18f + scaleOffset, center = leftTail)
                    drawCircle(color = WarmYellow, radius = 10f + scaleOffset, center = leftTail)

                    drawCircle(color = MRed, radius = 18f + scaleOffset, center = rightTail)
                    drawCircle(color = WarmYellow, radius = 10f + scaleOffset, center = rightTail)

                    // Draw draft speed-lines
                    drawLine(
                        color = NeonCyan.copy(alpha = 0.4f),
                        start = Offset(px - playerW, py),
                        end = Offset(px - playerW, py + playerH * 1.5f),
                        strokeWidth = 6f
                    )
                    drawLine(
                        color = NeonCyan.copy(alpha = 0.4f),
                        start = Offset(px + playerW, py),
                        end = Offset(px + playerW, py + playerH * 1.5f),
                        strokeWidth = 6f
                    )
                }

                // Main body
                drawRoundRect(
                    color = activeCarColor,
                    topLeft = Offset(px - playerW / 2, py - playerH / 2),
                    size = androidx.compose.ui.geometry.Size(playerW, playerH),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                )

                // Sports Stripes
                drawRect(
                    color = Color.Black.copy(alpha = 0.15f),
                    topLeft = Offset(px - playerW * 0.2f, py - playerH / 2),
                    size = androidx.compose.ui.geometry.Size(playerW * 0.1f, playerH)
                )

                // Cabin Windshield
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.82f),
                    topLeft = Offset(px - playerW * 0.35f, py - playerH * 0.16f),
                    size = androidx.compose.ui.geometry.Size(playerW * 0.7f, playerH * 0.35f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                )

                // Tail spoiler line
                drawRect(
                    color = Color.Black,
                    topLeft = Offset(px - playerW * 0.45f, py + playerH * 0.42f),
                    size = androidx.compose.ui.geometry.Size(playerW * 0.9f, 10f)
                )

                // Brake glowing light indicators
                drawRect(
                    color = MRed,
                    topLeft = Offset(px - playerW * 0.4f, py + playerH * 0.46f),
                    size = androidx.compose.ui.geometry.Size(12f, 6f)
                )
                drawRect(
                    color = MRed,
                    topLeft = Offset(px + playerW * 0.4f - 12f, py + playerH * 0.46f),
                    size = androidx.compose.ui.geometry.Size(12f, 6f)
                )
            }

            // HUD Overlays
            if (raceState == RaceState.PLAYING) {
                // Gasoline reserve indicator
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .width(130.dp)
                ) {
                    Text(
                        text = "⛽ GASOLINE: ${gasolineReserve.toInt()}%",
                        color = if (gasolineReserve > 30f) Color.White else MRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { gasolineReserve / 100f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = if (gasolineReserve > 30f) NeonCyan else MRed,
                        trackColor = Color.DarkGray
                    )
                }

                // Nitro injection trigger button
                IconButton(
                    onClick = {
                        isNitroActive = true
                        SoundSynth.playNitro()
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .size(66.dp)
                        .border(
                            2.dp,
                            if (isNitroActive) MRed else NeonCyan,
                            RoundedCornerShape(12.dp)
                        )
                        .background(Color.Black.copy(alpha = 0.8f)),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(
                            text = "⚡",
                            fontSize = 24.sp
                        )
                        Text(
                            text = "NOS",
                            color = if (isNitroActive) MRed else Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Splash game states panel overlay
            if (raceState != RaceState.PLAYING) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        when (raceState) {
                            RaceState.IDLE -> {
                                Text(
                                    text = "🏁",
                                    fontSize = 48.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "MUNICH OUTLAW LEAGUE",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Dodge traffic, grab coins and fuel cans to survive. Keyboard (A/D or Arrows) supported!",
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { raceState = RaceState.PLAYING },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                                ) {
                                    Text("START ENGINE (3..2..1)", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                            RaceState.CRASHED -> {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Crashed",
                                    tint = MRed,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "💥 VEHICLE CRASHED!",
                                    color = MRed,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Bumper and hood took heavy damage! Car repaired? Head back to the Garage to restore active showroom shine.",
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Earned +$currencySymbol$cashEarned | +$milesTraveled Miles",
                                    color = BrightGold,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { raceState = RaceState.PLAYING },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                                ) {
                                    Text("RE-ENTRY RACE TRACK", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                            RaceState.FINISHED -> {
                                Text(
                                    text = "⛽",
                                    fontSize = 48.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "⛽ FUEL EXHAUSTED",
                                    color = NeonCyan,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "You safely completed the street tour, returning to the garage with your earnings.",
                                    color = Color.LightGray,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Earned +$currencySymbol$cashEarned | +$milesTraveled Miles",
                                    color = BrightGold,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { raceState = RaceState.PLAYING },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                                ) {
                                    Text("FILL FUEL & RE-RUN", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                            RaceState.PLAYING -> {}
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Large Responsive Steer Touch Control pads (Safe click on mobile/emulator!)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Steering Left
            Button(
                onClick = {
                    if (raceState == RaceState.PLAYING) {
                        playerXPercent = (playerXPercent - 0.25f).coerceIn(0.25f, 0.75f)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Steer Left", tint = NeonCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("STEER LEFT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            // Steering Right
            Button(
                onClick = {
                    if (raceState == RaceState.PLAYING) {
                        playerXPercent = (playerXPercent + 0.25f).coerceIn(0.25f, 0.75f)
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("STEER RIGHT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = "Steer Right", tint = NeonCyan)
                }
            }
        }
    }
}

@Composable
fun StatTelemetry(label: String, valStr: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = valStr, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}
