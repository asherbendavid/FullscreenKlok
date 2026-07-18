package cvc.dashingdog.fullscreenklok

import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private var scale = Prefs.SCALE_DEFAULT

    private lateinit var previewTime: TextView
    private lateinit var previewDate: TextView
    private lateinit var previewBattery: BatteryView
    private lateinit var scaleValueText: TextView

    private lateinit var batterySwitch: Switch
    private lateinit var dateSwitch: Switch
    private lateinit var secondsSwitch: Switch
    private var previewBatteryWrapper: View? = null
    private lateinit var rotationLockSwitch: Switch
    private lateinit var rotationLockRow: View

    private val scaleKey: String
        get() = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
            Prefs.KEY_TEXT_SCALE_LAND else Prefs.KEY_TEXT_SCALE_PORTRAIT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        setContentView(R.layout.activity_settings)

        prefs = getSharedPreferences(Prefs.FILE, MODE_PRIVATE)
        bindViews()
        loadAndApply()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setContentView(R.layout.activity_settings)
        bindViews()
        loadAndApply()
    }

    private fun bindViews() {
        // IDs come from the included activity_main.xml -- same view tree,
        // no separate "preview" ID set to keep in sync.
        previewTime = findViewById(R.id.timeText)
        previewDate = findViewById(R.id.dateText)
        previewBattery = findViewById(R.id.batteryView)
        previewBatteryWrapper = findViewById(R.id.batteryWrapper) // null in portrait

        scaleValueText = findViewById(R.id.scaleValueText)
        batterySwitch = findViewById(R.id.batterySwitch)
        dateSwitch = findViewById(R.id.dateSwitch)
        secondsSwitch = findViewById(R.id.secondsSwitch)

        findViewById<Button>(R.id.scaleDownButton).setOnClickListener {
            scale = (scale - Prefs.SCALE_STEP).coerceAtLeast(Prefs.SCALE_MIN)
            prefs.edit().putFloat(scaleKey, scale).apply()
            applyPreview()
        }

        findViewById<Button>(R.id.scaleUpButton).setOnClickListener {
            scale = (scale + Prefs.SCALE_STEP).coerceAtMost(Prefs.SCALE_MAX)
            prefs.edit().putFloat(scaleKey, scale).apply()
            applyPreview()
        }

        batterySwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(Prefs.KEY_SHOW_BATTERY, checked).apply()
            applyPreview()
        }
        dateSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(Prefs.KEY_SHOW_DATE, checked).apply()
            applyPreview()
        }
        secondsSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(Prefs.KEY_SHOW_SECONDS, checked).apply()
            applyPreview()
        }

        rotationLockSwitch = findViewById(R.id.rotationLockSwitch)
        rotationLockSwitch.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean(Prefs.KEY_IGNORE_ROTATION_LOCK, checked).apply()
        }

        rotationLockRow = findViewById(R.id.rotationLockRow)
        rotationLockRow.visibility = if (resources.configuration.orientation ==
            Configuration.ORIENTATION_LANDSCAPE) View.GONE else View.VISIBLE
    }

    private fun loadAndApply() {
        scale = prefs.getFloat(scaleKey, Prefs.SCALE_DEFAULT)
        batterySwitch.isChecked = prefs.getBoolean(Prefs.KEY_SHOW_BATTERY, true)
        dateSwitch.isChecked = prefs.getBoolean(Prefs.KEY_SHOW_DATE, true)
        secondsSwitch.isChecked = prefs.getBoolean(Prefs.KEY_SHOW_SECONDS, true)
        rotationLockSwitch.isChecked = prefs.getBoolean(Prefs.KEY_IGNORE_ROTATION_LOCK, false)
        previewBattery.setBatteryState(72, false)
        applyPreview()
    }

    private fun applyPreview() {
        scaleValueText.text = String.format("%.1fx", scale)

        // Base sizes match MainActivity's defaults -- keep these two in sync
        // if you ever change the base textSize values in activity_main.xml
        previewTime.textSize = 72f * scale
        previewDate.textSize = 28f * scale
        previewTime.text = if (secondsSwitch.isChecked) "17:26:14" else "17:26"
        previewDate.text = "Fri 17 Jul"

        previewDate.visibility = if (dateSwitch.isChecked) View.VISIBLE else View.GONE
        previewBattery.visibility = if (batterySwitch.isChecked) View.VISIBLE else View.GONE
        previewBatteryWrapper?.visibility = if (batterySwitch.isChecked) View.VISIBLE else View.GONE


        val density = resources.displayMetrics.density
        previewBattery.layoutParams = previewBattery.layoutParams.apply {
            width = (64f * scale * density).toInt()
            height = (32f * scale * density).toInt()
        }
        previewBattery.requestLayout()

        previewBatteryWrapper?.let { wrapper ->
            wrapper.layoutParams = wrapper.layoutParams.apply {
                width = (32f * scale * density).toInt()
                height = (64f * scale * density).toInt()
            }
            wrapper.requestLayout()
        }
    }
}