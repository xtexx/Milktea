package net.pantasystem.milktea.common_android_ui

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.text.style.ReplacementSpan
import kotlin.math.roundToInt

/**
 * MFM の `$[border ...]` 構文用 Span。
 * テキストの周囲に枠線を描画する。
 *
 * サポートする style: solid / dotted / dashed / double
 * (groove / ridge / inset / outset は solid にフォールバック)
 */
internal class MfmBorderSpan(
    private val style: String,
    private val strokeWidth: Float,
    private val color: Int,
    private val radius: Float,
) : ReplacementSpan() {

    private val padding get() = strokeWidth + CONTENT_PADDING

    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?,
    ): Int {
        if (fm != null) {
            paint.getFontMetricsInt(fm)
            val extra = (padding * 2).roundToInt()
            fm.top    -= extra
            fm.ascent -= extra
            fm.bottom += extra
            fm.descent += extra
        }
        return (paint.measureText(text, start, end) + padding * 2).roundToInt()
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
        val textWidth = paint.measureText(text, start, end)
        val left   = x
        val right  = x + textWidth + padding * 2
        val rectTop    = top.toFloat()
        val rectBottom = bottom.toFloat()

        when (style) {
            "double" -> drawDouble(canvas, left, rectTop, right, rectBottom, paint)
            else     -> drawSingle(canvas, left, rectTop, right, rectBottom, paint)
        }

        // テキストを枠内に描画（左パディング分オフセット）
        canvas.drawText(text, start, end, x + padding, y.toFloat(), paint)
    }

    private fun borderPaint(base: Paint): Paint = Paint(base).apply {
        style  = Paint.Style.STROKE
        color  = this@MfmBorderSpan.color
        strokeWidth = this@MfmBorderSpan.strokeWidth
        pathEffect = when (this@MfmBorderSpan.style) {
            "dotted" -> DashPathEffect(floatArrayOf(strokeWidth * 2, strokeWidth * 2), 0f)
            "dashed" -> DashPathEffect(floatArrayOf(strokeWidth * 4, strokeWidth * 2), 0f)
            else     -> null
        }
    }

    private fun drawSingle(
        canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, base: Paint,
    ) {
        canvas.drawRoundRect(RectF(left, top, right, bottom), radius, radius, borderPaint(base))
    }

    private fun drawDouble(
        canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float, base: Paint,
    ) {
        val p = borderPaint(base)
        val gap = strokeWidth * 1.5f
        canvas.drawRoundRect(RectF(left, top, right, bottom), radius, radius, p)
        canvas.drawRoundRect(
            RectF(left + gap, top + gap, right - gap, bottom - gap),
            (radius - gap).coerceAtLeast(0f), (radius - gap).coerceAtLeast(0f), p
        )
    }

    companion object {
        private const val CONTENT_PADDING = 4f
    }
}
