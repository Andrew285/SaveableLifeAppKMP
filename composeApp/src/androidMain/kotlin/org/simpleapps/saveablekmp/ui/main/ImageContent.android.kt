package org.simpleapps.saveablekmp.ui.main

import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.BitmapFactory
import org.simpleapps.saveablekmp.base64DataUrlToBytes

@Composable
actual fun Base64Image(dataUrl: String, modifier: Modifier) {
    val bitmap = remember(dataUrl) {
        dataUrl.base64DataUrlToBytes()?.let {
            BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap()
        }
    }
    bitmap?.let {
        Image(
            bitmap = it,
            contentDescription = null,
            modifier = modifier,
        )
    }
}