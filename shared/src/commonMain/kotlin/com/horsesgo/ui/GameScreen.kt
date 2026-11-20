package com.horsesgo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.horsesgo.game.Position
import com.horsesgo.game.Side

@Composable
internal fun GameScreen(
    state: GameUiState,
    onPlay: (Position) -> Unit,
    onRestart: () -> Unit,
    onChangeMap: () -> Unit,
    onViewMinimaxTree: () -> Unit,
    onCloseMinimaxTree: () -> Unit,
) {
    val match = state.match ?: return
    val board = match.board
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(match.difficulty.label, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        val moves = board.moveNumber
        val moveLabel = if (moves == 1) "1 jugada" else "$moves jugadas"
        Text("${board.size} × ${board.size} · $moveLabel", color = GameMuted)
    }

    val status = when {
        state.winner == Side.Black -> "Ganaste"
        state.winner == Side.White -> "Ganó la máquina"
        state.saving -> "Guardando…"
        match.turn == Side.Black -> "Tu turno"
        else -> "Turno de la máquina"
    }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (state.winner != null) GameAccent else GamePanel,
        contentColor = if (state.winner != null) GameBackground else GameInk,
        modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Text(status, Modifier.padding(14.dp), fontWeight = FontWeight.SemiBold)
    }

    val playableDestinations = if (state.canPlay) state.playerDestinations else emptyList()
    GameBoard(board, playableDestinations, onPlay)
    if (state.winner != null) {
        Button(onClick = onRestart, enabled = state.canRestart, modifier = Modifier.fillMaxWidth()) {
            Text("Reiniciar partida")
        }
    }
    if (state.canViewMinimaxTree) {
        OutlinedButton(onClick = onViewMinimaxTree, modifier = Modifier.fillMaxWidth()) {
            Text("Ver árbol MiniMax")
        }
    }
    TextButton(onClick = onChangeMap, enabled = state.canChangeMap, modifier = Modifier.fillMaxWidth()) {
        Text("Cambiar mapa")
    }
    state.minimaxTree?.let { MinimaxTreeDialog(board, it, onCloseMinimaxTree) }
}

@Composable
internal fun GameNotice(message: GameMessage, onRetry: () -> Unit) {
    Surface(color = GamePanel, shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp).semantics { liveRegion = LiveRegionMode.Polite }) {
            val text = when (message) {
                GameMessage.InvalidSave -> "No se pudo recuperar la partida guardada."
                GameMessage.ReadFailed -> "No se pudo leer el guardado."
                GameMessage.SaveFailed -> "No se pudo guardar el cambio."
            }
            Text(text, color = MaterialTheme.colorScheme.error)
            if (message == GameMessage.SaveFailed) {
                TextButton(onClick = onRetry) { Text("Reintentar guardado") }
            }
        }
    }
}
