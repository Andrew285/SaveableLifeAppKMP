package org.simpleapps.saveablekmp.domain.usecase

import kotlinx.datetime.Clock
import org.simpleapps.saveablekmp.data.model.SavedItem
import org.simpleapps.saveablekmp.data.model.generateId
import org.simpleapps.saveablekmp.data.repository.SaveableRepository
import org.simpleapps.saveablekmp.domain.detector.CategoryDetector

class SaveItemUseCase(private val repository: SaveableRepository) {
    suspend operator fun invoke(
        value: String,
        title: String = "",
        description: String = "",
        categoryOverride: String? = null,
        subcategory: String = "",
        priority: org.simpleapps.saveablekmp.data.model.Priority = org.simpleapps.saveablekmp.data.model.Priority.MEDIUM,
    ): SavedItem {
        val category = categoryOverride ?: CategoryDetector.detect(value)
        val now = Clock.System.now().toEpochMilliseconds()
        val item = SavedItem(
            id = generateId(),
            value = value,
            title = title,
            description = description,
            category = category,
            subcategory = subcategory,
            priority = priority,
            createdAt = now,
            updatedAt = now,
        )
        repository.insertItem(item)
        return item
    }
}