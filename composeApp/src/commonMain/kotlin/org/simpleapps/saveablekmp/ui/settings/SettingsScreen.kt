package org.simpleapps.saveablekmp.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.simpleapps.saveablekmp.data.model.Category
import org.simpleapps.saveablekmp.ui.main.parseColor
import org.simpleapps.saveablekmp.ui.theme.*

val PRESET_COLORS = listOf(
    "#4ade80", "#60a5fa", "#f472b6", "#fbbf24",
    "#a78bfa", "#fb923c", "#34d399", "#f87171",
    "#38bdf8", "#e879f9",
)

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
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
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedMedium)
                        .background(AppColors.Bg3)
                        .border(1.dp, AppColors.Border, RoundedMedium)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("←", style = AppTypography.body.copy(color = AppColors.Text2))
                }
                Column {
                    Text("Налаштування", style = AppTypography.titleLarge)
                    Text("Керуйте категоріями та правилами", style = AppTypography.subtitle)
                }
            }

            Spacer(Modifier.height(20.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                // ── Add category section ──────────────────────────────
                item {
                    SettingsSection(title = "Додати нову категорію") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text("НАЗВА КАТЕГОРІЇ", style = AppTypography.caption)
                                    AppTextField(
                                        value = state.newCatName,
                                        onValueChange = { viewModel.onEvent(SettingsEvent.NewCatNameChanged(it)) },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = "Введіть назву...",
                                    )
                                }
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text("БАТЬКІВСЬКА КАТЕГОРІЯ", style = AppTypography.caption)
                                    val topLevel = state.categories.filter { it.parentId == null }
                                    var expanded by remember { mutableStateOf(false) }
                                    val selectedParent = topLevel.find { it.id == state.newCatParentId }
                                    Box {
                                        GhostButton(
                                            text = selectedParent?.name ?: "Без батьківської",
                                            onClick = { expanded = true },
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                        androidx.compose.material3.DropdownMenu(
                                            expanded = expanded,
                                            onDismissRequest = { expanded = false },
                                            modifier = Modifier.background(AppColors.Bg3),
                                        ) {
                                            androidx.compose.material3.DropdownMenuItem(
                                                text = { Text("Без батьківської", style = AppTypography.bodySmall.copy(color = AppColors.Text3)) },
                                                onClick = { viewModel.onEvent(SettingsEvent.NewCatParentChanged("")); expanded = false },
                                            )
                                            topLevel.forEach { cat ->
                                                androidx.compose.material3.DropdownMenuItem(
                                                    text = {
                                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                            ColorDot(parseColor(cat.color))
                                                            Text(cat.name, style = AppTypography.bodySmall)
                                                        }
                                                    },
                                                    onClick = { viewModel.onEvent(SettingsEvent.NewCatParentChanged(cat.id)); expanded = false },
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Color picker
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Text("КОЛІР", style = AppTypography.caption)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    PRESET_COLORS.forEach { colorHex ->
                                        val color = parseColor(colorHex)
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(color)
                                                .then(
                                                    if (state.newCatColor == colorHex)
                                                        Modifier.border(2.dp, Color.White, CircleShape)
                                                    else Modifier
                                                )
                                                .clickable { viewModel.onEvent(SettingsEvent.NewCatColorChanged(colorHex)) }
                                        )
                                    }
                                }
                            }

                            AppButton(
                                text = "+ Додати категорію",
                                onClick = { viewModel.onEvent(SettingsEvent.AddCategory) },
                            )
                        }
                    }
                }

                // ── Categories list ───────────────────────────────────
                item {
                    SettingsSection(title = "Категорії (${state.categories.size})") {
                        val topLevel = state.categories.filter { it.parentId == null }
                        val children = state.categories.filter { it.parentId != null }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            topLevel.forEach { cat ->
                                CategoryRow(
                                    cat = cat,
                                    badge = if (cat.isBuiltin) "Базова" else "Власна",
                                    onDelete = if (!cat.isBuiltin) {
                                        { viewModel.onEvent(SettingsEvent.DeleteCategory(cat.id)) }
                                    } else null,
                                )
                                children.filter { it.parentId == cat.id }.forEach { sub ->
                                    CategoryRow(
                                        cat = sub,
                                        badge = "↳ ${cat.name}",
                                        indent = true,
                                        onDelete = { viewModel.onEvent(SettingsEvent.DeleteCategory(sub.id)) },
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Danger zone ───────────────────────────────────────
                item {
                    SettingsSection(
                        title = "Небезпечна зона",
                        titleColor = AppColors.PriorityHigh,
                        borderColor = AppColors.PriorityHigh.copy(alpha = 0.2f),
                    ) {
                        GhostButton(
                            text = "Видалити всі дані",
                            onClick = { viewModel.onEvent(SettingsEvent.ClearAll) },
                        )
                    }
                }
            }
        }

        // ── Toast ─────────────────────────────────────────────────────────
        state.toastMessage?.let { msg ->
            LaunchedEffect(msg) {
                kotlinx.coroutines.delay(2500)
                viewModel.onEvent(SettingsEvent.ClearToast)
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

@Composable
fun SettingsSection(
    title: String,
    titleColor: Color = AppColors.Text,
    borderColor: Color = AppColors.Border,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedLarge)
            .background(AppColors.Bg2)
            .border(1.dp, borderColor, RoundedLarge)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(title, style = AppTypography.body.copy(fontWeight = FontWeight.Medium, color = titleColor))
        content()
    }
}

@Composable
fun CategoryRow(
    cat: Category,
    badge: String,
    indent: Boolean = false,
    onDelete: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (indent) 16.dp else 0.dp)
            .clip(RoundedMedium)
            .background(AppColors.Bg3)
            .border(1.dp, AppColors.Border, RoundedMedium)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ColorDot(parseColor(cat.color))
        Text(
            cat.name,
            style = AppTypography.body.copy(fontWeight = FontWeight.Medium),
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(AppColors.Bg4)
                .padding(horizontal = 7.dp, vertical = 2.dp)
        ) {
            Text(badge, style = AppTypography.caption)
        }
        if (onDelete != null) {
            IconButton28(onClick = onDelete) {
                Text("✕", style = AppTypography.caption.copy(color = AppColors.PriorityHigh))
            }
        }
    }
}