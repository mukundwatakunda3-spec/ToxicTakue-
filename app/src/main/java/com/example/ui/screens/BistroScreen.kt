package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.SoundSynth
import com.example.ui.theme.*
import com.example.ui.viewmodel.GameViewModel
import kotlinx.coroutines.delay

data class BistroItem(
    val id: String,
    val name: String,
    val baseCost: Int,
    val energyGain: Int,
    val iconEmoji: String,
    val color: Color
)

data class CrumbParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var alpha: Float = 1.0f,
    val size: Float
)

@Composable
fun BistroScreen(viewModel: GameViewModel) {
    val playerState by viewModel.playerState.collectAsState()
    val currencySymbol = if (playerState?.activeLocation == "Germany") "€" else "$"
    val convertFactor = if (playerState?.activeLocation == "Germany") 0.9f else 1.0f

    val isGermany = playerState?.activeLocation == "Germany"

    val menuItems = remember(isGermany) {
        if (isGermany) {
            listOf(
                BistroItem("item_coffee", "Autobahn Nitro Cold Brew", 5, 12, "☕", NeonCyan),
                BistroItem("item_pretzel", "Munich Bavarian Pretzel", 12, 35, "🥨", WarmYellow),
                BistroItem("item_currywurst", "Berlin Currywurst Bowl", 28, 75, "🌭", MRed)
            )
        } else {
            listOf(
                BistroItem("item_coffee", "California Nitro Cold Brew", 6, 12, "☕", NeonCyan),
                BistroItem("item_pretzel", "Gourmet Austin Pretzel", 14, 35, "🥨", WarmYellow),
                BistroItem("item_wagyu", "Smoked Wagyu Ribeye Steak", 45, 75, "🥩", MRed)
            )
        }
    }

    var selectedItemForChomping by remember { mutableStateOf<BistroItem?>(null) }
    var bitesRemaining by remember { mutableIntStateOf(0) }
    val crumbParticles = remember { mutableStateListOf<CrumbParticle>() }

    // Particle updater loop
    LaunchedEffect(selectedItemForChomping, bitesRemaining) {
        while (selectedItemForChomping != null && crumbParticles.isNotEmpty()) {
            delay(16) // tick
            val iterator = crumbParticles.listIterator()
            while (iterator.hasNext()) {
                val p = iterator.next()
                p.x += p.vx
                p.y += p.vy
                p.vy += 0.4f // gravity
                p.alpha -= 0.03f
                if (p.alpha <= 0f) {
                    iterator.remove()
                } else {
                    iterator.set(p)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCarbon)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Bistro Header card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, WarmYellow.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(WarmYellow.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🍽️",
                        fontSize = 24.sp
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "🍔 FLAME-BROILED M-BISTRO",
                        color = WarmYellow,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Refuel your energy bar before hitting high-speed races.",
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Chomping simulation panel when item selected
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.3f)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, WarmYellow.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .background(DarkSurface),
            contentAlignment = Alignment.Center
        ) {
            val item = selectedItemForChomping
            if (item != null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize().padding(16.dp)
                ) {
                    Text(
                        text = "TAP PLATE TO TAKE A BITE!",
                        color = WarmYellow,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.name,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Bites Remaining: $bitesRemaining / 4",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Canvas click zones for eating
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f))
                            .border(3.dp, item.color, CircleShape)
                            .clickable {
                                if (bitesRemaining > 0) {
                                    SoundSynth.playChomp()
                                    bitesRemaining--

                                    // Sprout canvas crumb particles exploding outwards
                                    val count = (12..18).random()
                                    repeat(count) {
                                        val angle = (0..360).random() * Math.PI / 180.0
                                        val velocity = (3..9).random().toFloat()
                                        crumbParticles.add(
                                            CrumbParticle(
                                                x = 0f, // center relative
                                                y = 0f,
                                                vx = (kotlin.math.cos(angle) * velocity).toFloat(),
                                                vy = (kotlin.math.sin(angle) * velocity - 2f).toFloat(), // initial jump
                                                size = (6..14).random().toFloat()
                                            )
                                        )
                                    }

                                    // If finished chewing, deduct money and award energy
                                    if (bitesRemaining == 0) {
                                        val finalItem = selectedItemForChomping
                                        if (finalItem != null) {
                                            viewModel.eatFood(finalItem.energyGain, finalItem.baseCost)
                                        }
                                        selectedItemForChomping = null
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            // Draw plate rings
                            drawCircle(
                                color = Color.Gray.copy(alpha = 0.2f),
                                radius = w * 0.45f,
                                center = Offset(w * 0.5f, h * 0.5f),
                                style = Stroke(width = 6f)
                            )

                            // Draw dish silhouette representation depending on bitesRemaining
                            if (bitesRemaining > 0) {
                                val radiusFraction = 0.1f + (bitesRemaining * 0.08f)
                                drawCircle(
                                    color = item.color,
                                    radius = w * radiusFraction,
                                    center = Offset(w * 0.5f, h * 0.5f)
                                )

                                // Draw little bite markings on edges
                                if (bitesRemaining < 4) {
                                    drawCircle(
                                        color = DarkSurface,
                                        radius = w * 0.12f,
                                        center = Offset(w * 0.35f, h * 0.4f)
                                    )
                                }
                                if (bitesRemaining < 3) {
                                    drawCircle(
                                        color = DarkSurface,
                                        radius = w * 0.12f,
                                        center = Offset(w * 0.65f, h * 0.55f)
                                    )
                                }
                                if (bitesRemaining < 2) {
                                    drawCircle(
                                        color = DarkSurface,
                                        radius = w * 0.15f,
                                        center = Offset(w * 0.5f, h * 0.7f)
                                    )
                                }
                            }

                            // Draw crumb particles live
                            crumbParticles.forEach { p ->
                                val cx = w * 0.5f + p.x
                                val cy = h * 0.5f + p.y
                                drawCircle(
                                    color = item.color.copy(alpha = p.alpha),
                                    radius = p.size,
                                    center = Offset(cx, cy)
                                )
                            }
                        }

                        // Icon in plate center
                        Text(
                            text = item.iconEmoji,
                            fontSize = 36.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    TextButton(onClick = { selectedItemForChomping = null }) {
                        Text("CANCEL MEAL", color = MRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "🍽️",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "ORDER TO BEGIN CHOMPING",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap on a recipe card below, then feast on the plate to replenish your energy reserves.",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recipes Selection List
        Text(
            text = "TODAY'S SELECTION MENU",
            color = Color.LightGray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            menuItems.forEach { menuItem ->
                val costConverted = menuItem.baseCost
                val canAfford = (playerState?.money ?: 0) >= costConverted

                Card(
                     modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = canAfford && selectedItemForChomping == null) {
                            selectedItemForChomping = menuItem
                            bitesRemaining = 4
                            crumbParticles.clear()
                            SoundSynth.playCoin()
                        },
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (canAfford) menuItem.color.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.05f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(menuItem.color.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = menuItem.iconEmoji,
                                    fontSize = 20.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = menuItem.name,
                                    color = if (canAfford) Color.White else Color.Gray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Restores: +${menuItem.energyGain} Energy",
                                    color = NeonCyan,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "$currencySymbol$costConverted",
                                color = if (canAfford) BrightGold else MRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            if (!canAfford) {
                                Text("BROKE", color = MRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
