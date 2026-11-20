package com.horsesgo.ui

import com.horsesgo.ai.MinimaxNode
import com.horsesgo.game.Difficulty
import com.horsesgo.game.MatchState
import com.horsesgo.game.Position
import com.horsesgo.game.Side

internal enum class Screen { Loading, Selector, Board }

internal enum class GameMessage {
    InvalidSave,
    ReadFailed,
    SaveFailed,
}

internal data class GameUiState(
    val screen: Screen = Screen.Loading,
    val match: MatchState? = null,
    val selectedLevel: Difficulty? = null,
    val winner: Side? = null,
    val saving: Boolean = false,
    val thinking: Boolean = false,
    val message: GameMessage? = null,
    val playerDestinations: List<Position> = emptyList(),
    val minimaxTree: MinimaxNode? = null,
) {
    val canPlay: Boolean
        get() = screen == Screen.Board && match?.turn == Side.Black &&
            winner == null && !saving && message != GameMessage.SaveFailed && minimaxTree == null

    val canViewMinimaxTree: Boolean
        get() = canPlay && match?.difficulty == Difficulty.Intermediate5x5

    val canChangeMap: Boolean
        get() = screen == Screen.Board && !saving && minimaxTree == null

    val canRestart: Boolean
        get() = canChangeMap && winner != null && message != GameMessage.SaveFailed

    val canLoad: Boolean
        get() = screen == Screen.Selector && selectedLevel != null && !saving
}
