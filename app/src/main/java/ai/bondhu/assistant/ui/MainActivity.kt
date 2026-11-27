package ai.bondhu.assistant.ui

import ai.bondhu.assistant.platform.AssistantForegroundService
import ai.bondhu.assistant.platform.FloatingOrbService
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val vm: HomeViewModel by viewModels()
    private var voiceHandled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ComposeView(this).apply {
            setContent { BondhuApp(vm, onStartServices = { startServices() }) }
        })
        startServices()
    }

    override fun onResume() {
        super.onResume()
        vm.refresh()
        val shouldVoice = intent?.getBooleanExtra("start_voice", false) == true
        if (shouldVoice && !voiceHandled) {
            voiceHandled = true
            intent?.removeExtra("start_voice")
            vm.startVoiceScoped()
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        voiceHandled = false
        // On a Transsion/Infinix ROM onResume() is not always delivered when a
        // singleTop activity is already resumed. The floating orb uses this path.
        if (intent?.getBooleanExtra("start_voice", false) == true) {
            voiceHandled = true
            intent.removeExtra("start_voice")
            vm.startVoiceScoped()
        }
    }

    private fun startServices() {
        val mic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (mic) {
            val i = android.content.Intent(this, AssistantForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(i) else startService(i)
        }
        if (Settings.canDrawOverlays(this)) {
            startService(android.content.Intent(this, FloatingOrbService::class.java))
        }
    }
}
