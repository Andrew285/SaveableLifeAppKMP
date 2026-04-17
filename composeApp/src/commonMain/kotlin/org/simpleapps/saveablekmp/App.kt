package org.simpleapps.saveablekmp

import androidx.compose.runtime.*
import org.koin.compose.viewmodel.koinViewModel
import org.simpleapps.saveablekmp.ui.flashcards.FlashcardsScreen
import org.simpleapps.saveablekmp.ui.flashcards.FlashcardsViewModel
import org.simpleapps.saveablekmp.ui.main.MainScreen
import org.simpleapps.saveablekmp.ui.main.MainViewModel
import org.simpleapps.saveablekmp.ui.settings.SettingsScreen
import org.simpleapps.saveablekmp.ui.settings.SettingsViewModel
import org.simpleapps.saveablekmp.ui.theme.SaveableTheme

enum class Screen { MAIN, SETTINGS, FLASHCARDS }

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
                    onNavigateFlashcards = { currentScreen = Screen.FLASHCARDS },
                )
            }
            Screen.SETTINGS -> {
                val viewModel = koinViewModel<SettingsViewModel>()
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = Screen.MAIN },
                )
            }
            Screen.FLASHCARDS -> {
                val viewModel = koinViewModel<FlashcardsViewModel>()
                FlashcardsScreen(
                    viewModel = viewModel,
                    onBack = { currentScreen = Screen.MAIN },
                )
            }
        }
    }
}