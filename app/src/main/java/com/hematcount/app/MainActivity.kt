package com.hematcount.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hematcount.app.ui.CounterScreen
import com.hematcount.app.ui.HistoryScreen
import com.hematcount.app.ui.ResultsScreen
import com.hematcount.app.ui.SettingsScreen
import com.hematcount.app.ui.theme.HematCountTheme

enum class Screen { COUNTER, RESULTS, HISTORY, SETTINGS }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val vm: CounterViewModel = viewModel()
            HematCountTheme(themeMode = vm.themeMode, dynamicColor = vm.dynamicColor) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
                    HematCountApp(vm)
                }
            }
        }
    }
}

@Composable
fun HematCountApp(vm: CounterViewModel) {
    var screen by rememberSaveable { mutableStateOf(Screen.COUNTER) }
    var settingsReturn by rememberSaveable { mutableStateOf(Screen.COUNTER) }
    BackHandler(enabled = screen != Screen.COUNTER) {
        screen = if (screen == Screen.SETTINGS) settingsReturn else Screen.COUNTER
    }

    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            val forward = targetState.ordinal > initialState.ordinal
            (slideInHorizontally(tween(280)) { if (forward) it / 4 else -it / 4 } + fadeIn(tween(280)))
                .togetherWith(slideOutHorizontally(tween(220)) { if (forward) -it / 6 else it / 6 } + fadeOut(tween(180)))
        },
        label = "screen",
    ) { s ->
        when (s) {
            Screen.COUNTER -> CounterScreen(
                vm = vm,
                onOpenResults = { screen = Screen.RESULTS },
                onOpenSettings = { settingsReturn = Screen.COUNTER; screen = Screen.SETTINGS },
                onOpenHistory = { screen = Screen.HISTORY },
            )
            Screen.RESULTS -> ResultsScreen(
                vm = vm,
                onBack = { screen = Screen.COUNTER },
                onOpenAiSettings = { settingsReturn = Screen.RESULTS; screen = Screen.SETTINGS },
            )
            Screen.HISTORY -> HistoryScreen(
                vm = vm,
                onBack = { screen = Screen.COUNTER },
                onOpened = { vm.resultsTab = 0; screen = Screen.RESULTS },
            )
            Screen.SETTINGS -> SettingsScreen(vm = vm, onBack = { screen = settingsReturn })
        }
    }
}
