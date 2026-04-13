package org.simpleapps.saveablekmp

import java.awt.Image
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

actual object Clipboard {
    actual fun copy(text: String) {
        if (text.startsWith("data:image")) {
            // Копіюємо як реальне зображення
            try {
                val bytes = text.substringAfter("base64,")
                    .let { java.util.Base64.getDecoder().decode(it) }
                val image = ImageIO.read(ByteArrayInputStream(bytes))
                if (image != null) {
                    Toolkit.getDefaultToolkit().systemClipboard
                        .setContents(ImageTransferable(image), null)
                    return
                }
            } catch (e: Exception) {
                // fallback до тексту
            }
        }
        // Звичайний текст
        val selection = StringSelection(text)
        Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, selection)
    }
}

private class ImageTransferable(private val image: BufferedImage) : Transferable {
    override fun getTransferDataFlavors() = arrayOf(DataFlavor.imageFlavor)
    override fun isDataFlavorSupported(flavor: DataFlavor) = flavor == DataFlavor.imageFlavor
    override fun getTransferData(flavor: DataFlavor): Any {
        if (flavor == DataFlavor.imageFlavor) return image
        throw UnsupportedFlavorException(flavor)
    }
}