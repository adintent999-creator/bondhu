package ai.bondhu.assistant.platform

import ai.bondhu.assistant.R
import ai.bondhu.assistant.core.SessionController
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import ai.bondhu.assistant.ui.MainActivity

class AssistantForegroundService : Service() {
    private var lastScreenOff = 0L
    private val screenOff = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val now = System.currentTimeMillis()
            if (now - lastScreenOff < 450) {
                SessionController.abort()
                stopSelf()
            }
            lastScreenOff = now
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        registerReceiver(screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val pi = PendingIntent.getActivity(
            this, 1, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification: Notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, "bondhu_voice")
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }.setContentTitle("Bondhu")
            .setContentText(getString(R.string.notif_voice))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pi)
            .build()
        try {
            startForeground(1001, notification)
        } catch (e: Exception) {
            stopSelf()
        }
        return START_STICKY
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(
                    "bondhu_voice",
                    getString(R.string.channel_voice),
                    NotificationManager.IMPORTANCE_LOW,
                )
            )
        }
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(screenOff) }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
