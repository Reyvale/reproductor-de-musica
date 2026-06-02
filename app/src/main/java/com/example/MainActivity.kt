package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.BiasAlignment
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.database.EqualizerPresetEntity
import com.example.data.database.PlaylistEntity
import com.example.data.model.Track
import com.example.data.model.TrackList
import com.example.ui.theme.CustomTheme
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.MusicThemes
import com.example.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainPlayerScreen()
            }
        }
    }
}

@Composable
fun MainPlayerScreen(viewModel: PlayerViewModel = viewModel()) {
    val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
    val isHiResDirect by viewModel.isHiResDirect.collectAsStateWithLifecycle()
    val resamplingRate by viewModel.resamplingRate.collectAsStateWithLifecycle()
    val dacFilter by viewModel.dacFilter.collectAsStateWithLifecycle()
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val playPositionMs by viewModel.playPositionMs.collectAsStateWithLifecycle()
    val isShuffleEnabled by viewModel.isShuffleEnabled.collectAsStateWithLifecycle()
    val isRepeatEnabled by viewModel.isRepeatEnabled.collectAsStateWithLifecycle()

    var activeScreen by remember { mutableStateOf(0) } // 0: Player, 1: Equalizer, 2: Library, 3: Settings/Themes/AI

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            CustomBottomNavBar(
                selectedScreen = activeScreen,
                onScreenChange = { activeScreen = it },
                currentTheme = currentTheme
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = currentTheme.gradientColors
                    )
                )
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (activeScreen) {
                        0 -> {
                            LosslessArtworkStage(
                                currentTrack = currentTrack,
                                currentTheme = currentTheme,
                                isPlaying = isPlaying,
                                playPositionMs = playPositionMs,
                                onPrev = { viewModel.prevTrack() },
                                onNext = { viewModel.nextTrack() },
                                onPlayPause = { viewModel.togglePlayPause() },
                                onSeek = { pos -> viewModel.seekTo(pos) },
                                spectrumValues = viewModel.spectrumValues.collectAsStateWithLifecycle().value,
                                isShuffleEnabled = isShuffleEnabled,
                                onToggleShuffle = { viewModel.toggleShuffle() },
                                isRepeatEnabled = isRepeatEnabled,
                                onToggleRepeat = { viewModel.toggleRepeat() }
                            )
                        }
                        1 -> {
                            EqualizerPanel(
                                viewModel = viewModel,
                                currentTheme = currentTheme
                            )
                        }
                        2 -> {
                            LibraryPanel(viewModel = viewModel, currentTheme = currentTheme)
                        }
                        3 -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(top = 24.dp)
                            ) {
                                PlayerInfoPanel(currentTheme = currentTheme)
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                }

                if (activeScreen != 0) {
                    MiniPlaybackPlayerBar(
                        currentTrack = currentTrack,
                        isPlaying = isPlaying,
                        onPlayPause = { viewModel.togglePlayPause() },
                        onClick = { activeScreen = 0 },
                        currentTheme = currentTheme
                    )
                }
            }
        }
    }
}

@Composable
fun CustomBottomNavBar(
    selectedScreen: Int,
    onScreenChange: (Int) -> Unit,
    currentTheme: CustomTheme
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = Color(0xFF0F1113).copy(alpha = 0.95f),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Item 1: Grid (Library) -> screen = 2
            BottomNavItem(
                onClick = { onScreenChange(2) },
                isSelected = selectedScreen == 2,
                currentTheme = currentTheme
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(6.dp).background(LocalContentColor.current, RoundedCornerShape(1f)))
                        Box(modifier = Modifier.size(6.dp).background(LocalContentColor.current, RoundedCornerShape(1f)))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(6.dp).background(LocalContentColor.current, RoundedCornerShape(1f)))
                        Box(modifier = Modifier.size(6.dp).background(LocalContentColor.current, RoundedCornerShape(1f)))
                    }
                }
            }

            // Item 2: Equalizer levels -> screen = 1
            BottomNavItem(
                onClick = { onScreenChange(1) },
                isSelected = selectedScreen == 1,
                currentTheme = currentTheme
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(3.dp, 16.dp).background(LocalContentColor.current, RoundedCornerShape(1f)))
                    Box(modifier = Modifier.size(3.dp, 12.dp).background(LocalContentColor.current, RoundedCornerShape(1f)))
                    Box(modifier = Modifier.size(3.dp, 18.dp).background(LocalContentColor.current, RoundedCornerShape(1f)))
                }
            }

            // Item 4: Home Player icon -> screen = 0
            BottomNavItem(
                onClick = { onScreenChange(0) },
                isSelected = selectedScreen == 0,
                currentTheme = currentTheme
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(16.dp, 3.dp).background(LocalContentColor.current, RoundedCornerShape(1f)))
                    Box(modifier = Modifier.size(16.dp, 3.dp).background(LocalContentColor.current, RoundedCornerShape(1f)))
                    Box(modifier = Modifier.size(16.dp, 3.dp).background(LocalContentColor.current, RoundedCornerShape(1f)))
                }
            }

            // Item 3: Info -> screen = 3
            BottomNavItem(
                onClick = { onScreenChange(3) },
                isSelected = selectedScreen == 3,
                currentTheme = currentTheme
            ) {
                Text("ℹ️", fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun BottomNavItem(
    onClick: () -> Unit,
    isSelected: Boolean,
    currentTheme: CustomTheme,
    iconContent: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (isSelected) currentTheme.primary else Color.Gray.copy(alpha = 0.6f)
        ) {
            iconContent()
        }
    }
}

@Composable
fun MiniPlaybackPlayerBar(
    currentTrack: Track,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onClick: () -> Unit,
    currentTheme: CustomTheme
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1113).copy(alpha = 0.9f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.White.copy(0.06f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Black, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("💿", fontSize = 20.sp)
                }
                
                Spacer(modifier = Modifier.width(10.dp))
                
                Column {
                    Text(
                        text = currentTrack.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = currentTrack.artist,
                        fontSize = 10.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            IconButton(onClick = onPlayPause) {
                Text(
                    text = if (isPlaying) "⏸" else "▶",
                    fontSize = 20.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun TopHudBar(
    currentTheme: CustomTheme,
    isHiResDirect: Boolean,
    resamplingRate: String,
    dacFilter: String,
    onToggleDirect: () -> Unit
) {
    var showDacDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .border(
                1.dp,
                currentTheme.primary.copy(alpha = 0.15f),
                RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = currentTheme.surface.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Highlighting indicator matching Poweramp's premium direct sound path indicator
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            if (isHiResDirect) currentTheme.accent else Color.Gray,
                            CircleShape
                        )
                        .drawBehind {
                            if (isHiResDirect) {
                                drawCircle(
                                    color = currentTheme.accent,
                                    radius = size.minDimension * 0.9f,
                                    alpha = 0.45f,
                                    style = Stroke(width = 3f)
                                )
                            }
                        }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "RUTA DE AUDIO DIRECTA HI-RES",
                        fontWeight = FontWeight.Bold,
                        color = currentTheme.textPrimary,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (isHiResDirect) "Bit-Perfect • $resamplingRate • $dacFilter" else "Filtros Apagados • SRC nativo",
                        color = currentTheme.textSecondary,
                        fontSize = 9.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Toggle Direct audio processing using high end unicode
                IconButton(
                    onClick = onToggleDirect,
                    modifier = Modifier.size(32.dp)
                ) {
                    Text(
                        text = if (isHiResDirect) "⚡" else "🔌",
                        fontSize = 16.sp,
                        color = if (isHiResDirect) currentTheme.accent else currentTheme.textSecondary
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Configure dac emulator configuration options
                IconButton(
                    onClick = { showDacDialog = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Text(
                        text = "⚙️",
                        fontSize = 16.sp,
                        color = currentTheme.textPrimary
                    )
                }
            }
        }
    }

    if (showDacDialog) {
        AlertDialog(
            onDismissRequest = { showDacDialog = false },
            containerColor = currentTheme.surface,
            title = {
                Text(
                    "Configuración del DAC y Remuestreo",
                    fontWeight = FontWeight.Bold,
                    color = currentTheme.textPrimary
                )
            },
            text = {
                Column {
                    Text(
                        "Filtro Digital Reconstructor",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = currentTheme.accent,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    
                    val filters = listOf("Minimum Phase Fast", "Linear Phase Slow", "Apodizing Fast Roll-off", "Hi-Res Direct Filterless")
                    filters.forEach { filter ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleDirect() } // Toggle updates the state indirectly for feedback
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (filter == dacFilter),
                                onClick = { onToggleDirect() },
                                colors = RadioButtonDefaults.colors(selectedColor = currentTheme.primary)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(filter, color = currentTheme.textPrimary, fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Tasa de Remuestreo Activa",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = currentTheme.accent,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    val rates = listOf("44.1 kHz", "96 kHz", "192 kHz", "352.8 kHz DSD")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        rates.forEach { rate ->
                            SuggestionChip(
                                onClick = { /* Updates active sampling rate */ },
                                label = { Text(rate, fontSize = 10.sp) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    labelColor = if (rate == resamplingRate) currentTheme.primary else currentTheme.textSecondary
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDacDialog = false }) {
                    Text("ACEPTAR", color = currentTheme.primary)
                }
            }
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LosslessArtworkStage(
    currentTrack: Track,
    currentTheme: CustomTheme,
    isPlaying: Boolean,
    playPositionMs: Long,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    spectrumValues: List<Float>,
    isShuffleEnabled: Boolean,
    onToggleShuffle: () -> Unit,
    isRepeatEnabled: Boolean,
    onToggleRepeat: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Large Premium Display Cover Art Frame
        Card(
            modifier = Modifier
                .aspectRatio(1.0f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(32.dp),
                    ambientColor = currentTheme.glowColor,
                    spotColor = currentTheme.glowColor
                ),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f)),
            shape = RoundedCornerShape(32.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        // Ambient dynamic background bloom glowing orb
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    currentTheme.primary.copy(alpha = if (isPlaying) 0.35f else 0.15f),
                                    Color.Transparent
                                )
                            ),
                            radius = size.minDimension * 0.9f
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                // Vinyl / Album Artwork Graphic mimicking Poweramp's center core
                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .drawBehind {
                            // Metallic vinyl circles
                            for (i in 1..10) {
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.03f * i),
                                    radius = (size.minDimension / 2) * (i / 10f),
                                    style = Stroke(width = 2f)
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Central Art Core
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .drawBehind {
                                drawCircle(
                                    color = currentTheme.primary,
                                    radius = size.minDimension / 2,
                                    style = Stroke(width = 3f)
                                )
                            }
                            .clip(CircleShape)
                            .background(currentTheme.surface)
                    ) {
                        // Decorative glowing node
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(currentTheme.accent.copy(alpha = 0.2f), CircleShape)
                                .border(1.dp, currentTheme.accent, CircleShape)
                                .align(Alignment.Center)
                        )
                        
                        Text(
                            text = "💿",
                            fontSize = 64.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 2. 3 dot menu Row directly below Artwork (Image 1)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {},
                modifier = Modifier
                    .background(Color.Black.copy(0.4f), CircleShape)
                    .size(36.dp)
            ) {
                Text("⋮", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Metadata Info Block (Title in Pill, secondary artist text scroll)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = currentTrack.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "${currentTrack.artist} - Topic - ${currentTrack.album} - Topic",
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                color = Color.LightGray.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4. Quick modes buttons (repeat loop, shuffle icon)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 48.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleRepeat, modifier = Modifier.size(48.dp).align(Alignment.CenterVertically)) {
                Text(
                    text = "🔁",
                    fontSize = 24.sp,
                    color = if (isRepeatEnabled) currentTheme.primary else Color.White.copy(0.4f)
                )
            }
            Spacer(modifier = Modifier.width(36.dp))
            IconButton(onClick = onToggleShuffle, modifier = Modifier.size(48.dp).align(Alignment.CenterVertically)) {
                Text(
                    text = "🔀",
                    fontSize = 24.sp,
                    color = if (isShuffleEnabled) currentTheme.primary else Color.White.copy(0.4f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 5. Symmetric Waveform seekable bar with Overlay Playback Controls (Image 1)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Waveform seekbar Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(currentTrack.id) {
                        detectTapGestures { offset ->
                            val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                            onSeek((fraction * currentTrack.durationMs).toLong())
                        }
                    }
                    .pointerInput(currentTrack.id) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                            onSeek((fraction * currentTrack.durationMs).toLong())
                        }
                    }
            ) {
                val numBars = 45
                val spacing = 6f
                val barWidth = (size.width - (spacing * (numBars - 1))) / numBars
                val progressFraction = playPositionMs.toFloat() / currentTrack.durationMs.toFloat()
                
                for (i in 0 until numBars) {
                    val seed = i + currentTrack.id * 13
                    // Symmetrical waveform envelope representation using kotlin.math
                    val heightFactor = (kotlin.math.sin(seed * 0.18f) * 0.45f + 0.55f) * (kotlin.math.cos(seed * 0.08f) * 0.4f + 0.6f)
                    val barHeight = (heightFactor * size.height * 0.75f).coerceAtLeast(8f)
                    
                    val x = i * (barWidth + spacing)
                    val yCenter = size.height / 2f
                    
                    val isPlayed = (i.toFloat() / numBars) <= progressFraction
                    val barColor = if (isPlayed) {
                        Color.White.copy(alpha = 0.85f)
                    } else {
                        Color.Gray.copy(alpha = 0.25f)
                    }
                    
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, yCenter - (barHeight / 2f)),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f)
                    )
                }
            }

            // Layered Playback Buttons Row aligned perfectly over the center of the waveform (Image 1)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Prev
                IconButton(
                    onClick = onPrev,
                    modifier = Modifier
                        .size(76.dp)
                        .background(Color.Black, CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                ) {
                    Text("⏪", fontSize = 26.sp, color = Color.White)
                }
                
                Spacer(modifier = Modifier.width(20.dp))
                
                // Play/Pause
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .background(Color.White, CircleShape)
                        .clickable { onPlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isPlaying) "⏸" else "▶",
                        fontSize = 30.sp,
                        color = Color.Black
                    )
                }
                
                Spacer(modifier = Modifier.width(20.dp))
                
                // Next
                IconButton(
                    onClick = onNext,
                    modifier = Modifier
                        .size(76.dp)
                        .background(Color.Black, CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape)
                ) {
                    Text("⏩", fontSize = 26.sp, color = Color.White)
                }
            }

            // Time indicators on outer left/right of the seekbar box
            Text(
                text = formatTime(playPositionMs),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 6.dp, start = 8.dp)
            )

            Text(
                text = formatTime(currentTrack.durationMs),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 6.dp, end = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 6. Active Folder representation card (Image 1 bottom-most label)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("📁", fontSize = 11.sp, color = Color.White.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = currentTrack.genre.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun CanvasSpectrumVisualizer(
    spectrumValues: List<Float>,
    currentTheme: CustomTheme,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val spacing = 8f
        val numBars = spectrumValues.size
        val barWidth = (size.width - (spacing * (numBars - 1))) / numBars

        for (i in 0 until numBars) {
            val h = spectrumValues[i] * size.height
            val left = i * (barWidth + spacing)
            
            // Draw a futuristic neon bar with glowing top points
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        currentTheme.accent,
                        currentTheme.primary,
                        currentTheme.primary.copy(alpha = 0.15f)
                    )
                ),
                topLeft = Offset(left, size.height - h),
                size = androidx.compose.ui.geometry.Size(barWidth, h),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
            )

            // Dynamic glow points floating on top of spectral values
            drawCircle(
                color = currentTheme.accent,
                radius = 2.5f,
                center = Offset(left + (barWidth / 2f), (size.height - h).coerceAtLeast(3f))
            )
        }
    }
}

@Composable
fun InteractiveControlDeck(
    viewModel: PlayerViewModel,
    currentTheme: CustomTheme
) {
    var activeTab by remember { mutableStateOf(0) } // 0: EQ, 1: Biblioteca, 2: Temas/Widgets, 3: Sinc/IA
    val tabs = listOf("🎚️ Ecualizador", "📂 Biblioteca", "🎨 Temas & Widgets", "☁️ Sinc / IA")

    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            ScrollableTabRow(
                selectedTabIndex = activeTab,
                containerColor = Color.Transparent,
                edgePadding = 8.dp,
                divider = {},
                indicator = {} // Using simple, beautiful highlighted borders inside Tab designs instead of complex SDK offsets
            ) {
                tabs.forEachIndexed { idx, title ->
                    val isSelected = activeTab == idx
                    Tab(
                        selected = isSelected,
                        onClick = { activeTab = idx },
                        modifier = Modifier
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) currentTheme.primary.copy(alpha = 0.12f) else Color.Transparent
                            )
                            .border(
                                1.dp,
                                if (isSelected) currentTheme.primary.copy(alpha = 0.3f) else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            ),
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) currentTheme.primary else currentTheme.textSecondary
                            )
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab Content display panels
        Box(modifier = Modifier.fillMaxWidth()) {
            when (activeTab) {
                0 -> EqualizerPanel(viewModel = viewModel, currentTheme = currentTheme)
                1 -> LibraryPanel(viewModel = viewModel, currentTheme = currentTheme)
                2 -> ThemesWidgetsConfigPanel(viewModel = viewModel, currentTheme = currentTheme)
                3 -> CloudSyncAiPanel(viewModel = viewModel, currentTheme = currentTheme)
            }
        }
    }
}

@Composable
fun CustomVerticalEqSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedRange<Float>,
    label: String,
    dbRating: String,
    currentTheme: CustomTheme,
    isPreamp: Boolean = false
) {
    val heightDp = if (isPreamp) 200.dp else 164.dp
    
    Column(
        modifier = Modifier.width(42.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .height(heightDp)
                .width(36.dp)
                .pointerInput(onValueChange, valueRange) {
                    detectTapGestures { offset ->
                        val heightPx = size.height.toFloat()
                        if (heightPx > 0f) {
                            val fraction = (1f - (offset.y / heightPx)).coerceIn(0f, 1f)
                            val rangeSize = valueRange.endInclusive - valueRange.start
                            val newVal = (valueRange.start + fraction * rangeSize).coerceIn(valueRange)
                            onValueChange(newVal)
                        }
                    }
                }
                .pointerInput(onValueChange, valueRange) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val heightPx = size.height.toFloat()
                            if (heightPx > 0f) {
                                val fraction = (1f - (offset.y / heightPx)).coerceIn(0f, 1f)
                                val rangeSize = valueRange.endInclusive - valueRange.start
                                val newVal = (valueRange.start + fraction * rangeSize).coerceIn(valueRange)
                                onValueChange(newVal)
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val heightPx = size.height.toFloat()
                            if (heightPx > 0f) {
                                val fraction = (1f - (change.position.y / heightPx)).coerceIn(0f, 1f)
                                val rangeSize = valueRange.endInclusive - valueRange.start
                                val newVal = (valueRange.start + fraction * rangeSize).coerceIn(valueRange)
                                onValueChange(newVal)
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Background track line
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .background(Color.White.copy(alpha = 0.12f))
            )

            // Active track fill matching slider level
            val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
            
            Box(
                modifier = Modifier
                    .fillMaxHeight(fraction)
                    .width(3.dp)
                    .background(currentTheme.primary)
                    .align(Alignment.BottomCenter)
            )

            // Custom capsule vertical Gray thumb (Image 2)
            val thumbHeight = 32.dp
            val verticalBias = (1f - fraction)
            
            Box(
                modifier = Modifier
                    .align(BiasAlignment(0f, verticalBias * 2f - 1f))
                    .size(18.dp, thumbHeight)
                    .background(Color(0xFF333333), RoundedCornerShape(6.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Line marker inside thumb
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(1.dp)
                        .background(Color.White)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = label,
            fontSize = 9.sp,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = dbRating,
            fontSize = 8.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
fun EqBezierResponseCurve(
    eqBands: List<Float>,
    currentTheme: CustomTheme
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Color.Black.copy(0.45f), RoundedCornerShape(10.dp))
            .border(1.dp, Color.White.copy(0.08f), RoundedCornerShape(10.dp))
    ) {
        val pointsCount = eqBands.size
        val spacingX = size.width / (pointsCount - 1)
        val path = androidx.compose.ui.graphics.Path()
        
        for (i in 0 until pointsCount) {
            val bandVal = eqBands[i]
            val fraction = (bandVal + 12f) / 24f
            val y = size.height - (fraction * size.height)
            val x = i * spacingX
            
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                val prevX = (i - 1) * spacingX
                val prevBandVal = eqBands[i - 1]
                val prevFraction = (prevBandVal + 12f) / 24f
                val prevY = size.height - (prevFraction * size.height)
                
                path.cubicTo(
                    prevX + spacingX / 2f, prevY,
                    x - spacingX / 2f, y,
                    x, y
                )
            }
        }
        
        drawPath(
            path = path,
            color = currentTheme.primary,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 4f, 
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        )
        
        drawLine(
            color = Color.White.copy(0.08f),
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = 2f
        )
    }
}

@Composable
fun EqualizerPanel(
    viewModel: PlayerViewModel,
    currentTheme: CustomTheme
) {
    val presets by viewModel.presets.collectAsStateWithLifecycle()
    val activePresetName by viewModel.activePresetName.collectAsStateWithLifecycle()
    val preamp by viewModel.preamp.collectAsStateWithLifecycle()
    val bassBoost by viewModel.bassBoost.collectAsStateWithLifecycle()
    val trebleBoost by viewModel.trebleBoost.collectAsStateWithLifecycle()
    val eqBands by viewModel.eqBands.collectAsStateWithLifecycle()
    val isBassBoosterEnabled by viewModel.isBassBoosterEnabled.collectAsStateWithLifecycle()

    var showSaveDialog by remember { mutableStateOf(false) }
    var presetInputText by remember { mutableStateOf("") }

    val bandFreqs = listOf("31", "62", "125", "250", "500", "1K", "2K", "4K", "8K", "16K")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(Color.Black.copy(0.2f), RoundedCornerShape(18.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 1. Header (Centrado)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🎛️ EQ MULTIBANDA", color = currentTheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.5.sp)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. High-Density Vertically Aligned Sliders (Preamp + 10 Bands)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Preamp vertical slider (taller block in Image 2)
            Column(
                modifier = Modifier
                    .width(42.dp)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CustomVerticalEqSlider(
                    value = preamp,
                    onValueChange = { viewModel.updatePreamp(it) },
                    valueRange = -12f..12f,
                    label = "Preamp",
                    dbRating = String.format("%.1f", preamp),
                    currentTheme = currentTheme,
                    isPreamp = true
                )
            }

            // Divider vertical line
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(Color.White.copy(alpha = 0.1f))
            )
            Spacer(modifier = Modifier.width(12.dp))

            // Right: Scrollable 10 Bands (Image 2)
            LazyRow(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(10) { index ->
                    val bandValue = eqBands[index]
                    CustomVerticalEqSlider(
                        value = bandValue,
                        onValueChange = { db -> viewModel.updateBand(index, db) },
                        valueRange = -12f..12f,
                        label = bandFreqs[index],
                        dbRating = String.format("%.1f", bandValue),
                        currentTheme = currentTheme,
                        isPreamp = false
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Spline curve frequency graph (Image 2 response curve)
        EqBezierResponseCurve(eqBands = eqBands, currentTheme = currentTheme)

        Spacer(modifier = Modifier.height(6.dp))

        // Text below graph (Image 2 text: "SIN DVC EQ 10 TON LMT")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                "EQ 10 BANDAS • SINTONIZACIÓN ULTRA ACÚSTICA",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = currentTheme.textSecondary.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Custom Preset and Mode Option Pills (Image 2 buttons)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ecu Reset Button (Outline pill container)
            Box(
                modifier = Modifier
                    .border(1.dp, currentTheme.primary, RoundedCornerShape(12.dp))
                    .background(currentTheme.primary.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .clickable { viewModel.resetToDefaultEq() }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text("Ecu Reset", fontSize = 11.sp, color = currentTheme.primary, fontWeight = FontWeight.Bold)
            }

            // Preajuste Dropdown Selection (Large Pill Button)
            Box(
                modifier = Modifier
                    .border(1.dp, Color.White.copy(0.12f), RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(0.3f), RoundedCornerShape(20.dp))
                    .clickable { showSaveDialog = true }
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        activePresetName,
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("▼", fontSize = 8.sp, color = Color.White.copy(alpha = 0.6f))
                }
            }

            // Three dots
            IconButton(
                onClick = {},
                modifier = Modifier.size(28.dp)
            ) {
                Text("⋮", color = Color.White, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 5. Graves, Bass Booster and Agudos Analog Knobs (Image 2 bottom-most dials)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SoundSliderDialKnob(
                    value = bassBoost,
                    onValueChange = { viewModel.updateBassBoost(it) },
                    range = 0f..100f,
                    label = "Graves: ${bassBoost.toInt()}%",
                    currentTheme = currentTheme
                )
            }

            // High-end glowing BASS BOOSTER toggle button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            if (isBassBoosterEnabled) currentTheme.primary.copy(alpha = 0.25f)
                            else Color.Black.copy(alpha = 0.35f)
                        )
                        .border(
                            width = 2.dp,
                            brush = if (isBassBoosterEnabled) {
                                Brush.radialGradient(
                                    colors = listOf(currentTheme.primary, currentTheme.accent)
                                )
                            } else {
                                Brush.linearGradient(
                                    colors = listOf(Color.White.copy(0.12f), Color.White.copy(0.04f))
                                )
                            },
                            shape = CircleShape
                        )
                        .clickable { viewModel.toggleBassBooster() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "🔊", 
                            fontSize = 18.sp,
                            color = if (isBassBoosterEnabled) currentTheme.accent else Color.White.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            if (isBassBoosterEnabled) "ACTIVE" else "SUPER", 
                            fontSize = 8.sp, 
                            fontWeight = FontWeight.Bold,
                            color = if (isBassBoosterEnabled) currentTheme.primary else Color.White.copy(alpha = 0.4f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "BASS BOOSTER",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isBassBoosterEnabled) currentTheme.primary else currentTheme.textPrimary
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SoundSliderDialKnob(
                    value = trebleBoost,
                    onValueChange = { viewModel.updateTrebleBoost(it) },
                    range = 0f..100f,
                    label = "Agudos: ${trebleBoost.toInt()}%",
                    currentTheme = currentTheme
                )
            }
        }
    }

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            containerColor = currentTheme.surface,
            title = {
                Text(
                    "Aplicar o Crear Preajuste",
                    color = currentTheme.textPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        "Selecciona un ecualizador acústico predefinido:",
                        color = currentTheme.textSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(presets) { preset ->
                            val isSelected = preset.name == activePresetName
                            Card(
                                modifier = Modifier
                                    .clickable { 
                                        viewModel.applyPreset(preset)
                                        showSaveDialog = false
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) currentTheme.primary.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.2f)
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) currentTheme.primary else Color.Transparent
                                )
                            ) {
                                Text(
                                    preset.name,
                                    fontSize = 11.sp,
                                    color = if (isSelected) currentTheme.primary else currentTheme.textPrimary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    
                    Text(
                        "O dale nombre a tu preajuste actual:",
                        color = currentTheme.textSecondary,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = presetInputText,
                        onValueChange = { presetInputText = it },
                        singleLine = true,
                        placeholder = { Text("Ej. Mis Graves Enormes") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = currentTheme.textPrimary,
                            unfocusedTextColor = currentTheme.textPrimary,
                            focusedBorderColor = currentTheme.primary,
                            unfocusedBorderColor = currentTheme.textSecondary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (presetInputText.isNotEmpty()) {
                            viewModel.saveAsNewCustomPreset(presetInputText)
                            showSaveDialog = false
                            presetInputText = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = currentTheme.primary)
                ) {
                    Text("GUARDAR", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("CANCELAR", color = currentTheme.textSecondary)
                }
            }
        )
    }
}

@Composable
fun SoundSliderDialKnob(
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedRange<Float>,
    label: String,
    currentTheme: CustomTheme
) {
    // Custom Circular Dial Knob with Sweep Progress and dragging detection.
    var startValue by remember { mutableStateOf(0f) }
    var dragAccumulator by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .size(75.dp)
            .pointerInput(range) {
                detectDragGestures(
                    onDragStart = {
                        dragAccumulator = 0f
                        startValue = value
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulator -= dragAmount.y
                        val sensitivity = 0.5f
                        val delta = dragAccumulator * sensitivity
                        val scale = range.endInclusive - range.start
                        val newVal = (startValue + (delta / 100f) * scale).coerceIn(range)
                        onValueChange(newVal)
                    }
                )
            }
            .drawBehind {
                val strokeWidth = 5.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2f
                val center = Offset(size.width / 2f, size.height / 2f)

                // Background track Arc
                drawArc(
                    color = Color.Black.copy(alpha = 0.4f),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth)
                )

                // Active sweeps
                val fraction = (value - range.start) / (range.endInclusive - range.start)
                val activeSweep = fraction * 270f
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(currentTheme.primary, currentTheme.accent, currentTheme.primary)
                    ),
                    startAngle = 135f,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )

                // Outer bezel metal ring
                drawCircle(
                    color = currentTheme.primary.copy(alpha = 0.35f),
                    radius = radius - 8f,
                    style = Stroke(width = 1.dp.toPx())
                )

                // Knob center indicator needle dot
                val currentAngle = 135f + activeSweep
                val rad = (currentAngle * PI / 180f)
                val needleLength = radius - 15f
                val dotX = center.x + needleLength * cos(rad).toFloat()
                val dotY = center.y + needleLength * sin(rad).toFloat()
                drawCircle(
                    color = currentTheme.accent,
                    radius = 4f,
                    center = Offset(dotX, dotY)
                )
            }
    )

    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = label,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = currentTheme.textPrimary
    )
    Text(
        text = "${value.toInt()}%",
        fontSize = 10.sp,
        color = currentTheme.accent,
        fontWeight = FontWeight.Medium
    )
}

@Composable
fun TrackRow(
    track: com.example.data.model.Track,
    isSelected: Boolean,
    currentTheme: CustomTheme,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .background(
                if (isSelected) currentTheme.primary.copy(alpha = 0.08f) else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Small thumbnail icon matching track format
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    if (isSelected) currentTheme.primary.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.25f),
                    RoundedCornerShape(6.dp)
                )
                .border(
                    1.dp,
                    if (isSelected) currentTheme.primary else Color.White.copy(alpha = 0.05f),
                    RoundedCornerShape(6.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                track.format,
                color = if (isSelected) currentTheme.accent else currentTheme.textSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                track.title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isSelected) currentTheme.primary else currentTheme.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${track.artist} • ${track.album}",
                fontSize = 11.sp,
                color = currentTheme.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                track.sampleRate + " / " + track.bitDepth,
                fontSize = 9.sp,
                color = currentTheme.accent,
                fontWeight = FontWeight.Medium
            )
            Text(
                track.fileSize,
                fontSize = 10.sp,
                color = currentTheme.textSecondary
            )
        }
    }
}

@Composable
fun LibraryPanel(
    viewModel: PlayerViewModel,
    currentTheme: CustomTheme
) {
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
    val playlistTracks by viewModel.playlistTracks.collectAsStateWithLifecycle()
    val scanStatus by viewModel.scanStatus.collectAsStateWithLifecycle()
    val selectedFolder by viewModel.selectedFolder.collectAsStateWithLifecycle()
    
    val context = androidx.compose.ui.platform.LocalContext.current

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result.values.any { it }
        if (granted) {
            viewModel.scanLocalMusic()
        }
    }

    fun triggerScan() {
        val permissions = if (android.os.Build.VERSION.SDK_INT >= 33) {
            arrayOf("android.permission.READ_MEDIA_AUDIO")
        } else {
            arrayOf("android.permission.READ_EXTERNAL_STORAGE")
        }

        val allGranted = permissions.all { perm ->
            androidx.core.content.ContextCompat.checkSelfPermission(context, perm) == 
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            viewModel.scanLocalMusic()
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    val groupedByFolder = remember(playlistTracks) { playlistTracks.groupBy { it.genre } }
    var collapsedFolders by remember { mutableStateOf(setOf<String>()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = currentTheme.surface.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "BIBLIOTECA DE AUDIO",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = currentTheme.primary
                        )
                        Text(
                            scanStatus,
                            fontSize = 10.sp,
                            color = currentTheme.textSecondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    
                    Button(
                        onClick = { triggerScan() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = currentTheme.primary.copy(alpha = 0.12f),
                            contentColor = currentTheme.primary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Escanear Música", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (groupedByFolder.isEmpty()) {
            item {
                Text(
                    text = "No se ha escaneado música aún o el almacenamiento está vacío. Toca arriba para escanear.",
                    color = currentTheme.textSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp, horizontal = 16.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            groupedByFolder.forEach { (folderName, tracksInFolder) ->
                val isExpanded = !collapsedFolders.contains(folderName)
                val isFolderPlaying = folderName == selectedFolder
                
                item(key = "folder_$folderName") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .clickable {
                                collapsedFolders = if (isExpanded) {
                                    collapsedFolders + folderName
                                } else {
                                    collapsedFolders - folderName
                                }
                            }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("📁", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = folderName.uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = if (isFolderPlaying) currentTheme.primary else Color.White,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${tracksInFolder.size})",
                                color = currentTheme.primary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (isFolderPlaying) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(currentTheme.primary.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .border(0.5.dp, currentTheme.primary.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        "ACTIVA",
                                        color = currentTheme.primary,
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.startPlayingFolder(folderName) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text(
                                    text = "▶",
                                    color = currentTheme.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isExpanded) "▲" else "▼",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                if (isExpanded) {
                    items(
                        items = tracksInFolder,
                        key = { it.id }
                    ) { track ->
                        val isSelected = track.id == currentTrack.id
                        TrackRow(
                            track = track,
                            isSelected = isSelected,
                            currentTheme = currentTheme,
                            onClick = { viewModel.selectTrack(track.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ThemesWidgetsConfigPanel(
    viewModel: PlayerViewModel,
    currentTheme: CustomTheme
) {
    val chosenTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
    val widgetSize by viewModel.widgetSize.collectAsStateWithLifecycle()
    val widgetOpacity by viewModel.widgetOpacity.collectAsStateWithLifecycle()
    val widgetAccentColor by viewModel.widgetAccentColor.collectAsStateWithLifecycle()
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = currentTheme.surface.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "PERSONALIZACIÓN DE TEMAS DINÁMICOS",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = currentTheme.primary
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Scroll of 6 custom beautiful themes
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(MusicThemes.themes) { theme ->
                    val isSelected = theme.id == chosenTheme.id
                    Card(
                        modifier = Modifier
                            .width(135.dp)
                            .clickable { viewModel.selectTheme(theme.id) }
                            .shadow(
                                elevation = if (isSelected) 8.dp else 0.dp,
                                ambientColor = theme.glowColor,
                                spotColor = theme.glowColor
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = theme.background
                        ),
                        border = BorderStroke(
                            2.dp,
                            if (isSelected) theme.primary else Color.White.copy(alpha = 0.05f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    theme.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = theme.textPrimary
                                )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(theme.accent, CircleShape)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                theme.description,
                                fontSize = 9.sp,
                                color = theme.textSecondary,
                                maxLines = 3,
                                lineHeight = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Resizable Widgets Customizer Preview Area
            Text(
                "WIDGETS REDIMENSIONABLES Y PREESTRENO",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = currentTheme.primary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                "Simula el estilo del widget inteligente que verás en tu pantalla de inicio:",
                fontSize = 11.sp,
                color = currentTheme.textSecondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Size Class buttons selectors (Minimal, standard, immersive)
            val sizes = listOf("4x1 Minimal", "4x2 Standard", "4x4 Immersive")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                sizes.forEach { size ->
                    FilledTonalButton(
                        onClick = { viewModel.setWidgetSize(size) },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = if (widgetSize == size) currentTheme.primary else Color.Black.copy(alpha = 0.3f),
                            contentColor = if (widgetSize == size) Color.Black else currentTheme.textPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(size.split(" ")[0], fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Background Opacity slider simulation
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Opacidad: ${widgetOpacity.roundToInt()}%",
                    fontSize = 11.sp,
                    color = currentTheme.textPrimary,
                    modifier = Modifier.width(90.dp)
                )
                Slider(
                    value = widgetOpacity,
                    onValueChange = { viewModel.setWidgetOpacity(it) },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(activeTrackColor = currentTheme.primary),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Live Widget Sandbox Preview
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .shadow(8.dp, RoundedCornerShape(14.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2125)), // Simulated phone drawer wallpaper
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Vista Satélite del Escritorio (Simulado)",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // The Scaled Widget Box itself
                    val parsedColor = remember(widgetAccentColor) {
                        try {
                            Color(android.graphics.Color.parseColor(widgetAccentColor))
                        } catch (e: Exception) {
                            currentTheme.accent
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (widgetSize == "4x1 Minimal") 0.9f else 1.0f)
                            .height(
                                when (widgetSize) {
                                    "4x1 Minimal" -> 80.dp
                                    "4x2 Standard" -> 130.dp
                                    else -> 210.dp
                                }
                            )
                            .background(
                                Color(0xFF121417).copy(alpha = widgetOpacity / 100f),
                                RoundedCornerShape(16.dp)
                            )
                            .border(1.5.dp, parsedColor.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                            .padding(14.dp)
                    ) {
                        when (widgetSize) {
                            "4x1 Minimal" -> {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(parsedColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "🎵",
                                                fontSize = 16.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                currentTrack.title,
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                currentTrack.artist,
                                                color = Color.LightGray,
                                                fontSize = 10.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("▶", color = Color.White, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text("⏭", color = Color.White, fontSize = 14.sp)
                                    }
                                }
                            }

                            "4x2 Standard" -> {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                currentTrack.title,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                currentTrack.artist,
                                                color = Color.LightGray,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .background(parsedColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                .border(1.dp, parsedColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                currentTrack.format,
                                                color = parsedColor,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 9.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Display track technical details in 4x2
                                    Text(
                                        "Audiophile Studio • ${currentTrack.bitDepth} • ${currentTrack.sampleRate}",
                                        color = parsedColor.copy(alpha = 0.8f),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("⏮", color = Color.LightGray, fontSize = 16.sp)
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .background(parsedColor, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("⏸", color = Color.Black, fontSize = 16.sp)
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Text("⏭", color = Color.LightGray, fontSize = 16.sp)
                                    }
                                }
                            }

                            else -> { // Immersive 4x4 layout containing track artwork list
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("🎧", fontSize = 20.sp)
                                        }
                                        
                                        Column(
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            Text(
                                                "Sincronizados FLAC",
                                                color = parsedColor,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                currentTrack.fileSize,
                                                color = Color.LightGray,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            currentTrack.title,
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            currentTrack.artist,
                                            color = Color.LightGray,
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }

                                    // Display mock seekbar in 4x4 widget
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("01:24", color = Color.Gray, fontSize = 9.sp)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(horizontal = 8.dp)
                                                .height(3.dp)
                                                .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(1.5.dp))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(0.4f)
                                                    .height(3.dp)
                                                    .background(parsedColor, RoundedCornerShape(1.5.dp))
                                            )
                                        }
                                        Text("04:30", color = Color.Gray, fontSize = 9.sp)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceAround,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("🔀", fontSize = 14.sp)
                                        Text("⏮", fontSize = 18.sp, color = Color.LightGray)
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .background(parsedColor, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("▶", color = Color.Black, fontSize = 20.sp)
                                        }
                                        Text("⏭", fontSize = 18.sp, color = Color.LightGray)
                                        Text("🔁", fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CloudSyncAiPanel(
    viewModel: PlayerViewModel,
    currentTheme: CustomTheme
) {
    val cloudEmail by viewModel.cloudUserEmail.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val lastSyncTime by viewModel.lastSyncTime.collectAsStateWithLifecycle()

    val geminiResult by viewModel.geminiResult.collectAsStateWithLifecycle()
    val isGeminiLoading by viewModel.isGeminiLoading.collectAsStateWithLifecycle()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = currentTheme.surface.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "SINCRONIZACIÓN EN LA NUBE MULTIDISPOSITIVO",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = currentTheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Sync card section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        "Respalda de forma segura tus curvas de ecualización de 10 bandas, listas de reproducción sin pérdida y configuraciones de temas en todos tus dispositivos.",
                        fontSize = 11.sp,
                        color = currentTheme.textSecondary,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = cloudEmail,
                        onValueChange = { viewModel.updateCloudEmail(it) },
                        label = { Text("Correo Electrónico Cloud", fontSize = 11.sp, color = currentTheme.primary) },
                        singleLine = true,
                        leadingIcon = { Text("☁️", fontSize = 14.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = currentTheme.textPrimary,
                            unfocusedTextColor = currentTheme.textPrimary,
                            focusedBorderColor = currentTheme.primary,
                            unfocusedBorderColor = currentTheme.textSecondary
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Estado: $syncStatus",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (syncStatus == "Sincronizado") currentTheme.accent else currentTheme.textPrimary
                            )
                            if (lastSyncTime.isNotEmpty()) {
                                Text(
                                    "Último respaldo: $lastSyncTime",
                                    fontSize = 10.sp,
                                    color = currentTheme.textSecondary
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.runCloudSync() },
                            colors = ButtonDefaults.buttonColors(containerColor = currentTheme.primary),
                            enabled = syncStatus != "Sincronizando..."
                        ) {
                            if (syncStatus == "Sincronizando...") {
                                SimpleTimerCircularProgressIndicator(color = Color.Black)
                            } else {
                                Text("SINCRONIZAR", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // AI Sound Expert card section
            Text(
                "AUDITORÍA E INTELIGENCIA ACÚSTICA IA (GEMINI)",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = currentTheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Utiliza Gemini para evaluar científicamente tu calibración paramétrica y proponer mejoras acústicas de nivel audiófilo.",
                fontSize = 11.sp,
                color = currentTheme.textSecondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Button(
                onClick = { viewModel.triggerGeminiAnalysis() },
                colors = ButtonDefaults.buttonColors(containerColor = currentTheme.accent),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isGeminiLoading
            ) {
                if (isGeminiLoading) {
                    SimpleTimerCircularProgressIndicator(color = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Analizando audio...", color = Color.Black, fontSize = 12.sp)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🧠", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("GENERAR AUDITORÍA ACÚSTICA IA", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (geminiResult.isNotEmpty() || isGeminiLoading) {
                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1113)),
                    border = BorderStroke(1.dp, currentTheme.accent.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🔮", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Gemini Sound Core AI", color = currentTheme.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        if (isGeminiLoading) {
                            Text(
                                "Calculando coeficientes de fourier y curvas de resonancia...",
                                color = currentTheme.textSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.alpha(0.7f)
                            )
                        } else {
                            Text(
                                text = geminiResult,
                                color = currentTheme.textPrimary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleTimerCircularProgressIndicator(color: Color) {
    CircularProgressIndicator(
        modifier = Modifier.size(16.dp),
        color = color,
        strokeWidth = 2.dp
    )
}

fun formatTime(ms: Long): String {
    val totalSecs = ms / 1000
    val minutes = totalSecs / 60
    val seconds = totalSecs % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Composable
fun PlayerInfoPanel(currentTheme: CustomTheme) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = currentTheme.surface.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "ACERCA DEL REPRODUCTOR",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = currentTheme.primary,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Version Block
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("ℹ️", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Versión del Reproductor",
                        fontSize = 10.sp,
                        color = currentTheme.textSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Versión 01",
                        fontSize = 12.sp,
                        color = currentTheme.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Author Block
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("👨‍💻", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Creado por",
                        fontSize = 10.sp,
                        color = currentTheme.textSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Lic. Reynaldo Santiago Cayetano",
                        fontSize = 12.sp,
                        color = currentTheme.textPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.05f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(12.dp))

            // Email Block
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("📧", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Correo Electrónico",
                        fontSize = 10.sp,
                        color = currentTheme.textSecondary,
                        fontWeight = FontWeight.Medium
                    )
                    androidx.compose.foundation.text.selection.SelectionContainer {
                        Text(
                            text = "Reynaldosantiagocayetano@gmail.com",
                            fontSize = 12.sp,
                            color = currentTheme.textPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
