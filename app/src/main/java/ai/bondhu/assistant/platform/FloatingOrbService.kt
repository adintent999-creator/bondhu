package ai.bondhu.assistant.platform

import ai.bondhu.assistant.ui.MainActivity
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button

class FloatingOrbService : Service() {
    private var view: Button? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (view != null) return START_STICKY
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val button = Button(this).apply {
            text = getString(ai.bondhu.assistant.R.string.orb_label)
            setBackgroundColor(Color.parseColor("#0B1F3A"))
            setTextColor(Color.WHITE)
            alpha = 0.9f
            setOnClickListener {
                val i = Intent(this@FloatingOrbService, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra("start_voice", true)
                }
                startActivity(i)
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            android.graphics.PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 24
            y = 220
        }
        runCatching { wm.addView(button, params) }
        view = button
        return START_STICKY
    }

    override fun onDestroy() {
        view?.let { runCatching { (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it) } }
        view = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
