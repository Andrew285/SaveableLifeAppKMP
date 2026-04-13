package org.simpleapps.saveablekmp.data.model

private val idChars = ('a'..'z') + ('0'..'9')
fun generateId(): String = buildString {
    repeat(12) { append(idChars.random()) }
}