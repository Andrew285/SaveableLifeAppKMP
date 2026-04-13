package org.simpleapps.saveablekmp

import java.awt.RenderingHints
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

actual object ImagePicker {

    actual fun pickFromGallery(onResult: (ByteArray?) -> Unit) {
        val chooser = JFileChooser().apply {
            dialogTitle = "Оберіть зображення"
            fileFilter = FileNameExtensionFilter(
                "Зображення (jpg, png, gif, bmp, webp)",
                "jpg", "jpeg", "png", "gif", "bmp", "webp"
            )
        }
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            val image = ImageIO.read(chooser.selectedFile)
            onResult(image?.let { compressImage(it) })
        } else {
            onResult(null)
        }
    }

    actual fun pasteFromClipboard(onResult: (ByteArray?) -> Unit) {
        try {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            val contents = clipboard.getContents(null) ?: run { onResult(null); return }

            when {
                // Пряме зображення з буфера (напр. скріншот через Snipping Tool)
                contents.isDataFlavorSupported(DataFlavor.imageFlavor) -> {
                    val image = contents.getTransferData(DataFlavor.imageFlavor) as BufferedImage
                    onResult(compressImage(image))
                }
                // Файл скопійований в провіднику
                contents.isDataFlavorSupported(DataFlavor.javaFileListFlavor) -> {
                    @Suppress("UNCHECKED_CAST")
                    val files = contents.getTransferData(DataFlavor.javaFileListFlavor) as List<File>
                    val imageFile = files.firstOrNull {
                        it.extension.lowercase() in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
                    }
                    val image = imageFile?.let { ImageIO.read(it) }
                    onResult(image?.let { compressImage(it) })
                }
                // URI як текст (саме твій випадок — вставляється шлях)
                contents.isDataFlavorSupported(DataFlavor.stringFlavor) -> {
                    val text = contents.getTransferData(DataFlavor.stringFlavor) as String
                    val file = File(text.trim().removePrefix("file:///").replace("%20", " "))
                    if (file.exists() && file.extension.lowercase() in listOf("jpg","jpeg","png","gif","bmp","webp")) {
                        val image = ImageIO.read(file)
                        onResult(image?.let { compressImage(it) })
                    } else {
                        onResult(null)
                    }
                }
                else -> onResult(null)
            }
        } catch (e: Exception) {
            onResult(null)
        }
    }

    private fun compressImage(image: BufferedImage): ByteArray {
        // Масштабуємо до максимум 800px по більшій стороні
        val maxSize = 800
        val scale = minOf(maxSize.toFloat() / image.width, maxSize.toFloat() / image.height, 1f)
        val newW = (image.width * scale).toInt()
        val newH = (image.height * scale).toInt()

        val scaled = BufferedImage(newW, newH, BufferedImage.TYPE_INT_RGB)
        val g = scaled.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.drawImage(image, 0, 0, newW, newH, null)
        g.dispose()

        val out = ByteArrayOutputStream()
        ImageIO.write(scaled, "jpg", out)
        return out.toByteArray()
    }
}