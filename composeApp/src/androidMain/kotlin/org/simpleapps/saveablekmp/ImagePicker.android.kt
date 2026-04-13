package org.simpleapps.saveablekmp

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.ByteArrayOutputStream

actual object ImagePicker : KoinComponent {
    private val context: Context by inject()
    private var pendingCallback: ((ByteArray?) -> Unit)? = null

    actual fun pickFromGallery(onResult: (ByteArray?) -> Unit) {
        pendingCallback = onResult
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
        }
        ImagePickerActivity.launch(context, intent)
    }

    actual fun pasteFromClipboard(onResult: (ByteArray?) -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = cm.primaryClip
                if (clip == null || clip.itemCount == 0) {
                    withContext(Dispatchers.Main) { onResult(null) }
                    return@launch
                }
                val item = clip.getItemAt(0)
                val uri: Uri? = item.uri
                if (uri != null) {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    val compressed = bytes?.let { compressImage(it) }
                    withContext(Dispatchers.Main) { onResult(compressed) }
                } else {
                    withContext(Dispatchers.Main) { onResult(null) }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(null) }
            }
        }
    }

    fun deliverResult(bytes: ByteArray?) {
        if (bytes != null) {
            GlobalScope.launch(Dispatchers.IO) {
                val compressed = compressImage(bytes)
                withContext(Dispatchers.Main) {
                    pendingCallback?.invoke(compressed)
                    pendingCallback = null
                }
            }
        } else {
            pendingCallback?.invoke(null)
            pendingCallback = null
        }
    }

    private fun compressImage(bytes: ByteArray): ByteArray {
        return try {
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return bytes
            // Масштабуємо до максимум 800px по більшій стороні
            val maxSize = 800
            val scale = minOf(maxSize.toFloat() / bitmap.width, maxSize.toFloat() / bitmap.height, 1f)
            val newW = (bitmap.width * scale).toInt()
            val newH = (bitmap.height * scale).toInt()
            val scaled = android.graphics.Bitmap.createScaledBitmap(bitmap, newW, newH, true)
            val out = ByteArrayOutputStream()
            scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 75, out)
            out.toByteArray()
        } catch (e: Exception) {
            bytes
        }
    }
}