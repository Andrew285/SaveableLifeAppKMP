package org.simpleapps.saveablekmp.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
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
import org.simpleapps.saveablekmp.sync.DriveSync
import org.simpleapps.saveablekmp.sync.GoogleAuthManager
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
    val isLoadingMore: Boolean = false,
    val hasMoreItems: Boolean = true,
    val categories: List<Category> = emptyList(),
    val filterCategory: String = "",
    val filterTime: TimeFilter = TimeFilter.ALL,
    val editingItem: SavedItem? = null,
    val isEditDialogOpen: Boolean = false,
    val toastMessage: String? = null,
    val pendingImageBytes: ByteArray? = null,
    val isSyncing: Boolean = false,
    val lastSyncTime: Long? = null,
    val scrollToTop: Boolean = false,
    val newItemsFromSync: Int = 0,
) {
    val filteredItems: List<SavedItem>
        get() {
            val now = System.currentTimeMillis()
            val timeFiltered = when (filterTime) {
                TimeFilter.ALL -> items
                TimeFilter.TODAY -> {
                    val startOfDay = java.util.Calendar.getInstance().apply {
                        set(java.util.Calendar.HOUR_OF_DAY, 0)
                        set(java.util.Calendar.MINUTE, 0)
                        set(java.util.Calendar.SECOND, 0)
                        set(java.util.Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    items.filter { it.createdAt >= startOfDay }
                }
                TimeFilter.YESTERDAY -> {
                    val cal = java.util.Calendar.getInstance()
                    cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
                    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    cal.set(java.util.Calendar.MINUTE, 0)
                    cal.set(java.util.Calendar.SECOND, 0)
                    val start = cal.timeInMillis
                    val end = start + 24 * 60 * 60 * 1000
                    items.filter { it.createdAt in start until end }
                }
                TimeFilter.WEEK -> items.filter { it.createdAt >= now - 7L * 24 * 60 * 60 * 1000 }
                TimeFilter.MONTH -> items.filter { it.createdAt >= now - 30L * 24 * 60 * 60 * 1000 }
            }
            return timeFiltered.filter { item ->
                filterCategory.isEmpty() ||
                        item.category == filterCategory ||
                        item.subcategory == filterCategory
            }
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
    object ScrollHandled : MainEvent
    object ScrollToTopAndClearBanner : MainEvent
}

class MainViewModel(
    private val repository: SaveableRepository,
    private val saveItem: SaveItemUseCase,
    private val deleteItem: DeleteItemUseCase,
    private val updateItem: UpdateItemUseCase,
    private val authManager: GoogleAuthManager,
    private val driveSync: DriveSync,
) : ViewModel() {

    private val _state = MutableStateFlow(MainState())
    val state: StateFlow<MainState> = _state.asStateFlow()

    private val PAGE_SIZE = 30
    private var currentOffset = 0L
    private var isLoadingMore = false
    private var hasMoreItems = true

    // Channel для debounce синхронізації
    private val syncTrigger = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST,
    )

    init {
        viewModelScope.launch { repository.initDefaultCategories() }
        viewModelScope.launch { repository.cleanupOldItems() }

        // Тільки категорії через Flow — НЕ items
        viewModelScope.launch {
            repository.observeCategories().collect { cats ->
                _state.update { it.copy(categories = cats) }
            }
        }

        // Items через пагінацію
        viewModelScope.launch { loadFirstPage() }

        viewModelScope.launch { initSync() }
        viewModelScope.launch {
            syncTrigger.debounce(3000).collect { pushToDrive() }
        }
        viewModelScope.launch {
            delay(35_000)
            while (true) {
                if (authManager.isSignedIn()) pullFromDrive()
                delay(30_000)
            }
        }
    }
    private suspend fun loadFirstPage() {
        currentOffset = 0L
        hasMoreItems = true
        val items = repository.getItemsPaged(PAGE_SIZE.toLong(), 0L)
        val total = repository.getActiveItemsCount()
        hasMoreItems = items.size >= PAGE_SIZE && currentOffset + PAGE_SIZE < total
        currentOffset = items.size.toLong()
        _state.update { it.copy(
            items = items,
            hasMoreItems = hasMoreItems,
        )}
    }

    fun loadNextPage() {
        if (isLoadingMore || !_state.value.hasMoreItems) return
        isLoadingMore = true
        viewModelScope.launch {
            val newItems = repository.getItemsPaged(PAGE_SIZE.toLong(), currentOffset)
            val total = repository.getActiveItemsCount()
            currentOffset += newItems.size
            hasMoreItems = currentOffset < total

            // Фільтруємо дублікати
            val existingIds = _state.value.items.map { it.id }.toSet()
            val uniqueNewItems = newItems.filter { it.id !in existingIds }

            _state.update { it.copy(
                items = _state.value.items + uniqueNewItems,
                hasMoreItems = hasMoreItems,
                isLoadingMore = false,
            )}
            isLoadingMore = false
        }
    }

    private suspend fun initSync() {
        println("=== Android initSync start")
        println("=== isSignedIn: ${authManager.isSignedIn()}")
        val saved = authManager.getSavedToken()
        println("=== savedToken: ${saved?.take(10)}")
        if (!authManager.isSignedIn()) {
            println("=== initSync: not signed in, skip")
            return
        }

        val savedToken = authManager.getSavedToken()
        println("=== getSavedToken: $savedToken")

        val token = when {
            // Реальний токен є в пам'яті
            savedToken != null && savedToken != "__needs_refresh__" -> {
                println("=== using saved token")
                savedToken
            }
            // StoredCredential є але потрібно відновити через signIn()
            savedToken == "__needs_refresh__" || savedToken == null -> {
                println("=== refreshing token via signIn()")
                try {
                    val newToken = authManager.signIn()
                    println("=== signIn result: ${newToken?.take(10)}")
                    newToken
                } catch (e: Exception) {
                    println("=== signIn error: ${e.message}")
                    null
                }
            }
            else -> null
        }

        if (token == null || token == "__needs_refresh__") {
            println("=== initSync: could not get valid token, skip")
            return
        }

        driveSync.setToken(token)
        println("=== token set successfully: ${token.take(10)}...")

        _state.update { it.copy(isSyncing = true) }
        pullFromDrive()
        pushToDrive()
        _state.update { it.copy(
            isSyncing = false,
            lastSyncTime = System.currentTimeMillis(),
        )}
        println("=== initSync done")
    }

    private suspend fun pullFromDrive() {
        try {
            val remoteItems = driveSync.pullItems()
            processRemoteItems(remoteItems)
            loadFirstPage()
        } catch (e: Exception) {
            val msg = e.message ?: ""
            if (msg.contains("401") || msg.contains("UNAUTHENTICATED") || msg.contains("No 'id'")) {
                println("=== token expired, refreshing...")
                try {
                    val newToken = authManager.signIn()
                    if (newToken != null) {
                        driveSync.setToken(newToken)
                        println("=== token refreshed, retrying pull")
                        val remoteItems = driveSync.pullItems()
                        processRemoteItems(remoteItems)
                        loadFirstPage()
                    }
                } catch (refreshError: Exception) {
                    println("=== token refresh failed: ${refreshError.message}")
                }
            } else {
                println("=== Pull failed: ${e.message}")
            }
        }
    }

    private suspend fun processRemoteItems(remoteItems: List<SavedItem>) {
        if (remoteItems.isEmpty()) return
        val localItemsMap = repository.getAllItemsIncludingDeleted().associateBy { it.id }
        var newCount = 0
        var updatedCount = 0

        remoteItems.forEach { remoteItem ->
            val localItem = localItemsMap[remoteItem.id]
            when {
                remoteItem.isDeleted -> repository.deleteItem(remoteItem.id)
                localItem == null -> {
                    // Перевіряємо чи елемент не був видалений через cleanup
                    // (якщо його немає локально але він synced в Drive — вставляємо)
                    repository.insertItem(remoteItem.copy(isSynced = true))
                    newCount++
                }
                !localItem.isSynced -> { /* локальні зміни */ }
                remoteItem.updatedAt > localItem.updatedAt -> {
                    repository.insertItem(remoteItem.copy(isSynced = true))
                    updatedCount++
                }
                else -> { /* skip */ }
            }
        }

        if (newCount > 0) _state.update { it.copy(newItemsFromSync = newCount) }
        println("=== Pulled ${remoteItems.size}, $newCount new, $updatedCount updated")
    }

    // В MainViewModel додай:
    private val pushMutex = kotlinx.coroutines.sync.Mutex()

    private suspend fun pushToDrive() {
        if (pushMutex.isLocked) {
            println("=== pushToDrive: already running, skip")
            return
        }
        pushMutex.withLock {
            try {
                val unsynced = repository.getUnsyncedItems()
                if (unsynced.isEmpty()) return
                println("=== pushToDrive: ${unsynced.size} items")
                driveSync.pushItems()
                println("=== Pushed ${unsynced.size} items")
            } catch (e: Exception) {
                val msg = e.message ?: ""
                if (msg.contains("401") || msg.contains("UNAUTHENTICATED")) {
                    try {
                        val newToken = authManager.signIn()
                        if (newToken != null) {
                            driveSync.setToken(newToken)
                            driveSync.pushItems()
                        }
                    } catch (refreshError: Exception) {
                        println("=== push refresh failed: ${refreshError.message}")
                    }
                } else {
                    println("=== Push failed: ${e.message}")
                }
            }
        }
    }

    // Тригерить debounce sync після будь-якої зміни
    private fun triggerSync() {
        println("=== triggerSync called, isSignedIn: ${authManager.isSignedIn()}")
        if (authManager.isSignedIn()) {
            syncTrigger.tryEmit(Unit)
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
                            scrollToTop = true
                        )
                    }
                    loadFirstPage()
                    triggerSync()
                }
            }
            is MainEvent.Delete -> {
                viewModelScope.launch {
                    repository.softDeleteItem(event.id)
                    _state.update { s ->
                        s.copy(
                            items = s.items.filter { it.id != event.id },
                            toastMessage = "Видалено",
                        )
                    }
                    // debounce замість прямого push — збирає кілька видалень в одне
                    triggerSync()
                }
            }
            is MainEvent.Copy -> {
                Clipboard.copy(event.item.value)
                _state.update { it.copy(toastMessage = "Скопійовано ✓") }
            }
            is MainEvent.StartEdit -> _state.update { it.copy(editingItem = event.item, isEditDialogOpen = true) }
            is MainEvent.CloseEdit -> _state.update { it.copy(editingItem = null, isEditDialogOpen = false) }
            is MainEvent.SaveEdit -> {
                viewModelScope.launch {
                    updateItem(event.item)
                    // Оновлюємо елемент в списку одразу
                    _state.update { s ->
                        s.copy(
                            items = s.items.map { if (it.id == event.item.id) event.item else it },
                            editingItem = null,
                            isEditDialogOpen = false,
                            toastMessage = "Оновлено ✓",
                        )
                    }
                    // Пушимо одразу
                    pushToDrive()
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

            is MainEvent.ScrollHandled -> _state.update { it.copy(scrollToTop = false) }

            is MainEvent.ScrollToTopAndClearBanner -> {
                _state.update { it.copy(newItemsFromSync = 0, scrollToTop = true) }
            }
        }
    }
}