package net.pantasystem.milktea.common_android_ui

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.ReplacementSpan
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * MFM の `$[rotate.deg=30 ...]` 構文用 Span。
 * テキストを指定角度だけ回転して描画する。
 * getSize() では回転後のバウンディングボックス幅を近似計算する。
 */
internal class MfmRotateSpan(private val degrees: Float) : ReplacementSpan() {

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
        val w = paint.measureText(text, start, end)
        val rad = Math.toRadians(degrees.toDouble())
        // 回転後の水平方向占有幅を近似
        return (w * abs(cos(rad)) + paint.textSize * abs(sin(rad))).roundToInt()
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
        canvas.rotate(degrees, cx, cy)
        canvas.drawText(text, start, end, x, y.toFloat(), paint)
        canvas.restore()
    }
}
