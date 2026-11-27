package ai.bondhu.assistant.domain.tool

import ai.bondhu.assistant.R
import ai.bondhu.assistant.core.SessionController
import ai.bondhu.assistant.core.ToolRequest
import ai.bondhu.assistant.core.ToolResult
import ai.bondhu.assistant.data.repo.BondhuRepository
import ai.bondhu.assistant.util.toBengaliDigits
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.BatteryManager
import android.provider.Settings
import android.view.WindowManager
import ai.bondhu.assistant.ui.MainActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemToolExecutor @Inject constructor(
    private val context: Context,
    private val repo: BondhuRepository,
) {
    suspend fun execute(request: ToolRequest): ToolResult {
        if (SessionController.isAborted()) return ToolResult(false, "কাজ বাতিল করা হয়েছে।", error = "aborted")
        return when (request.id) {
            "device_status" -> deviceStatus()
            "battery_detail" -> battery()
            "get_time" -> time()
            "list_apps" -> listApps()
            "open_app" -> openApp(request.args["target"].orEmpty())
            "set_torch" -> torch(request.args["enabled"]?.toBoolean() ?: true)
            "set_volume" -> volume(request.args["percent"]?.toIntOrNull() ?: 50)
            "set_brightness" -> brightness(request.args["percent"]?.toIntOrNull() ?: 60)
            "set_dnd" -> dnd(request.args["enabled"]?.toBoolean() ?: true)
            "set_alarm" -> alarm(request.args["minutes"]?.toIntOrNull() ?: 0)
            "set_timer" -> timer(request.args["minutes"]?.toIntOrNull() ?: 5)
            "remember" -> remember(request.spokenText)
            else -> ToolResult(false, "অচেনা টুল: ${request.id}", error = "unknown_tool")
        }
    }

    private fun deviceStatus(): ToolResult {
        val net = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val active = net.activeNetworkInfo
        val msg = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}, Android ${android.os.Build.VERSION.RELEASE}, " +
            "network=${active?.typeName ?: "none"}"
        return ToolResult(true, msg, "build:${android.os.Build.MODEL}")
    }

    private fun battery(): ToolResult {
        val intent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val pct = if (level >= 0) level * 100 / scale else -1
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        return ToolResult(
            true,
            "ব্যাটারি ${pct.toString().toBengaliDigits()} শতাংশ, ${if (plugged > 0) "চার্জ হচ্ছে" else "চার্জ নেই"}।",
            "battery:$pct",
        )
    }

    private fun time(): ToolResult {
        val s = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()).toBengaliDigits()
        return ToolResult(true, "এখন $s।", "time:$s")
    }

    private fun listApps(): ToolResult {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .sortedBy { it.loadLabel(pm).toString() }
            .take(20)
            .joinToString(", ") { it.loadLabel(pm).toString() }
        return ToolResult(true, "অ্যাপ: $apps", "apps:${apps.take(80)}")
    }

    private fun openApp(target: String): ToolResult {
        val pm = context.packageManager
        val candidates = when (target.lowercase()) {
            "messages", "messaging", "মেসেজ" -> listOf("com.google.android.apps.messaging", "com.android.messaging")
            "dialer", "ফোন" -> listOf("com.google.android.dialer", "com.android.dialer")
            "camera", "ক্যামেরা" -> listOf("com.android.camera2", "com.google.android.GoogleCamera")
            else -> listOf(target)
        }
        for (pkg in candidates) {
            val launch = pm.getLaunchIntentForPackage(pkg)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launch)
                return ToolResult(true, "$pkg খোলা হয়েছে।", "opened:$pkg")
            }
        }
        val byLabel = pm.getInstalledApplications(0).firstOrNull {
            it.loadLabel(pm).toString().contains(target, ignoreCase = true)
        }
        if (byLabel != null) {
            val launch = pm.getLaunchIntentForPackage(byLabel.packageName)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launch)
                return ToolResult(true, "${byLabel.loadLabel(pm)} খোলা হয়েছে।", "opened:${byLabel.packageName}")
            }
        }
        return ToolResult(false, "$target অ্যাপ পাওয়া যায়নি।", error = "app_not_found")
    }

    private fun torch(on: Boolean): ToolResult {
        val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val ids = cm.cameraIdList
        val cam = ids.firstOrNull { id ->
            cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return ToolResult(false, "এই ফোনে টর্চ নেই।", error = "no_flash")
        return try {
            cm.setTorchMode(cam, on)
            ToolResult(true, if (on) "টর্চ জ্বলে গেছে।" else "টর্চ নিভে গেছে।", "torch:$cam=$on")
        } catch (e: Exception) {
            ToolResult(false, "টর্চ বদলানো গেল না: ${e.message}", error = "torch_failed")
        }
    }

    private fun volume(percent: Int): ToolResult {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val v = (max * percent.coerceIn(0, 100) / 100f).toInt()
        am.setStreamVolume(AudioManager.STREAM_MUSIC, v, AudioManager.FLAG_SHOW_UI)
        return ToolResult(true, "ভলিউম ${percent.toString().toBengaliDigits()} শতাংশ করা হয়েছে।", "volume:$v/$max")
    }

    private fun brightness(percent: Int): ToolResult {
        val value = (percent.coerceIn(1, 100) * 255 / 100)
        return try {
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
            val read = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            ToolResult(true, "ব্রাইটনেস ${percent.toString().toBengaliDigits()} শতাংশ করা হয়েছে।", "brightness:$read")
        } catch (e: Exception) {
            ToolResult(false, "ব্রাইটনেস বদলানো গেল না।", error = "brightness_failed")
        }
    }

    private fun dnd(on: Boolean): ToolResult {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!nm.isNotificationPolicyAccessGranted) {
            return ToolResult(false, "ডিএনডি অনুমতি দেওয়া হয়নি।", error = "dnd_permission")
        }
        val mode = if (on) NotificationManager.INTERRUPTION_FILTER_NONE else NotificationManager.INTERRUPTION_FILTER_ALL
        nm.setInterruptionFilter(mode)
        return ToolResult(true, if (on) "ডিএনডি চালু হয়েছে।" else "ডিএনডি বন্ধ হয়েছে।", "dnd:$mode")
    }

    private fun alarm(minutes: Int): ToolResult {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MainActivity::class.java).setAction("bondhu_alarm")
        val pi = PendingIntent.getActivity(context, 101, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val at = System.currentTimeMillis() + minutes * 60_000L
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        return ToolResult(true, "$minutes মিনিট পর অ্যালার্ম সেট করা হয়েছে।", "alarm:$at")
    }

    private fun timer(minutes: Int): ToolResult {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, MainActivity::class.java).setAction("bondhu_timer")
        val pi = PendingIntent.getActivity(context, 102, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val at = System.currentTimeMillis() + minutes * 60_000L
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        return ToolResult(true, "$minutes মিনিটের টাইমার চালু করা হয়েছে।", "timer:$at")
    }

    private suspend fun remember(text: String): ToolResult {
        val key = "note:${System.currentTimeMillis()}"
        repo.remember(key, text)
        return ToolResult(true, "মনে রাখলাম।", "memory:$key")
    }
}
