package ai.bondhu.assistant.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun DiagnosticsScreen(
    state: UiState,
    onBack: () -> Unit,
    onSaveKey: (String) -> Unit,
) {
    var key by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("ডায়গনস্টিকস", style = MaterialTheme.typography.headlineSmall)
        Text("মাইক অনুমতি: ${if (state.micGranted) "হ্যাঁ" else "না"}")
        Text("ওভারলে অনুমতি: ${if (state.overlayGranted) "হ্যাঁ" else "না"}")
        Text("মডেল: ${if (state.keyPresent) "Gemini Live keypresent" else "Local bn-BD mode"}")
        Text("মোট action_log: ${state.actionCount}")
        OutlinedTextField(
            value = key,
            onValueChange = { key = it },
            label = { Text("Gemini API key (ঐচ্ছিক, ডিভাইসেই এনক্রিপ্টেড)") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(onClick = { onSaveKey(key); key = "" }, modifier = Modifier.fillMaxWidth()) {
            Text("key সংরক্ষণ করুন")
        }
        Divider(Modifier.padding(vertical = 8.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(state.actions) { row ->
                Text("${row.tool} — ${row.status} / ${row.risk} / ${row.latencyMs}ms")
                Text("proof: ${row.proofRef} ${row.resultJson.take(80)}", style = MaterialTheme.typography.bodySmall)
                Divider(Modifier.padding(vertical = 6.dp))
            }
        }
        Button(onClick = onBack) { Text("ফিরে যান") }
    }
}
