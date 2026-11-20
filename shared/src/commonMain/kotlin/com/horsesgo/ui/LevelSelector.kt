package com.horsesgo.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.horsesgo.game.Difficulty
import com.horsesgo.resources.Res
import com.horsesgo.resources.horses_go_icon
import org.jetbrains.compose.resources.painterResource

@Composable
internal fun LevelSelector(
    state: GameUiState,
    onSelect: (Difficulty) -> Unit,
    onLoad: () -> Unit,
    onReturn: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(Res.drawable.horses_go_icon),
            contentDescription = null,
            modifier = Modifier.size(54.dp).clip(RoundedCornerShape(12.dp)),
        )
        Column {
            Text("Horses Go", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("Gotta destroy them all!", color = GameAccent, style = MaterialTheme.typography.bodySmall)
        }
    }
    Text(
        "Salta en L. La casilla que dejas se destruye. Gana quien hace el último movimiento.",
        color = GameMuted,
        style = MaterialTheme.typography.bodyMedium,
    )
    Column(
        Modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Dificultad", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
        Difficulty.entries.forEach { level ->
            val selected = level == state.selectedLevel
            val shape = RoundedCornerShape(14.dp)
            Surface(
                modifier = Modifier.fillMaxWidth().clip(shape).selectable(
                    selected = selected,
                    enabled = !state.saving,
                    role = Role.RadioButton,
                    onClick = { onSelect(level) },
                ),
                shape = shape,
                color = GamePanel,
                border = BorderStroke(1.dp, if (selected) GameAccent else GameLine),
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            "${level.label} ${level.size} × ${level.size}",
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "MiniMax · ${level.searchDepth} jugadas",
                            color = GameMuted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    RadioButton(selected, onClick = null, enabled = !state.saving)
                }
            }
        }
    }
    Button(onClick = onLoad, enabled = state.canLoad, modifier = Modifier.fillMaxWidth()) {
        Text(if (state.saving) "Guardando…" else "Cargar mapa")
    }
    if (state.match != null) {
        TextButton(onClick = onReturn, enabled = !state.saving, modifier = Modifier.fillMaxWidth()) {
            Text("Volver a la partida")
        }
    }
}
