package org.simpleapps.saveablekmp.ui.tasks

import androidx.compose.runtime.Composable

@Composable
expect fun DeadlineField(
    deadlineMs: Long,
    onDeadlineChange: (Long) -> Unit,
)

// Спільні утиліти для обох платформ
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

fun epochMsToTimeString(ms: Long): String {
    val cal = java.util.Calendar.getInstance()
    cal.timeInMillis = ms
    val h = cal.get(java.util.Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
    val m = cal.get(java.util.Calendar.MINUTE).toString().padStart(2, '0')
    return "$h:$m"
}