package org.simpleapps.saveablekmp.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Формат повторення:
//   "once"         — одноразове (default)
//   "daily"        — кожен день
//   "every_other"  — через день
//   "every_N:3"    — кожні 3 дні
//   "weekdays:1,3,5" — пн, ср, пт (1=пн..7=нд)
@Serializable
data class TaskMeta(
    @SerialName("d")      val deadlineMs: Long = 0L,
    @SerialName("r")      val recurrence: String = "once",
    @SerialName("seq")    val sequence: List<String> = emptyList(),
    @SerialName("si")     val seqIndex: Int = 0,
    @SerialName("strict") val strictSequence: Boolean = false,
    @SerialName("note")   val note: String = "",
)

private val taskJson = Json { ignoreUnknownKeys = true }

/** Розпарсити TaskMeta з поля description. Якщо не JSON — повертає default. */
fun String.toTaskMeta(): TaskMeta {
    if (!startsWith("{")) return TaskMeta(note = this)
    return try {
        taskJson.decodeFromString<TaskMeta>(this)
    } catch (_: Exception) {
        TaskMeta(note = this)
    }
}

/** Серіалізувати TaskMeta назад у рядок для збереження в description. */
fun TaskMeta.toDescriptionString(): String =
    taskJson.encodeToString(TaskMeta.serializer(), this)

/** Чи є завдання сьогодні активним (periodic або одноразове без дедлайну). */
fun SavedItem.isTaskDueToday(): Boolean {
    val meta = description.toTaskMeta()
    val todayStart = todayStartMs()
    val now = System.currentTimeMillis()

    return when {
        meta.recurrence == "once" ->
            meta.deadlineMs == 0L || meta.deadlineMs >= todayStart
        else -> isRecurrenceActiveToday(meta)
    }
}
fun SavedItem.isRecurrenceActiveToday(meta: TaskMeta): Boolean {
    val todayStart = todayStartMs()
    return when {
        meta.recurrence == "daily" -> true
        meta.recurrence == "every_other" -> {
            val daysSinceCreation = ((todayStart - createdAt) / (24 * 60 * 60 * 1000)).toInt()
            daysSinceCreation % 2 == 0
        }
        meta.recurrence.startsWith("every_N:") -> {
            val n = meta.recurrence.removePrefix("every_N:").toIntOrNull() ?: 1
            val daysSinceCreation = ((todayStart - createdAt) / (24 * 60 * 60 * 1000)).toInt()
            daysSinceCreation % n == 0
        }
        meta.recurrence.startsWith("weekdays:") -> {
            val days = meta.recurrence.removePrefix("weekdays:")
                .split(",").mapNotNull { it.trim().toIntOrNull() }
            val cal = java.util.Calendar.getInstance()
            // Calendar: 1=нд,2=пн..7=сб → конвертуємо в 1=пн..7=нд
            val dow = ((cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7) + 1
            dow in days
        }
        else -> true
    }
}

fun todayStartMs(): Long {
    val cal = java.util.Calendar.getInstance()
    cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
    cal.set(java.util.Calendar.MINUTE, 0)
    cal.set(java.util.Calendar.SECOND, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

/** Поточний лейбл послідовності (або title якщо немає послідовності). */
fun SavedItem.currentSequenceLabel(): String {
    val meta = description.toTaskMeta()
    if (meta.sequence.isEmpty()) return title
    val idx = meta.seqIndex.coerceIn(0, meta.sequence.size - 1)
    return "$title: ${meta.sequence[idx]}"
}

/** Розрахувати nextReviewAt після виконання (для periodic tasks). */
fun TaskMeta.calcNextReviewAt(): Long {
    val now = System.currentTimeMillis()
    val dayMs = 24 * 60 * 60 * 1000L
    val todayStart = todayStartMs()

    return when {
        recurrence == "once" -> 0L
        recurrence == "daily" -> todayStart + dayMs
        recurrence == "every_other" -> todayStart + 2 * dayMs
        recurrence.startsWith("every_N:") -> {
            val n = recurrence.removePrefix("every_N:").toLongOrNull() ?: 1L
            todayStart + n * dayMs
        }
        recurrence.startsWith("weekdays:") -> {
            val days = recurrence.removePrefix("weekdays:")
                .split(",").mapNotNull { it.trim().toIntOrNull() }.sorted()
            // Знаходимо наступний підходящий день тижня
            val cal = java.util.Calendar.getInstance()
            val todayDow = ((cal.get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7) + 1
            val nextDow = days.firstOrNull { it > todayDow } ?: days.first()
            val daysAhead = if (nextDow > todayDow) nextDow - todayDow
            else 7 - todayDow + nextDow
            todayStart + daysAhead * dayMs
        }
        else -> now + dayMs
    }
}

/** Наступний індекс послідовності. */
fun TaskMeta.nextSeqIndex(): Int {
    if (sequence.isEmpty()) return 0
    return (seqIndex + 1) % sequence.size
}