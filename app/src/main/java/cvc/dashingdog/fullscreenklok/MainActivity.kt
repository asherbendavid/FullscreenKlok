package cvc.dashingdog.fullscreenklok

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var timeText: TextView
    private lateinit var dateText: TextView
    private lateinit var batteryView: BatteryView

    private val tickHandler = Handler(Looper.getMainLooper())
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("EEE d MMM", Locale.getDefault())

    private val tickRunnable = object : Runnable {
        override fun run() {
            updateClock()
            // Align next tick to the next whole second for a steady display
            val delay = 1000 - (System.currentTimeMillis() % 1000)
            tickHandler.postDelayed(this, delay)
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else 0
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            batteryView.setBatteryState(pct, charging)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep the screen on for as long as this activity is in the foreground.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_main)
        bindViews()

        @Suppress("DEPRECATION")
        window.decorView.setOnSystemUiVisibilityChangeListener {visibility ->
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0){
                applyImmersiveMode()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        // configChanges="orientation|screenSize" in the manifest means the activity
        // survives rotation -- we just need to swap in the right layout ourselves.
        setContentView(R.layout.activity_main)
        bindViews()
        updateClock()
        window.decorView.post { applyImmersiveMode() }
    }

    private fun bindViews() {
        timeText = findViewById(R.id.timeText)
        dateText = findViewById(R.id.dateText)
        batteryView = findViewById(R.id.batteryView)
    }

    private fun updateClock() {
        val now = java.util.Date()
        timeText.text = timeFormat.format(now)
        dateText.text = dateFormat.format(now)
    }

    override fun onResume() {
        super.onResume()
        tickHandler.post(tickRunnable)
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun onPause() {
        super.onPause()
        tickHandler.removeCallbacks(tickRunnable)
        try {
            unregisterReceiver(batteryReceiver)
        } catch (e: IllegalArgumentException) {
            // not registered -- safe to ignore
        }
    }

    /**
     * Hard lockout: swallow every touch event before it reaches any view.
     * This is deliberate belt-and-braces on top of the views already being
     * non-clickable/non-focusable -- the whole point of this app is that
     * an accidental touch does absolutely nothing.
     */
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        return true
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyImmersiveMode()
    }

    private fun applyImmersiveMode() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
    }
}
