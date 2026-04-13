package org.simpleapps.saveablekmp

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
fun ByteArray.toBase64DataUrl(): String {
    val base64 = Base64.encode(this)
    return "data:image/png;base64,$base64"
}

@OptIn(ExperimentalEncodingApi::class)
fun String.base64DataUrlToBytes(): ByteArray? {
    return try {
        val base64 = this.substringAfter("base64,")
        Base64.decode(base64)
    } catch (e: Exception) {
        null
    }
}