package com.example.viewmodel

import android.app.Application
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundEffectsManager
import com.example.data.GameProgressRepository
import com.example.data.PlayerProgress
import com.example.engine.PuzzleEngine
import com.example.model.ArrowTile
import com.example.model.BlockedArrowAnimation
import com.example.model.ConfettiPiece
import com.example.model.FlyingArrowAnimation
import com.example.model.Particle
import com.example.model.PuzzleLevel
import com.example.model.PuzzleShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

data class GameUiState(
    val level: PuzzleLevel? = null,
    val remainingArrows: List<ArrowTile> = emptyList(),
    val flyingArrows: List<FlyingArrowAnimation> = emptyList(),
    val blockedAnims: List<BlockedArrowAnimation> = emptyList(),
    val particles: List<Particle> = emptyList(),
    val confetti: List<ConfettiPiece> = emptyList(),
    val heartsRemaining: Int = 3,
    val hintedArrowId: Int? = null,
    val isVictory: Boolean = false,
    val isGameOver: Boolean = false,
    val starsEarned: Int = 0,
    val isLoading: Boolean = true,
    val movesCount: Int = 0,
    val overrideShape: PuzzleShape? = null
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GameProgressRepository(application)
    val soundManager = SoundEffectsManager()

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        application.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        application.getSystemService(Vibrator::class.java)
    }

    val playerProgress: StateFlow<PlayerProgress> = repository.progressFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = PlayerProgress()
        )

    private val _gameState = MutableStateFlow(GameUiState())
    val gameState: StateFlow<GameUiState> = _gameState.asStateFlow()

    init {
        soundManager.prewarm()
        viewModelScope.launch {
            playerProgress.collect { progress ->
                soundManager.isEnabled = progress.soundEnabled
            }
        }
    }

    fun loadLevel(levelNumber: Int, shapeOverride: PuzzleShape? = null) {
        _gameState.value = GameUiState(isLoading = true, overrideShape = shapeOverride)
        viewModelScope.launch {
            val generated = withContext(Dispatchers.Default) {
                PuzzleEngine.generateLevel(levelNumber, shapeOverride)
            }
            _gameState.value = GameUiState(
                level = generated,
                remainingArrows = generated.initialArrows,
                heartsRemaining = generated.maxHearts,
                isLoading = false,
                overrideShape = shapeOverride
            )
        }
    }

    fun restartLevel() {
        val current = _gameState.value.level ?: return
        soundManager.playTap()
        _gameState.value = GameUiState(
            level = current,
            remainingArrows = current.initialArrows,
            heartsRemaining = current.maxHearts,
            isLoading = false,
            overrideShape = _gameState.value.overrideShape
        )
    }

    fun onBoardTapped(boardX: Float, boardY: Float, cellSize: Float, originX: Float, originY: Float) {
        val state = _gameState.value
        val level = state.level ?: return
        if (state.isLoading || state.isVictory || state.isGameOver) return

        val tappedArrow = findClosestArrow(
            arrows = state.remainingArrows,
            tapX = boardX,
            tapY = boardY,
            cellSize = cellSize,
            originX = originX,
            originY = originY
        ) ?: return

        val blocker = PuzzleEngine.findBlocker(
            arrow = tappedArrow,
            remainingArrows = state.remainingArrows,
            cols = level.cols,
            rows = level.rows
        )

        val now = System.nanoTime()

        if (blocker == null) {
            // Arrow path to boundary is clear -> launch!
            soundManager.playSwoosh()
            vibrateShort()

            val updatedRemaining = state.remainingArrows.filter { it.id != tappedArrow.id }
            val newFlying = FlyingArrowAnimation(
                arrow = tappedArrow,
                progress = 0f,
                startTimeNanos = now
            )

            // Spawn electric sky-blue speed particles along the arrow head
            val headPxX = originX + (tappedArrow.head.x + 0.5f) * cellSize
            val headPxY = originY + (tappedArrow.head.y + 0.5f) * cellSize
            val speedParticles = spawnDirectionalParticles(
                x = headPxX,
                y = headPxY,
                dx = tappedArrow.direction.dx.toFloat(),
                dy = tappedArrow.direction.dy.toFloat(),
                color = Color(0xFF38BDF8),
                count = 12
            )

            val clearedAll = updatedRemaining.isEmpty()

            _gameState.update {
                it.copy(
                    remainingArrows = updatedRemaining,
                    flyingArrows = it.flyingArrows + newFlying,
                    particles = it.particles + speedParticles,
                    hintedArrowId = if (it.hintedArrowId == tappedArrow.id) null else it.hintedArrowId,
                    movesCount = it.movesCount + 1
                )
            }

            if (clearedAll) {
                triggerVictory(level)
            }
        } else {
            // Blocked -> shake, red collision flash, particles, lose 1 heart
            soundManager.playError()
            vibrateError()

            val hitPxX = originX + (blocker.collisionCell.x + 0.5f) * cellSize
            val hitPxY = originY + (blocker.collisionCell.y + 0.5f) * cellSize
            val errorParticles = spawnBurstParticles(
                x = hitPxX,
                y = hitPxY,
                color = Color(0xFFEF4444),
                count = 16
            )

            val newHearts = (state.heartsRemaining - 1).coerceAtLeast(0)
            val updatedRemaining = state.remainingArrows.map {
                if (it.id == tappedArrow.id) it.copy(hasFailedBefore = true) else it
            }
            val blockedAnim = BlockedArrowAnimation(
                arrowId = tappedArrow.id,
                direction = tappedArrow.direction,
                gapCells = blocker.gapCells,
                collisionCell = blocker.collisionCell,
                progress = 0f,
                startTimeNanos = now
            )

            _gameState.update {
                it.copy(
                    remainingArrows = updatedRemaining,
                    blockedAnims = it.blockedAnims.filter { b -> b.arrowId != tappedArrow.id } + blockedAnim,
                    particles = it.particles + errorParticles,
                    heartsRemaining = newHearts,
                    movesCount = it.movesCount + 1
                )
            }

            if (newHearts == 0) {
                viewModelScope.launch {
                    delay(320)
                    _gameState.update { it.copy(isGameOver = true) }
                }
            }
        }
    }

    fun activateHint() {
        val state = _gameState.value
        val level = state.level ?: return
        if (state.isVictory || state.isGameOver || state.remainingArrows.isEmpty()) return

        viewModelScope.launch {
            val hints = playerProgress.value.hintsRemaining
            if (hints <= 0) {
                // Fair anti-frustration: refill 2 free hints when empty so player never gets stuck
                repository.refillFreeHint()
            }
            val consumed = repository.consumeHint()
            if (consumed) {
                val bestArrow = PuzzleEngine.findHintArrow(state.remainingArrows, level.cols, level.rows)
                if (bestArrow != null) {
                    soundManager.playHint()
                    vibrateShort()
                    _gameState.update { it.copy(hintedArrowId = bestArrow.id) }
                }
            }
        }
    }

    fun stepAnimations(nowNanos: Long, dtSeconds: Float) {
        val state = _gameState.value
        if (state.flyingArrows.isEmpty() && state.blockedAnims.isEmpty() && state.particles.isEmpty() && state.confetti.isEmpty()) {
            return
        }

        val flyDurationNanos = 440_000_000L
        val blockDurationNanos = 320_000_000L

        val nextFlying = state.flyingArrows.mapNotNull { anim ->
            val p = ((nowNanos - anim.startTimeNanos).toFloat() / flyDurationNanos).coerceIn(0f, 1f)
            if (p >= 1f) null else anim.copy(progress = p)
        }

        val nextBlocked = state.blockedAnims.mapNotNull { anim ->
            val p = ((nowNanos - anim.startTimeNanos).toFloat() / blockDurationNanos).coerceIn(0f, 1f)
            if (p >= 1f) null else anim.copy(progress = p)
        }

        val nextParticles = state.particles.mapNotNull { p ->
            val newLife = p.life - dtSeconds * 2.4f
            if (newLife <= 0f) null
            else p.copy(
                x = p.x + p.vx * dtSeconds,
                y = p.y + p.vy * dtSeconds,
                vx = p.vx * 0.94f,
                vy = p.vy * 0.94f,
                life = newLife,
                alpha = newLife.coerceIn(0f, 1f)
            )
        }

        val nextConfetti = if (state.isVictory) {
            state.confetti.map { c ->
                var ny = c.yFraction + c.vy * dtSeconds
                var nx = c.xFraction + c.vx * dtSeconds
                if (ny > 1.1f) {
                    ny = -0.08f
                    nx = Random.nextFloat()
                }
                c.copy(
                    xFraction = nx,
                    yFraction = ny,
                    rotation = (c.rotation + c.rotationSpeed * dtSeconds) % 360f
                )
            }
        } else {
            emptyList()
        }

        _gameState.update {
            it.copy(
                flyingArrows = nextFlying,
                blockedAnims = nextBlocked,
                particles = nextParticles,
                confetti = nextConfetti
            )
        }
    }

    private fun triggerVictory(level: PuzzleLevel) {
        viewModelScope.launch {
            delay(360)
            val hearts = _gameState.value.heartsRemaining
            val stars = hearts.coerceIn(1, 3)
            soundManager.playVictory()
            vibrateVictory()
            repository.recordLevelComplete(
                levelNumber = level.levelNumber,
                starsEarned = stars,
                arrowsCleared = level.initialArrows.size
            )
            _gameState.update {
                it.copy(
                    isVictory = true,
                    starsEarned = stars,
                    confetti = createConfettiBurst()
                )
            }
        }
    }

    private fun findClosestArrow(
        arrows: List<ArrowTile>,
        tapX: Float,
        tapY: Float,
        cellSize: Float,
        originX: Float,
        originY: Float
    ): ArrowTile? {
        val maxThreshold = cellSize * 0.65f
        var bestArrow: ArrowTile? = null
        var bestDist = Float.MAX_VALUE

        for (arrow in arrows) {
            val cells = arrow.cells
            if (cells.isEmpty()) continue
            if (cells.size == 1) {
                val cx = originX + (cells[0].x + 0.5f) * cellSize
                val cy = originY + (cells[0].y + 0.5f) * cellSize
                val d = hypot(tapX - cx, tapY - cy)
                if (d < bestDist && d <= maxThreshold) {
                    bestDist = d
                    bestArrow = arrow
                }
                continue
            }
            for (i in 0 until cells.size - 1) {
                val ax = originX + (cells[i].x + 0.5f) * cellSize
                val ay = originY + (cells[i].y + 0.5f) * cellSize
                val bx = originX + (cells[i + 1].x + 0.5f) * cellSize
                val by = originY + (cells[i + 1].y + 0.5f) * cellSize
                val d = pointToSegmentDistance(tapX, tapY, ax, ay, bx, by)
                if (d < bestDist && d <= maxThreshold) {
                    bestDist = d
                    bestArrow = arrow
                }
            }
        }
        return bestArrow
    }

    private fun pointToSegmentDistance(
        px: Float,
        py: Float,
        ax: Float,
        ay: Float,
        bx: Float,
        by: Float
    ): Float {
        val abx = bx - ax
        val aby = by - ay
        val lenSq = abx * abx + aby * aby
        if (lenSq == 0f) return hypot(px - ax, py - ay)
        val t = (((px - ax) * abx + (py - ay) * aby) / lenSq).coerceIn(0f, 1f)
        val projX = ax + t * abx
        val projY = ay + t * aby
        return hypot(px - projX, py - projY)
    }

    private fun spawnDirectionalParticles(
        x: Float,
        y: Float,
        dx: Float,
        dy: Float,
        color: Color,
        count: Int
    ): List<Particle> {
        val baseAngle = kotlin.math.atan2(dy.toDouble(), dx.toDouble())
        return List(count) {
            val angle = baseAngle + Random.nextDouble(-0.75, 0.75)
            val speed = Random.nextDouble(120.0, 340.0).toFloat()
            Particle(
                x = x,
                y = y,
                vx = (cos(angle) * speed).toFloat(),
                vy = (sin(angle) * speed).toFloat(),
                radius = Random.nextDouble(3.0, 6.5).toFloat(),
                color = color
            )
        }
    }

    private fun spawnBurstParticles(
        x: Float,
        y: Float,
        color: Color,
        count: Int
    ): List<Particle> {
        return List(count) { idx ->
            val angle = (2.0 * PI * idx / count) + Random.nextDouble(-0.2, 0.2)
            val speed = Random.nextDouble(90.0, 260.0).toFloat()
            Particle(
                x = x,
                y = y,
                vx = (cos(angle) * speed).toFloat(),
                vy = (sin(angle) * speed).toFloat(),
                radius = Random.nextDouble(3.5, 7.0).toFloat(),
                color = color
            )
        }
    }

    private fun createConfettiBurst(): List<ConfettiPiece> {
        val colors = listOf(
            Color(0xFFFFFFFF),
            Color(0xFFFACC15),
            Color(0xFF38BDF8),
            Color(0xFF34D399),
            Color(0xFFFB7185),
            Color(0xFFC084FC)
        )
        return List(48) {
            ConfettiPiece(
                xFraction = Random.nextFloat(),
                yFraction = Random.nextFloat() * -0.9f,
                vx = Random.nextDouble(-0.08, 0.08).toFloat(),
                vy = Random.nextDouble(0.28, 0.56).toFloat(),
                width = Random.nextDouble(10.0, 18.0).toFloat(),
                height = Random.nextDouble(16.0, 28.0).toFloat(),
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = Random.nextDouble(-220.0, 220.0).toFloat(),
                color = colors[it % colors.size]
            )
        }
    }

    fun toggleSound(enabled: Boolean) {
        viewModelScope.launch { repository.setSoundEnabled(enabled) }
    }

    fun toggleHaptics(enabled: Boolean) {
        viewModelScope.launch { repository.setHapticsEnabled(enabled) }
    }

    fun toggleGridDots(show: Boolean) {
        viewModelScope.launch { repository.setShowGridDots(show) }
    }

    fun toggleDarkTheme(dark: Boolean) {
        viewModelScope.launch { repository.setDarkTheme(dark) }
    }

    private fun vibrateShort() {
        if (!playerProgress.value.hapticsEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(18)
            }
        } catch (_: Throwable) {
        }
    }

    private fun vibrateError() {
        if (!playerProgress.value.hapticsEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 45, 35, 60), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(90)
            }
        } catch (_: Throwable) {
        }
    }

    private fun vibrateVictory() {
        if (!playerProgress.value.hapticsEnabled) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 30, 40, 50), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(60)
            }
        } catch (_: Throwable) {
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.release()
    }
}
