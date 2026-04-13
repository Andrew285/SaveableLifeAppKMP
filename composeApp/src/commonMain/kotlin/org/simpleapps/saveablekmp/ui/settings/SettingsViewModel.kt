package org.simpleapps.saveablekmp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.simpleapps.saveablekmp.data.model.Category
import org.simpleapps.saveablekmp.data.model.generateId
import org.simpleapps.saveablekmp.data.repository.SaveableRepository
import org.simpleapps.saveablekmp.sync.DriveSync
import org.simpleapps.saveablekmp.sync.GoogleAuthManager

data class SettingsState(
    val categories: List<Category> = emptyList(),
    val newCatName: String = "",
    val newCatParentId: String = "",
    val newCatColor: String = "#4ade80",
    val toastMessage: String? = null,
    val isSyncing: Boolean = false,
    val isSignedIn: Boolean = false,
    val syncStatusMessage: String? = null,
)

sealed interface SettingsEvent {
    data class NewCatNameChanged(val value: String) : SettingsEvent
    data class NewCatParentChanged(val id: String) : SettingsEvent
    data class NewCatColorChanged(val color: String) : SettingsEvent
    object AddCategory : SettingsEvent
    data class DeleteCategory(val id: String) : SettingsEvent
    object ClearAll : SettingsEvent
    object ClearToast : SettingsEvent
    object SignInGoogle : SettingsEvent
    object SyncNow : SettingsEvent
    object PullFromDrive : SettingsEvent
    object SignOut : SettingsEvent
}

class SettingsViewModel(
    private val repository: SaveableRepository,
    private val driveSync: DriveSync,
    private val authManager: GoogleAuthManager,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeCategories().collect { cats ->
                _state.update { it.copy(categories = cats) }
            }
        }
        viewModelScope.launch {
            val isSignedIn = authManager.isSignedIn()
            _state.update { it.copy(isSignedIn = isSignedIn) }
            if (isSignedIn) {
                val savedToken = authManager.getSavedToken()
                val token = when {
                    savedToken != null && savedToken != "__needs_refresh__" -> savedToken
                    else -> try { authManager.signIn() } catch (e: Exception) { null }
                }
                if (token != null && token != "__needs_refresh__") {
                    driveSync.setToken(token)
                    println("=== SettingsVM token set: ${token.take(10)}...")
                }
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

            is SettingsEvent.SignInGoogle -> {
                viewModelScope.launch {
                    _state.update { it.copy(isSyncing = true) }
                    try {
                        val token = authManager.signIn()
                        println("=== SignIn token: $token")  // ← додай
                        if (token != null) {
                            driveSync.setToken(token)
                            println("=== DriveSync token set: $token")  // ← додай
                            _state.update { it.copy(
                                isSignedIn = true,
                                isSyncing = false,
                                toastMessage = "Успішно увійшли ✓",
                            )}
                        } else {
                            _state.update { it.copy(
                                isSyncing = false,
                                toastMessage = "Токен не отримано — спробуй ще раз",
                            )}
                        }
                    } catch (e: Exception) {
                        println("=== SignIn ERROR: ${e.stackTraceToString()}")
                        _state.update { it.copy(
                            isSyncing = false,
                            toastMessage = "Помилка входу: ${e.message}",
                        )}
                    }
                }
            }

            is SettingsEvent.SyncNow -> {
                viewModelScope.launch {
                    if (!_state.value.isSignedIn) {
                        _state.update { it.copy(toastMessage = "Спочатку увійдіть через Google") }
                        return@launch
                    }
                    _state.update { it.copy(isSyncing = true, toastMessage = "Синхронізація...") }
                    try {
                        driveSync.pushItems()
                        _state.update { it.copy(
                            isSyncing = false,
                            toastMessage = "Дані завантажено в Drive ✓",
                        )}
                    } catch (e: Exception) {
                        _state.update { it.copy(
                            isSyncing = false,
                            toastMessage = "Помилка синхронізації: ${e.message}",
                        )}
                    }
                }
            }

            is SettingsEvent.PullFromDrive -> {
                viewModelScope.launch {
                    if (!_state.value.isSignedIn) {
                        _state.update { it.copy(toastMessage = "Спочатку увійдіть через Google") }
                        return@launch
                    }
                    _state.update { it.copy(isSyncing = true, toastMessage = "Отримання даних...") }
                    try {
                        val remoteItems = driveSync.pullItems()
                        remoteItems.forEach { item -> repository.insertItem(item) }
                        _state.update { it.copy(
                            isSyncing = false,
                            toastMessage = "Отримано ${remoteItems.size} елементів ✓",
                        )}
                    } catch (e: Exception) {
                        // Детальне логування
                        println("=== PullFromDrive ERROR ===")
                        println("Type: ${e::class.simpleName}")
                        println("Message: ${e.message}")
                        println("Cause: ${e.cause}")
                        println("Stack: ${e.stackTraceToString()}")
                        println("==========================")
                        _state.update { it.copy(
                            isSyncing = false,
                            toastMessage = "Помилка: ${e::class.simpleName}: ${e.message ?: e.cause?.message ?: "невідома"}",
                        )}
                    }
                }
            }

            is SettingsEvent.SignOut -> {
                viewModelScope.launch {
                    authManager.signOut()
                    _state.update { it.copy(
                        isSignedIn = false,
                        toastMessage = "Вийшли з Google ✓",
                    )}
                }
            }
        }
    }
}