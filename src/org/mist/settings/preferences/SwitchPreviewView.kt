/*
 * SPDX-FileCopyrightText: MistOS
 * SPDX-License-Identifier: Apache-2.0
 */

package org.mist.settings.preferences

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import com.android.settings.R
import com.android.settingslib.widget.SwitchThumbIconDrawable

/**
 * A non-interactive Material 3 Expressive switch used by the switch icon preview.
 *
 * It uses the same colors as the system expressive switch and draws the ON icon through
 * [SwitchThumbIconDrawable], so what is previewed is what real switches show. It does not depend on
 * any Material theme attribute, which the Settings theme does not provide.
 */
class SwitchPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    var isOn = false
        set(value) {
            field = value
            invalidate()
        }

    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    private val trackOn = context.getColor(R.color.switch_icon_track_on)
    private val thumbOn = context.getColor(R.color.switch_icon_thumb_on)
    private val trackOff = context.getColor(R.color.switch_icon_track_off)
    private val outlineOff = context.getColor(R.color.switch_icon_outline_off)

    private var onIcon: Drawable? = null
    private val offIcon: Drawable? =
        context.getDrawable(R.drawable.ic_switch_icon_close)?.mutate()?.apply { setTint(trackOff) }

    /** Shows [bitmap] in the thumb, or the stock check mark when it is null. */
    fun setOnIcon(bitmap: Bitmap?) {
        onIcon = SwitchThumbIconDrawable.createPreview(context, bitmap).apply { setTint(trackOn) }
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            resolveSize((WIDTH_DP * density).toInt(), widthMeasureSpec),
            resolveSize((HEIGHT_DP * density).toInt(), heightMeasureSpec),
        )
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val stroke = STROKE_DP * density

        // Track
        paint.style = Paint.Style.FILL
        paint.color = if (isOn) trackOn else trackOff
        rect.set(0f, 0f, w, h)
        canvas.drawRoundRect(rect, h / 2f, h / 2f, paint)
        if (!isOn) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = stroke
            paint.color = outlineOff
            rect.set(stroke / 2f, stroke / 2f, w - stroke / 2f, h - stroke / 2f)
            canvas.drawRoundRect(rect, h / 2f, h / 2f, paint)
        }

        // Thumb
        val thumbRadius = h * THUMB_RATIO / 2f
        val cx = if (isOn) w - h / 2f else h / 2f
        val cy = h / 2f
        paint.style = Paint.Style.FILL
        paint.color = if (isOn) thumbOn else outlineOff
        canvas.drawCircle(cx, cy, thumbRadius, paint)

        // Icon inside the thumb
        val half = thumbRadius * ICON_RATIO
        (if (isOn) onIcon else offIcon)?.let {
            it.setBounds((cx - half).toInt(), (cy - half).toInt(), (cx + half).toInt(), (cy + half).toInt())
            it.draw(canvas)
        }
    }

    private companion object {
        const val WIDTH_DP = 88f
        const val HEIGHT_DP = 56f
        const val STROKE_DP = 3f
        const val THUMB_RATIO = 0.75f
        const val ICON_RATIO = 0.66f
    }
}
