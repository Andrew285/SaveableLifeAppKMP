package org.simpleapps.saveablekmp.domain.usecase

import org.simpleapps.saveablekmp.data.repository.SaveableRepository

class DeleteItemUseCase(private val repository: SaveableRepository) {
    suspend operator fun invoke(id: String) = repository.deleteItem(id)
}