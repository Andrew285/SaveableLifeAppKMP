package org.simpleapps.saveablekmp.data.mappers

import org.simpleapps.saveablekmp.data.model.Category
import org.simpleapps.saveablekmp.data.model.Priority
import org.simpleapps.saveablekmp.data.model.SavedItem

fun org.simpleapps.saveablekmp.data.db.SavedItem.toModel() = SavedItem(
    id = id,
    value = value_,
    title = title,
    description = description,
    category = category,
    subcategory = subcategory,
    priority = Priority.fromString(priority),
    createdAt = created_at,
    updatedAt = updated_at,
    isSynced = is_synced == 1L,
    isDeleted = is_deleted == 1L,
    nextReviewAt = next_review_at,
    easeFactor = ease_factor,
    interval = interval_.toInt(),
    repetitions = repetitions.toInt(),
)
fun org.simpleapps.saveablekmp.data.db.Category.toModel() = Category(
    id = id,
    name = name,
    color = color,
    parentId = parent_id,
    isBuiltin = is_builtin == 1L,
)