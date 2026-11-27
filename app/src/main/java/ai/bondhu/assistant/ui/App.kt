package ai.bondhu.assistant.ui

import ai.bondhu.assistant.ui.theme.BondhuTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun BondhuApp(vm: HomeViewModel, onStartServices: () -> Unit) {
    val state by vm.ui.collectAsState()
    BondhuTheme {
        if (!state.onboarded) {
            OnboardingScreen {
                vm.setOnboarded()
                onStartServices()
            }
        } else {
            var screen by remember { mutableStateOf("home") }
            when (screen) {
                "diag" -> DiagnosticsScreen(
                    state = state,
                    onBack = { screen = "home" },
                    onSaveKey = { vm.saveKey(it) },
                )
                else -> HomeScreen(
                    state = state,
                    onVoice = { vm.startVoiceScoped() },
                    onText = { vm.onText(it) },
                    onStop = { vm.stop() },
                    onDiagnostics = { screen = "diag"; vm.refresh() },
                )
            }
        }
    }
}
