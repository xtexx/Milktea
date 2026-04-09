package net.pantasystem.milktea.common_android_ui

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.ReplacementSpan
import kotlin.math.roundToInt

/**
 * MFM の `$[flip ...]` / `$[flip.v ...]` / `$[flip.h,v ...]` 構文用 Span。
 * Canvas を反転させてテキストを描画する。
 */
internal class MfmFlipSpan(
    private val horizontal: Boolean,
    private val vertical: Boolean,
) : ReplacementSpan() {

    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?,
    ): Int {
        if (fm != null) {
            paint.getFontMetricsInt(fm)
        }
        return paint.measureText(text, start, end).roundToInt()
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint,
    ) {
        val w = paint.measureText(text, start, end)
        val cx = x + w / 2f
        val cy = (top + bottom) / 2f
        canvas.save()
        canvas.scale(
            if (horizontal) -1f else 1f,
            if (vertical) -1f else 1f,
            cx, cy,
        )
        canvas.drawText(text, start, end, x, y.toFloat(), paint)
        canvas.restore()
    }
}
