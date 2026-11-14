package com.example

import com.example.engine.PuzzleEngine
import com.example.model.ArrowTile
import com.example.model.Direction
import com.example.model.GridPos
import com.example.model.PuzzleShape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PuzzleEngineTest {

    @Test
    fun allShapesGenerateSolvableLevels() {
        for (shape in PuzzleShape.entries) {
            val level = PuzzleEngine.generateLevel(levelNumber = 25, overrideShape = shape)
            assertTrue("Shape $shape should have arrows", level.initialArrows.isNotEmpty())
            assertTrue(
                "Shape $shape must be 100% mathematically solvable",
                PuzzleEngine.verifySolvable(level.initialArrows, level.cols, level.rows)
            )
            val hint = PuzzleEngine.findHintArrow(level.initialArrows, level.cols, level.rows)
            assertNotNull("Shape $shape must always have at least one free arrow", hint)
        }
    }

    @Test
    fun firstHundredCampaignLevelsAreAllSolvable() {
        for (levelNum in 1..100) {
            val level = PuzzleEngine.generateLevel(levelNum)
            assertTrue("Level $levelNum should have arrows", level.initialArrows.isNotEmpty())
            assertTrue(
                "Level $levelNum (${level.shape}) must be solvable",
                PuzzleEngine.verifySolvable(level.initialArrows, level.cols, level.rows)
            )
        }
    }

    @Test
    fun blockerDetectionIdentifiesCollisionAndClearPath() {
        val blockerArrow = ArrowTile(
            id = 1,
            cells = listOf(GridPos(3, 1), GridPos(3, 2)),
            direction = Direction.DOWN
        )
        val blockedArrow = ArrowTile(
            id = 2,
            cells = listOf(GridPos(0, 2), GridPos(1, 2)),
            direction = Direction.RIGHT
        )
        val arrows = listOf(blockerArrow, blockedArrow)

        assertTrue(PuzzleEngine.isArrowFree(blockerArrow, arrows, cols = 5, rows = 5))
        assertFalse(PuzzleEngine.isArrowFree(blockedArrow, arrows, cols = 5, rows = 5))

        val blockerInfo = PuzzleEngine.findBlocker(blockedArrow, arrows, cols = 5, rows = 5)
        assertNotNull(blockerInfo)
        assertEquals(1, blockerInfo!!.blockingArrowId)
        assertEquals(GridPos(3, 2), blockerInfo.collisionCell)
        assertEquals(2, blockerInfo.gapCells)

        // Once blockerArrow leaves, blockedArrow becomes free
        val afterRemoval = listOf(blockedArrow)
        assertNull(PuzzleEngine.findBlocker(blockedArrow, afterRemoval, cols = 5, rows = 5))
    }
}
