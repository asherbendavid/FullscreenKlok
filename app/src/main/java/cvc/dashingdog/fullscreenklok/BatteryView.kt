package cvc.dashingdog.fullscreenklok

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

/**
 * Simple battery indicator: outline body + terminal nub, fill proportional
 * to charge level, colour thresholds matching Vaart's status styling.
 * Pulses (alpha breathing) while charging.
 *
 * Not clickable/focusable by design -- this app has no touch targets.
 */
class BatteryView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var level: Int = 100
    private var charging: Boolean = false

    private var currentAlpha: Int = 255
    private var pulseAnimator: ValueAnimator? = null

    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        color = ContextCompat.getColor(context, R.color.battery_outline)
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    init {
        isClickable = false
        isFocusable = false
        isLongClickable = false
    }

    fun setBatteryState(newLevel: Int, isCharging: Boolean) {
        level = newLevel.coerceIn(0, 100)
        charging = isCharging
        val shouldPulse = charging || level < 25
        val pulseChanged = shouldPulse != (pulseAnimator != null)
        invalidate()
        if (pulseChanged) {
            if (shouldPulse) startPulse() else stopPulse()
        }
    }

    private fun startPulse() {
        pulseAnimator?.cancel()
        pulseAnimator = ValueAnimator.ofInt(255, 90, 255).apply {
            duration = 1600
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                currentAlpha = it.animatedValue as Int
                invalidate()
            }
            start()
        }
    }

    private fun stopPulse() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        currentAlpha = 255
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopPulse()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val nubWidth = height * 0.12f
        val bodyRight = width - nubWidth
        val strokeInset = outlinePaint.strokeWidth / 2f

        // Battery body outline
        val bodyRect = RectF(
            strokeInset,
            strokeInset,
            bodyRight,
            height - strokeInset
        )
        val corner = height * 0.15f
        canvas.drawRoundRect(bodyRect, corner, corner, outlinePaint)

        // Terminal nub
        val nubTop = height * 0.28f
        val nubBottom = height * 0.72f
        canvas.drawRect(bodyRight, nubTop, bodyRight + nubWidth - strokeInset, nubBottom, outlinePaint)

// Fill
        val fillColor = when {
            charging -> ContextCompat.getColor(context, R.color.battery_charging)
            level > 75 -> ContextCompat.getColor(context, R.color.battery_good)
            level > 50 -> ContextCompat.getColor(context, R.color.battery_mid)
            else -> ContextCompat.getColor(context, R.color.battery_low) // covers >25 red and <25 pulsating red
        }
        fillPaint.color = fillColor
        fillPaint.alpha = if (charging || level < 25) currentAlpha else 255

        val padding = height * 0.22f
        val fillMaxWidth = bodyRect.width() - padding * 2
        val fillWidth = fillMaxWidth * (level / 100f)
        val fillRect = RectF(
            bodyRect.left + padding,
            bodyRect.top + padding,
            bodyRect.left + padding + fillWidth,
            bodyRect.bottom - padding
        )
        val fillCorner = corner * 0.6f
        canvas.drawRoundRect(fillRect, fillCorner, fillCorner, fillPaint)
    }
}
