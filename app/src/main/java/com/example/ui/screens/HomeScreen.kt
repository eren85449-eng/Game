package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChangeHistory
import androidx.compose.material.icons.rounded.Cyclone
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.ViewQuilt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.PlayerProgress
import com.example.engine.PuzzleEngine
import com.example.model.PuzzleShape
import com.example.ui.components.MiniBoardPreview
import com.example.ui.theme.LocalPuzzleColors

@Composable
fun HomeScreen(
    progress: PlayerProgress,
    onPlayLevel: (levelNumber: Int, shapeOverride: PuzzleShape?) -> Unit,
    onOpenSettings: () -> Unit
) {
    val puzzleColors = LocalPuzzleColors.current
    val nextLevel = progress.highestUnlockedLevel.coerceIn(1, 250)
    val heroPreviewLevel = remember(nextLevel) {
        PuzzleEngine.generateLevel(nextLevel)
    }
    val totalStars = remember(progress.levelStars) {
        progress.levelStars.values.sum()
    }
    val clearedCount = remember(progress.levelStars) {
        progress.levelStars.count { it.value > 0 }
    }

    // Show levels up to unlocked + 15 upcoming preview levels
    val visibleLevelCount = (nextLevel + 15).coerceIn(24, 250)
    val levelRows = remember(visibleLevelCount) {
        (1..visibleLevelCount).chunked(4)
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Top Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Puzzle & Brain Challenge",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 3.dp
                        ) {
                            IconButton(
                                onClick = onOpenSettings,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("home_settings_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Settings,
                                    contentDescription = stringResource(R.string.cd_settings),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Stats Strip
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatChipCard(
                            icon = Icons.Rounded.AutoAwesome,
                            iconTint = MaterialTheme.colorScheme.primary,
                            value = "$clearedCount",
                            label = "Cleared",
                            modifier = Modifier.weight(1f)
                        )
                        StatChipCard(
                            icon = Icons.Rounded.Star,
                            iconTint = puzzleColors.goldStar,
                            value = "$totalStars",
                            label = "Stars",
                            modifier = Modifier.weight(1f)
                        )
                        StatChipCard(
                            icon = Icons.Rounded.Lightbulb,
                            iconTint = Color(0xFF0EA5E9),
                            value = "${progress.hintsRemaining}",
                            label = "Hints",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Hero Play Card
                item {
                    Card(
                        shape = RoundedCornerShape(26.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF0F2657),
                                            Color(0xFF1A84F5)
                                        )
                                    )
                                )
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color(0x2EFFFFFF)
                                    ) {
                                        Text(
                                            text = "${heroPreviewLevel.shape.displayName} • ${heroPreviewLevel.difficulty.label}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = stringResource(R.string.level_label, nextLevel),
                                        style = MaterialTheme.typography.displaySmall.copy(
                                            fontWeight = FontWeight.ExtraBold
                                        ),
                                        color = Color.White
                                    )
                                    Text(
                                        text = "${heroPreviewLevel.initialArrows.size} arrows to clear",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFFDCEBFF)
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(
                                        onClick = { onPlayLevel(nextLevel, null) },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White,
                                            contentColor = Color(0xFF0F2657)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                                        modifier = Modifier
                                            .height(48.dp)
                                            .testTag("play_continue_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.continue_level, nextLevel),
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = Color.White,
                                    shadowElevation = 4.dp,
                                    modifier = Modifier.size(104.dp)
                                ) {
                                    MiniBoardPreview(
                                        level = heroPreviewLevel,
                                        arrowColor = Color(0xFF0F1B35),
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Signature Shape Packs (Instant access to Heart, Spiral, Pyramid, Maze, etc.)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Shape Challenges",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val showcaseShapes = listOf(
                                Triple(PuzzleShape.HEART, 16, Icons.Rounded.Favorite),
                                Triple(PuzzleShape.SPIRAL, 24, Icons.Rounded.Cyclone),
                                Triple(PuzzleShape.PYRAMID, 12, Icons.Rounded.ChangeHistory),
                                Triple(PuzzleShape.MAZE, 32, Icons.Rounded.ViewQuilt),
                                Triple(PuzzleShape.LEAF, 40, Icons.Rounded.Eco),
                                Triple(PuzzleShape.DIAMOND, 18, Icons.Rounded.Diamond)
                            )
                            items(showcaseShapes) { (shape, demoLevel, icon) ->
                                ShapeChallengeCard(
                                    shape = shape,
                                    icon = icon,
                                    onClick = { onPlayLevel(demoLevel, shape) }
                                )
                            }
                        }
                    }
                }

                // Campaign Levels Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "All Levels",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Unlocked: $nextLevel / 250",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // 4-column level tiles
                items(levelRows) { rowLevels ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (lvl in rowLevels) {
                            val isUnlocked = lvl <= nextLevel
                            val stars = progress.levelStars[lvl] ?: 0
                            LevelTileCard(
                                levelNumber = lvl,
                                isUnlocked = isUnlocked,
                                isCurrent = lvl == nextLevel,
                                stars = stars,
                                onClick = { if (isUnlocked) onPlayLevel(lvl, null) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(4 - rowLevels.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                // Developer Attribution Footer
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.developer_attribution),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChipCard(
    icon: ImageVector,
    iconTint: Color,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ShapeChallengeCard(
    shape: PuzzleShape,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val accent = when (shape) {
        PuzzleShape.HEART -> Color(0xFFEF4444)
        PuzzleShape.SPIRAL -> Color(0xFF8B5CF6)
        PuzzleShape.PYRAMID -> Color(0xFFF59E0B)
        PuzzleShape.MAZE -> Color(0xFF1A84F5)
        PuzzleShape.LEAF -> Color(0xFF10B981)
        PuzzleShape.DIAMOND -> Color(0xFF06B6D4)
        PuzzleShape.GRID -> Color(0xFF0F172A)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f)),
        modifier = Modifier
            .width(142.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("shape_card_${shape.name.lowercase()}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accent.copy(alpha = 0.14f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Text(
                text = shape.displayName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = shape.subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 11.sp, lineHeight = 14.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LevelTileCard(
    levelNumber: Int,
    isUnlocked: Boolean,
    isCurrent: Boolean,
    stars: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val puzzleColors = LocalPuzzleColors.current
    val bgColor = when {
        isCurrent -> MaterialTheme.colorScheme.primary
        isUnlocked -> MaterialTheme.colorScheme.surface
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    }
    val contentColor = when {
        isCurrent -> Color.White
        isUnlocked -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = bgColor,
        shadowElevation = if (isUnlocked) 3.dp else 0.dp,
        border = if (stars > 0 && !isCurrent) BorderStroke(1.dp, puzzleColors.goldStar.copy(alpha = 0.45f)) else null,
        modifier = modifier
            .aspectRatio(0.95f)
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = isUnlocked, onClick = onClick)
            .testTag("level_tile_$levelNumber")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!isUnlocked) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = "Locked",
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$levelNumber",
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor
                )
            } else {
                Text(
                    text = "$levelNumber",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold
                    ),
                    color = contentColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
                    for (s in 1..3) {
                        val earned = s <= stars
                        Icon(
                            imageVector = if (earned) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                            contentDescription = null,
                            tint = when {
                                earned -> puzzleColors.goldStar
                                isCurrent -> Color.White.copy(alpha = 0.55f)
                                else -> MaterialTheme.colorScheme.outline
                            },
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}
