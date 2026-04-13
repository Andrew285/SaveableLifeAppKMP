package org.simpleapps.saveablekmp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform