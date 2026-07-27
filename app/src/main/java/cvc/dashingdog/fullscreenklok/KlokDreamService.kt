package cvc.dashingdog.fullscreenklok

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.service.dreams.DreamService
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class KlokDreamService : DreamService() {

    private lateinit var timeText: TextView
    private lateinit var dateText: TextView
    private lateinit var batteryView: BatteryView
    private lateinit var blockContainer: android.widget.LinearLayout

    private val handler = Handler(Looper.getMainLooper())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault()) // no seconds, per spec
    private val dateFormat = SimpleDateFormat("EEE d MMM", Locale.getDefault())

    // Ticks the clock every minute (matches no-seconds display, no need to redraw faster)
    private val tickRunnable = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 60_000)
        }
    }

    // Gentle burn-in mitigation: nudge the whole block a few px, periodically
    private val shiftRunnable = object : Runnable {
        override fun run() {
            shiftContainer()
            handler.postDelayed(this, Prefs.SHIFT_INTERVAL_MS) // every 5 minutes
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

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        isInteractive = false      // fully passive, no touch handling needed
        isFullscreen = true
        setContentView(R.layout.dream_klok)

        timeText = findViewById(R.id.dreamTimeText)
        dateText = findViewById(R.id.dreamDateText)
        batteryView = findViewById(R.id.dreamBatteryView)
        blockContainer = findViewById(R.id.dreamBlockContainer)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        updateClock()
        handler.post(tickRunnable)
        handler.post(shiftRunnable)
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun onDreamingStopped() {
        super.onDreamingStopped()
        handler.removeCallbacks(tickRunnable)
        handler.removeCallbacks(shiftRunnable)
        try {
            unregisterReceiver(batteryReceiver)
        } catch (e: IllegalArgumentException) {
            // not registered -- safe to ignore
        }
    }

    private fun updateClock() {
        val now = Date()
        timeText.text = timeFormat.format(now)
        dateText.text = dateFormat.format(now)
    }

    private fun shiftContainer() {
        val maxOffsetPx = Prefs.SHIFT_MAX_PX // small enough to stay unnoticed, enough to matter for burn-in
        val dx = Random.nextFloat() * maxOffsetPx * 2 - maxOffsetPx
        val dy = Random.nextFloat() * maxOffsetPx * 2 - maxOffsetPx
        blockContainer.animate()
            .translationX(dx)
            .translationY(dy)
            .setDuration(2000)
            .start()
    }
}