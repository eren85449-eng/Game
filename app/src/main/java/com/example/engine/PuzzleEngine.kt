package com.example.engine

import com.example.model.ArrowTile
import com.example.model.Difficulty
import com.example.model.Direction
import com.example.model.GridPos
import com.example.model.PuzzleLevel
import com.example.model.PuzzleShape
import kotlin.random.Random

data class BlockerInfo(
    val blockingArrowId: Int,
    val collisionCell: GridPos,
    val gapCells: Int
)

object PuzzleEngine {

    /**
     * Checks if [arrow] has an unobstructed straight ray from its head in [arrow.direction]
     * all the way outside the [cols] x [rows] grid.
     */
    fun isArrowFree(
        arrow: ArrowTile,
        remainingArrows: Collection<ArrowTile>,
        cols: Int,
        rows: Int
    ): Boolean = findBlocker(arrow, remainingArrows, cols, rows) == null

    /**
     * Finds the nearest blocking cell along [arrow]'s exit ray, if any.
     */
    fun findBlocker(
        arrow: ArrowTile,
        remainingArrows: Collection<ArrowTile>,
        cols: Int,
        rows: Int
    ): BlockerInfo? {
        val occupiedMap = HashMap<GridPos, Int>(remainingArrows.size * 6)
        for (other in remainingArrows) {
            for (cell in other.cells) {
                occupiedMap[cell] = other.id
            }
        }
        var step = 1
        var cursor = arrow.head + arrow.direction
        while (cursor.x in 0 until cols && cursor.y in 0 until rows) {
            val hitId = occupiedMap[cursor]
            if (hitId != null) {
                return BlockerInfo(
                    blockingArrowId = hitId,
                    collisionCell = cursor,
                    gapCells = step
                )
            }
            cursor += arrow.direction
            step++
        }
        return null
    }

    /**
     * Picks an optimal free arrow for the Hint booster.
     */
    fun findHintArrow(
        remainingArrows: List<ArrowTile>,
        cols: Int,
        rows: Int
    ): ArrowTile? {
        val freeArrows = remainingArrows.filter { isArrowFree(it, remainingArrows, cols, rows) }
        if (freeArrows.isEmpty()) return null
        // Prefer an arrow whose removal unblocks the most other arrows
        return freeArrows.maxByOrNull { candidate ->
            val afterRemoval = remainingArrows.filter { it.id != candidate.id }
            afterRemoval.count { isArrowFree(it, afterRemoval, cols, rows) }
        } ?: freeArrows.first()
    }

    /**
     * Verifies 100% mathematical solvability via topological peeling.
     */
    fun verifySolvable(
        arrows: List<ArrowTile>,
        cols: Int,
        rows: Int
    ): Boolean {
        if (arrows.isEmpty()) return false
        val remaining = arrows.toMutableList()
        while (remaining.isNotEmpty()) {
            val freeIndex = remaining.indexOfFirst { isArrowFree(it, remaining, cols, rows) }
            if (freeIndex == -1) return false
            remaining.removeAt(freeIndex)
        }
        return true
    }

    fun configForLevel(levelNumber: Int, overrideShape: PuzzleShape? = null): Triple<PuzzleShape, Difficulty, Int> {
        val difficulty = when {
            levelNumber <= 5 -> Difficulty.EASY
            levelNumber <= 20 -> Difficulty.NORMAL
            levelNumber <= 60 -> Difficulty.HARD
            else -> Difficulty.EXPERT
        }
        val shape = overrideShape ?: when {
            levelNumber <= 3 -> PuzzleShape.GRID
            else -> {
                val rotation = listOf(
                    PuzzleShape.GRID,
                    PuzzleShape.HEART,
                    PuzzleShape.PYRAMID,
                    PuzzleShape.SPIRAL,
                    PuzzleShape.MAZE,
                    PuzzleShape.LEAF,
                    PuzzleShape.DIAMOND
                )
                rotation[(levelNumber - 1) % rotation.size]
            }
        }
        val gridSize = when {
            levelNumber == 1 -> 5
            levelNumber <= 3 -> 6
            levelNumber <= 7 -> 7
            levelNumber <= 15 -> 9
            levelNumber <= 30 -> 11
            levelNumber <= 60 -> 13
            else -> 14
        }.let { base ->
            // Heart/Leaf/Spiral need at least 8x8 to show clear silhouettes
            if (shape != PuzzleShape.GRID && base < 9) 9 else base
        }
        return Triple(shape, difficulty, gridSize)
    }

    /**
     * Generates a deterministic, 100% mathematically solvable puzzle level using
     * Reverse Topological Arrow Construction.
     *
     * Proof of solvability:
     * Arrows are placed in reverse removal order (A_1, A_2, ..., A_m).
     * When placing A_k, we enforce that A_k's exit ray from head H_k in direction D_k
     * contains ZERO cells occupied by A_1..A_{k-1} and ZERO cells of A_k itself.
     * Therefore, reversing the sequence (A_m -> A_{m-1} -> ... -> A_1) is guaranteed
     * to clear the entire board with at least one free arrow at every move.
     */
    fun generateLevel(
        levelNumber: Int,
        overrideShape: PuzzleShape? = null
    ): PuzzleLevel {
        val (shape, difficulty, gridSize) = configForLevel(levelNumber, overrideShape)
        val mask = PuzzleShapes.buildMask(shape, gridSize)

        var bestArrows: List<ArrowTile> = emptyList()
        var bestScore = -1.0

        // Run up to 3 deterministic attempts and pick the densest, most intricate board
        for (attempt in 0 until 3) {
            val seed = levelNumber * 100003L + shape.ordinal * 7919L + attempt * 997L
            val candidate = generateSingleAttempt(
                mask = mask,
                cols = gridSize,
                rows = gridSize,
                difficulty = difficulty,
                random = Random(seed)
            )
            if (candidate.isNotEmpty() && verifySolvable(candidate, gridSize, gridSize)) {
                val occupiedCells = candidate.sumOf { it.cells.size }
                val coverage = occupiedCells.toDouble() / mask.size.coerceAtLeast(1)
                val initialFree = candidate.count { isArrowFree(it, candidate, gridSize, gridSize) }
                // Reward high coverage and tighter initial bottlenecks (fewer initially free arrows on hard)
                val bottleneckBonus = if (difficulty == Difficulty.EASY) 0.0 else (1.0 / (initialFree + 1.0))
                val score = coverage * 10.0 + bottleneckBonus
                if (score > bestScore) {
                    bestScore = score
                    bestArrows = candidate
                }
            }
        }

        // Fallback guarantee: simple valid arrows if mask was extreme
        if (bestArrows.isEmpty()) {
            bestArrows = generateFallback(mask, gridSize, gridSize)
        }

        return PuzzleLevel(
            levelNumber = levelNumber,
            shape = shape,
            difficulty = difficulty,
            cols = gridSize,
            rows = gridSize,
            validMask = mask,
            initialArrows = bestArrows
        )
    }

    private fun generateSingleAttempt(
        mask: Set<GridPos>,
        cols: Int,
        rows: Int,
        difficulty: Difficulty,
        random: Random
    ): List<ArrowTile> {
        val unoccupied = mask.toMutableSet()
        val occupied = mutableSetOf<GridPos>()
        val arrows = mutableListOf<ArrowTile>()

        val (minLen, maxLen, bendChance) = when (difficulty) {
            Difficulty.EASY -> Triple(2, 5, 0.35f)
            Difficulty.NORMAL -> Triple(3, 8, 0.55f)
            Difficulty.HARD -> Triple(4, 12, 0.68f)
            Difficulty.EXPERT -> Triple(4, 15, 0.75f)
        }

        var nextId = 1
        val allDirs = Direction.entries

        // Phase 1: Main snaking arrows with Warnsdorff-inspired packing
        while (unoccupied.size >= 2) {
            val candidates = ArrayList<Pair<GridPos, Direction>>(unoccupied.size * 2)
            for (cell in unoccupied) {
                for (dir in allDirs) {
                    val neck = cell - dir
                    if (neck in unoccupied && isRayClearOfSet(cell, dir, occupied, cols, rows)) {
                        candidates.add(cell to dir)
                    }
                }
            }
            if (candidates.isEmpty()) break

            // Sample up to 24 candidates and pick one that creates interesting dependencies
            candidates.shuffle(random)
            val sampleLimit = minOf(candidates.size, 24)
            var chosen = candidates[0]
            var bestCandidateScore = Int.MIN_VALUE

            for (i in 0 until sampleLimit) {
                val (h, d) = candidates[i]
                val rayUnoccupiedCount = countRayInSet(h, d, unoccupied, cols, rows)
                val neckNeighbors = countNeighborsInSet(h - d, unoccupied)
                // Prefer heads whose ray crosses unoccupied cells (future blockers!) and tight corners
                val score = when (difficulty) {
                    Difficulty.EASY -> (4 - neckNeighbors) * 2 + random.nextInt(3)
                    Difficulty.NORMAL -> rayUnoccupiedCount * 2 + (4 - neckNeighbors) * 3 + random.nextInt(3)
                    Difficulty.HARD, Difficulty.EXPERT -> rayUnoccupiedCount * 4 + (4 - neckNeighbors) * 4 + random.nextInt(2)
                }
                if (score > bestCandidateScore) {
                    bestCandidateScore = score
                    chosen = h to d
                }
            }

            val (head, dir) = chosen
            val neck = head - dir
            val rayCells = collectRayCells(head, dir, cols, rows)

            val targetLen = random.nextInt(minLen, maxLen + 1)
            val bodyReversed = mutableListOf(head, neck) // head -> neck -> ... -> tail
            val localVisited = HashSet<GridPos>(targetLen * 2)
            localVisited.add(head)
            localVisited.add(neck)

            var currentDir = dir.opposite() // walking backwards from neck
            while (bodyReversed.size < targetLen) {
                val tail = bodyReversed.last()
                val validNext = allDirs.mapNotNull { stepDir ->
                    val nextPos = tail + stepDir
                    if (nextPos in unoccupied && nextPos !in localVisited && nextPos !in rayCells) {
                        nextPos to stepDir
                    } else null
                }
                if (validNext.isEmpty()) break

                // Score valid extensions: Warnsdorff (fewer free neighbors) + bend preference
                val picked = validNext.minByOrNull { (pos, stepDir) ->
                    val freedom = countNeighborsInSet(pos, unoccupied) { it !in localVisited }
                    val isBend = stepDir != currentDir
                    val bendPenalty = if (random.nextFloat() < bendChance) {
                        if (isBend) 0 else 2
                    } else {
                        if (isBend) 2 else 0
                    }
                    freedom * 3 + bendPenalty + random.nextInt(2)
                }!!

                bodyReversed.add(picked.first)
                localVisited.add(picked.first)
                currentDir = picked.second
            }

            val cells = bodyReversed.reversed()
            arrows.add(
                ArrowTile(
                    id = nextId++,
                    cells = cells,
                    direction = dir
                )
            )
            unoccupied.removeAll(cells.toSet())
            occupied.addAll(cells)
        }

        return arrows
    }

    private fun generateFallback(mask: Set<GridPos>, cols: Int, rows: Int): List<ArrowTile> {
        val result = mutableListOf<ArrowTile>()
        val used = mutableSetOf<GridPos>()
        var id = 1
        for (pos in mask.sortedWith(compareBy({ it.y }, { it.x }))) {
            if (pos in used) continue
            for (dir in Direction.entries) {
                val neck = pos - dir
                if (neck in mask && neck !in used && isRayClearOfSet(pos, dir, used, cols, rows)) {
                    result.add(ArrowTile(id = id++, cells = listOf(neck, pos), direction = dir))
                    used.add(pos)
                    used.add(neck)
                    break
                }
            }
        }
        return result
    }

    private fun isRayClearOfSet(
        head: GridPos,
        dir: Direction,
        blockedSet: Set<GridPos>,
        cols: Int,
        rows: Int
    ): Boolean {
        var cursor = head + dir
        while (cursor.x in 0 until cols && cursor.y in 0 until rows) {
            if (cursor in blockedSet) return false
            cursor += dir
        }
        return true
    }

    private fun countRayInSet(
        head: GridPos,
        dir: Direction,
        targetSet: Set<GridPos>,
        cols: Int,
        rows: Int
    ): Int {
        var count = 0
        var cursor = head + dir
        while (cursor.x in 0 until cols && cursor.y in 0 until rows) {
            if (cursor in targetSet) count++
            cursor += dir
        }
        return count
    }

    private fun collectRayCells(
        head: GridPos,
        dir: Direction,
        cols: Int,
        rows: Int
    ): Set<GridPos> {
        val set = HashSet<GridPos>()
        var cursor = head + dir
        while (cursor.x in 0 until cols && cursor.y in 0 until rows) {
            set.add(cursor)
            cursor += dir
        }
        return set
    }

    private inline fun countNeighborsInSet(
        pos: GridPos,
        set: Set<GridPos>,
        extraFilter: (GridPos) -> Boolean = { true }
    ): Int {
        var count = 0
        for (d in Direction.entries) {
            val nb = pos + d
            if (nb in set && extraFilter(nb)) count++
        }
        return count
    }
}
