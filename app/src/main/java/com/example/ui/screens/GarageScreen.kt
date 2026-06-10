package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.CarState
import com.example.data.database.PlayerState
import com.example.ui.theme.*
import com.example.ui.viewmodel.GameViewModel

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun GarageScreen(viewModel: GameViewModel) {
    val playerState by viewModel.playerState.collectAsState()
    val cars by viewModel.allCars.collectAsState()

    val currencySymbol = if (playerState?.activeLocation == "Germany") "€" else "$"
    val convertFactor = if (playerState?.activeLocation == "Germany") 0.9f else 1.0f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCarbon)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "⚡ M-POWER CUSTOM GARAGE",
                        color = NeonCyan,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Customize and tune your dream fleet. Current Location: ${playerState?.activeLocation}",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }
            }
        }

        items(cars) { car ->
            val isActive = car.id == playerState?.activeCarId
            CarRowCard(
                car = car,
                isActive = isActive,
                currencySymbol = currencySymbol,
                convertFactor = convertFactor,
                onSelect = { viewModel.changeActiveCar(car.id) },
                onBuy = { viewModel.buyCar(car.id, (car.basePrice * convertFactor).toInt()) },
                onUpgrade = { type, cost -> viewModel.upgradeCar(car.id, type, cost) },
                onSpray = { color -> viewModel.sprayCarColor(car.id, color) },
                onRepair = { viewModel.repairCar(car.id, (car.damagePercent * 12 * convertFactor).toInt()) },
                playerMoney = playerState?.money ?: 0
            )
        }
    }
}

@Composable
fun CarRowCard(
    car: CarState,
    isActive: Boolean,
    currencySymbol: String,
    convertFactor: Float,
    onSelect: () -> Unit,
    onBuy: () -> Unit,
    onUpgrade: (String, Int) -> Unit,
    onSpray: (String) -> Unit,
    onRepair: () -> Unit,
    playerMoney: Int
) {
    val carColor = try {
        Color(android.graphics.Color.parseColor(car.colorHex))
    } catch (e: Exception) {
        NeonCyan
    }

    val priceConverted = (car.basePrice * convertFactor).toInt()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) NeonCyan else Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) DarkSurface else DarkCarbon.copy(alpha = 0.7f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(carColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = car.name,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (!car.isOwned) {
                        Text(
                            text = "Price: $currencySymbol$priceConverted",
                            color = BrightGold,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            text = if (isActive) "ACTIVE STREET VEHICLE" else "OWNED",
                            color = if (isActive) NeonCyan else Color.Gray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (car.isOwned) {
                    if (!isActive) {
                        Button(
                            onClick = onSelect,
                            colors = ButtonDefaults.buttonColors(containerColor = MBlue),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("DRIVE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Active",
                            tint = NeonCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = onBuy,
                        enabled = playerMoney >= priceConverted,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrightGold,
                            disabledContainerColor = Color.DarkGray
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "BUY",
                            color = if (playerMoney >= priceConverted) Color.Black else Color.LightGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // BMW Silhouette representation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                // Vector-drawn cool sports car side profile utilizing canvas
                Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    val w = size.width
                    val h = size.height
                    
                    // Draw neon glow underglow
                    drawCircle(
                        color = carColor.copy(alpha = 0.3f),
                        radius = h * 0.45f,
                        center = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.8f)
                    )

                    val strokeColor = carColor
                    // Simple beautiful wireframe car outline
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(w * 0.1f, h * 0.75f) // Rear bumper
                        lineTo(w * 0.15f, h * 0.65f)
                        lineTo(w * 0.15f, h * 0.52f) // Spoiler backing
                        lineTo(w * 0.22f, h * 0.52f) // Spoiler top
                        lineTo(w * 0.22f, h * 0.55f)
                        lineTo(w * 0.35f, h * 0.55f) // Rear windshield
                        lineTo(w * 0.45f, h * 0.32f) // Roof peak
                        lineTo(w * 0.62f, h * 0.32f) // Sunroof
                        lineTo(w * 0.82f, h * 0.52f) // Front windshield
                        lineTo(w * 0.92f, h * 0.58f) // Hood
                        lineTo(w * 0.95f, h * 0.68f) // Front bumper
                        lineTo(w * 0.88f, h * 0.75f) // Underbelly front
                        // Wheel wells
                        lineTo(w * 0.82f, h * 0.75f)
                        arcTo(
                            rect = androidx.compose.ui.geometry.Rect(w * 0.70f, h * 0.6f, w * 0.82f, h * 0.85f),
                            startAngleDegrees = 0f,
                            sweepAngleDegrees = -180f,
                            forceMoveTo = false
                        )
                        lineTo(w * 0.38f, h * 0.75f)
                        arcTo(
                            rect = androidx.compose.ui.geometry.Rect(w * 0.26f, h * 0.6f, w * 0.38f, h * 0.85f),
                            startAngleDegrees = 0f,
                            sweepAngleDegrees = -180f,
                            forceMoveTo = false
                        )
                        lineTo(w * 0.1f, h * 0.75f)
                    }

                    drawPath(
                        path = path,
                        color = strokeColor,
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Draw wheels
                    drawCircle(
                        color = Color.DarkGray,
                        radius = h * 0.12f,
                        center = androidx.compose.ui.geometry.Offset(w * 0.76f, h * 0.72f)
                    )
                    drawCircle(
                        color = NeonCyan,
                        radius = h * 0.05f,
                        center = androidx.compose.ui.geometry.Offset(w * 0.76f, h * 0.72f)
                    )

                    drawCircle(
                        color = Color.DarkGray,
                        radius = h * 0.12f,
                        center = androidx.compose.ui.geometry.Offset(w * 0.32f, h * 0.72f)
                    )
                    drawCircle(
                        color = NeonCyan,
                        radius = h * 0.05f,
                        center = androidx.compose.ui.geometry.Offset(w * 0.32f, h * 0.72f)
                    )
                }

                // Show Stats on top-right
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .align(Alignment.CenterEnd)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.SpaceAround,
                    horizontalAlignment = Alignment.End
                ) {
                    Badge(containerColor = MRed) {
                        Text(
                            text = "SPEED: ${130 + car.turboLevel * 25} mph",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    Badge(containerColor = MBlue) {
                        Text(
                            text = "HANDLING: G-${0.8 + car.tyresLevel * 0.15}",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    Badge(containerColor = GrassGreen) {
                        Text(
                            text = "NOS: L-${car.nitroLevel}",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                // Damage indicator if owned
                if (car.isOwned) {
                    val repairCost = (car.damagePercent * 12 * convertFactor).toInt()
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🛠️ DMG: ${car.damagePercent}%",
                            color = if (car.damagePercent > 35) MRed else Color.Green,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (car.damagePercent > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Card(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable(enabled = playerMoney >= repairCost) { onRepair() },
                                colors = CardDefaults.cardColors(containerColor = MRed),
                            ) {
                                Text(
                                    text = "REPAIR ($currencySymbol$repairCost)",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (car.isOwned) {
                // Custom Performance Upgrades
                Text("STAGE TUNING", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Turbo Level
                    val turboUpgradeCost = (1200 * car.turboLevel * convertFactor).toInt()
                    UpgradeButton(
                        modifier = Modifier.weight(1f),
                        title = "Turbo Engine",
                        level = car.turboLevel,
                        cost = turboUpgradeCost,
                        canAfford = playerMoney >= turboUpgradeCost,
                        currencySymbol = currencySymbol,
                        onUpgrade = { onUpgrade("turbo", turboUpgradeCost) }
                    )

                    // Tyres Level
                    val tyresUpgradeCost = (800 * car.tyresLevel * convertFactor).toInt()
                    UpgradeButton(
                        modifier = Modifier.weight(1f),
                        title = "Grid Tyres",
                        level = car.tyresLevel,
                        cost = tyresUpgradeCost,
                        canAfford = playerMoney >= tyresUpgradeCost,
                        currencySymbol = currencySymbol,
                        onUpgrade = { onUpgrade("tyres", tyresUpgradeCost) }
                    )

                    // Nitro level
                    val nitroUpgradeCost = (1000 * car.nitroLevel * convertFactor).toInt()
                    UpgradeButton(
                        modifier = Modifier.weight(1f),
                        title = "Nitrous NOS",
                        level = car.nitroLevel,
                        cost = nitroUpgradeCost,
                        canAfford = playerMoney >= nitroUpgradeCost,
                        currencySymbol = currencySymbol,
                        onUpgrade = { onUpgrade("nitro", nitroUpgradeCost) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Spray Paint shop Custom color HEX changer
                Text("SPRAY SPA PAINT DEPOT", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val colorsPalette = listOf(
                        "#E31E24" to "Alpine Red",
                        "#1F2E8E" to "Kyalami Blue",
                        "#0C7550" to "Isle of Man Green",
                        "#02F2EC" to "Frozen Cyber Cyan",
                        "#FFFAFA" to "Polar White",
                        "#1A1A1A" to "Midnight Carbon Black"
                    )

                    colorsPalette.forEach { (colorHexVal, name) ->
                        val paintColor = Color(android.graphics.Color.parseColor(colorHexVal))
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(paintColor)
                                .border(
                                    width = if (car.colorHex.equals(colorHexVal, ignoreCase = true)) 2.dp else 0.dp,
                                    color = Color.White,
                                    shape = CircleShape
                                )
                                .clickable { onSpray(colorHexVal) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UpgradeButton(
    modifier: Modifier = Modifier,
    title: String,
    level: Int,
    cost: Int,
    canAfford: Boolean,
    currencySymbol: String,
    onUpgrade: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = level < 5 && canAfford) { onUpgrade() },
        colors = CardDefaults.cardColors(
            containerColor = if (level >= 5) Color.DarkGray.copy(alpha = 0.5f) else DarkCarbon
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (level >= 5) Color.Transparent else if (canAfford) NeonCyan.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text("Lvl $level / 5", color = NeonCyan, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            if (level < 5) {
                Text(
                    text = "$currencySymbol$cost",
                    color = if (canAfford) BrightGold else MRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text("MAX OUT", color = GrassGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
