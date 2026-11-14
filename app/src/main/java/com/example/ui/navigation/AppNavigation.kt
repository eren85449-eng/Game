package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.model.PuzzleShape
import com.example.ui.components.SettingsDialog
import com.example.ui.screens.GameScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.TapAwayArrowsTheme
import com.example.viewmodel.GameViewModel

const val ROUTE_SPLASH = "splash"
const val ROUTE_HOME = "home"
const val ROUTE_GAME = "game/{level}/{shape}"

@Composable
fun TapAwayApp(
    viewModel: GameViewModel = viewModel()
) {
    val progress by viewModel.playerProgress.collectAsStateWithLifecycle()
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    var showSettings by remember { mutableStateOf(false) }

    TapAwayArrowsTheme(darkTheme = progress.darkTheme) {
        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = ROUTE_SPLASH
        ) {
            composable(ROUTE_SPLASH) {
                SplashScreen(
                    onFinished = {
                        navController.navigate(ROUTE_HOME) {
                            popUpTo(ROUTE_SPLASH) { inclusive = true }
                        }
                    }
                )
            }

            composable(ROUTE_HOME) {
                HomeScreen(
                    progress = progress,
                    onPlayLevel = { levelNumber, shapeOverride ->
                        viewModel.soundManager.playTap()
                        val shapeArg = shapeOverride?.name ?: "AUTO"
                        navController.navigate("game/$levelNumber/$shapeArg")
                    },
                    onOpenSettings = {
                        viewModel.soundManager.playTap()
                        showSettings = true
                    }
                )
            }

            composable(
                route = ROUTE_GAME,
                arguments = listOf(
                    navArgument("level") { type = NavType.IntType },
                    navArgument("shape") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val levelNum = backStackEntry.arguments?.getInt("level") ?: 1
                val shapeName = backStackEntry.arguments?.getString("shape") ?: "AUTO"
                val shapeOverride = remember(shapeName) {
                    if (shapeName == "AUTO") null
                    else runCatching { PuzzleShape.valueOf(shapeName) }.getOrNull()
                }

                LaunchedEffect(levelNum, shapeOverride) {
                    viewModel.loadLevel(levelNum, shapeOverride)
                }

                GameScreen(
                    state = gameState,
                    progress = progress,
                    onBack = {
                        viewModel.soundManager.playTap()
                        navController.popBackStack()
                    },
                    onOpenSettings = {
                        viewModel.soundManager.playTap()
                        showSettings = true
                    },
                    onTapBoard = viewModel::onBoardTapped,
                    onUseHint = viewModel::activateHint,
                    onToggleGridDots = {
                        viewModel.soundManager.playTap()
                        viewModel.toggleGridDots(!progress.showGridDots)
                    },
                    onRestart = viewModel::restartLevel,
                    onNextLevel = {
                        viewModel.soundManager.playTap()
                        val nextLvl = (levelNum + 1).coerceAtMost(250)
                        navController.navigate("game/$nextLvl/AUTO") {
                            popUpTo(ROUTE_HOME) { inclusive = false }
                        }
                    },
                    onMainMenu = {
                        viewModel.soundManager.playTap()
                        navController.popBackStack(ROUTE_HOME, inclusive = false)
                    },
                    onStepAnimations = viewModel::stepAnimations
                )
            }
        }

        if (showSettings) {
            SettingsDialog(
                progress = progress,
                onToggleSound = viewModel::toggleSound,
                onToggleHaptics = viewModel::toggleHaptics,
                onToggleGridDots = viewModel::toggleGridDots,
                onToggleDarkTheme = viewModel::toggleDarkTheme,
                onDismiss = { showSettings = false }
            )
        }
    }
}
