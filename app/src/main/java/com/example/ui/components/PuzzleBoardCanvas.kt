package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import com.example.model.ArrowTile
import com.example.model.BlockedArrowAnimation
import com.example.model.Direction
import com.example.model.FlyingArrowAnimation
import com.example.model.Particle
import com.example.model.PuzzleLevel
import com.example.ui.theme.HintGlowBlue
import com.example.ui.theme.LocalPuzzleColors
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

@Composable
fun PuzzleBoardCanvas(
    level: PuzzleLevel,
    remainingArrows: List<ArrowTile>,
    flyingArrows: List<FlyingArrowAnimation>,
    blockedAnims: List<BlockedArrowAnimation>,
    particles: List<Particle>,
    hintedArrowId: Int?,
    showGridDots: Boolean,
    resetViewTrigger: Int,
    onTapBoard: (boardX: Float, boardY: Float, cellSize: Float, originX: Float, originY: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val puzzleColors = LocalPuzzleColors.current
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(level.levelNumber, level.shape, resetViewTrigger) {
        zoomScale = 1f
        panOffset = Offset.Zero
    }

    val infiniteTransition = rememberInfiniteTransition(label = "hintPulse")
    val hintPulse by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hintPulseAlpha"
    )

    val paddingPx = 44f
    val cellSize = remember(canvasSize, level.cols, level.rows) {
        if (canvasSize.width == 0 || canvasSize.height == 0) 40f
        else {
            val availW = (canvasSize.width - paddingPx * 2f).coerceAtLeast(100f)
            val availH = (canvasSize.height - paddingPx * 2f).coerceAtLeast(100f)
            min(availW / level.cols, availH / level.rows)
        }
    }
    val originX = (canvasSize.width - cellSize * level.cols) / 2f
    val originY = (canvasSize.height - cellSize * level.rows) / 2f

    Box(
        modifier = modifier
            .background(puzzleColors.boardBackground)
            .onSizeChanged { canvasSize = it }
            .pointerInput(level.levelNumber, level.shape) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val nextZoom = (zoomScale * zoom).coerceIn(0.85f, 3.0f)
                    zoomScale = nextZoom
                    val maxPanX = canvasSize.width * 0.45f * nextZoom
                    val maxPanY = canvasSize.height * 0.45f * nextZoom
                    panOffset = Offset(
                        x = (panOffset.x + pan.x).coerceIn(-maxPanX, maxPanX),
                        y = (panOffset.y + pan.y).coerceIn(-maxPanY, maxPanY)
                    )
                }
            }
            .pointerInput(level.levelNumber, level.shape, cellSize, originX, originY) {
                detectTapGestures { tapOffset ->
                    // Invert graphicsLayer transform (centered pivot)
                    val cx = canvasSize.width / 2f
                    val cy = canvasSize.height / 2f
                    val boardX = (tapOffset.x - panOffset.x - cx) / zoomScale + cx
                    val boardY = (tapOffset.y - panOffset.y - cy) / zoomScale + cy
                    onTapBoard(boardX, boardY, cellSize, originX, originY)
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoomScale
                    scaleY = zoomScale
                    translationX = panOffset.x
                    translationY = panOffset.y
                }
        ) {
            if (showGridDots) {
                val dotRadius = (cellSize * 0.085f).coerceIn(2f, 5.5f)
                for (pos in level.validMask) {
                    val dx = originX + (pos.x + 0.5f) * cellSize
                    val dy = originY + (pos.y + 0.5f) * cellSize
                    drawCircle(
                        color = puzzleColors.gridDot,
                        radius = dotRadius,
                        center = Offset(dx, dy)
                    )
                }
            }

            val strokeWidth = (cellSize * 0.24f).coerceIn(4f, 16f)
            val blockedById = blockedAnims.associateBy { it.arrowId }

            // 1. Draw static & blocked/shaking arrows
            for (arrow in remainingArrows) {
                val blockAnim = blockedById[arrow.id]
                val isHinted = arrow.id == hintedArrowId

                var offsetDelta = Offset.Zero
                var arrowColor = when {
                    blockAnim != null -> puzzleColors.arrowBlockedRed
                    isHinted -> HintGlowBlue
                    arrow.hasFailedBefore -> puzzleColors.arrowBlockedRed
                    else -> puzzleColors.arrowDefault
                }

                if (blockAnim != null) {
                    val p = blockAnim.progress
                    // Lunge forward (0..0.32), hit and shake (0.32..0.75), ease back (0.75..1.0)
                    val maxLungePx = ((blockAnim.gapCells - 0.35f).coerceAtLeast(0.28f)) * cellSize
                    val forwardFactor = when {
                        p < 0.32f -> p / 0.32f
                        p < 0.72f -> 1f - ((p - 0.32f) / 0.40f) * 0.25f
                        else -> (1f - ((p - 0.72f) / 0.28f)) * 0.75f
                    }
                    val shakePx = if (p in 0.25f..0.78f) {
                        sin(p * 6f * PI.toFloat()) * (cellSize * 0.14f) * (1f - p)
                    } else 0f

                    val dir = blockAnim.direction
                    val perpX = -dir.dy.toFloat()
                    val perpY = dir.dx.toFloat()
                    offsetDelta = Offset(
                        x = dir.dx * maxLungePx * forwardFactor + perpX * shakePx,
                        y = dir.dy * maxLungePx * forwardFactor + perpY * shakePx
                    )
                }

                val points = arrow.cells.map { pos ->
                    Offset(
                        x = originX + (pos.x + 0.5f) * cellSize + offsetDelta.x,
                        y = originY + (pos.y + 0.5f) * cellSize + offsetDelta.y
                    )
                }

                if (isHinted) {
                    drawArrowPolyline(
                        points = points,
                        direction = arrow.direction,
                        cellSize = cellSize,
                        strokeWidth = strokeWidth * 1.75f,
                        color = HintGlowBlue.copy(alpha = 0.28f * hintPulse),
                        drawShadow = false,
                        highlightColor = Color.Transparent
                    )
                }

                drawArrowPolyline(
                    points = points,
                    direction = arrow.direction,
                    cellSize = cellSize,
                    strokeWidth = strokeWidth,
                    color = arrowColor,
                    drawShadow = true,
                    shadowColor = puzzleColors.arrowShadow,
                    highlightColor = if (arrow.hasFailedBefore || blockAnim != null || isHinted) {
                        Color.White.copy(alpha = 0.28f)
                    } else {
                        puzzleColors.arrowHighlightEdge.copy(alpha = 0.38f)
                    }
                )

                // Collision impact ring on blocker
                if (blockAnim != null && blockAnim.progress in 0.18f..0.85f) {
                    val ringProgress = ((blockAnim.progress - 0.18f) / 0.67f).coerceIn(0f, 1f)
                    val hitCenter = Offset(
                        x = originX + (blockAnim.collisionCell.x + 0.5f) * cellSize,
                        y = originY + (blockAnim.collisionCell.y + 0.5f) * cellSize
                    )
                    drawCircle(
                        color = puzzleColors.arrowBlockedRed.copy(alpha = (1f - ringProgress) * 0.75f),
                        radius = cellSize * (0.25f + 0.55f * ringProgress),
                        center = hitCenter,
                        style = Stroke(width = 3.5f)
                    )
                }
            }

            // 2. Draw flying arrows gliding along their exact track off screen
            val offscreenMargin = min(size.width, size.height).coerceAtLeast(cellSize * 6f) * 1.4f
            for (flying in flyingArrows) {
                val arrow = flying.arrow
                val basePoints = arrow.cells.map { pos ->
                    Offset(
                        x = originX + (pos.x + 0.5f) * cellSize,
                        y = originY + (pos.y + 0.5f) * cellSize
                    )
                }
                val headPoint = basePoints.last()
                val exitEnd = Offset(
                    x = headPoint.x + arrow.direction.dx * offscreenMargin,
                    y = headPoint.y + arrow.direction.dy * offscreenMargin
                )
                val fullTrack = basePoints + exitEnd
                val bodyLen = polylineLength(basePoints)
                // Smooth accelerating easing for snappy launch
                val eased = flying.progress * flying.progress * (1.8f - 0.8f * flying.progress)
                val shift = eased * (bodyLen + offscreenMargin * 0.92f)
                val sliced = slicePolyline(fullTrack, shift, shift + bodyLen)

                if (sliced.size >= 2) {
                    // Speed glow trail
                    drawArrowPolyline(
                        points = sliced,
                        direction = arrow.direction,
                        cellSize = cellSize,
                        strokeWidth = strokeWidth * 1.45f,
                        color = puzzleColors.arrowActiveBlue.copy(alpha = 0.25f * (1f - flying.progress)),
                        drawShadow = false,
                        highlightColor = Color.Transparent
                    )
                    drawArrowPolyline(
                        points = sliced,
                        direction = arrow.direction,
                        cellSize = cellSize,
                        strokeWidth = strokeWidth,
                        color = puzzleColors.arrowActiveBlue,
                        drawShadow = true,
                        shadowColor = puzzleColors.arrowShadow,
                        highlightColor = Color.White.copy(alpha = 0.4f)
                    )
                }
            }

            // 3. Draw particles
            for (p in particles) {
                drawCircle(
                    color = p.color.copy(alpha = p.alpha),
                    radius = p.radius,
                    center = Offset(p.x, p.y)
                )
            }
        }
    }
}

@Composable
fun MiniBoardPreview(
    level: PuzzleLevel,
    modifier: Modifier = Modifier,
    arrowColor: Color = Color(0xFF0F1B35)
) {
    Canvas(modifier = modifier) {
        val padding = size.minDimension * 0.08f
        val cellSize = min(
            (size.width - padding * 2f) / level.cols,
            (size.height - padding * 2f) / level.rows
        )
        val originX = (size.width - cellSize * level.cols) / 2f
        val originY = (size.height - cellSize * level.rows) / 2f
        val strokeWidth = (cellSize * 0.25f).coerceIn(1.5f, 6f)

        for (arrow in level.initialArrows) {
            val pts = arrow.cells.map { pos ->
                Offset(
                    x = originX + (pos.x + 0.5f) * cellSize,
                    y = originY + (pos.y + 0.5f) * cellSize
                )
            }
            drawArrowPolyline(
                points = pts,
                direction = arrow.direction,
                cellSize = cellSize,
                strokeWidth = strokeWidth,
                color = arrowColor,
                drawShadow = false,
                highlightColor = Color.Transparent
            )
        }
    }
}

private fun DrawScope.drawArrowPolyline(
    points: List<Offset>,
    direction: Direction,
    cellSize: Float,
    strokeWidth: Float,
    color: Color,
    drawShadow: Boolean,
    shadowColor: Color = Color(0x22000000),
    highlightColor: Color = Color.Transparent
) {
    if (points.isEmpty()) return

    val headCenter = points.last()
    // Shorten shaft slightly at head so the arrowhead tip stays crisp and pointed
    val tipAdvance = cellSize * 0.28f
    val tip = Offset(
        x = headCenter.x + direction.dx * tipAdvance,
        y = headCenter.y + direction.dy * tipAdvance
    )
    val headBase = Offset(
        x = headCenter.x - direction.dx * (cellSize * 0.10f),
        y = headCenter.y - direction.dy * (cellSize * 0.10f)
    )
    val headHalfWidth = strokeWidth * 1.32f
    val perpX = -direction.dy.toFloat()
    val perpY = direction.dx.toFloat()

    val shaftPath = Path().apply {
        moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size) {
            lineTo(points[i].x, points[i].y)
        }
    }

    val headPath = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(headBase.x + perpX * headHalfWidth, headBase.y + perpY * headHalfWidth)
        lineTo(headBase.x - perpX * headHalfWidth, headBase.y - perpY * headHalfWidth)
        close()
    }

    // Soft 2.5D drop shadow beneath shaft & arrowhead
    if (drawShadow) {
        val shadowDy = (strokeWidth * 0.28f).coerceIn(1.5f, 4.5f)
        val shadowShaft = Path().apply {
            addPath(shaftPath, Offset(0f, shadowDy))
        }
        val shadowHead = Path().apply {
            addPath(headPath, Offset(0f, shadowDy))
        }
        drawPath(
            path = shadowShaft,
            color = shadowColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawPath(path = shadowHead, color = shadowColor)
    }

    // Main shaft & arrowhead
    drawPath(
        path = shaftPath,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    drawPath(path = headPath, color = color)

    // Subtle 3D specular top highlight
    if (highlightColor != Color.Transparent) {
        val highlightShaft = Path().apply {
            addPath(shaftPath, Offset(0f, -strokeWidth * 0.14f))
        }
        drawPath(
            path = highlightShaft,
            color = highlightColor,
            style = Stroke(
                width = (strokeWidth * 0.22f).coerceAtLeast(1f),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

private fun polylineLength(pts: List<Offset>): Float {
    var total = 0f
    for (i in 0 until pts.size - 1) {
        total += hypot(pts[i + 1].x - pts[i].x, pts[i + 1].y - pts[i].y)
    }
    return total
}

private fun slicePolyline(pts: List<Offset>, startDist: Float, endDist: Float): List<Offset> {
    if (pts.size < 2 || endDist <= startDist) return emptyList()
    val result = mutableListOf<Offset>()
    var accumulated = 0f

    for (i in 0 until pts.size - 1) {
        val a = pts[i]
        val b = pts[i + 1]
        val segLen = hypot(b.x - a.x, b.y - a.y)
        if (segLen <= 0.0001f) continue
        val segStart = accumulated
        val segEnd = accumulated + segLen

        if (segEnd >= startDist && segStart <= endDist) {
            val t0 = ((startDist - segStart) / segLen).coerceIn(0f, 1f)
            val t1 = ((endDist - segStart) / segLen).coerceIn(0f, 1f)
            val p0 = Offset(a.x + (b.x - a.x) * t0, a.y + (b.y - a.y) * t0)
            val p1 = Offset(a.x + (b.x - a.x) * t1, a.y + (b.y - a.y) * t1)
            if (result.isEmpty()) result.add(p0)
            result.add(p1)
        }
        accumulated = segEnd
    }
    return result
}
