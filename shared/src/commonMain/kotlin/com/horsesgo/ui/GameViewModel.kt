package com.horsesgo.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.horsesgo.ai.MinimaxTree
import com.horsesgo.ai.MoveSearch
import com.horsesgo.game.Difficulty
import com.horsesgo.game.GameRules
import com.horsesgo.game.MatchState
import com.horsesgo.game.Position
import com.horsesgo.game.Side
import com.horsesgo.storage.MatchRead
import com.horsesgo.storage.MatchStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class GameViewModel(
    private val rules: GameRules,
    private val search: MoveSearch,
    private val store: MatchStore,
    private val searchDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    private val state = MutableStateFlow(GameUiState())
    val uiState = state.asStateFlow()
    private val tree = MinimaxTree(rules)

    private data class PendingSave(val match: MatchState, val opensMap: Boolean)
    private data class SearchToken(val generation: Long, val moveNumber: Int)

    // UI actions and all changes to these fields are confined to the main dispatcher.
    private var pending: PendingSave? = null
    private var generation = 0L
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            val saved = withContext(ioDispatcher) { store.load() }
            when (saved) {
                is MatchRead.Present -> publishMatch(saved.match, opensMap = true)
                MatchRead.Absent -> showSelector()
                MatchRead.Invalid -> showSelector(GameMessage.InvalidSave)
                MatchRead.Unavailable -> showSelector(GameMessage.ReadFailed)
            }
        }
    }

    fun selectLevel(level: Difficulty) {
        val current = state.value
        if (current.screen != Screen.Selector || current.saving) return
        if (pending?.opensMap == true) pending = null
        state.update { it.copy(selectedLevel = level, message = if (pending == null) null else it.message) }
    }

    fun loadSelectedMap() {
        val current = state.value
        val level = current.selectedLevel ?: return
        if (!current.canLoad) return
        startMatch(level)
    }

    fun restartMatch() {
        val current = state.value
        val match = current.match ?: return
        if (current.canRestart) startMatch(match.difficulty)
    }

    private fun startMatch(level: Difficulty) {
        generation++
        searchJob?.cancel()
        searchJob = null
        state.update { it.copy(thinking = false) }
        save(PendingSave(rules.open(level), opensMap = true))
    }

    fun play(destination: Position) {
        val current = state.value
        val match = current.match ?: return
        if (!current.canPlay) return
        val next = rules.play(match, destination) ?: return
        save(PendingSave(next, opensMap = false))
    }

    fun retrySave() {
        val retry = pending ?: return
        if (!state.value.saving) save(retry)
    }

    fun openMinimaxTree() {
        val current = state.value
        val match = current.match ?: return
        if (!current.canViewMinimaxTree) return
        val root = tree.build(match)
        state.update { it.copy(minimaxTree = root) }
    }

    fun closeMinimaxTree() {
        state.update { it.copy(minimaxTree = null) }
    }

    fun openSelector() {
        val current = state.value
        if (current.canChangeMap) {
            state.update { it.copy(screen = Screen.Selector, selectedLevel = current.match?.difficulty) }
        }
    }

    fun returnToMatch() {
        val current = state.value
        if (current.screen != Screen.Selector || current.match == null || current.saving) return
        if (pending?.opensMap == true) pending = null
        state.update { it.copy(screen = Screen.Board, message = if (pending == null) null else it.message) }
        searchIfNeeded()
    }

    private fun showSelector(message: GameMessage? = null) {
        state.update { it.copy(screen = Screen.Selector, message = message) }
    }

    private fun save(change: PendingSave) {
        pending = change
        state.update { it.copy(saving = true, message = null) }
        viewModelScope.launch {
            if (withContext(ioDispatcher) { store.save(change.match) }) {
                pending = null
                publishMatch(change.match, change.opensMap)
            } else {
                state.update { it.copy(saving = false, message = GameMessage.SaveFailed) }
            }
        }
    }

    private fun publishMatch(match: MatchState, opensMap: Boolean) {
        val playerDestinations = if (match.turn == Side.Black) {
            rules.legalDestinations(match.board, Side.Black)
        } else {
            emptyList()
        }
        state.update {
            it.copy(
                screen = if (opensMap) Screen.Board else it.screen,
                match = match,
                winner = rules.winner(match),
                selectedLevel = if (opensMap) match.difficulty else it.selectedLevel,
                saving = false,
                thinking = false,
                message = null,
                playerDestinations = playerDestinations,
                minimaxTree = null,
            )
        }
        searchIfNeeded()
    }

    private fun searchIfNeeded() {
        val current = state.value
        val match = current.match ?: return
        if (match.turn != Side.White || current.winner != null || pending != null ||
            current.thinking
        ) return
        val token = SearchToken(generation, match.board.moveNumber)
        state.update { it.copy(thinking = true) }
        searchJob = viewModelScope.launch {
            val move = withContext(searchDispatcher) { search.chooseMove(match) }
            val active = state.value.match ?: return@launch
            if (token != SearchToken(generation, active.board.moveNumber) || active.turn != Side.White) {
                return@launch
            }
            state.update { it.copy(thinking = false) }
            val next = move?.let { rules.play(active, it) } ?: return@launch
            save(PendingSave(next, opensMap = false))
        }
    }
}
