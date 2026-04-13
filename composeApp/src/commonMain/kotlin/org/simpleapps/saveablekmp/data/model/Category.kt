package org.simpleapps.saveablekmp.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val id: String,
    val name: String,
    val color: String = "#4ade80",
    val parentId: String? = null,
    val isBuiltin: Boolean = false,
)