package org.simpleapps.saveablekmp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.util.Base64

actual object Clipboard : KoinComponent {
    private val context: Context by inject()

    actual fun copy(text: String) {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        if (text.startsWith("data:image")) {
            try {
                val bytes = Base64.getDecoder().decode(text.substringAfter("base64,"))
                // Зберігаємо як тимчасовий файл і копіюємо URI
                val tempFile = File(context.cacheDir, "clipboard_image.jpg")
                tempFile.writeBytes(bytes)
                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    tempFile,
                )
                val clip = ClipData.newUri(context.contentResolver, "image", uri)
                cm.setPrimaryClip(clip)
                return
            } catch (e: Exception) {
                // fallback до тексту
            }
        }

        cm.setPrimaryClip(ClipData.newPlainText("saveable", text))
    }
}