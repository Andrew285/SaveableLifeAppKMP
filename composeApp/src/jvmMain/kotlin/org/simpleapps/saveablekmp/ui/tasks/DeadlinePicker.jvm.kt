// desktopMain/.../ui/tasks/DeadlinePicker.desktop.kt
package org.simpleapps.saveablekmp.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.simpleapps.saveablekmp.ui.main.LabeledField
import org.simpleapps.saveablekmp.ui.theme.*
import java.util.Calendar

@Composable
actual fun DeadlineField(
    deadlineMs: Long,
    onDeadlineChange: (Long) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }

    // Розбиваємо deadlineMs на компоненти
    val initCal = remember(deadlineMs) {
        Calendar.getInstance().apply {
            if (deadlineMs > 0L) timeInMillis = deadlineMs
        }
    }
    var pickerYear  by remember(deadlineMs) { mutableStateOf(initCal.get(Calendar.YEAR)) }
    var pickerMonth by remember(deadlineMs) { mutableStateOf(initCal.get(Calendar.MONTH)) } // 0-based
    var pickerDay   by remember(deadlineMs) { mutableStateOf(if (deadlineMs > 0L) initCal.get(Calendar.DAY_OF_MONTH) else 0) }
    var pickerHour  by remember(deadlineMs) { mutableStateOf(initCal.get(Calendar.HOUR_OF_DAY)) }
    var pickerMin   by remember(deadlineMs) { mutableStateOf(initCal.get(Calendar.MINUTE)) }

    LabeledField("Дедлайн") {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // Кнопка відкриття
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppColors.Bg3)
                        .border(1.dp, if (showPicker) AppColors.Green else AppColors.Border, RoundedCornerShape(8.dp))
                        .clickable { showPicker = !showPicker }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = if (deadlineMs == 0L) "📅 Оберіть дату та час"
                        else "📅 ${epochMsToDateString(deadlineMs)} ${epochMsToTimeString(deadlineMs)}",
                        style = AppTypography.bodySmall.copy(
                            color = if (deadlineMs == 0L) AppColors.Text3 else AppColors.Text,
                        ),
                    )
                }
                if (deadlineMs > 0L) {
                    Text(
                        "✕",
                        style = AppTypography.caption.copy(color = AppColors.PriorityHigh),
                        modifier = Modifier
                            .clickable { onDeadlineChange(0L); showPicker = false }
                            .padding(8.dp),
                    )
                }
            }

            // Inline picker
            if (showPicker) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(AppColors.Bg3)
                        .border(1.dp, AppColors.Border, RoundedCornerShape(10.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // ── Навігація місяця ──────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "‹",
                            style = AppTypography.body.copy(color = AppColors.Text2, fontSize = 20.sp),
                            modifier = Modifier
                                .clickable {
                                    if (pickerMonth == 0) { pickerMonth = 11; pickerYear-- }
                                    else pickerMonth--
                                }
                                .padding(8.dp),
                        )
                        Text(
                            "${MONTH_NAMES_UA[pickerMonth]} $pickerYear",
                            style = AppTypography.body.copy(fontWeight = FontWeight.Medium),
                        )
                        Text(
                            "›",
                            style = AppTypography.body.copy(color = AppColors.Text2, fontSize = 20.sp),
                            modifier = Modifier
                                .clickable {
                                    if (pickerMonth == 11) { pickerMonth = 0; pickerYear++ }
                                    else pickerMonth++
                                }
                                .padding(8.dp),
                        )
                    }

                    // ── Дні тижня ─────────────────────────────────────
                    Row(modifier = Modifier.fillMaxWidth()) {
                        listOf("Пн","Вт","Ср","Чт","Пт","Сб","Нд").forEach { dow ->
                            Text(
                                dow,
                                modifier = Modifier.weight(1f),
                                style = AppTypography.caption.copy(
                                    textAlign = TextAlign.Center,
                                    color = AppColors.Text3,
                                ),
                            )
                        }
                    }

                    // ── Сітка днів ────────────────────────────────────
                    val cal = Calendar.getInstance().apply { set(pickerYear, pickerMonth, 1) }
                    // Перший день тижня (1=нд..7=сб → 0=пн..6=нд)
                    val firstDow = ((cal.get(Calendar.DAY_OF_WEEK) + 5) % 7)
                    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                    val todayCal = Calendar.getInstance()

                    val cells = firstDow + daysInMonth
                    val rows = (cells + 6) / 7

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        repeat(rows) { row ->
                            Row(modifier = Modifier.fillMaxWidth()) {
                                repeat(7) { col ->
                                    val cellIndex = row * 7 + col
                                    val day = cellIndex - firstDow + 1
                                    val isValid = day in 1..daysInMonth
                                    val isSelected = isValid && day == pickerDay &&
                                            pickerMonth == initCal.get(Calendar.MONTH) &&
                                            pickerYear == initCal.get(Calendar.YEAR)
                                    val isToday = isValid &&
                                            day == todayCal.get(Calendar.DAY_OF_MONTH) &&
                                            pickerMonth == todayCal.get(Calendar.MONTH) &&
                                            pickerYear == todayCal.get(Calendar.YEAR)

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(CircleShape)
                                            .background(
                                                when {
                                                    isSelected -> AppColors.Green
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .border(
                                                width = if (isToday && !isSelected) 1.dp else 0.dp,
                                                color = if (isToday && !isSelected) AppColors.Green else Color.Transparent,
                                                shape = CircleShape,
                                            )
                                            .then(
                                                if (isValid) Modifier.clickable {
                                                    pickerDay = day
                                                    applyAndNotify(
                                                        pickerYear, pickerMonth, day,
                                                        pickerHour, pickerMin, onDeadlineChange,
                                                    )
                                                } else Modifier
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        if (isValid) {
                                            Text(
                                                "$day",
                                                style = AppTypography.caption.copy(
                                                    color = when {
                                                        isSelected -> Color.Black
                                                        isToday    -> AppColors.Green
                                                        else       -> AppColors.Text2
                                                    },
                                                    fontWeight = if (isSelected || isToday)
                                                        FontWeight.Bold else FontWeight.Normal,
                                                ),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Час ───────────────────────────────────────────
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("⏰", style = AppTypography.bodySmall)
                        Text("Час:", style = AppTypography.bodySmall.copy(color = AppColors.Text3))
                        // Години
                        TimeSpinner(
                            value = pickerHour,
                            range = 0..23,
                            format = { it.toString().padStart(2, '0') },
                            onChange = { h ->
                                pickerHour = h
                                if (pickerDay > 0) applyAndNotify(
                                    pickerYear, pickerMonth, pickerDay, h, pickerMin, onDeadlineChange,
                                )
                            },
                        )
                        Text(":", style = AppTypography.body.copy(fontWeight = FontWeight.Bold))
                        // Хвилини
                        TimeSpinner(
                            value = pickerMin,
                            range = 0..59,
                            format = { it.toString().padStart(2, '0') },
                            onChange = { m ->
                                pickerMin = m
                                if (pickerDay > 0) applyAndNotify(
                                    pickerYear, pickerMonth, pickerDay, pickerHour, m, onDeadlineChange,
                                )
                            },
                        )
                        Spacer(Modifier.weight(1f))
                        if (pickerDay > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AppColors.Green)
                                    .clickable { showPicker = false }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            ) {
                                Text("Готово", style = AppTypography.caption.copy(color = Color.Black))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeSpinner(
    value: Int,
    range: IntRange,
    format: (Int) -> String,
    onChange: (Int) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            "‹",
            style = AppTypography.body.copy(color = AppColors.Text2),
            modifier = Modifier
                .clickable { onChange(if (value == range.first) range.last else value - 1) }
                .padding(4.dp),
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(AppColors.Bg4)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(format(value), style = AppTypography.body.copy(fontWeight = FontWeight.Medium))
        }
        Text(
            "›",
            style = AppTypography.body.copy(color = AppColors.Text2),
            modifier = Modifier
                .clickable { onChange(if (value == range.last) range.first else value + 1) }
                .padding(4.dp),
        )
    }
}

private fun applyAndNotify(
    year: Int, month: Int, day: Int,
    hour: Int, min: Int,
    onDeadlineChange: (Long) -> Unit,
) {
    val cal = Calendar.getInstance()
    cal.set(year, month, day, hour, min, 0)
    cal.set(Calendar.MILLISECOND, 0)
    onDeadlineChange(cal.timeInMillis)
}

private val MONTH_NAMES_UA = listOf(
    "Січень","Лютий","Березень","Квітень","Травень","Червень",
    "Липень","Серпень","Вересень","Жовтень","Листопад","Грудень",
)