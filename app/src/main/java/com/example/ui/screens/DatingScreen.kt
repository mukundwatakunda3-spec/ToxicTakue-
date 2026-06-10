package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.SoundSynth
import com.example.ui.theme.*
import com.example.ui.viewmodel.GameViewModel
import kotlinx.coroutines.delay

data class Partner(
    val name: String,
    val title: String,
    val bio: String,
    val color: Color,
    val favoriteGift: String,
    val dialogueTree: List<DialogueScene>
)

data class DialogueScene(
    val prompt: String,
    val choices: List<DialogueChoice>
)

data class DialogueChoice(
    val text: String,
    val scoreChange: Int,
    val partnerReply: String
)

data class HeartParticle(
    var x: Float,
    var y: Float,
    var vy: Float,
    var vx: Float,
    var scale: Float,
    var alpha: Float = 1.0f
)

@Composable
fun DatingScreen(viewModel: GameViewModel) {
    val playerState by viewModel.playerState.collectAsState()
    val currencySymbol = if (playerState?.activeLocation == "Germany") "€" else "$"

    // Preconfigured partners
    val partners = remember {
        listOf(
            Partner(
                name = "Mia 'Apex' Vance",
                title = "Munich Drift Queen",
                bio = "A wild outlaw drifter who dominates the Autobahn track circuits. Loves high handling, tyres drift specs and tires upgrades.",
                color = MRed,
                favoriteGift = "Titanium Drift Keyfob ($300)",
                dialogueTree = listOf(
                    DialogueScene(
                        prompt = "Hey speeder! Wanna drift this corner at 120 mph? How tuned is your rear axle suspension?",
                        choices = listOf(
                            DialogueChoice("I upgraded my tires setup, let's slide sideways!", 10, "Heck yeah! That grip line is flawless!"),
                            DialogueChoice("Let's keep it safe in traffic.", -5, "Ugh, boring. Call me when you get real horsepower.")
                        )
                    )
                )
            ),
            Partner(
                name = "Sarah 'ECU' Chen",
                title = "Silicon Tech Tuner",
                bio = "A tech wiz from California who maps customized engine curves. Obsessed with stage turbo compressors and ECU chips.",
                color = NeonCyan,
                favoriteGift = "Custom ECU Wire Harness ($400)",
                dialogueTree = listOf(
                    DialogueScene(
                        prompt = "Your air-fuel mapping looks sub-optimal. Want me to compile a custom twin-turbo program?",
                        choices = listOf(
                            DialogueChoice("Re-flash my stage engine immediately!", 10, "Downloading hex code... Your throttle spool is insane now!"),
                            DialogueChoice("I prefer running bone stock.", -5, "Stock? What a waste of premium cylinder displacement.")
                        )
                    )
                )
            ),
            Partner(
                name = "Leo Sterling",
                title = "Luxury Detailing Connoisseur",
                bio = "An elite collector of rare BMW V10 chassis. Expects pristine showroom detail shine and expensive body polish.",
                color = BrightGold,
                favoriteGift = "Ceramic Quartz Coat Wax ($500)",
                dialogueTree = listOf(
                    DialogueScene(
                        prompt = "Hmm, your front grille seems to have bug splatters. How often do you mud-wash this machine?",
                        choices = listOf(
                            DialogueChoice("Just detailed it to 100% showroom gleam!", 15, "Positively radiant profile. You have immaculate taste!"),
                            DialogueChoice("Road grime is a badge of honor.", -8, "Ghastly. My seat bolsters deserve premium cleanliness.")
                        )
                    )
                )
            )
        )
    }

    var selectedPartner by remember { mutableStateOf(partners[0]) }
    val partnerAffinity = when (selectedPartner.name) {
        "Mia 'Apex' Vance" -> playerState?.miaAffinity ?: 0
        "Sarah 'ECU' Chen" -> playerState?.sarahAffinity ?: 0
        "Leo Sterling" -> playerState?.leoAffinity ?: 0
        else -> 0
    }

    var dialogueReplyText by remember { mutableStateOf<String?>(null) }
    val activeKissPartner by viewModel.romanceActiveKiss.collectAsState()

    // Floating heart particles
    val heartParticles = remember { mutableStateListOf<HeartParticle>() }

    // Hearts animation ticker loop
    LaunchedEffect(activeKissPartner) {
        if (activeKissPartner != null) {
            heartParticles.clear()
            repeat(35) {
                heartParticles.add(
                    HeartParticle(
                        x = (150..650).random().toFloat(),
                        y = 1000f,
                        vy = -((4..9).random().toFloat()),
                        vx = ((-3..3).random().toFloat()),
                        scale = ((2..5).random().toFloat()) / 3.2f
                    )
                )
            }
            while (heartParticles.isNotEmpty()) {
                delay(16)
                val iterator = heartParticles.listIterator()
                while (iterator.hasNext()) {
                    val p = iterator.next()
                    p.y += p.vy
                    p.x += p.vx
                    p.alpha -= 0.015f
                    if (p.alpha <= 0f) {
                        iterator.remove()
                    } else {
                        iterator.set(p)
                    }
                }
            }
            viewModel.clearKissAnimation()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCarbon)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Select partner row
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 12.dp)
            ) {
                items(partners) { p ->
                    val isCurrent = p.name == selectedPartner.name
                    Card(
                        modifier = Modifier
                            .width(130.dp)
                            .border(
                                width = if (isCurrent) 2.dp else 1.dp,
                                color = if (isCurrent) p.color else Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                selectedPartner = p
                                dialogueReplyText = null
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isCurrent) DarkSurface else DarkCarbon.copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(p.color.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = p.name,
                                    tint = p.color,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                p.name.split(" ")[0],
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Visual Novel Container Box
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, selectedPartner.color.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header title info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = selectedPartner.name.uppercase(),
                                color = selectedPartner.color,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = selectedPartner.title,
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }

                        // Affinity progress hearts
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Affinity",
                                tint = selectedPartner.color,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Affinity: $partnerAffinity%",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Character Bio segment card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.3f))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = selectedPartner.bio,
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            fontStyle = FontStyle.Italic
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Main chat bubble novel dialogue
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, selectedPartner.color.copy(alpha = 0.5f)))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "💬",
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            val gameEnergy = playerState?.energy ?: 0
                            if (gameEnergy < 20) {
                                // Tired out dialogues
                                Text(
                                    text = "\"You look completely exhausted. Head to the Bistro for a currywurst or slide in the thermal springs spa first. Only speeders with stamina can drift with me!\"",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontStyle = FontStyle.Italic
                                )
                            } else {
                                val scene = selectedPartner.dialogueTree[0]
                                if (dialogueReplyText != null) {
                                    Text(
                                        text = "\"$dialogueReplyText\"",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontStyle = FontStyle.Italic
                                    )
                                } else {
                                    Text(
                                        text = "\"${scene.prompt}\"",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontStyle = FontStyle.Italic
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Dialogue Choice Options
                    val gameEnergy = playerState?.energy ?: 0
                    if (gameEnergy >= 20 && dialogueReplyText == null) {
                        val scene = selectedPartner.dialogueTree[0]
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            scene.choices.forEach { choice ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            dialogueReplyText = choice.partnerReply
                                            viewModel.romanceTalk(selectedPartner.name.split(" ")[0], choice.scoreChange)
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = DarkCarbon),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                                ) {
                                    Text(
                                        text = choice.text,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(10.dp),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    } else if (dialogueReplyText != null) {
                        Button(
                            onClick = { dialogueReplyText = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                        ) {
                            Text("CONTINUE CONVERSATION", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sandbox Actions (Gift, Kiss keys)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Gift Option
                val giftCostVal = when (selectedPartner.name.split(" ")[0]) {
                    "Mia" -> 300
                    "Sarah" -> 400
                    "Leo" -> 500
                    else -> 300
                }
                val canAffordGift = (playerState?.money ?: 0) >= giftCostVal

                Button(
                    onClick = { viewModel.romanceGift(selectedPartner.name.split(" ")[0], giftCostVal) },
                    enabled = canAffordGift,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, selectedPartner.color.copy(alpha = 0.4f))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text("🎁", fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "GIFT ($currencySymbol$giftCostVal)",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Kiss Option (Threshold at 70%+)
                val canKiss = partnerAffinity >= 70
                Button(
                    onClick = { viewModel.romanceKiss(selectedPartner.name.split(" ")[0]) },
                    enabled = canKiss,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canKiss) SoftPink else Color.DarkGray.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text("💋", fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (canKiss) "SWEET KISS" else "KISS (PARTNER Lvl 70)",
                            color = if (canKiss) Color.White else Color.LightGray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        // Float particle overlays on Canvas
        if (activeKissPartner != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                heartParticles.forEach { p ->
                    val sizeScale = 14f * p.scale
                    val ox = p.x
                    val oy = p.y

                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(ox, oy + sizeScale * 0.35f)
                        cubicTo(ox - sizeScale * 0.5f, oy - sizeScale * 0.45f, ox - sizeScale * 1.1f, oy + sizeScale * 0.15f, ox, oy + sizeScale * 0.95f)
                        cubicTo(ox + sizeScale * 1.1f, oy + sizeScale * 0.15f, ox + sizeScale * 0.5f, oy - sizeScale * 0.45f, ox, oy + sizeScale * 0.35f)
                    }

                    drawPath(
                        path = path,
                        color = Color(0xFFFF69B4).copy(alpha = p.alpha)
                    )
                }
            }

            // Big kissing banner text overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SoftPink),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "💏 SWEET ROMANTIC KISS!",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "You leaned in and kissed $activeKissPartner happily! Hearts exploded!",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
