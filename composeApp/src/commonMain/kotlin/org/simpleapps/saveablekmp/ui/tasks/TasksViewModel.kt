package org.simpleapps.saveablekmp.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.simpleapps.saveablekmp.data.model.*
import org.simpleapps.saveablekmp.data.repository.SaveableRepository
import org.simpleapps.saveablekmp.domain.usecase.SaveItemUseCase
import org.simpleapps.saveablekmp.domain.usecase.UpdateItemUseCase
import org.simpleapps.saveablekmp.sync.SyncManager

data class TasksState(
    val todayTasks: List<SavedItem> = emptyList(),
    val allActiveTasks: List<SavedItem> = emptyList(),
    val completedTasks: List<SavedItem> = emptyList(),
    val showCompleted: Boolean = false,
    val isAddDialogOpen: Boolean = false,
    val editingItem: SavedItem? = null,
    val isEditDialogOpen: Boolean = false,
    val toastMessage: String? = null,
    // Поля нового завдання
    val inputTitle: String = "",
    val inputNote: String = "",
    val inputPriority: Priority = Priority.MEDIUM,
    val inputDeadlineMs: Long = 0L,
    val inputRecurrence: String = "once",
    val inputSequence: String = "",   // "англ, нім, фр"
    val inputStrictSeq: Boolean = false,
)

sealed interface TasksEvent {
    object ToggleCompleted : TasksEvent
    object OpenAddDialog : TasksEvent
    object CloseAddDialog : TasksEvent
    data class Complete(val item: SavedItem) : TasksEvent
    data class Uncomplete(val item: SavedItem) : TasksEvent
    data class Delete(val id: String) : TasksEvent
    data class StartEdit(val item: SavedItem) : TasksEvent
    data class SaveEdit(val item: SavedItem) : TasksEvent
    object CloseEdit : TasksEvent
    object SaveNew : TasksEvent
    object ClearToast : TasksEvent
    // Input fields
    data class TitleChanged(val v: String) : TasksEvent
    data class NoteChanged(val v: String) : TasksEvent
    data class PriorityChanged(val p: Priority) : TasksEvent
    data class DeadlineChanged(val ms: Long) : TasksEvent
    data class RecurrenceChanged(val r: String) : TasksEvent
    data class SequenceChanged(val s: String) : TasksEvent
    data class StrictSeqChanged(val v: Boolean) : TasksEvent
}

class TasksViewModel(
    private val repository: SaveableRepository,
    private val saveItem: SaveItemUseCase,
    private val updateItem: UpdateItemUseCase,
    private val syncManager: SyncManager,
) : ViewModel() {

    private val _state = MutableStateFlow(TasksState())
    val state: StateFlow<TasksState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeTaskItems().collect { allTasks ->
                val now = System.currentTimeMillis()

                // Активні = не виконані + periodic що вже пора знову виконати
                val active = allTasks.filter { task ->
                    val meta = task.description.toTaskMeta()
                    val isRecurring = meta.recurrence != "once"
                    when {
                        // Одноразове виконане — неактивне
                        task.isCompleted && !isRecurring -> false
                        // Periodic виконане — активне тільки якщо настав час
                        task.isCompleted && isRecurring -> task.nextReviewAt <= now
                        // Не виконане — завжди активне
                        else -> true
                    }
                }.sortedWith(taskComparator())

                // Сьогоднішні = з active тільки ті що due today
                val today = active.filter { it.isTaskDueToday() }

                // Виконані = тільки одноразові виконані
                // Periodic виконані НЕ показуємо у "виконаних" — вони повернуться в active
                val done = allTasks.filter { task ->
                    val meta = task.description.toTaskMeta()
                    task.isCompleted && meta.recurrence == "once"
                }.sortedByDescending { it.updatedAt }

                _state.update { it.copy(
                    todayTasks = today,
                    allActiveTasks = active,
                    completedTasks = done,
                )}
            }
        }
    }

    private fun taskComparator(): Comparator<SavedItem> = compareBy(
        // 1. Дедлайн: задані раніше — вище (0 = немає дедлайну → вниз)
        { item ->
            val dl = item.description.toTaskMeta().deadlineMs
            if (dl == 0L) Long.MAX_VALUE else dl
        },
        // 2. Пріоритет: HIGH=0, MEDIUM=1, LOW=2
        { item ->
            when (item.priority) {
                Priority.HIGH   -> 0
                Priority.MEDIUM -> 1
                Priority.LOW    -> 2
            }
        },
        // 3. Дата створення: новіші вище
        { item -> -item.createdAt }
    )

    fun onEvent(event: TasksEvent) {
        when (event) {
            is TasksEvent.ToggleCompleted ->
                _state.update { it.copy(showCompleted = !it.showCompleted) }

            is TasksEvent.OpenAddDialog ->
                _state.update { it.copy(isAddDialogOpen = true) }

            is TasksEvent.CloseAddDialog ->
                _state.update { it.copy(isAddDialogOpen = false) }

            is TasksEvent.Complete -> {
                viewModelScope.launch {
                    val meta = event.item.description.toTaskMeta()
                    val isRecurring = meta.recurrence != "once"

                    val newMeta = if (meta.sequence.isNotEmpty()) {
                        meta.copy(seqIndex = meta.nextSeqIndex())
                    } else meta

                    val nextReview = if (isRecurring) newMeta.calcNextReviewAt() else 0L

                    val updatedItem = event.item.copy(
                        description = newMeta.toDescriptionString(),
                        // Для periodic: is_completed = true тільки до nextReviewAt,
                        // потім воно автоматично стане "активним" знову через фільтр
                        isCompleted = true,
                        nextReviewAt = nextReview,
                        isSynced = false,
                    )
                    repository.insertItem(updatedItem)
                    syncManager.trigger()
                }
            }

            is TasksEvent.Uncomplete -> {
                viewModelScope.launch {
                    repository.markCompleted(event.item.id, false)
                    syncManager.trigger()
                }
            }

            is TasksEvent.Delete -> {
                viewModelScope.launch {
                    repository.softDeleteItem(event.id)
                    syncManager.trigger()
                }
            }

            is TasksEvent.StartEdit ->
                _state.update { it.copy(editingItem = event.item, isEditDialogOpen = true) }

            is TasksEvent.CloseEdit ->
                _state.update { it.copy(editingItem = null, isEditDialogOpen = false) }

            is TasksEvent.SaveEdit -> {
                viewModelScope.launch {
                    updateItem(event.item)
                    _state.update { it.copy(
                        editingItem = null,
                        isEditDialogOpen = false,
                        toastMessage = "Оновлено ✓",
                    )}
                    syncManager.trigger()
                }
            }

            is TasksEvent.SaveNew -> {
                val s = _state.value
                if (s.inputTitle.isBlank()) {
                    _state.update { it.copy(toastMessage = "Введіть назву завдання") }
                    return
                }
                val seqList = if (s.inputSequence.isBlank()) emptyList()
                else s.inputSequence.split(",").map { it.trim() }.filter { it.isNotEmpty() }

                val meta = TaskMeta(
                    deadlineMs    = s.inputDeadlineMs,
                    recurrence    = s.inputRecurrence,
                    sequence      = seqList,
                    seqIndex      = 0,
                    strictSequence = s.inputStrictSeq,
                    note           = s.inputNote,
                )
                val valueText = if (seqList.isEmpty()) s.inputTitle
                else "${s.inputTitle}: [${seqList.joinToString(", ")}]"

                viewModelScope.launch {
                    saveItem(
                        value = valueText,
                        title = s.inputTitle,
                        description = meta.toDescriptionString(),
                        categoryOverride = "task",
                        subcategory = "",
                        priority = s.inputPriority,
                    )
                    _state.update { it.copy(
                        isAddDialogOpen = false,
                        inputTitle = "", inputNote = "", inputSequence = "",
                        inputDeadlineMs = 0L, inputRecurrence = "once",
                        inputPriority = Priority.MEDIUM, inputStrictSeq = false,
                        toastMessage = "Завдання додано ✓",
                    )}
                    syncManager.trigger()
                }
            }

            is TasksEvent.ClearToast     -> _state.update { it.copy(toastMessage = null) }
            is TasksEvent.TitleChanged   -> _state.update { it.copy(inputTitle = event.v) }
            is TasksEvent.NoteChanged    -> _state.update { it.copy(inputNote = event.v) }
            is TasksEvent.PriorityChanged-> _state.update { it.copy(inputPriority = event.p) }
            is TasksEvent.DeadlineChanged-> _state.update { it.copy(inputDeadlineMs = event.ms) }
            is TasksEvent.RecurrenceChanged -> _state.update { it.copy(inputRecurrence = event.r) }
            is TasksEvent.SequenceChanged -> _state.update { it.copy(inputSequence = event.s) }
            is TasksEvent.StrictSeqChanged -> _state.update { it.copy(inputStrictSeq = event.v) }
        }
    }
}