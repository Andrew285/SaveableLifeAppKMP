package org.simpleapps.saveablekmp

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.koin.compose.viewmodel.koinViewModel
import org.simpleapps.saveablekmp.ui.flashcards.FlashcardsScreen
import org.simpleapps.saveablekmp.ui.flashcards.FlashcardsViewModel
import org.simpleapps.saveablekmp.ui.main.MainScreen
import org.simpleapps.saveablekmp.ui.main.MainViewModel
import org.simpleapps.saveablekmp.ui.settings.SettingsScreen
import org.simpleapps.saveablekmp.ui.settings.SettingsViewModel
import org.simpleapps.saveablekmp.ui.tasks.TasksScreen
import org.simpleapps.saveablekmp.ui.tasks.TasksViewModel
import org.simpleapps.saveablekmp.ui.theme.SaveableTheme
import org.simpleapps.saveablekmp.ui.theme.AppColors
import org.simpleapps.saveablekmp.ui.theme.AppTypography

enum class Screen { MAIN, TASKS, FLASHCARDS, SETTINGS }

private data class NavItem(
    val screen: Screen,
    val icon: String,
    val label: String,
)

private val NAV_ITEMS = listOf(
    NavItem(Screen.MAIN,       "⊞",  "Сховище"),
    NavItem(Screen.TASKS,      "✓",  "Завдання"),
    NavItem(Screen.FLASHCARDS, "🃏", "Картки"),
    NavItem(Screen.SETTINGS,   "⚙",  "Налаштування"),
)

@Composable
fun App() {
    SaveableTheme {
        var currentScreen by remember { mutableStateOf(Screen.MAIN) }

        // ViewModel-и живуть весь час — не перестворюються при перемиканні вкладок
        val mainViewModel       = koinViewModel<MainViewModel>()
        val tasksViewModel      = koinViewModel<TasksViewModel>()
        val flashcardsViewModel = koinViewModel<FlashcardsViewModel>()
        val settingsViewModel   = koinViewModel<SettingsViewModel>()

        Scaffold(
            containerColor = AppColors.Bg,
            bottomBar = {
                AppBottomNavBar(
                    currentScreen = currentScreen,
                    onNavigate = { currentScreen = it },
                )
            },
        ) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                ) { screen ->
                    when (screen) {
                        Screen.MAIN -> MainScreen(
                            viewModel = mainViewModel,
                        )
                        Screen.TASKS -> TasksScreen(
                            viewModel = tasksViewModel,
                        )
                        Screen.FLASHCARDS -> FlashcardsScreen(
                            viewModel = flashcardsViewModel,
                        )
                        Screen.SETTINGS -> SettingsScreen(
                            viewModel = settingsViewModel,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppBottomNavBar(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit,
) {
    NavigationBar(
        containerColor = AppColors.Bg2,
        tonalElevation = 0.dp,
    ) {
        NAV_ITEMS.forEach { item ->
            val selected = currentScreen == item.screen
            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.screen) },
                icon = {
                    Text(
                        text = item.icon,
                        fontSize = 20.sp,
                        color = if (selected) AppColors.Green else AppColors.Text3,
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = AppTypography.caption.copy(
                            fontSize = 10.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor   = AppColors.Green,
                    unselectedIconColor = AppColors.Text3,
                    selectedTextColor   = AppColors.Green,
                    unselectedTextColor = AppColors.Text3,
                    indicatorColor      = AppColors.Green.copy(alpha = 0.12f),
                ),
            )
        }
    }
}