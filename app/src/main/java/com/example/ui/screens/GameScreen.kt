package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.HeartBroken
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.PlayerProgress
import com.example.model.ConfettiPiece
import com.example.model.PuzzleLevel
import com.example.ui.components.MiniBoardPreview
import com.example.ui.components.PuzzleBoardCanvas
import com.example.ui.theme.LocalPuzzleColors
import com.example.viewmodel.GameUiState

@Composable
fun GameScreen(
    state: GameUiState,
    progress: PlayerProgress,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onTapBoard: (boardX: Float, boardY: Float, cellSize: Float, originX: Float, originY: Float) -> Unit,
    onUseHint: () -> Unit,
    onToggleGridDots: () -> Unit,
    onRestart: () -> Unit,
    onNextLevel: () -> Unit,
    onMainMenu: () -> Unit,
    onStepAnimations: (nowNanos: Long, dtSeconds: Float) -> Unit
) {
    val puzzleColors = LocalPuzzleColors.current
    var resetViewTrigger by remember { mutableIntStateOf(0) }

    // Continuous 60fps animation ticker whenever arrows, particles, or confetti are active
    val hasActiveAnimations = state.flyingArrows.isNotEmpty() ||
        state.blockedAnims.isNotEmpty() ||
        state.particles.isNotEmpty() ||
        state.confetti.isNotEmpty()

    LaunchedEffect(hasActiveAnimations) {
        if (!hasActiveAnimations) return@LaunchedEffect
        var lastNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                val dt = if (lastNanos == 0L) 0.016f else ((frameNanos - lastNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                lastNanos = frameNanos
                onStepAnimations(frameNanos, dt)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = puzzleColors.boardBackground
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .widthIn(max = 720.dp)
                    .align(Alignment.Center)
            ) {
                // Top Bar: Back, Level X, Settings
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("game_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBackIos,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = state.level?.let { stringResource(R.string.level_label, it.levelNumber) } ?: "",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        state.level?.let { lvl ->
                            Text(
                                text = lvl.shape.displayName,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("game_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = stringResource(R.string.cd_settings),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // HUD Status Row: Arrow Counter Pill | 3 Hearts | Difficulty Pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Remaining arrows counter pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = puzzleColors.pillBackground
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.NearMe,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${state.remainingArrows.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("remaining_arrows_count")
                            )
                        }
                    }

                    // 3 Hearts
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val maxHearts = state.level?.maxHearts ?: 3
                        for (i in 1..maxHearts) {
                            val isFull = i <= state.heartsRemaining
                            val heartScale by animateFloatAsState(
                                targetValue = if (isFull) 1f else 0.82f,
                                animationSpec = spring(dampingRatio = 0.5f),
                                label = "heartScale_$i"
                            )
                            Icon(
                                imageVector = Icons.Rounded.Favorite,
                                contentDescription = stringResource(
                                    if (isFull) R.string.cd_heart_full else R.string.cd_heart_empty
                                ),
                                tint = if (isFull) puzzleColors.heartFull else puzzleColors.heartEmpty,
                                modifier = Modifier
                                    .size(26.dp)
                                    .scale(heartScale)
                            )
                        }
                    }

                    // Difficulty Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = puzzleColors.pillBackground
                    ) {
                        Text(
                            text = state.level?.difficulty?.label ?: "Normal",
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }

                // Helpful onboarding banner on Level 1
                AnimatedVisibility(visible = state.level?.levelNumber == 1 && !state.isVictory && state.movesCount == 0) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 6.dp)
                    ) {
                        Text(
                            text = "Tap an arrow with a clear path to fly it away!",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }

                // Puzzle Board Area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (state.isLoading || state.level == null) {
                        CircularProgressIndicator()
                    } else {
                        PuzzleBoardCanvas(
                            level = state.level,
                            remainingArrows = state.remainingArrows,
                            flyingArrows = state.flyingArrows,
                            blockedAnims = state.blockedAnims,
                            particles = state.particles,
                            hintedArrowId = state.hintedArrowId,
                            showGridDots = progress.showGridDots,
                            resetViewTrigger = resetViewTrigger,
                            onTapBoard = onTapBoard,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("puzzle_board_canvas")
                        )
                    }
                }

                // Bottom Action Buttons (Hint with badge, Grid toggle, Recenter, Restart)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hint circular button with blue badge
                    Box(contentAlignment = Alignment.TopEnd) {
                        CircularToolButton(
                            icon = Icons.Rounded.Lightbulb,
                            contentDescription = stringResource(R.string.cd_hint),
                            isActive = state.hintedArrowId != null,
                            onClick = onUseHint,
                            testTag = "game_hint_button"
                        )
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .offset(x = 4.dp, y = (-4).dp)
                                .size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (progress.hintsRemaining > 0) "${progress.hintsRemaining}" else "+",
                                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Grid Guides Toggle (#)
                    CircularToolButton(
                        icon = Icons.Rounded.Tag,
                        contentDescription = stringResource(R.string.cd_grid_toggle),
                        isActive = progress.showGridDots,
                        onClick = onToggleGridDots,
                        testTag = "game_grid_toggle_button"
                    )

                    // Recenter Zoom/Pan
                    CircularToolButton(
                        icon = Icons.Rounded.CenterFocusStrong,
                        contentDescription = stringResource(R.string.cd_recenter),
                        isActive = false,
                        onClick = { resetViewTrigger++ },
                        testTag = "game_recenter_button"
                    )

                    // Restart Level
                    CircularToolButton(
                        icon = Icons.Rounded.Refresh,
                        contentDescription = stringResource(R.string.cd_restart),
                        isActive = false,
                        onClick = onRestart,
                        testTag = "game_restart_button"
                    )
                }
            }
        }

        // Victory Screen Overlay (faithful to reference Screenshot 2 & video)
        AnimatedVisibility(
            visible = state.isVictory && state.level != null,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(200))
        ) {
            state.level?.let { level ->
                VictoryOverlay(
                    level = level,
                    starsEarned = state.starsEarned,
                    confetti = state.confetti,
                    onNextLevel = onNextLevel,
                    onMainMenu = onMainMenu
                )
            }
        }

        // Game Over / Out of Hearts Overlay
        AnimatedVisibility(
            visible = state.isGameOver,
            enter = fadeIn(tween(220)) + scaleIn(initialScale = 0.9f),
            exit = fadeOut(tween(150))
        ) {
            OutOfHeartsOverlay(
                onRetry = onRestart,
                onMainMenu = onMainMenu
            )
        }
    }
}

@Composable
private fun CircularToolButton(
    icon: ImageVector,
    contentDescription: String,
    isActive: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        shape = CircleShape,
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
        tonalElevation = 2.dp,
        modifier = Modifier.size(56.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .fillMaxSize()
                .testTag(testTag)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun VictoryOverlay(
    level: PuzzleLevel,
    starsEarned: Int,
    confetti: List<ConfettiPiece>,
    onNextLevel: () -> Unit,
    onMainMenu: () -> Unit
) {
    val puzzleColors = LocalPuzzleColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "victorySunburst")
    val beamRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "beamRot"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        puzzleColors.victoryGradientStart,
                        puzzleColors.victoryGradientEnd
                    )
                )
            )
    ) {
        // Rotating Sunburst Rays + Confetti
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.45f)
            val radius = size.maxDimension * 1.1f
            rotate(degrees = beamRotation, pivot = center) {
                val rayCount = 16
                val sweep = 360f / (rayCount * 2)
                for (i in 0 until rayCount) {
                    val startAngle = i * (360f / rayCount)
                    val path = Path().apply {
                        moveTo(center.x, center.y)
                        val a1 = Math.toRadians(startAngle.toDouble())
                        val a2 = Math.toRadians((startAngle + sweep).toDouble())
                        lineTo(
                            center.x + (radius * kotlin.math.cos(a1)).toFloat(),
                            center.y + (radius * kotlin.math.sin(a1)).toFloat()
                        )
                        lineTo(
                            center.x + (radius * kotlin.math.cos(a2)).toFloat(),
                            center.y + (radius * kotlin.math.sin(a2)).toFloat()
                        )
                        close()
                    }
                    drawPath(path = path, color = Color.White.copy(alpha = 0.08f))
                }
            }

            // Confetti pieces
            for (c in confetti) {
                val cx = c.xFraction * size.width
                val cy = c.yFraction * size.height
                rotate(degrees = c.rotation, pivot = Offset(cx, cy)) {
                    drawRect(
                        color = c.color,
                        topLeft = Offset(cx - c.width / 2f, cy - c.height / 2f),
                        size = Size(c.width, c.height)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .widthIn(max = 520.dp)
                .align(Alignment.Center)
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.train_your_brain),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 32.sp,
                        lineHeight = 38.sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.level_completed),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White.copy(alpha = 0.95f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))
                // 3 Star Rating
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (s in 1..3) {
                        val earned = s <= starsEarned
                        Icon(
                            imageVector = if (earned) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                            contentDescription = null,
                            tint = if (earned) Color(0xFFFACC15) else Color.White.copy(alpha = 0.45f),
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }
            }

            // Crisp white miniature board card (matching reference Screenshot 2!)
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                shadowElevation = 14.dp,
                modifier = Modifier.size(230.dp)
            ) {
                MiniBoardPreview(
                    level = level,
                    arrowColor = Color(0xFF0F1B35),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onNextLevel,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF1A84F5)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag("victory_next_button")
                ) {
                    Text(
                        text = stringResource(R.string.next_level),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                TextButton(
                    onClick = onMainMenu,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("victory_main_button")
                ) {
                    Text(
                        text = stringResource(R.string.main_menu),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun OutOfHeartsOverlay(
    onRetry: () -> Unit,
    onMainMenu: () -> Unit
) {
    val puzzleColors = LocalPuzzleColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99091124))
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 16.dp,
            modifier = Modifier.widthIn(max = 380.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = puzzleColors.heartFull.copy(alpha = 0.14f),
                    modifier = Modifier.size(68.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.HeartBroken,
                            contentDescription = null,
                            tint = puzzleColors.heartFull,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.out_of_hearts),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.out_of_hearts_desc),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onRetry,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("gameover_retry_button")
                ) {
                    Text(
                        text = stringResource(R.string.retry_level),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = onMainMenu,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("gameover_main_button")
                ) {
                    Text(
                        text = stringResource(R.string.main_menu),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
