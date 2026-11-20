package com.horsesgo.ui

import androidx.lifecycle.viewModelScope
import com.horsesgo.ai.MoveSearch
import com.horsesgo.game.Difficulty
import com.horsesgo.game.GameRules
import com.horsesgo.game.MatchState
import com.horsesgo.game.Position
import com.horsesgo.game.Side
import com.horsesgo.game.testBoard
import com.horsesgo.game.testOpening
import com.horsesgo.storage.MatchRead
import com.horsesgo.storage.MatchStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelPersistenceTest {
    @Test
    fun publishesOnlyAfterSaveAndRetriesRejectedMoveOnce() = withGame { vm, store ->
        vm.selectLevel(Difficulty.Beginner)
        vm.loadSelectedMap()
        runCurrent()
        assertEquals(0, vm.uiState.value.match?.board?.moveNumber)

        store.accepted = false
        vm.play(vm.uiState.value.playerDestinations.first())
        runCurrent()
        assertEquals(GameMessage.SaveFailed, vm.uiState.value.message)
        assertEquals(0, vm.uiState.value.match?.board?.moveNumber)

        store.accepted = true
        store.onSave = { next ->
            assertEquals(next.board.moveNumber - 1, vm.uiState.value.match?.board?.moveNumber)
        }
        vm.retrySave()
        vm.retrySave()
        runCurrent()
        assertEquals(listOf(0, 1, 2), store.saved.map { it.board.moveNumber })
        assertNull(vm.uiState.value.message)
        assertEquals(Side.Black, vm.uiState.value.match?.turn)
    }

    @Test
    fun resumesExactlyOneWhiteMoveFromStorage() {
        val opening = testOpening()
        val match = assertNotNull(GameRules().play(opening, Position(3, 0)))
        withGame(MatchRead.Present(match)) { vm, store ->
            assertEquals(listOf(2), store.saved.map { it.board.moveNumber })
            assertEquals(Side.Black, vm.uiState.value.match?.turn)
        }
    }

    @Test
    fun restartingFinishedMatchSavesOneNewOpeningAtTheSameLevel() {
        val level = Difficulty.Intermediate5x5
        val finished = MatchState(level, testBoard(level = level), Side.White)
        withGame(MatchRead.Present(finished)) { vm, store ->
            assertEquals(Side.Black, vm.uiState.value.winner)
            store.accepted = false
            vm.restartMatch()
            runCurrent()
            assertEquals(finished, vm.uiState.value.match)
            assertEquals(GameMessage.SaveFailed, vm.uiState.value.message)

            store.accepted = true
            vm.retrySave()
            vm.restartMatch()
            runCurrent()
            val opening = store.saved.single()
            assertEquals(Screen.Board, vm.uiState.value.screen)
            assertEquals(opening, vm.uiState.value.match)
            assertEquals(level, opening.difficulty)
            assertEquals(0, opening.board.moveNumber)
            assertEquals(Side.Black, opening.turn)
            assertNull(vm.uiState.value.winner)

            vm.restartMatch()
            runCurrent()
            assertEquals(listOf(opening), store.saved)
        }
    }

    @Test
    fun restoresWinnerAndInvalidDataReturnsToSelector() {
        withGame(MatchRead.Present(MatchState(Difficulty.Beginner, testBoard(), Side.Black))) { vm, store ->
            assertEquals(Side.White, vm.uiState.value.winner)
            assertEquals(Screen.Board, vm.uiState.value.screen)
            assertEquals(emptyList(), store.saved)
        }
        withGame(MatchRead.Invalid) { vm, _ ->
            assertEquals(Screen.Selector, vm.uiState.value.screen)
            assertEquals(GameMessage.InvalidSave, vm.uiState.value.message)
        }
    }
}

internal class TestMatchStore(var stored: MatchRead = MatchRead.Absent) : MatchStore {
    val saved = mutableListOf<MatchState>()
    var accepted = true
    var onSave: (MatchState) -> Unit = {}

    override fun load() = stored

    override fun save(match: MatchState): Boolean {
        onSave(match)
        if (accepted) {
            saved += match
            stored = MatchRead.Present(match)
        }
        return accepted
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
internal fun withGame(
    initial: MatchRead = MatchRead.Absent,
    search: MoveSearch = MoveSearch { GameRules().legalMoves(it).firstOrNull() },
    rules: GameRules = GameRules(),
    block: suspend TestScope.(GameViewModel, TestMatchStore) -> Unit,
) = runTest {
    val dispatcher = StandardTestDispatcher(testScheduler)
    Dispatchers.setMain(dispatcher)
    val store = TestMatchStore(initial)
    val vm = GameViewModel(rules, search, store, dispatcher, dispatcher)
    try {
        runCurrent()
        block(vm, store)
    } finally {
        vm.viewModelScope.cancel()
        Dispatchers.resetMain()
    }
}
