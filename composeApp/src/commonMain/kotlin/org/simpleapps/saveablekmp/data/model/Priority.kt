package org.simpleapps.saveablekmp.data.model

enum class Priority(val label: String) {
    HIGH("Високий"),
    MEDIUM("Середній"),
    LOW("Низький");

    companion object {
        fun fromString(s: String) = entries.firstOrNull { it.name.equals(s, ignoreCase = true) } ?: MEDIUM
    }
}