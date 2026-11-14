package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onFinished: () -> Unit
) {
    val progressAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        progressAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1900, easing = FastOutSlowInEasing)
        )
        delay(220)
        onFinished()
    }

    val infiniteTransition = rememberInfiniteTransition(label = "splashArrows")
    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outerRot"
    )
    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "innerRot"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val bgBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF09132B),
            Color(0xFF0F2657),
            Color(0xFF0A193A)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 520.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top subtle badge
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0x1E38BDF8),
                border = BorderStroke(1.dp, Color(0x4438BDF8))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LOGIC & SPATIAL PUZZLE",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF7DD3FC),
                        letterSpacing = 1.1.sp
                    )
                }
            }

            // Centerpiece: Rotating Arrows + Title + Progress Bar
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(168.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val outerR = size.minDimension * 0.42f * pulseScale
                        val innerR = size.minDimension * 0.26f

                        // Glow halo
                        drawCircle(
                            color = Color(0x2238BDF8),
                            radius = outerR * 1.15f,
                            center = center
                        )

                        // Outer 4 rotating directional arrows
                        rotate(degrees = outerRotation, pivot = center) {
                            val strokeW = 11f
                            for (i in 0 until 4) {
                                rotate(degrees = i * 90f, pivot = center) {
                                    val arcPath = Path().apply {
                                        addArc(
                                            oval = androidx.compose.ui.geometry.Rect(
                                                left = center.x - outerR,
                                                top = center.y - outerR,
                                                right = center.x + outerR,
                                                bottom = center.y + outerR
                                            ),
                                            startAngleDegrees = 12f,
                                            sweepAngleDegrees = 56f
                                        )
                                    }
                                    drawPath(
                                        path = arcPath,
                                        color = if (i % 2 == 0) Color(0xFF38BDF8) else Color(0xFFFFFFFF),
                                        style = Stroke(width = strokeW, cap = StrokeCap.Round)
                                    )
                                    // Arrowhead at end of arc (68 deg)
                                    val rad = Math.toRadians(68.0)
                                    val tipX = center.x + (outerR * kotlin.math.cos(rad)).toFloat()
                                    val tipY = center.y + (outerR * kotlin.math.sin(rad)).toFloat()
                                    rotate(degrees = 68f, pivot = Offset(tipX, tipY)) {
                                        val head = Path().apply {
                                            moveTo(tipX, tipY + 14f)
                                            lineTo(tipX - 11f, tipY - 6f)
                                            lineTo(tipX + 11f, tipY - 6f)
                                            close()
                                        }
                                        drawPath(
                                            path = head,
                                            color = if (i % 2 == 0) Color(0xFF38BDF8) else Color(0xFFFFFFFF)
                                        )
                                    }
                                }
                            }
                        }

                        // Inner counter-rotating emblem arrows
                        rotate(degrees = innerRotation, pivot = center) {
                            val colorPrimary = Color(0xFF1A84F5)
                            val colorWhite = Color(0xFFFFFFFF)
                            for (k in 0 until 2) {
                                rotate(degrees = k * 180f, pivot = center) {
                                    val c = if (k == 0) colorPrimary else colorWhite
                                    val shaft = Path().apply {
                                        moveTo(center.x - innerR * 0.6f, center.y - innerR * 0.25f)
                                        lineTo(center.x + innerR * 0.35f, center.y - innerR * 0.25f)
                                    }
                                    drawPath(
                                        path = shaft,
                                        color = c,
                                        style = Stroke(width = 12f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                    )
                                    val tip = Offset(center.x + innerR * 0.72f, center.y - innerR * 0.25f)
                                    val head = Path().apply {
                                        moveTo(tip.x, tip.y)
                                        lineTo(tip.x - 20f, tip.y - 14f)
                                        lineTo(tip.x - 20f, tip.y + 14f)
                                        close()
                                    }
                                    drawPath(path = head, color = c)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(R.string.game_full_title),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 26.sp,
                        lineHeight = 32.sp
                    ),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("splash_title")
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Smooth progress bar
                Column(
                    modifier = Modifier.fillMaxWidth(0.82f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progressAnim.value.coerceIn(0.05f, 1f))
                                .height(10.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(Color(0xFF1A84F5), Color(0xFF38BDF8))
                                    )
                                )
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.loading_puzzles),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFBBD7FF)
                    )
                }
            }

            // Prominent Developer Attribution Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF13264D)),
                border = BorderStroke(1.dp, Color(0x5538BDF8)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("developer_attribution_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0x2838BDF8),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Code,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "CREATED & ENGINEERED",
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp),
                            color = Color(0xFF7DD3FC),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = stringResource(R.string.developer_attribution),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
