package org.simpleapps.saveablekmp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.simpleapps.saveablekmp.data.model.Category
import org.simpleapps.saveablekmp.data.model.generateId
import org.simpleapps.saveablekmp.data.repository.SaveableRepository

data class SettingsState(
    val categories: List<Category> = emptyList(),
    val newCatName: String = "",
    val newCatParentId: String = "",
    val newCatColor: String = "#4ade80",
    val toastMessage: String? = null,
)

sealed interface SettingsEvent {
    data class NewCatNameChanged(val value: String) : SettingsEvent
    data class NewCatParentChanged(val id: String) : SettingsEvent
    data class NewCatColorChanged(val color: String) : SettingsEvent
    object AddCategory : SettingsEvent
    data class DeleteCategory(val id: String) : SettingsEvent
    object ClearAll : SettingsEvent
    object ClearToast : SettingsEvent
}

class SettingsViewModel(
    private val repository: SaveableRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeCategories().collect { cats ->
                _state.update { it.copy(categories = cats) }
            }
        }
    }

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.NewCatNameChanged    -> _state.update { it.copy(newCatName = event.value) }
            is SettingsEvent.NewCatParentChanged  -> _state.update { it.copy(newCatParentId = event.id) }
            is SettingsEvent.NewCatColorChanged   -> _state.update { it.copy(newCatColor = event.color) }

            is SettingsEvent.AddCategory -> {
                val s = _state.value
                if (s.newCatName.isBlank()) {
                    _state.update { it.copy(toastMessage = "Введіть назву категорії") }
                    return
                }
                viewModelScope.launch {
                    val cat = Category(
                        id = generateId(),
                        name = s.newCatName.trim(),
                        color = s.newCatColor,
                        parentId = s.newCatParentId.ifBlank { null },
                        isBuiltin = false,
                    )
                    repository.insertCategory(cat)
                    _state.update { it.copy(
                        newCatName = "",
                        newCatParentId = "",
                        toastMessage = "Категорію додано ✓",
                    )}
                }
            }

            is SettingsEvent.DeleteCategory -> {
                viewModelScope.launch {
                    repository.deleteCategory(event.id)
                    _state.update { it.copy(toastMessage = "Видалено") }
                }
            }

            is SettingsEvent.ClearAll -> {
                viewModelScope.launch {
                    repository.deleteAllItems()
                    _state.update { it.copy(toastMessage = "Всі дані видалено") }
                }
            }

            is SettingsEvent.ClearToast -> _state.update { it.copy(toastMessage = null) }
        }
    }
}