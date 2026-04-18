package org.simpleapps.saveablekmp.ui.main

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.simpleapps.saveablekmp.UrlOpener
import org.simpleapps.saveablekmp.data.model.Category
import org.simpleapps.saveablekmp.data.model.Priority
import org.simpleapps.saveablekmp.data.model.SavedItem
import org.simpleapps.saveablekmp.data.model.TimeFilter
import org.simpleapps.saveablekmp.toBase64DataUrl
import org.simpleapps.saveablekmp.ui.theme.*

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateSettings: () -> Unit,
    onNavigateFlashcards: () -> Unit,
    onNavigateTasks: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    // Детектор доскролювання
    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            lastVisible >= total - 5 // починаємо завантажувати коли 5 елементів до кінця
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value && state.hasMoreItems && !state.isLoadingMore) {
            viewModel.loadNextPage()
        }
    }

    LaunchedEffect(state.scrollToTop) {
        if (state.scrollToTop) {
            listState.animateScrollToItem(0)
            viewModel.onEvent(MainEvent.ScrollHandled)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(AppColors.Bg)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ───────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 0.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Сховище даних", style = AppTypography.titleLarge)
                    Spacer(Modifier.height(3.dp))
                    // Показуємо статус синхронізації під заголовком
                    when {
                        state.isSyncing -> Text(
                            "↻ Синхронізація...",
                            style = AppTypography.caption.copy(color = AppColors.Green),
                        )
                        state.lastSyncTime != null -> Text(
                            "✓ ${formatTimestamp(state.lastSyncTime!!)}",
                            style = AppTypography.caption.copy(color = AppColors.Text3),
                        )
                        else -> Text(
                            "Зберігайте будь-яку інформацію в одному місці",
                            style = AppTypography.subtitle,
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GhostButton("✅ Завдання", onClick = onNavigateTasks)
                    GhostButton("🃏", onClick = onNavigateFlashcards)
                    GhostButton("⚙ Налаштування", onClick = onNavigateSettings)
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Input area ───────────────────────────────────────────────
            InputArea(
                state = state,
                onEvent = viewModel::onEvent,
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            Spacer(Modifier.height(14.dp))

            // ── Filters ──────────────────────────────────────────────────
            FiltersRow(
                categories = state.categories,
                selectedCategory = state.filterCategory,
                selectedTime = state.filterTime,
                onCategoryChange = { viewModel.onEvent(MainEvent.FilterCategory(it)) },
                onTimeChange = { viewModel.onEvent(MainEvent.FilterTime(it)) },
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            Spacer(Modifier.height(10.dp))

            if (state.newItemsFromSync > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .clip(RoundedMedium)
                        .background(AppColors.GreenDim)
                        .border(1.dp, AppColors.Green.copy(alpha = 0.3f), RoundedMedium)
                        .clickable { viewModel.onEvent(MainEvent.ScrollToTopAndClearBanner) }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "↑ ${state.newItemsFromSync} нових елементів",
                            style = AppTypography.bodySmall.copy(color = AppColors.Green),
                        )
                        Text(
                            "Показати",
                            style = AppTypography.caption.copy(color = AppColors.Green),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // ── Items ────────────────────────────────────────────────────
            if (state.filteredItems.isEmpty()) {
                EmptyState(
                    hasItems = state.items.isNotEmpty(),
                    modifier = Modifier.weight(1f),
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(state.filteredItems, key = { it.id }) { item ->
                        ItemCard(
                            item = item,
                            categories = state.categories,
                            onCopy = { viewModel.onEvent(MainEvent.Copy(item)) },
                            onEdit = { viewModel.onEvent(MainEvent.StartEdit(item)) },
                            onDelete = { viewModel.onEvent(MainEvent.Delete(item.id)) },
                        )
                    }
                    // Індикатор завантаження
                    if (state.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("Завантаження...", style = AppTypography.caption)
                            }
                        }
                    }
                }
            }

            // ── Status bar ───────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = AppColors.Border, shape = RoundedCornerShape(0.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    buildString {
                        append("Всього збережено: ")
                        append(state.totalCount)
                        append(" елементів")
                    },
                    style = AppTypography.caption,
                )
            }
        }

        // ── Edit dialog ──────────────────────────────────────────────────
        if (state.isEditDialogOpen && state.editingItem != null) {
            EditDialog(
                item = state.editingItem!!,
                categories = state.categories,
                onDismiss = { viewModel.onEvent(MainEvent.CloseEdit) },
                onSave = { viewModel.onEvent(MainEvent.SaveEdit(it)) },
            )
        }

        // ── Toast ────────────────────────────────────────────────────────
        state.toastMessage?.let { msg ->
            LaunchedEffect(msg) {
                kotlinx.coroutines.delay(2500)
                viewModel.onEvent(MainEvent.ClearToast)
            }
            Box(
                modifier = Modifier.fillMaxSize().padding(bottom = 32.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedMedium)
                        .background(AppColors.Bg3)
                        .border(1.dp, AppColors.Border2, RoundedMedium)
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text(msg, style = AppTypography.bodySmall.copy(color = AppColors.Text))
                }
            }
        }
    }
}

// ── Input area ────────────────────────────────────────────────────────────────

@Composable
private fun InputArea(
    state: MainState,
    onEvent: (MainEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedLarge)
            .background(AppColors.Bg2)
            .border(
                width = 1.dp,
                color = if (state.inputValue.isNotEmpty()) AppColors.Green else AppColors.Border,
                shape = RoundedLarge,
            )
    ) {
        if (state.pendingImageBytes != null) {
            // Превью зображення
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Base64Image(
                        dataUrl = state.pendingImageBytes.toBase64DataUrl(),
                        modifier = Modifier
                            .height(80.dp)
                            .widthIn(max = 120.dp)
                            .clip(RoundedCornerShape(6.dp)),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Зображення готове до збереження",
                            style = AppTypography.bodySmall.copy(color = AppColors.Green),
                        )
                        Text(
                            "${(state.pendingImageBytes.size / 1024)} КБ",
                            style = AppTypography.caption,
                        )
                        GhostButton("✕ Видалити", onClick = {
                            onEvent(MainEvent.InputChanged(""))
                            // очищаємо pendingImage через окремий івент
                            onEvent(MainEvent.ClearPendingImage)
                        })
                    }
                }
            }
        } else {
            BasicTextField(
                value = state.inputValue,
                onValueChange = { onEvent(MainEvent.InputChanged(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 80.dp)
                    .padding(14.dp)
                    .onKeyEvent { event ->
                        if (event.key == Key.Enter && !event.isShiftPressed &&
                            event.type == KeyEventType.KeyDown && !state.isExpanded
                        ) {
                            onEvent(MainEvent.Save)
                            true
                        } else false
                    },
                textStyle = AppTypography.body,
                cursorBrush = SolidColor(AppColors.Green),
                decorationBox = { inner ->
                    if (state.inputValue.isEmpty()) {
                        Text(
                            "Вставте або введіть будь-яку інформацію (Ctrl+V або Paste)...",
                            style = AppTypography.body.copy(color = AppColors.Text3),
                        )
                    }
                    inner()
                }
            )
        }

        // Expanded fields
        AnimatedVisibility(visible = state.isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = AppColors.Border, shape = RoundedCornerShape(0.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (state.inputCategory == "flashcard") {
                    // Спеціальні поля для флешкартки
                    LabeledField("ПЕРЕДНЯ СТОРОНА") {
                        AppTextField(
                            value = state.inputValue,
                            onValueChange = { onEvent(MainEvent.InputChanged(it)) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = "Питання або термін...",
                        )
                    }
                    LabeledField("ЗАДНЯ СТОРОНА (опис)") {
                        AppTextField(
                            value = state.inputDescription,
                            onValueChange = { onEvent(MainEvent.DescChanged(it)) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = "Відповідь або пояснення...",
                        )
                    }
                    LabeledField("КОЛОДА (обов'язково)") {
                        val flashcardSubs = state.categories.filter { it.parentId == "flashcard" }
                        CategoryDropdown(
                            categories = flashcardSubs,
                            selected = state.inputSubcategory,
                            onChange = { onEvent(MainEvent.SubcategoryChanged(it)) },
                            emptyLabel = "Оберіть колоду...",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LabeledField("Назва", Modifier.weight(1f)) {
                            AppTextField(
                                value = state.inputTitle,
                                onValueChange = { onEvent(MainEvent.TitleChanged(it)) },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = "Назва запису...",
                            )
                        }
                        LabeledField("Пріоритет", Modifier.weight(1f)) {
                            PriorityDropdown(
                                selected = state.inputPriority,
                                onChange = { onEvent(MainEvent.PriorityChanged(it)) },
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LabeledField("Категорія", Modifier.weight(1f)) {
                            CategoryDropdown(
                                categories = state.categories.filter { it.parentId == null },
                                selected = state.inputCategory,
                                onChange = { onEvent(MainEvent.CategoryChanged(it)) },
                            )
                        }
                        LabeledField("Підкатегорія", Modifier.weight(1f)) {
                            val subs = state.categories.filter { it.parentId == state.inputCategory }
                            CategoryDropdown(
                                categories = subs,
                                selected = state.inputSubcategory,
                                onChange = { onEvent(MainEvent.SubcategoryChanged(it)) },
                                emptyLabel = "— немає —",
                            )
                        }
                    }
                    LabeledField("Опис") {
                        AppTextField(
                            value = state.inputDescription,
                            onValueChange = { onEvent(MainEvent.DescChanged(it)) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = "Додатковий опис...",
                        )
                    }
                }
            }
        }

        // Bottom row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostButton(
                    text = if (state.isExpanded) "▲ Поля" else "▼ Поля",
                    onClick = { onEvent(MainEvent.ToggleExpand) },
                )
                GhostButton(
                    text = "🖼",
                    onClick = { onEvent(MainEvent.PickImage) },
                )
                GhostButton(
                    text = "📋",
                    onClick = { onEvent(MainEvent.PasteImage) },
                )
            }
            AppButton(text = "+ Зберегти", onClick = { onEvent(MainEvent.Save) })
        }
    }
}

// ── Filters row ───────────────────────────────────────────────────────────────

@Composable
fun FiltersRow(
    categories: List<Category>,
    selectedCategory: String,
    selectedTime: TimeFilter,
    onCategoryChange: (String) -> Unit,
    onTimeChange: (TimeFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("▽ Фільтри:", style = AppTypography.caption)

        CategoryDropdown(
            categories = categories.filter { it.parentId == null },
            selected = selectedCategory,
            onChange = onCategoryChange,
            emptyLabel = "Всі категорії",
            modifier = Modifier.weight(1f),
        )

        // Time filter dropdown
        var expanded by remember { mutableStateOf(false) }
        Box(modifier = Modifier.weight(1f)) {
            GhostButton(
                text = selectedTime.label,
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
            )
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(AppColors.Bg3),
            ) {
                TimeFilter.entries.forEach { filter ->
                    DropdownMenuItem(
                        text = { Text(filter.label, style = AppTypography.bodySmall) },
                        onClick = { onTimeChange(filter); expanded = false },
                    )
                }
            }
        }
    }
}

// ── Item card ─────────────────────────────────────────────────────────────────

@Composable
fun ItemCard(
    item: SavedItem,
    categories: List<Category>,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val cat = categories.find { it.id == item.category }
    val subcat = if (item.subcategory.isNotEmpty()) categories.find { it.id == item.subcategory } else null
    var passwordVisible by remember { mutableStateOf(false) }

    AppCard {
        // Tags row
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            cat?.let { CategoryTag(it.name, parseColor(it.color)) }
            subcat?.let { CategoryTag(it.name, parseColor(it.color)) }
            PriorityTag(item.priority)
            Spacer(Modifier.weight(1f))
            // Actions
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton28(onClick = onCopy) {
                    Text("⎘", style = AppTypography.caption.copy(color = AppColors.Text2))
                }
                // Кнопка ↗
                if (item.category == "link") {
                    IconButton28(onClick = { UrlOpener.open(item.value) }) {
                        Text("↗", style = AppTypography.caption.copy(color = AppColors.Text2))
                    }
                }

//                // Значення посилання — зроби клікабельним
//                if (item.category == "link") {
//                    Text(
//                        text = item.value,
//                        style = AppTypography.mono.copy(
//                            color = AppColors.Green,
//                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
//                        ),
//                        maxLines = 2,
//                        modifier = Modifier.clickable { UrlOpener.open(item.value) },
//                    )
//                }
                if (item.category == "password") {
                    IconButton28(onClick = { passwordVisible = !passwordVisible }) {
                        Text(if (passwordVisible) "○" else "●", style = AppTypography.caption.copy(color = AppColors.Text2))
                    }
                }
                IconButton28(onClick = onEdit) {
                    Text("✎", style = AppTypography.caption.copy(color = AppColors.Text2))
                }
                IconButton28(onClick = onDelete) {
                    Text("✕", style = AppTypography.caption.copy(color = AppColors.PriorityHigh))
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Title
        if (item.title.isNotBlank()) {
            Text(item.title, style = AppTypography.body.copy(fontWeight = FontWeight.Medium))
            Spacer(Modifier.height(4.dp))
        }

        // Value
        if (item.category == "image" && item.value.startsWith("data:image")) {
            Base64Image(
                dataUrl = item.value,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .clip(RoundedCornerShape(6.dp)),
            )
        } else {
            val displayValue = when {
                item.category == "password" && !passwordVisible -> "••••••••••••"
                else -> item.value
            }
            val valueStyle = when (item.category) {
                "password", "link", "phone", "email" ->
                    AppTypography.mono.copy(color = if (item.category == "link") AppColors.Green else AppColors.Text2)
                else -> AppTypography.bodySmall
            }
            Text(displayValue, style = valueStyle, maxLines = if (item.category == "text") 4 else 2)
        }

        // Description
        if (item.description.isNotBlank()) {
            Spacer(Modifier.height(5.dp))
            Text(item.description, style = AppTypography.caption)
        }

        // Date
        Spacer(Modifier.height(8.dp))
        Text(formatTimestamp(item.createdAt), style = AppTypography.caption)
    }
}

// ── Edit dialog ───────────────────────────────────────────────────────────────

@Composable
fun EditDialog(
    item: SavedItem,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (SavedItem) -> Unit,
) {
    var value by remember { mutableStateOf(item.value) }
    var title by remember { mutableStateOf(item.title) }
    var desc by remember { mutableStateOf(item.description) }
    var category by remember { mutableStateOf(item.category) }
    var priority by remember { mutableStateOf(item.priority) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(12.dp))
                .background(AppColors.Bg2)
                .border(1.dp, AppColors.Border2, RoundedCornerShape(12.dp))
                .clickable { /* consume */ }
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Редагувати запис", style = AppTypography.titleMedium)

            LabeledField("Значення") {
                BasicTextField(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 60.dp)
                        .background(AppColors.Bg3, RoundedMedium)
                        .border(1.dp, AppColors.Border, RoundedMedium)
                        .padding(10.dp),
                    textStyle = AppTypography.body,
                    cursorBrush = SolidColor(AppColors.Green),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LabeledField("Назва", Modifier.weight(1f)) {
                    AppTextField(value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth())
                }
                LabeledField("Пріоритет", Modifier.weight(1f)) {
                    PriorityDropdown(selected = priority, onChange = { priority = it })
                }
            }

            LabeledField("Категорія") {
                CategoryDropdown(
                    categories = categories.filter { it.parentId == null },
                    selected = category,
                    onChange = { category = it },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            LabeledField("Опис") {
                AppTextField(value = desc, onValueChange = { desc = it }, modifier = Modifier.fillMaxWidth(), placeholder = "Опис...")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                GhostButton("Скасувати", onDismiss)
                AppButton("Зберегти", onClick = {
                    onSave(item.copy(value = value, title = title, description = desc, category = category, priority = priority))
                })
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
fun LabeledField(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label.uppercase(), style = AppTypography.caption.copy(letterSpacing = 0.05.sp))
        content()
    }
}

@Composable
fun CategoryDropdown(
    categories: List<Category>,
    selected: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    emptyLabel: String = "Оберіть...",
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCat = categories.find { it.id == selected }

    Box(modifier = modifier) {
        GhostButton(
            text = selectedCat?.name ?: emptyLabel,
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(AppColors.Bg3),
        ) {
            if (emptyLabel != "Оберіть...") {
                DropdownMenuItem(
                    text = { Text(emptyLabel, style = AppTypography.bodySmall.copy(color = AppColors.Text3)) },
                    onClick = { onChange(""); expanded = false },
                )
            }
            categories.forEach { cat ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ColorDot(parseColor(cat.color))
                            Text(cat.name, style = AppTypography.bodySmall)
                        }
                    },
                    onClick = { onChange(cat.id); expanded = false },
                )
            }
        }
    }
}

@Composable
fun PriorityDropdown(selected: Priority, onChange: (Priority) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        GhostButton(selected.label, onClick = { expanded = true }, modifier = Modifier.fillMaxWidth())
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(AppColors.Bg3),
        ) {
            Priority.entries.forEach { p ->
                DropdownMenuItem(
                    text = { Text(p.label, style = AppTypography.bodySmall) },
                    onClick = { onChange(p); expanded = false },
                )
            }
        }
    }
}

@Composable
fun EmptyState(hasItems: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("📦", style = AppTypography.titleLarge.copy(fontSize = 40.sp))
        Spacer(Modifier.height(12.dp))
        Text(
            if (hasItems) "Нічого не знайдено" else "Поки що порожньо",
            style = AppTypography.bodySmall.copy(fontWeight = FontWeight.Medium),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (hasItems) "Спробуйте змінити фільтри" else "Вставте або введіть дані вище",
            style = AppTypography.caption,
        )
    }
}

fun formatTimestamp(epochMs: Long): String {
    val instant = Instant.fromEpochMilliseconds(epochMs)
    val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val day = dt.dayOfMonth.toString().padStart(2, '0')
    val month = dt.monthNumber.toString().padStart(2, '0')
    val hour = dt.hour.toString().padStart(2, '0')
    val min = dt.minute.toString().padStart(2, '0')
    return "$day.$month.${dt.year}, $hour:$min"
}

fun parseColor(hex: String): Color {
    return try {
        val clean = hex.removePrefix("#")
        val r = clean.substring(0, 2).toInt(16)
        val g = clean.substring(2, 4).toInt(16)
        val b = clean.substring(4, 6).toInt(16)
        Color(r, g, b)
    } catch (e: Exception) {
        Color(0xFF4ADE80)
    }
}