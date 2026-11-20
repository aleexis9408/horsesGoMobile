package com.horsesgo.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.horsesgo.game.Board
import com.horsesgo.game.Position
import com.horsesgo.resources.Res
import com.horsesgo.resources.knight_black
import com.horsesgo.resources.knight_white
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun GameBoard(board: Board, playableDestinations: List<Position>, onPlay: (Position) -> Unit) {
    Column(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(6.dp))) {
        for (row in board.size - 1 downTo 0) {
            Row(Modifier.fillMaxWidth().weight(1f)) {
                for (column in 0 until board.size) {
                    val position = Position(row, column)
                    BoardSquare(
                        position, board[position], position in playableDestinations, onPlay,
                        Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun BoardSquare(
    position: Position,
    cell: Int,
    legal: Boolean,
    onPlay: (Position) -> Unit,
    modifier: Modifier,
) {
    val notation = position.notation()
    val description = when (cell) {
        Board.BLACK -> "caballo negro"
        Board.WHITE -> "caballo blanco"
        Board.DESTROYED -> "casilla destruida"
        else -> if (legal) "casilla libre, destino legal" else "casilla libre"
    }
    val background = when {
        cell == Board.DESTROYED -> Color(0xFF3E5950)
        (position.row + position.column) % 2 == 0 -> Color(0xFFC0AE91)
        else -> Color(0xFFF0D9B5)
    }
    Box(modifier.background(background).drawBehind {
        if (legal) {
            drawCircle(Color(0x3D1C6541), size.minDimension * 0.22f)
            drawCircle(Color(0xB81C6541), size.minDimension * 0.12f)
        }
    }.clickable(enabled = legal, role = Role.Button) { onPlay(position) }
        .semantics(mergeDescendants = true) { contentDescription = "$notation, $description" }) {
        when (cell) {
            Board.BLACK, Board.WHITE -> Image(
                painterResource(if (cell == Board.BLACK) Res.drawable.knight_black else Res.drawable.knight_white),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
            Board.DESTROYED -> Canvas(Modifier.fillMaxSize()) {
                val ink = Color(0xB8BCD0BD)
                drawLine(ink, Offset(size.width * 0.15f, 0f), Offset(size.width * 0.85f, size.height), 2.dp.toPx())
                drawLine(ink, Offset(size.width * 0.85f, 0f), Offset(size.width * 0.15f, size.height), 2.dp.toPx())
            }
        }
        val labelColor = if (cell == Board.DESTROYED) Color(0xFFFFF5E3) else Color(0xFF3E2A1D)
        if (position.column == 0) {
            Coordinate((position.row + 1).toString(), labelColor, Modifier.align(Alignment.TopStart))
        }
        if (position.row == 0) {
            Coordinate(notation.first().toString(), labelColor, Modifier.align(Alignment.BottomEnd))
        }
    }
}

@Composable
private fun Coordinate(text: String, color: Color, modifier: Modifier) {
    Text(text, modifier.padding(horizontal = 3.dp).clearAndSetSemantics {}, color = color,
        fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
}
