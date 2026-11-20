package com.horsesgo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.horsesgo.ai.MinimaxSearch
import com.horsesgo.game.GameRules
import com.horsesgo.storage.MatchStore

internal val GameBackground = Color(0xFF122522)
internal val GamePanel = Color(0xFF203833)
internal val GameInk = Color(0xFFFFF5E3)
internal val GameMuted = Color(0xFFBACBC0)
internal val GameAccent = Color(0xFFF9B968)
internal val GameLine = Color(0xFF49645A)

private val GameColors = darkColorScheme(
    primary = GameAccent,
    onPrimary = GameBackground,
    background = GameBackground,
    onBackground = GameInk,
    surface = GameBackground,
    onSurface = GameInk,
    surfaceVariant = GamePanel,
    onSurfaceVariant = GameMuted,
    outline = GameLine,
    error = Color(0xFFFFBAAA),
)

@Composable
internal fun HorsesGoApp(store: MatchStore) {
    val game = viewModel {
        val rules = GameRules()
        GameViewModel(rules, MinimaxSearch(rules), store)
    }
    val state by game.uiState.collectAsStateWithLifecycle()

    MaterialTheme(colorScheme = GameColors) {
        Surface(Modifier.fillMaxSize(), color = GameBackground) {
            if (state.screen == Screen.Loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    Modifier.fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        Modifier.widthIn(max = 480.dp).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        when (state.screen) {
                            Screen.Selector -> LevelSelector(
                                state, game::selectLevel, game::loadSelectedMap, game::returnToMatch,
                            )
                            Screen.Board -> GameScreen(
                                state = state,
                                onPlay = game::play,
                                onRestart = game::restartMatch,
                                onChangeMap = game::openSelector,
                                onViewMinimaxTree = game::openMinimaxTree,
                                onCloseMinimaxTree = game::closeMinimaxTree,
                            )
                            Screen.Loading -> Unit
                        }
                        state.message?.let { GameNotice(it, game::retrySave) }
                    }
                }
            }
        }
    }
}
