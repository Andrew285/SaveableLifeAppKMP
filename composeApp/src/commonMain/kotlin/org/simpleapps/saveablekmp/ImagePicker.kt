package org.simpleapps.saveablekmp

expect object ImagePicker {
    fun pickFromGallery(onResult: (ByteArray?) -> Unit)
    fun pasteFromClipboard(onResult: (ByteArray?) -> Unit)
}