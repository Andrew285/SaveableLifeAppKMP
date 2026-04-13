package org.simpleapps.saveablekmp.ui.main

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import org.simpleapps.saveablekmp.base64DataUrlToBytes

@Composable
actual fun Base64Image(dataUrl: String, modifier: Modifier) {
    val bitmap = remember(dataUrl) {
        dataUrl.base64DataUrlToBytes()?.let {
            Image.makeFromEncoded(it).toComposeImageBitmap()
        }
    }
    bitmap?.let {
        androidx.compose.foundation.Image(
            bitmap = it,
            contentDescription = null,
            modifier = modifier,
        )
    }
}