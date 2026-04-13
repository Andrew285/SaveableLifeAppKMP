package org.simpleapps.saveablekmp.domain.usecase

import org.simpleapps.saveablekmp.data.model.SavedItem
import org.simpleapps.saveablekmp.data.repository.SaveableRepository

class UpdateItemUseCase(private val repository: SaveableRepository) {
    suspend operator fun invoke(item: SavedItem) = repository.updateItem(item)
}