package org.simpleapps.saveablekmp

actual object UrlOpener {
    actual fun open(url: String) {
        java.awt.Desktop.getDesktop().browse(java.net.URI(url))
    }
}