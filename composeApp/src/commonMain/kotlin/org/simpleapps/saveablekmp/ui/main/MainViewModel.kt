package org.simpleapps.saveablekmp.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.simpleapps.saveablekmp.Clipboard
import org.simpleapps.saveablekmp.ImagePicker
import org.simpleapps.saveablekmp.data.model.Category
import org.simpleapps.saveablekmp.data.model.Priority
import org.simpleapps.saveablekmp.data.model.SavedItem
import org.simpleapps.saveablekmp.data.model.TimeFilter
import org.simpleapps.saveablekmp.data.repository.SaveableRepository
import org.simpleapps.saveablekmp.domain.detector.CategoryDetector
import org.simpleapps.saveablekmp.domain.usecase.DeleteItemUseCase
import org.simpleapps.saveablekmp.domain.usecase.SaveItemUseCase
import org.simpleapps.saveablekmp.domain.usecase.UpdateItemUseCase
import org.simpleapps.saveablekmp.toBase64DataUrl

data class MainState(
    val inputValue: String = "",
    val inputTitle: String = "",
    val inputDescription: String = "",
    val inputCategory: String = "text",
    val inputSubcategory: String = "",
    val inputPriority: Priority = Priority.MEDIUM,
    val isExpanded: Boolean = false,
    val items: List<SavedItem> = emptyList(),
    val categories: List<Category> = emptyList(),
    val filterCategory: String = "",
    val filterTime: TimeFilter = TimeFilter.ALL,
    val editingItem: SavedItem? = null,
    val isEditDialogOpen: Boolean = false,
    val toastMessage: String? = null,
    val pendingImageBytes: ByteArray? = null,
) {
    val filteredItems: List<SavedItem>
        get() = items.filter { item ->
            val catMatch = filterCategory.isEmpty() ||
                    item.category == filterCategory ||
                    item.subcategory == filterCategory
            catMatch
        }

    val totalCount get() = items.size
    val detectedCategory get() = CategoryDetector.detect(inputValue)
}

sealed interface MainEvent {
    data class InputChanged(val value: String) : MainEvent
    data class TitleChanged(val value: String) : MainEvent
    data class DescChanged(val value: String) : MainEvent
    data class CategoryChanged(val id: String) : MainEvent
    data class SubcategoryChanged(val id: String) : MainEvent
    data class PriorityChanged(val p: Priority) : MainEvent
    object ToggleExpand : MainEvent
    object Save : MainEvent
    data class Delete(val id: String) : MainEvent
    data class Copy(val item: SavedItem) : MainEvent
    data class StartEdit(val item: SavedItem) : MainEvent
    data class SaveEdit(val item: SavedItem) : MainEvent
    object CloseEdit : MainEvent
    data class FilterCategory(val id: String) : MainEvent
    data class FilterTime(val f: TimeFilter) : MainEvent
    object ClearToast : MainEvent
    object PasteImage : MainEvent
    object PickImage : MainEvent
    object ClearPendingImage : MainEvent
}

class MainViewModel(
    private val repository: SaveableRepository,
    private val saveItem: SaveItemUseCase,
    private val deleteItem: DeleteItemUseCase,
    private val updateItem: UpdateItemUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(MainState())
    val state: StateFlow<MainState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initDefaultCategories()
        }
        viewModelScope.launch {
            combine(
                repository.observeAllItems(),
                repository.observeCategories(),
            ) { items, cats -> Pair(items, cats) }
                .collect { (items, cats) ->
                    _state.update { it.copy(items = items, categories = cats) }
                }
        }
    }

    fun onEvent(event: MainEvent) {
        when (event) {
            is MainEvent.InputChanged -> {
                _state.update { s ->
                    s.copy(
                        inputValue = event.value,
                        inputCategory = if (s.isExpanded) CategoryDetector.detect(event.value) else s.inputCategory,
                    )
                }
            }
            is MainEvent.TitleChanged      -> _state.update { it.copy(inputTitle = event.value) }
            is MainEvent.DescChanged       -> _state.update { it.copy(inputDescription = event.value) }
            is MainEvent.CategoryChanged   -> _state.update { it.copy(inputCategory = event.id) }
            is MainEvent.SubcategoryChanged-> _state.update { it.copy(inputSubcategory = event.id) }
            is MainEvent.PriorityChanged   -> _state.update { it.copy(inputPriority = event.p) }
            is MainEvent.ToggleExpand      -> _state.update { s ->
                s.copy(
                    isExpanded = !s.isExpanded,
                    inputCategory = if (!s.isExpanded) CategoryDetector.detect(s.inputValue) else s.inputCategory,
                )
            }
            is MainEvent.Save -> {
                val s = _state.value

                // Якщо є зображення — підставляємо реальний Base64
                val finalValue = if (s.pendingImageBytes != null) {
                    s.pendingImageBytes.toBase64DataUrl()
                } else {
                    s.inputValue
                }

                if (finalValue.isBlank() || finalValue == "data:image/jpeg;base64,__pending__") {
                    _state.update { it.copy(toastMessage = "Введіть або вставте дані") }
                    return
                }

                viewModelScope.launch {
                    saveItem(
                        value = finalValue,
                        title = s.inputTitle,
                        description = s.inputDescription,
                        categoryOverride = if (s.isExpanded) s.inputCategory else null,
                        subcategory = s.inputSubcategory,
                        priority = s.inputPriority,
                    )
                    _state.update {
                        it.copy(
                            inputValue = "",
                            inputTitle = "",
                            inputDescription = "",
                            inputSubcategory = "",
                            inputPriority = Priority.MEDIUM,
                            isExpanded = false,
                            pendingImageBytes = null, // ← очищаємо
                            toastMessage = "Збережено ✓",
                        )
                    }
                }
            }
            is MainEvent.Delete -> {
                viewModelScope.launch {
                    deleteItem(event.id)
                    _state.update { it.copy(toastMessage = "Видалено") }
                }
            }
            is MainEvent.Copy -> {
                Clipboard.copy(event.item.value)
                _state.update { it.copy(toastMessage = "Скопійовано ✓") }
            }
            is MainEvent.StartEdit -> _state.update { it.copy(editingItem = event.item, isEditDialogOpen = true) }
            is MainEvent.CloseEdit -> _state.update { it.copy(editingItem = null, isEditDialogOpen = false) }
            is MainEvent.SaveEdit  -> {
                viewModelScope.launch {
                    updateItem(event.item)
                    _state.update { it.copy(editingItem = null, isEditDialogOpen = false, toastMessage = "Оновлено ✓") }
                }
            }
            is MainEvent.FilterCategory -> _state.update { it.copy(filterCategory = event.id) }
            is MainEvent.FilterTime     -> _state.update { it.copy(filterTime = event.f) }
            is MainEvent.ClearToast     -> _state.update { it.copy(toastMessage = null) }
            is MainEvent.PasteImage -> {
                ImagePicker.pasteFromClipboard { bytes ->
                    if (bytes != null) {
                        _state.update { s ->
                            s.copy(
                                pendingImageBytes = bytes,
                                inputCategory = "image",
                                inputValue = "data:image/jpeg;base64,__pending__", // маркер
                            )
                        }
                    } else {
                        _state.update { it.copy(toastMessage = "Немає зображення в буфері") }
                    }
                }
            }
            is MainEvent.PickImage -> {
                ImagePicker.pickFromGallery { bytes ->
                    if (bytes != null) {
                        _state.update { s ->
                            s.copy(
                                pendingImageBytes = bytes,
                                inputCategory = "image",
                                inputValue = "data:image/jpeg;base64,__pending__",
                            )
                        }
                    }
                }
            }
            is MainEvent.ClearPendingImage -> _state.update {
                it.copy(pendingImageBytes = null, inputValue = "", inputCategory = "text")
            }
        }
    }
}