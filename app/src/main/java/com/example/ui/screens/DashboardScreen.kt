package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.GameViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: GameViewModel) {
    val playerState by viewModel.playerState.collectAsState()
    val cars by viewModel.allCars.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) }
    var showRadioDialog by remember { mutableStateOf(false) }

    val currencySymbol = if (playerState?.activeLocation == "Germany") "€" else "$"
    val convertFactor = if (playerState?.activeLocation == "Germany") 0.9f else 1.0f

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCarbon)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        topBar = {
            // High-Performance custom telemetry stats bar
            Column {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DarkCarbon
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // User Profile & Location switch
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(NeonCyan.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "BMW Profile",
                                        tint = NeonCyan,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "ToxicTakue Outlaw",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Miles: ${playerState?.totalMileage ?: 0} mi",
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Germany / USA location switcher toggles!
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkSurface)
                                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)), RoundedCornerShape(8.dp))
                                        .padding(2.dp)
                                ) {
                                    LocationToggle(
                                        label = "GER 🇩🇪",
                                        isSelected = playerState?.activeLocation == "Germany",
                                        onClick = { viewModel.setLocation("Germany") }
                                    )
                                    LocationToggle(
                                        label = "USA 🇺🇸",
                                        isSelected = playerState?.activeLocation == "USA",
                                        onClick = { viewModel.setLocation("USA") }
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Neon Outlaw Radio Button
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(NeonCyan.copy(alpha = 0.15f))
                                        .border(BorderStroke(1.dp, NeonCyan.copy(alpha = 0.4f)), CircleShape)
                                        .clickable { showRadioDialog = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "🎵",
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Finance & Vitality reserves
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Money balance (Dollars vs Euros converted!)
                            val moneyConverted = ((playerState?.money ?: 0) * convertFactor).toInt()
                            Column {
                                Text("FINANCIAL BALANCE", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "$currencySymbol$moneyConverted",
                                    color = BrightGold,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Energy Bar (Vitality)
                            Column(
                                modifier = Modifier.width(140.dp),
                                horizontalAlignment = Alignment.End
                              ) {
                                val energyVal = playerState?.energy ?: 0
                                Row {
                                    Text("VITALITY: ", color = Color.LightGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text("$energyVal / 100", color = if (energyVal > 30) GrassGreen else MRed, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { energyVal / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = if (energyVal > 30) GrassGreen else MRed,
                                    trackColor = Color.DarkGray
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(color = NeonCyan.copy(alpha = 0.15f), thickness = 1.dp)
            }
        },
        bottomBar = {
            Column {
                MiniMusicBar(viewModel = viewModel, onExpand = { showRadioDialog = true })
                HorizontalDivider(color = Color.White.copy(alpha = 0.1f), thickness = 1.dp)
                NavigationBar(
                    modifier = Modifier.navigationBarsPadding(),
                    containerColor = DarkCarbon,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonCyan,
                            selectedTextColor = NeonCyan,
                            indicatorColor = NeonCyan.copy(alpha = 0.15f),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        ),
                        icon = { Icon(imageVector = Icons.Default.Build, contentDescription = "Garage") },
                        label = { Text("Garage", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    )
                    NavigationBarItem(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonCyan,
                            selectedTextColor = NeonCyan,
                            indicatorColor = NeonCyan.copy(alpha = 0.15f),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        ),
                        icon = { Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Race") },
                        label = { Text("Racing", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    )
                    NavigationBarItem(
                        selected = activeTab == 2,
                        onClick = { activeTab = 2 },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonCyan,
                            selectedTextColor = NeonCyan,
                            indicatorColor = NeonCyan.copy(alpha = 0.15f),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        ),
                        icon = { Icon(imageVector = Icons.Default.ShoppingCart, contentDescription = "Bistro") },
                        label = { Text("Bistro", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    )
                    NavigationBarItem(
                        selected = activeTab == 3,
                        onClick = { activeTab = 3 },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonCyan,
                            selectedTextColor = NeonCyan,
                            indicatorColor = NeonCyan.copy(alpha = 0.15f),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        ),
                        icon = { Icon(imageVector = Icons.Default.Refresh, contentDescription = "Spa") },
                        label = { Text("Spa/Detail", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    )
                    NavigationBarItem(
                        selected = activeTab == 4,
                        onClick = { activeTab = 4 },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonCyan,
                            selectedTextColor = NeonCyan,
                            indicatorColor = NeonCyan.copy(alpha = 0.15f),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        ),
                        icon = { Icon(imageVector = Icons.Default.Favorite, contentDescription = "Romance") },
                        label = { Text("Romance", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkCarbon)
        ) {
            when (activeTab) {
                0 -> GarageScreen(viewModel)
                1 -> RacingScreen(viewModel)
                2 -> BistroScreen(viewModel)
                3 -> SpaScreen(viewModel)
                4 -> DatingScreen(viewModel)
            }
        }
    }

    if (showRadioDialog) {
        RadioSystemDialog(
            viewModel = viewModel,
            onDismiss = { showRadioDialog = false }
        )
    }
}

@Composable
fun MiniMusicBar(
    viewModel: GameViewModel,
    onExpand: () -> Unit
) {
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isMusicPlaying.collectAsState()
    val progress by viewModel.musicProgress.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "music_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val trackColor = try {
        Color(android.graphics.Color.parseColor(currentTrack.hexColor))
    } catch (e: Exception) {
        NeonCyan
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .clickable { onExpand() }
    ) {
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp),
            color = trackColor,
            trackColor = Color.Transparent
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.Black)
                        .border(BorderStroke(1.dp, trackColor.copy(alpha = 0.4f)), CircleShape)
                        .rotate(if (isPlaying) rotation else 0f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(currentTrack.albumArtEmoji, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = currentTrack.title,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = currentTrack.artist,
                        color = trackColor.copy(alpha = 0.85f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { viewModel.prevTrack() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Text(
                        text = "⏮",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(trackColor.copy(alpha = 0.15f))
                        .clickable { viewModel.togglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    if (isPlaying) {
                        Text(
                            text = "⏸",
                            color = trackColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play",
                            tint = trackColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(
                    onClick = { viewModel.nextTrack() },
                    modifier = Modifier.size(36.dp)
                ) {
                    Text(
                        text = "⏭",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioSystemDialog(
    viewModel: GameViewModel,
    onDismiss: () -> Unit
) {
    val currentTrack by viewModel.currentTrack.collectAsState()
    val isPlaying by viewModel.isMusicPlaying.collectAsState()
    val progress by viewModel.musicProgress.collectAsState()
    val visualizer by viewModel.visualizerAmplitudes.collectAsState()
    
    var selectedArtistFilter by remember { mutableStateOf("ALL") }

    val trackColor = try {
        Color(android.graphics.Color.parseColor(currentTrack.hexColor))
    } catch (e: Exception) {
        NeonCyan
    }

    val currentSeconds = (currentTrack.durationSeconds * progress).toInt()
    val totalSeconds = currentTrack.durationSeconds
    val currentTimeString = String.format("%02d:%02d", currentSeconds / 60, currentSeconds % 60)
    val totalTimeString = String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60)

    val filteredTracks = when (selectedArtistFilter) {
        "KENDRICK" -> viewModel.tracks.filter { it.artist == "Kendrick Lamar" }
        "TRAVIS" -> viewModel.tracks.filter { it.artist == "Travis Scott" }
        "LITHE" -> viewModel.tracks.filter { it.artist == "Lithe" }
        "RUNNA" -> viewModel.tracks.filter { it.artist == "Runna rulez" }
        else -> viewModel.tracks
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCarbon),
        content = {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = DarkCarbon
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📻", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "OUTLAW STEREO DECK",
                                color = NeonCyan,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                              )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Radio",
                                tint = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = BorderStroke(1.dp, trackColor.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black)
                                    .border(BorderStroke(2.dp, trackColor), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(currentTrack.albumArtEmoji, fontSize = 42.sp)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = currentTrack.title,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = currentTrack.artist,
                                color = trackColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = currentTrack.genre,
                                color = Color.Gray,
                                fontSize = 10.sp
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                                    .padding(horizontal = 24.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                visualizer.forEach { amplitude ->
                                    Box(
                                        modifier = Modifier
                                            .width(5.dp)
                                            .fillMaxHeight(amplitude)
                                            .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                            .background(
                                                if (isPlaying) trackColor else Color.DarkGray
                                            )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Slider(
                                    value = progress,
                                    onValueChange = { },
                                    colors = SliderDefaults.colors(
                                        thumbColor = trackColor,
                                        activeTrackColor = trackColor
                                    ),
                                    modifier = Modifier.height(16.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        currentTimeString,
                                        color = Color.LightGray,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        totalTimeString,
                                        color = Color.LightGray,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { viewModel.prevTrack() },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Text(
                                        text = "⏮",
                                        color = Color.White,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(trackColor)
                                        .clickable { viewModel.togglePlayPause() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isPlaying) {
                                        Text(
                                            text = "⏸",
                                            color = Color.Black,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "Play",
                                            tint = Color.Black,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                IconButton(
                                    onClick = { viewModel.nextTrack() },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Text(
                                        text = "⏭",
                                        color = Color.White,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val filters = listOf("ALL", "KENDRICK", "TRAVIS", "LITHE", "RUNNA")
                        filters.forEach { filter ->
                            val filterSelected = selectedArtistFilter == filter
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (filterSelected) NeonCyan.copy(alpha = 0.2f) else Color.Transparent
                                    )
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            if (filterSelected) NeonCyan else Color.White.copy(alpha = 0.1f)
                                        ),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedArtistFilter = filter }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = filter,
                                    color = if (filterSelected) NeonCyan else Color.LightGray,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurface)
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), RoundedCornerShape(12.dp))
                    ) {
                        items(filteredTracks) { track ->
                            val isCurrent = currentTrack.title == track.title
                            val itemColor = try {
                                Color(android.graphics.Color.parseColor(track.hexColor))
                            } catch (e: Exception) {
                                NeonCyan
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isCurrent) itemColor.copy(alpha = 0.08f) else Color.Transparent
                                    )
                                    .clickable { viewModel.playTrack(track) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = track.albumArtEmoji, fontSize = 20.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = track.title,
                                            color = if (isCurrent) itemColor else Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                        )
                                        Text(
                                            text = track.artist,
                                            color = Color.Gray,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Badge(
                                        containerColor = if (isCurrent) itemColor.copy(alpha = 0.15f) else Color.DarkGray,
                                        modifier = Modifier.padding(end = 6.dp)
                                    ) {
                                        Text(
                                            track.genre,
                                            color = if (isCurrent) itemColor else Color.LightGray,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                    
                                    val durationMinSec = String.format("%d:%02d", track.durationSeconds / 60, track.durationSeconds % 60)
                                    Text(
                                        text = durationMinSec,
                                        color = Color.Gray,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun LocationToggle(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) NeonCyan else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.Black else Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    }
}
