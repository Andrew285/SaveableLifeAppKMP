package org.simpleapps.saveablekmp.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.simpleapps.saveablekmp.data.db.SaveableDatabase
import org.simpleapps.saveablekmp.data.mappers.toModel
import org.simpleapps.saveablekmp.data.model.Category
import org.simpleapps.saveablekmp.data.model.DEFAULT_CATEGORIES
import org.simpleapps.saveablekmp.data.model.Priority
import org.simpleapps.saveablekmp.data.model.SavedItem
import org.simpleapps.saveablekmp.data.model.TimeFilter

class SaveableRepository(db: SaveableDatabase) {

    private val itemQueries = db.saveableQueries
    private val dispatcher = Dispatchers.IO

    // ── Items ────────────────────────────────────────────────────────────────

    fun observeAllItems(): Flow<List<SavedItem>> =
        itemQueries.getAllItems().asFlow().mapToList(dispatcher).map { list ->
            list.map { it.toModel() }
        }

    fun observeItemsByCategory(categoryId: String): Flow<List<SavedItem>> =
        itemQueries.getItemsByCategory(categoryId, categoryId)
            .asFlow().mapToList(dispatcher).map { list -> list.map { it.toModel() } }

    fun observeItemsByTime(filter: TimeFilter): Flow<List<SavedItem>> {
        val now = Clock.System.now()
        val tz = TimeZone.currentSystemDefault()
        val today = now.toLocalDateTime(tz).date

        return when (filter) {
            TimeFilter.ALL -> observeAllItems()
            TimeFilter.TODAY -> {
                val start = today.atStartOfDayIn(tz).toEpochMilliseconds()
                itemQueries.getItemsToday(start).asFlow().mapToList(dispatcher).map { it.map { r -> r.toModel() } }
            }
            TimeFilter.YESTERDAY -> {
                val yDate = kotlinx.datetime.LocalDate(today.year, today.month, today.dayOfMonth - 1)
                val start = yDate.atStartOfDayIn(tz).toEpochMilliseconds()
                val end = today.atStartOfDayIn(tz).toEpochMilliseconds()
                itemQueries.getItemsBetween(start, end).asFlow().mapToList(dispatcher).map { it.map { r -> r.toModel() } }
            }
            TimeFilter.WEEK -> {
                val start = now.toEpochMilliseconds() - 7L * 24 * 60 * 60 * 1000
                itemQueries.getItemsToday(start).asFlow().mapToList(dispatcher).map { it.map { r -> r.toModel() } }
            }
            TimeFilter.MONTH -> {
                val start = now.toEpochMilliseconds() - 30L * 24 * 60 * 60 * 1000
                itemQueries.getItemsToday(start).asFlow().mapToList(dispatcher).map { it.map { r -> r.toModel() } }
            }
        }
    }

    suspend fun insertItem(item: SavedItem) {
        itemQueries.insertItem(
            id = item.id,
            value_ = item.value,
            title = item.title,
            description = item.description,
            category = item.category,
            subcategory = item.subcategory,
            priority = item.priority.name,
            created_at = item.createdAt,
            updated_at = item.updatedAt,
            is_synced = if (item.isSynced) 1L else 0L,
            is_deleted = if (item.isDeleted) 1L else 0L,
        )
    }

    suspend fun updateItem(item: SavedItem) {
        val newTime = Clock.System.now().toEpochMilliseconds()
        println("=== updateItem: id=${item.id.take(6)}, newTime=$newTime, oldTime=${item.updatedAt}")
        itemQueries.updateItem(
            value_ = item.value,
            title = item.title,
            description = item.description,
            category = item.category,
            subcategory = item.subcategory,
            priority = item.priority.name,
            updated_at = newTime,
            id = item.id,
        )
        // Перевіримо що реально збереглось в БД
        val saved = itemQueries.getAllItemsIncludingDeleted().executeAsList()
            .find { it.id == item.id }
        println("=== after update, db updatedAt=${saved?.updated_at}")
    }

    suspend fun softDeleteItem(id: String) {
        itemQueries.softDeleteItem(
            updated_at = Clock.System.now().toEpochMilliseconds(),
            id = id,
        )
    }

    suspend fun cleanupOldItems() {
        itemQueries.deleteOldSyncedItems()
    }

    fun getItemsPaged(limit: Long, offset: Long): List<SavedItem> =
        itemQueries.getItemsPaged(limit, offset).executeAsList().map { it.toModel() }

    fun getActiveItemsCount(): Long =
        itemQueries.getActiveItemsCount().executeAsOne()

    suspend fun deleteItem(id: String) {
        itemQueries.deleteItem(id)
    }

    // Всі елементи включно з видаленими — для синхронізації
    fun getAllItemsIncludingDeleted(): List<SavedItem> =
        itemQueries.getAllItemsIncludingDeleted().executeAsList().map { it.toModel() }

    // Видалити синхронізовані soft-deleted елементи
    suspend fun purgeDeletedSynced() {
        itemQueries.purgeDeletedSynced()
    }

    suspend fun deleteAllItems() {
        itemQueries.deleteAllItems()
    }

    fun getUnsyncedItems(): List<SavedItem> =
        itemQueries.getUnsyncedItems().executeAsList().map { it.toModel() }

    fun getAllItemsOnce(): List<SavedItem> =
        itemQueries.getAllItems().executeAsList().map { it.toModel() }

    suspend fun markSynced(id: String) {
        itemQueries.markSynced(id)
    }

    // ── Categories ───────────────────────────────────────────────────────────

    fun observeCategories(): Flow<List<Category>> =
        itemQueries.getAllCategories().asFlow().mapToList(dispatcher).map { list ->
            list.map { it.toModel() }
        }

    suspend fun initDefaultCategories() {
        val existing = itemQueries.getAllCategories().executeAsList()
        if (existing.isEmpty()) {
            DEFAULT_CATEGORIES.forEach { cat ->
                itemQueries.insertCategory(
                    id = cat.id,
                    name = cat.name,
                    color = cat.color,
                    parent_id = cat.parentId,
                    is_builtin = if (cat.isBuiltin) 1L else 0L,
                )
            }
        }
    }

    suspend fun insertCategory(cat: Category) {
        itemQueries.insertCategory(
            id = cat.id,
            name = cat.name,
            color = cat.color,
            parent_id = cat.parentId,
            is_builtin = if (cat.isBuiltin) 1L else 0L,
        )
    }

    suspend fun deleteCategory(id: String) {
        itemQueries.updateItemsCategoryOnDelete(id, id)
        itemQueries.deleteCategory(id)
    }
}