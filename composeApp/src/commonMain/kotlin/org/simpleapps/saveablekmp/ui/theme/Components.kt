package org.simpleapps.saveablekmp.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.simpleapps.saveablekmp.ui.main.LabeledField

val RoundedSmall  = RoundedCornerShape(6.dp)
val RoundedMedium = RoundedCornerShape(8.dp)
val RoundedLarge  = RoundedCornerShape(10.dp)

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val base = Modifier
        .fillMaxWidth()
        .background(AppColors.Bg2, RoundedLarge)
        .border(1.dp, AppColors.Border, RoundedLarge)
        .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
        .padding(14.dp)
    Column(modifier = modifier.then(base), content = content)
}

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .clip(RoundedMedium)
            .background(if (enabled) AppColors.Green else AppColors.Bg3)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 18.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = AppTypography.body.copy(
                color = if (enabled) Color.Black else AppColors.Text3,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
            )
        )
    }
}

@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedMedium)
            .background(Color.Transparent)
            .border(1.dp, AppColors.Border2, RoundedMedium)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = AppTypography.bodySmall.copy(color = AppColors.Text2))
    }
}

@Composable
fun IconButton28(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedSmall)
            .background(AppColors.Bg3)
            .border(1.dp, AppColors.Border, RoundedSmall)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    textStyle: TextStyle = AppTypography.body,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .background(AppColors.Bg3, RoundedMedium)
            .border(1.dp, AppColors.Border, RoundedMedium)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        textStyle = textStyle,
        singleLine = singleLine,
        cursorBrush = androidx.compose.ui.graphics.SolidColor(AppColors.Green),
        decorationBox = { inner ->
            if (value.isEmpty()) {
                Text(placeholder, style = textStyle.copy(color = AppColors.Text3))
            }
            inner()
        }
    )
}

@Composable
fun CategoryTag(name: String, color: Color) {
    val bg = color.copy(alpha = 0.13f)
    val border = color.copy(alpha = 0.28f)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(4.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(name, style = AppTypography.caption.copy(color = color, fontSize = 11.sp))
    }
}

@Composable
fun PriorityTag(priority: org.simpleapps.saveablekmp.data.model.Priority) {
    val (color, label) = when (priority) {
        org.simpleapps.saveablekmp.data.model.Priority.HIGH   -> AppColors.PriorityHigh   to "Високий"
        org.simpleapps.saveablekmp.data.model.Priority.MEDIUM -> AppColors.PriorityMedium to "Середній"
        org.simpleapps.saveablekmp.data.model.Priority.LOW    -> AppColors.PriorityLow    to "Низький"
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(label, style = AppTypography.caption.copy(color = color, fontSize = 11.sp))
    }
}

@Composable
fun ColorDot(color: Color, size: Int = 9) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun BasicTextField(
    value: String,
    noinline onValueChange: (String) -> Unit,
    modifier: Modifier,
    textStyle: TextStyle,
    singleLine: Boolean,
    cursorBrush: androidx.compose.ui.graphics.Brush,
    noinline decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit,
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        textStyle = textStyle,
        singleLine = singleLine,
        cursorBrush = cursorBrush,
        decorationBox = decorationBox,
    )
}

@Composable
fun DeadlineField(
    deadlineMs: Long,
    onDeadlineChange: (Long) -> Unit,
) {
    // Конвертуємо epochMs → "dd.mm.yyyy" для відображення
    var text by remember(deadlineMs) {
        mutableStateOf(if (deadlineMs == 0L) "" else epochMsToDateString(deadlineMs))
    }
    var isError by remember { mutableStateOf(false) }

    LabeledField("Дедлайн (дд.мм.рррр)") {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AppTextField(
                value = text,
                onValueChange = { input ->
                    // Автоформатування: вставляємо крапки автоматично
                    val digits = input.filter { it.isDigit() }.take(8)
                    text = buildString {
                        digits.forEachIndexed { i, c ->
                            if (i == 2 || i == 4) append('.')
                            append(c)
                        }
                    }
                    // Парсимо коли введено повну дату
                    isError = false
                    if (digits.length == 8) {
                        val ms = dateStringToEpochMs(text)
                        if (ms != null) {
                            onDeadlineChange(ms)
                            isError = false
                        } else {
                            isError = true
                        }
                    } else if (digits.isEmpty()) {
                        onDeadlineChange(0L)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = "31.12.2025",
            )
            if (isError) {
                Text(
                    "Невірний формат дати",
                    style = AppTypography.caption.copy(color = AppColors.PriorityHigh),
                )
            }
            if (deadlineMs > 0L && !isError) {
                Text(
                    "✓ ${epochMsToDateString(deadlineMs)}",
                    style = AppTypography.caption.copy(color = AppColors.Green),
                )
            }
        }
    }
}

// Парсинг "dd.mm.yyyy" → epochMs (початок дня)
fun dateStringToEpochMs(s: String): Long? {
    return try {
        val parts = s.split(".")
        if (parts.size != 3) return null
        val day   = parts[0].toInt()
        val month = parts[1].toInt()
        val year  = parts[2].toInt()
        if (day !in 1..31 || month !in 1..12 || year < 2000) return null

        val cal = java.util.Calendar.getInstance()
        cal.set(year, month - 1, day, 0, 0, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        cal.timeInMillis
    } catch (_: Exception) { null }
}

// epochMs → "dd.mm.yyyy"
fun epochMsToDateString(ms: Long): String {
    val cal = java.util.Calendar.getInstance()
    cal.timeInMillis = ms
    val d = cal.get(java.util.Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
    val m = (cal.get(java.util.Calendar.MONTH) + 1).toString().padStart(2, '0')
    val y = cal.get(java.util.Calendar.YEAR)
    return "$d.$m.$y"
}