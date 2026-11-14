package com.example.model

import androidx.compose.ui.graphics.Color

enum class Direction(val dx: Int, val dy: Int) {
    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0);

    fun opposite(): Direction = when (this) {
        UP -> DOWN
        DOWN -> UP
        LEFT -> RIGHT
        RIGHT -> LEFT
    }
}

data class GridPos(val x: Int, val y: Int) {
    operator fun plus(dir: Direction): GridPos = GridPos(x + dir.dx, y + dir.dy)
    operator fun minus(dir: Direction): GridPos = GridPos(x - dir.dx, y - dir.dy)
}

enum class PuzzleShape(val displayName: String, val subtitle: String) {
    GRID("Classic Grid", "Clean dense square challenge"),
    HEART("Heart", "Romantic interlocking paths"),
    PYRAMID("Pyramid", "Layered triangular ascent"),
    SPIRAL("Spiral", "Twisting vortex corridors"),
    MAZE("Labyrinth", "Complex frame & inner chambers"),
    LEAF("Botanic Leaf", "Organic asymmetrical flow"),
    DIAMOND("Diamond", "Faceted crystal puzzle")
}

enum class Difficulty(val label: String) {
    EASY("Easy"),
    NORMAL("Normal"),
    HARD("Hard"),
    EXPERT("Expert")
}

data class ArrowTile(
    val id: Int,
    val cells: List<GridPos>, // tail -> ... -> head
    val direction: Direction,
    val hasFailedBefore: Boolean = false
) {
    val head: GridPos get() = cells.last()
    val tail: GridPos get() = cells.first()
}

data class PuzzleLevel(
    val levelNumber: Int,
    val shape: PuzzleShape,
    val difficulty: Difficulty,
    val cols: Int,
    val rows: Int,
    val validMask: Set<GridPos>,
    val initialArrows: List<ArrowTile>,
    val maxHearts: Int = 3
)

data class FlyingArrowAnimation(
    val arrow: ArrowTile,
    val progress: Float = 0f, // 0f..1f
    val startTimeNanos: Long
)

data class BlockedArrowAnimation(
    val arrowId: Int,
    val direction: Direction,
    val gapCells: Int,
    val collisionCell: GridPos,
    val progress: Float = 0f, // 0f..1f
    val startTimeNanos: Long
)

data class Particle(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val radius: Float,
    val color: Color,
    val alpha: Float = 1f,
    val life: Float = 1f
)

data class ConfettiPiece(
    val xFraction: Float,
    val yFraction: Float,
    val vx: Float,
    val vy: Float,
    val width: Float,
    val height: Float,
    val rotation: Float,
    val rotationSpeed: Float,
    val color: Color
)
