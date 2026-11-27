package ai.bondhu.assistant.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val perms = mutableListOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
    if (Build.VERSION.SDK_INT >= 33) perms += Manifest.permission.POST_NOTIFICATIONS
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { onDone() }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("বন্ধুতে স্বাগতম", style = MaterialTheme.typography.headlineMedium)
        Text(
            "বলুন, বন্ধু আসল কাজ করবে। মাইক, ক্যামেরা টর্চ আর কাছে থাকা ভাসমান বাটন চালু করতে নিচের ধাপগুলো একবার দিন।",
            textAlign = TextAlign.Center,
        )
        Button(onClick = { launcher.launch(perms.toTypedArray()) }, modifier = Modifier.fillMaxWidth()) {
            Text("১. মাইক ও ক্যামেরা অনুমতি")
        }
        OutlinedButton(onClick = { openOverlaySettings(context) }, modifier = Modifier.fillMaxWidth()) {
            Text("২. ভাসমান বাটন অনুমতি")
        }
        OutlinedButton(onClick = { openWriteSettings(context) }, modifier = Modifier.fillMaxWidth()) {
            Text("৩. ব্রাইটনেস অনুমতি (ঐচ্ছিক)")
        }
        OutlinedButton(onClick = { openNotificationPolicy(context) }, modifier = Modifier.fillMaxWidth()) {
            Text("৪. ডিএনডি অনুমতি (ঐচ্ছিক)")
        }
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("সম্পন্ন") }
    }
}

private fun openOverlaySettings(context: Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

private fun openWriteSettings(context: Context) {
    runCatching {
        context.startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun openNotificationPolicy(context: Context) {
    runCatching {
        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
