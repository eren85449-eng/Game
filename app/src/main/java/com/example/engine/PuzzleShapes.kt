package com.example.engine

import com.example.model.GridPos
import com.example.model.PuzzleShape
import kotlin.math.abs
import kotlin.math.pow

object PuzzleShapes {

    fun buildMask(shape: PuzzleShape, size: Int): Set<GridPos> {
        return when (shape) {
            PuzzleShape.GRID -> buildGridMask(size)
            PuzzleShape.HEART -> buildHeartMask(size)
            PuzzleShape.PYRAMID -> buildPyramidMask(size)
            PuzzleShape.SPIRAL -> buildSpiralMask(size)
            PuzzleShape.MAZE -> buildMazeMask(size)
            PuzzleShape.LEAF -> buildLeafMask(size)
            PuzzleShape.DIAMOND -> buildDiamondMask(size)
        }
    }

    private fun buildGridMask(size: Int): Set<GridPos> = buildSet {
        for (y in 0 until size) {
            for (x in 0 until size) {
                add(GridPos(x, y))
            }
        }
    }

    private fun buildHeartMask(size: Int): Set<GridPos> {
        val result = mutableSetOf<GridPos>()
        val half = (size - 1) / 2.0
        for (y in 0 until size) {
            for (x in 0 until size) {
                // Map y so top lobes are at smaller y, tip at bottom (larger y)
                val nx = (x - half) / (size * 0.45)
                val ny = (half - y) / (size * 0.45) + 0.12
                val v = (nx * nx + ny * ny - 1.0).pow(3.0) - nx * nx * ny.pow(3.0)
                if (v <= 0.08) {
                    result.add(GridPos(x, y))
                }
            }
        }
        return if (result.size >= 12) result else buildGridMask(size)
    }

    private fun buildPyramidMask(size: Int): Set<GridPos> {
        val result = mutableSetOf<GridPos>()
        val center = (size - 1) / 2.0
        for (y in 0 until size) {
            val progress = (y + 1).toDouble() / size.toDouble()
            val halfWidth = progress * (size / 2.0) + 0.25
            for (x in 0 until size) {
                if (abs(x - center) <= halfWidth) {
                    result.add(GridPos(x, y))
                }
            }
        }
        return result
    }

    private fun buildDiamondMask(size: Int): Set<GridPos> {
        val result = mutableSetOf<GridPos>()
        val center = (size - 1) / 2.0
        val radius = size / 2.0 + 0.15
        for (y in 0 until size) {
            for (x in 0 until size) {
                if (abs(x - center) + abs(y - center) <= radius) {
                    result.add(GridPos(x, y))
                }
            }
        }
        return result
    }

    private fun buildSpiralMask(size: Int): Set<GridPos> {
        // Thick spiral corridor / notched ring silhouette so it looks visually like a spiral
        val result = mutableSetOf<GridPos>()
        var minX = 0
        var maxX = size - 1
        var minY = 0
        var maxY = size - 1
        val band = if (size >= 11) 2 else 1

        // Carve a spiral moat out of the full grid for larger grids, or add spiral arms
        for (y in 0 until size) {
            for (x in 0 until size) {
                result.add(GridPos(x, y))
            }
        }
        // Carve a thin 1-cell spiral gap to form a dramatic spiral shape
        var left = 1
        var right = size - 2
        var top = 1
        var bottom = size - 2
        var step = 0
        while (left <= right && top <= bottom && step < size) {
            when (step % 4) {
                0 -> {
                    for (x in (left - 1).coerceAtLeast(0)..right) result.remove(GridPos(x, top))
                    top += band + 1
                }
                1 -> {
                    for (y in (top - band - 1).coerceAtLeast(0)..bottom) result.remove(GridPos(right, y))
                    right -= band + 1
                }
                2 -> {
                    for (x in left..(right + band + 1).coerceAtMost(size - 1)) result.remove(GridPos(x, bottom))
                    bottom -= band + 1
                }
                3 -> {
                    for (y in top..(bottom + band + 1).coerceAtMost(size - 1)) result.remove(GridPos(left, y))
                    left += band + 1
                }
            }
            step++
        }
        return if (result.size >= 16) result else buildGridMask(size)
    }

    private fun buildMazeMask(size: Int): Set<GridPos> {
        val result = mutableSetOf<GridPos>()
        for (y in 0 until size) {
            for (x in 0 until size) {
                val isBorder = x == 0 || y == 0 || x == size - 1 || y == size - 1
                val isInnerCore = x in (size / 3)..(2 * size / 3) && y in (size / 3)..(2 * size / 3)
                val isMoat = (x == 2 || x == size - 3) && y in 2..(size - 3) && y != size / 2
                val isCrossBridge = (y == size / 2 || x == size / 2)
                if (isBorder || isInnerCore || isCrossBridge || !isMoat) {
                    result.add(GridPos(x, y))
                }
            }
        }
        // Carve symmetrical maze chambers for visual intrigue
        if (size >= 8) {
            val mid = size / 2
            result.remove(GridPos(1, mid - 1))
            result.remove(GridPos(size - 2, mid + 1))
            result.remove(GridPos(mid - 1, 1))
            result.remove(GridPos(mid + 1, size - 2))
        }
        return result
    }

    private fun buildLeafMask(size: Int): Set<GridPos> {
        val result = mutableSetOf<GridPos>()
        val half = (size - 1) / 2.0
        for (y in 0 until size) {
            for (x in 0 until size) {
                val nx = (x - half) / (size * 0.48)
                val ny = (y - half) / (size * 0.48)
                // Rotated lens / leaf silhouette with stem at bottom-right
                val u = (nx + ny) * 0.7071
                val v = (-nx + ny) * 0.7071
                val insideLeaf = (u * u) / 0.95 + (v * v) / 0.42 <= 1.0
                val isStem = x >= size - 2 && y >= size - 2
                if (insideLeaf || isStem) {
                    result.add(GridPos(x, y))
                }
            }
        }
        return if (result.size >= 14) result else buildGridMask(size)
    }
}
