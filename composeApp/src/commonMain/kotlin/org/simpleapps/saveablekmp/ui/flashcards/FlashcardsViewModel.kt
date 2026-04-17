// commonMain/ui/flashcards/FlashcardsViewModel.kt
package org.simpleapps.saveablekmp.ui.flashcards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.simpleapps.saveablekmp.data.model.SavedItem
import org.simpleapps.saveablekmp.data.model.generateId
import org.simpleapps.saveablekmp.data.repository.SaveableRepository
import org.simpleapps.saveablekmp.domain.srs.ReviewDifficulty
import org.simpleapps.saveablekmp.domain.srs.SpacedRepetition
import org.simpleapps.saveablekmp.sync.SyncManager

data class DeckInfo(
    val id: String,         // subcategory id
    val name: String,       // назва колоди (з категорій)
    val totalCards: Long,
    val dueCards: Long,
)

data class FlashcardsState(
    val decks: List<DeckInfo> = emptyList(),
    val selectedDeck: DeckInfo? = null,
    val cardsInDeck: List<SavedItem> = emptyList(),
    // Режим навчання
    val isStudying: Boolean = false,
    val studyQueue: List<SavedItem> = emptyList(),
    val currentCardIndex: Int = 0,
    val isFlipped: Boolean = false,
    val studySessionDone: Boolean = false,
    // Додавання/редагування
    val isAddingCard: Boolean = false,
    val editingCard: SavedItem? = null,
    val inputFront: String = "",
    val inputBack: String = "",
    val inputDeck: String = "",
    val toastMessage: String? = null,
)

sealed interface FlashcardsEvent {
    object LoadDecks : FlashcardsEvent
    data class SelectDeck(val deck: DeckInfo) : FlashcardsEvent
    object BackToDecks : FlashcardsEvent
    object StartStudy : FlashcardsEvent
    object FlipCard : FlashcardsEvent
    data class RateCard(val difficulty: ReviewDifficulty) : FlashcardsEvent
    object FinishStudy : FlashcardsEvent
    object AddCard : FlashcardsEvent
    data class StartEditCard(val card: SavedItem) : FlashcardsEvent
    data class DeleteCard(val id: String) : FlashcardsEvent
    data class FrontChanged(val v: String) : FlashcardsEvent
    data class BackChanged(val v: String) : FlashcardsEvent
    data class DeckChanged(val v: String) : FlashcardsEvent
    object SaveCard : FlashcardsEvent
    object CancelEdit : FlashcardsEvent
    object ClearToast : FlashcardsEvent
}

class FlashcardsViewModel(
    private val repository: SaveableRepository,
    private val syncManager: SyncManager,
) : ViewModel() {

    private val _state = MutableStateFlow(FlashcardsState())
    val state: StateFlow<FlashcardsState> = _state.asStateFlow()

    init {
        loadDecks()
        // Спостерігаємо за категоріями для отримання назв колод
        viewModelScope.launch {
            repository.observeCategories().collect { _ -> loadDecks() }
        }
    }

    private fun loadDecks() {
        viewModelScope.launch {
            val categories = repository.observeCategories().first()
            val deckPairs = repository.getDecks()
            val decks = deckPairs.map { (subcategoryId, count) ->
                val cat = categories.find { it.id == subcategoryId }
                val dueCount = repository.getDueCardsCount(subcategoryId)
                DeckInfo(
                    id = subcategoryId,
                    name = cat?.name ?: subcategoryId,
                    totalCards = count,
                    dueCards = dueCount,
                )
            }
            _state.update { it.copy(decks = decks) }
        }
    }

    fun onEvent(event: FlashcardsEvent) {
        when (event) {
            is FlashcardsEvent.LoadDecks -> loadDecks()

            is FlashcardsEvent.SelectDeck -> {
                viewModelScope.launch {
                    val cards = repository.getFlashcardsByDeck(event.deck.id)
                    _state.update { it.copy(
                        selectedDeck = event.deck,
                        cardsInDeck = cards,
                    )}
                }
            }

            is FlashcardsEvent.BackToDecks -> {
                _state.update { it.copy(
                    selectedDeck = null,
                    cardsInDeck = emptyList(),
                    isStudying = false,
                )}
                loadDecks()
            }

            is FlashcardsEvent.StartStudy -> {
                val deck = _state.value.selectedDeck ?: return
                val dueCards = repository.getDueFlashcards(deck.id)
                if (dueCards.isEmpty()) {
                    _state.update { it.copy(toastMessage = "Немає карток для повторення") }
                    return
                }
                _state.update { it.copy(
                    isStudying = true,
                    studyQueue = dueCards.toMutableList(),
                    currentCardIndex = 0,
                    isFlipped = false,
                    studySessionDone = false,
                )}
            }

            is FlashcardsEvent.FlipCard -> {
                _state.update { it.copy(isFlipped = !it.isFlipped) }
            }

            is FlashcardsEvent.RateCard -> {
                val s = _state.value
                val currentCard = s.studyQueue.getOrNull(s.currentCardIndex) ?: return
                viewModelScope.launch {
                    val result = SpacedRepetition.calculate(
                        difficulty = event.difficulty,
                        currentEaseFactor = currentCard.easeFactor,
                        currentInterval = currentCard.interval,
                        currentRepetitions = currentCard.repetitions,
                    )
                    repository.updateSrsData(currentCard, result)
                    syncManager.trigger()

                    val nextIndex = s.currentCardIndex + 1
                    if (nextIndex >= s.studyQueue.size) {
                        _state.update { it.copy(studySessionDone = true) }
                    } else {
                        _state.update { it.copy(
                            currentCardIndex = nextIndex,
                            isFlipped = false,
                        )}
                    }
                }
            }

            is FlashcardsEvent.FinishStudy -> {
                val deck = _state.value.selectedDeck
                _state.update { it.copy(
                    isStudying = false,
                    studySessionDone = false,
                    isFlipped = false,
                )}
                if (deck != null) {
                    viewModelScope.launch {
                        val cards = repository.getFlashcardsByDeck(deck.id)
                        _state.update { it.copy(cardsInDeck = cards) }
                    }
                }
                loadDecks()
            }

            is FlashcardsEvent.AddCard -> {
                _state.update { it.copy(
                    isAddingCard = true,
                    editingCard = null,
                    inputFront = "",
                    inputBack = "",
                    inputDeck = it.selectedDeck?.id ?: "",
                )}
            }

            is FlashcardsEvent.StartEditCard -> {
                _state.update { it.copy(
                    isAddingCard = true,
                    editingCard = event.card,
                    inputFront = event.card.value,
                    inputBack = event.card.description,
                    inputDeck = event.card.subcategory,
                )}
            }

            is FlashcardsEvent.DeleteCard -> {
                viewModelScope.launch {
                    repository.softDeleteItem(event.id)
                    syncManager.trigger()
                    val deck = _state.value.selectedDeck
                    if (deck != null) {
                        val cards = repository.getFlashcardsByDeck(deck.id)
                        _state.update { it.copy(cardsInDeck = cards) }
                    }
                    loadDecks()
                    _state.update { it.copy(toastMessage = "Картку видалено") }
                }
            }

            is FlashcardsEvent.FrontChanged -> _state.update { it.copy(inputFront = event.v) }
            is FlashcardsEvent.BackChanged  -> _state.update { it.copy(inputBack = event.v) }
            is FlashcardsEvent.DeckChanged  -> _state.update { it.copy(inputDeck = event.v) }

            is FlashcardsEvent.SaveCard -> {
                val s = _state.value
                if (s.inputFront.isBlank()) {
                    _state.update { it.copy(toastMessage = "Введіть передню сторону") }
                    return
                }
                if (s.inputDeck.isBlank()) {
                    _state.update { it.copy(toastMessage = "Оберіть колоду") }
                    return
                }
                viewModelScope.launch {
                    val now = Clock.System.now().toEpochMilliseconds()
                    if (s.editingCard != null) {
                        repository.updateItem(s.editingCard.copy(
                            value = s.inputFront,
                            description = s.inputBack,
                            subcategory = s.inputDeck,
                        ))
                    } else {
                        repository.insertItem(SavedItem(
                            id = generateId(),
                            value = s.inputFront,
                            description = s.inputBack,
                            category = "flashcard",
                            subcategory = s.inputDeck,
                            createdAt = now,
                            updatedAt = now,
                        ))
                    }
                    val deck = _state.value.selectedDeck
                    val cards = if (deck != null) repository.getFlashcardsByDeck(deck.id) else emptyList()
                    _state.update { it.copy(
                        isAddingCard = false,
                        editingCard = null,
                        cardsInDeck = cards,
                        toastMessage = if (s.editingCard != null) "Оновлено ✓" else "Картку додано ✓",
                    )}
                    loadDecks()
                    syncManager.trigger()
                }
            }

            is FlashcardsEvent.CancelEdit -> _state.update { it.copy(isAddingCard = false, editingCard = null) }
            is FlashcardsEvent.ClearToast -> _state.update { it.copy(toastMessage = null) }
        }
    }
}