package org.simpleapps.saveablekmp

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.koin.core.context.startKoin


fun main() = application {
    startKoin {
        modules(appModules)
    }

    val icon = painterResource("ic_saveable.png")

    val windowState = rememberWindowState(
        width = 1100.dp,
        height = 740.dp,
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "Сховище даних",
        state = windowState,
        icon = icon,
    ) {
        App()
    }
}