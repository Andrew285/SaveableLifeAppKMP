package org.simpleapps.saveablekmp.data.model

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

@Serializable
data class SavedItem(
    val id: String = generateId(),
    val value: String,
    val title: String = "",
    val description: String = "",
    val category: String = "text",
    val subcategory: String = "",
    val priority: Priority = Priority.MEDIUM,
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    val updatedAt: Long = Clock.System.now().toEpochMilliseconds(),
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
    val isCompleted: Boolean = false,

    // SRC fields
    val nextReviewAt: Long = 0L,    // next revision
    val easeFactor: Double = 2.5,   // difficulty coefficient (SM-2)
    val interval: Int = 0,          // current interval in days
    val repetitions: Int = 0,       // amount of success repetitions
)