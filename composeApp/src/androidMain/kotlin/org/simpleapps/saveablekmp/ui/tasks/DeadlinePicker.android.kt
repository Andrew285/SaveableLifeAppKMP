package org.simpleapps.saveablekmp.ui.tasks

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import org.simpleapps.saveablekmp.ui.main.LabeledField
import org.simpleapps.saveablekmp.ui.theme.*
import java.util.Calendar

@Composable
actual fun DeadlineField(
    deadlineMs: Long,
    onDeadlineChange: (Long) -> Unit,
) {
    val context = LocalContext.current
    val calendar = remember(deadlineMs) {
        Calendar.getInstance().apply {
            if (deadlineMs > 0L) timeInMillis = deadlineMs
        }
    }

    LabeledField("Дедлайн") {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            GhostButton(
                text = if (deadlineMs == 0L) "📅 Оберіть дату та час"
                else "📅 ${epochMsToDateString(deadlineMs)} ${epochMsToTimeString(deadlineMs)}",
                onClick = {
                    // Спочатку DatePicker
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            // Потім одразу TimePickerDialog
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    val cal = Calendar.getInstance()
                                    cal.set(year, month, day, hour, minute, 0)
                                    cal.set(Calendar.MILLISECOND, 0)
                                    onDeadlineChange(cal.timeInMillis)
                                },
                                calendar.get(Calendar.HOUR_OF_DAY),
                                calendar.get(Calendar.MINUTE),
                                true, // 24h формат
                            ).show()
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH),
                    ).show()
                },
                modifier = Modifier.weight(1f),
            )
            if (deadlineMs > 0L) {
                Text(
                    "✕",
                    style = AppTypography.caption.copy(color = AppColors.PriorityHigh),
                    modifier = Modifier
                        .clickable { onDeadlineChange(0L) }
                        .padding(8.dp),
                )
            }
        }
    }
}