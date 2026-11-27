package ai.bondhu.assistant.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen(
    state: UiState,
    onVoice: () -> Unit,
    onText: (String) -> Unit,
    onStop: () -> Unit,
    onDiagnostics: () -> Unit,
) {
    var typed by remember { mutableStateOf("") }
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("বন্ধু", style = MaterialTheme.typography.displaySmall)
        Text(state.status, style = MaterialTheme.typography.titleMedium)
        Button(
            onClick = onVoice,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        ) { Text(if (state.busy) "শুনছি..." else "বলুন") }
        Text("শোনা: ${state.lastHeard}", textAlign = TextAlign.Center)
        Text("উত্তর: ${state.lastReply}", textAlign = TextAlign.Center)
        Text("action_log: ${state.actionCount} টি কাজ")
        OutlinedTextField(
            value = typed,
            onValueChange = { typed = it },
            label = { Text("টেস্ট কমান্ড (ঐচ্ছিক)") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = { onText(typed); typed = "" }, modifier = Modifier.fillMaxWidth()) {
            Text("পাঠান")
        }
        OutlinedButton(onClick = onStop, modifier = Modifier.fillMaxWidth()) { Text("থামো") }
        OutlinedButton(onClick = onDiagnostics, modifier = Modifier.fillMaxWidth()) { Text("ডায়গনস্টিকস") }
    }
}
