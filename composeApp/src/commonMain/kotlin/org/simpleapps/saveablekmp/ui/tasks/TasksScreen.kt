package org.simpleapps.saveablekmp.ui.tasks

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.simpleapps.saveablekmp.data.model.*
import org.simpleapps.saveablekmp.ui.main.*
import org.simpleapps.saveablekmp.ui.theme.*

@Composable
fun TasksScreen(
    viewModel: TasksViewModel,
) {
    val state by viewModel.state.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(AppColors.Bg)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ───────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Завдання", style = AppTypography.titleLarge)
                    Text(
                        "Сьогодні: ${state.todayTasks.size} • Всього активних: ${state.allActiveTasks.size}",
                        style = AppTypography.subtitle,
                    )
                }
                AppButton("+ Додати", onClick = { viewModel.onEvent(TasksEvent.OpenAddDialog) })
            }

            Spacer(Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                // ── Сьогодні ─────────────────────────────────────────
                if (state.todayTasks.isNotEmpty()) {
                    item {
                        SectionHeader("📅 На сьогодні", state.todayTasks.size)
                    }
                    items(state.todayTasks, key = { "today_${it.id}" }) { task ->
                        TaskCard(
                            item = task,
                            onComplete = { viewModel.onEvent(TasksEvent.Complete(task)) },
                            onUncomplete = { viewModel.onEvent(TasksEvent.Uncomplete(task)) },
                            onEdit = { viewModel.onEvent(TasksEvent.StartEdit(task)) },
                            onDelete = { viewModel.onEvent(TasksEvent.Delete(task.id)) },
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }

                // ── Всі активні ───────────────────────────────────────
                val otherActive = state.allActiveTasks.filter { active ->
                    state.todayTasks.none { it.id == active.id }
                }
                if (otherActive.isNotEmpty()) {
                    item {
                        SectionHeader("📋 Всі завдання", otherActive.size)
                    }
                    items(otherActive, key = { "all_${it.id}" }) { task ->
                        TaskCard(
                            item = task,
                            onComplete = { viewModel.onEvent(TasksEvent.Complete(task)) },
                            onUncomplete = { viewModel.onEvent(TasksEvent.Uncomplete(task)) },
                            onEdit = { viewModel.onEvent(TasksEvent.StartEdit(task)) },
                            onDelete = { viewModel.onEvent(TasksEvent.Delete(task.id)) },
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }

                // ── Порожній стан ─────────────────────────────────────
                if (state.todayTasks.isEmpty() && state.allActiveTasks.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("✅", fontSize = 40.sp)
                            Text("Немає активних завдань", style = AppTypography.bodySmall.copy(fontWeight = FontWeight.Medium))
                            Text("Натисніть '+ Додати' щоб створити перше", style = AppTypography.caption)
                        }
                    }
                }

                // ── Виконані ──────────────────────────────────────────
                if (state.completedTasks.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedMedium)
                                .clickable { viewModel.onEvent(TasksEvent.ToggleCompleted) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                if (state.showCompleted) "▲" else "▼",
                                style = AppTypography.caption,
                            )
                            Text(
                                "Виконані (${state.completedTasks.size})",
                                style = AppTypography.bodySmall.copy(color = AppColors.Text3),
                            )
                        }
                    }
                    if (state.showCompleted) {
                        items(state.completedTasks, key = { "done_${it.id}" }) { task ->
                            TaskCard(
                                item = task,
                                onComplete = { viewModel.onEvent(TasksEvent.Complete(task)) },
                                onUncomplete = { viewModel.onEvent(TasksEvent.Uncomplete(task)) },
                                onEdit = { viewModel.onEvent(TasksEvent.StartEdit(task)) },
                                onDelete = { viewModel.onEvent(TasksEvent.Delete(task.id)) },
                                dimmed = true,
                            )
                        }
                    }
                }
            }
        }

        // ── Add dialog ───────────────────────────────────────────────────
        if (state.isAddDialogOpen) {
            AddTaskDialog(
                state = state,
                onEvent = viewModel::onEvent,
            )
        }

        // ── Edit dialog ──────────────────────────────────────────────────
        if (state.isEditDialogOpen && state.editingItem != null) {
            TaskEditDialog(
                item = state.editingItem!!,
                onDismiss = { viewModel.onEvent(TasksEvent.CloseEdit) },
                onSave = { viewModel.onEvent(TasksEvent.SaveEdit(it)) },
            )
        }

        // ── Toast ────────────────────────────────────────────────────────
        state.toastMessage?.let { msg ->
            LaunchedEffect(msg) {
                kotlinx.coroutines.delay(2500)
                viewModel.onEvent(TasksEvent.ClearToast)
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

// ── TaskCard ──────────────────────────────────────────────────────────────────

@Composable
fun TaskCard(
    item: SavedItem,
    onComplete: () -> Unit,
    onUncomplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    dimmed: Boolean = false,
) {
    val meta = remember(item.description) { item.description.toTaskMeta() }
    val isRecurring = meta.recurrence != "once"
    val displayTitle = remember(item) { item.currentSequenceLabel() }
    val alpha = if (dimmed) 0.5f else 1f

    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // ── Чекбокс ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        if (item.isCompleted) AppColors.Green.copy(alpha = 0.2f)
                        else Color.Transparent
                    )
                    .border(
                        width = 1.5.dp,
                        color = if (item.isCompleted) AppColors.Green else AppColors.Border2,
                        shape = CircleShape,
                    )
                    .clickable { if (item.isCompleted) onUncomplete() else onComplete() },
                contentAlignment = Alignment.Center,
            ) {
                if (item.isCompleted) {
                    Text("✓", style = AppTypography.caption.copy(
                        color = AppColors.Green, fontSize = 11.sp,
                    ))
                }
            }

            // ── Вміст ────────────────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                // Заголовок
                Text(
                    text = displayTitle,
                    style = AppTypography.body.copy(
                        fontWeight = FontWeight.Medium,
                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (dimmed) AppColors.Text3 else AppColors.Text,
                    ),
                )

                // Мета-рядок: дедлайн, тип повторення
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    PriorityTag(item.priority)

                    if (isRecurring) {
                        TagChip(recurrenceLabel(meta.recurrence), AppColors.Green)
                    }
                    if (meta.deadlineMs > 0L) {
                        val isOverdue = meta.deadlineMs < System.currentTimeMillis() && !item.isCompleted
                        TagChip(
                            "⏰ ${formatTimestamp(meta.deadlineMs).take(10)}",
                            if (isOverdue) AppColors.PriorityHigh else AppColors.Text3,
                        )
                    }
                    if (meta.sequence.isNotEmpty()) {
                        TagChip("↺ послідовність", AppColors.Text3)
                    }
                }

                // Примітка (note)
                if (meta.note.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(meta.note, style = AppTypography.caption)
                }

                // Для periodic виконаного — коли наступний раз
                if (item.isCompleted && isRecurring && item.nextReviewAt > 0L) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Наступне: ${formatTimestamp(item.nextReviewAt).take(10)}",
                        style = AppTypography.caption.copy(color = AppColors.Green),
                    )
                }
            }

            // ── Кнопки ───────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton28(onClick = onEdit) {
                    Text("✎", style = AppTypography.caption.copy(color = AppColors.Text2))
                }
                IconButton28(onClick = onDelete) {
                    Text("✕", style = AppTypography.caption.copy(color = AppColors.PriorityHigh))
                }
            }
        }
    }
}

// ── Add dialog ────────────────────────────────────────────────────────────────

@Composable
fun AddTaskDialog(
    state: TasksState,
    onEvent: (TasksEvent) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { onEvent(TasksEvent.CloseAddDialog) },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(12.dp))
                .background(AppColors.Bg2)
                .border(1.dp, AppColors.Border2, RoundedCornerShape(12.dp))
                .clickable { /* consume */ }
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Нове завдання", style = AppTypography.titleMedium)

            // Назва
            LabeledField("Назва завдання") {
                AppTextField(
                    value = state.inputTitle,
                    onValueChange = { onEvent(TasksEvent.TitleChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "Введіть назву...",
                )
            }

            // Примітка
            LabeledField("Примітка (опційно)") {
                AppTextField(
                    value = state.inputNote,
                    onValueChange = { onEvent(TasksEvent.NoteChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "Деталі завдання...",
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // Пріоритет
                LabeledField("Пріоритет", Modifier.weight(1f)) {
                    PriorityDropdown(
                        selected = state.inputPriority,
                        onChange = { onEvent(TasksEvent.PriorityChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                // Тип повторення
                LabeledField("Повторення", Modifier.weight(1f)) {
                    RecurrenceDropdown(
                        selected = state.inputRecurrence,
                        onChange = { onEvent(TasksEvent.RecurrenceChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            // Дедлайн
            DeadlineField(
                deadlineMs = state.inputDeadlineMs,
                onDeadlineChange = { onEvent(TasksEvent.DeadlineChanged(it)) },
            )

            // Послідовність (тільки для periodic)
            AnimatedVisibility(state.inputRecurrence != "once") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LabeledField("Послідовність (через кому, опційно)") {
                        AppTextField(
                            value = state.inputSequence,
                            onValueChange = { onEvent(TasksEvent.SequenceChanged(it)) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = "англійська, німецька, французька",
                        )
                    }
                    if (state.inputSequence.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Checkbox(
                                checked = state.inputStrictSeq,
                                onCheckedChange = { onEvent(TasksEvent.StrictSeqChanged(it)) },
                                colors = CheckboxDefaults.colors(checkedColor = AppColors.Green),
                            )
                            Text(
                                "Суворий порядок (не переходити якщо не виконано)",
                                style = AppTypography.caption,
                            )
                        }
                    }
                }
            }

            // Кнопки
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                GhostButton("Скасувати", { onEvent(TasksEvent.CloseAddDialog) })
                AppButton("Зберегти", { onEvent(TasksEvent.SaveNew) })
            }
        }
    }
}

// ── Task Edit Dialog ──────────────────────────────────────────────────────────

@Composable
fun TaskEditDialog(
    item: SavedItem,
    onDismiss: () -> Unit,
    onSave: (SavedItem) -> Unit,
) {
    val meta = remember { item.description.toTaskMeta() }
    var title by remember { mutableStateOf(item.title) }
    var note by remember { mutableStateOf(meta.note) }
    var priority by remember { mutableStateOf(item.priority) }
    var recurrence by remember { mutableStateOf(meta.recurrence) }
    var deadlineMs by remember { mutableStateOf(meta.deadlineMs) }
    var sequence by remember { mutableStateOf(meta.sequence.joinToString(", ")) }
    var strictSeq by remember { mutableStateOf(meta.strictSequence) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(12.dp))
                .background(AppColors.Bg2)
                .border(1.dp, AppColors.Border2, RoundedCornerShape(12.dp))
                .clickable { }
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Редагувати завдання", style = AppTypography.titleMedium)

            LabeledField("Назва") {
                AppTextField(title, { title = it }, Modifier.fillMaxWidth(), "Назва...")
            }
            LabeledField("Примітка") {
                AppTextField(note, { note = it }, Modifier.fillMaxWidth(), "Деталі...")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LabeledField("Пріоритет", Modifier.weight(1f)) {
                    PriorityDropdown(priority, { priority = it }, Modifier.fillMaxWidth())
                }
                LabeledField("Повторення", Modifier.weight(1f)) {
                    RecurrenceDropdown(recurrence, { recurrence = it }, Modifier.fillMaxWidth())
                }
            }
            DeadlineField(
                deadlineMs = deadlineMs,
                onDeadlineChange = { deadlineMs = it },
            )
            if (recurrence != "once") {
                LabeledField("Послідовність") {
                    AppTextField(sequence, { sequence = it }, Modifier.fillMaxWidth(),
                        "англ, нім, фр")
                }
                if (sequence.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Checkbox(
                            checked = strictSeq,
                            onCheckedChange = { strictSeq = it },
                            colors = CheckboxDefaults.colors(checkedColor = AppColors.Green),
                        )
                        Text("Суворий порядок", style = AppTypography.caption)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                GhostButton("Скасувати", onDismiss)
                GhostButton("Зберегти", onClick = {
                    val seqList = if (sequence.isBlank()) emptyList()
                    else sequence.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    val newMeta = meta.copy(
                        note = note, recurrence = recurrence,
                        deadlineMs = deadlineMs, sequence = seqList, strictSequence = strictSeq,
                    )
                    val newValue = if (seqList.isEmpty()) title
                    else "$title: [${seqList.joinToString(", ")}]"
                    onSave(item.copy(
                        title = title, value = newValue,
                        description = newMeta.toDescriptionString(),
                        priority = priority,
                    ))
                })
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

@Composable
fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = AppTypography.body.copy(fontWeight = FontWeight.Medium))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(AppColors.Bg3)
                .border(1.dp, AppColors.Border, RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text("$count", style = AppTypography.caption)
        }
    }
}

@Composable
fun TagChip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(text, style = AppTypography.caption.copy(color = color))
    }
}

@Composable
fun RecurrenceDropdown(
    selected: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(
        "once"        to "Одноразове",
        "daily"       to "Щодня",
        "every_other" to "Через день",
        "every_N:2"   to "Кожні 2 дні",
        "every_N:3"   to "Кожні 3 дні",
        "every_N:7"   to "Щотижня",
        "weekdays:1,3,5" to "Пн, Ср, Пт",
        "weekdays:2,4"   to "Вт, Чт",
        "weekdays:6,7"   to "Вихідні",
    )
    var expanded by remember { mutableStateOf(false) }
    val label = options.find { it.first == selected }?.second ?: selected
    Box(modifier = modifier) {
        GhostButton(label, { expanded = true }, Modifier.fillMaxWidth())
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(AppColors.Bg3),
        ) {
            options.forEach { (key, display) ->
                DropdownMenuItem(
                    text = { Text(display, style = AppTypography.bodySmall) },
                    onClick = { onChange(key); expanded = false },
                )
            }
        }
    }
}

fun recurrenceLabel(r: String): String = when {
    r == "once"           -> "одноразово"
    r == "daily"          -> "щодня"
    r == "every_other"    -> "через день"
    r.startsWith("every_N:") -> "кожні ${r.removePrefix("every_N:")} дн."
    r.startsWith("weekdays:") -> {
        val map = mapOf(1 to "пн", 2 to "вт", 3 to "ср", 4 to "чт", 5 to "пт", 6 to "сб", 7 to "нд")
        r.removePrefix("weekdays:").split(",")
            .mapNotNull { map[it.trim().toIntOrNull()] }.joinToString(", ")
    }
    else -> r
}