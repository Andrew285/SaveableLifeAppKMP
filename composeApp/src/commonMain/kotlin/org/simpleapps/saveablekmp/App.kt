package org.simpleapps.saveablekmp

import androidx.compose.runtime.*
import org.koin.compose.viewmodel.koinViewModel
import org.simpleapps.saveablekmp.ui.main.MainScreen
import org.simpleapps.saveablekmp.ui.main.MainViewModel
import org.simpleapps.saveablekmp.ui.settings.SettingsScreen
import org.simpleapps.saveablekmp.ui.settings.SettingsViewModel
import org.simpleapps.saveablekmp.ui.theme.SaveableTheme

enum class Screen {
    MAIN,
    SETTINGS
}

@Composable
fun App() {
    SaveableTheme {
        var currentScreen by remember { mutableStateOf(Screen.MAIN) }

        when (currentScreen) {
            Screen.MAIN -> {
                val viewModel = koinViewModel<MainViewModel>()
                MainScreen(
                    viewModel = viewModel,
                    onNavigateSettings = { currentScreen = Screen.SETTINGS },
                )
            }
            Screen.SETTINGS -> {
                val viewModel = koinViewModel<SettingsViewModel>()
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = Screen.MAIN },
                )
            }
        }
    }
}