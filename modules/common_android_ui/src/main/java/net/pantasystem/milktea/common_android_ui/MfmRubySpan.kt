package net.pantasystem.milktea.common_android_ui

import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.ReplacementSpan
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * MFM の `$[ruby 本文 ルビ]` 構文用 Span。
 * 本文の上部にルビを小さく描画する。
 */
internal class MfmRubySpan(
    private val baseText: String,
    private val rubyText: String,
) : ReplacementSpan() {

    companion object {
        private const val RUBY_SIZE_RATIO = 0.5f
    }

    override fun getSize(
        paint: Paint,
        text: CharSequence,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?,
    ): Int {
        val baseWidth = paint.measureText(baseText)
        val rubyPaint = rubyPaint(paint)
        val rubyWidth = rubyPaint.measureText(rubyText)

        if (fm != null) {
            val rubyMetrics = Paint.FontMetricsInt()
            rubyPaint.getFontMetricsInt(rubyMetrics)
            val rubyLineHeight = rubyMetrics.descent - rubyMetrics.ascent

            // ルビ分だけ行の上部に余白を追加
            fm.top    = (fm.top    - rubyLineHeight)
            fm.ascent = (fm.ascent - rubyLineHeight)
        }

        return max(baseWidth, rubyWidth).roundToInt()
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
        val baseWidth = paint.measureText(baseText)
        val rubyPaint = rubyPaint(paint)
        val rubyWidth = rubyPaint.measureText(rubyText)
        val totalWidth = max(baseWidth, rubyWidth)

        // ルビ: 本文の上に中央揃えで描画
        val rubyX = x + (totalWidth - rubyWidth) / 2f
        val rubyMetrics = Paint.FontMetricsInt()
        rubyPaint.getFontMetricsInt(rubyMetrics)
        val rubyY = y + paint.ascent() - rubyMetrics.descent
        canvas.drawText(rubyText, rubyX, rubyY, rubyPaint)

        // 本文: 中央揃えで描画
        val baseX = x + (totalWidth - baseWidth) / 2f
        canvas.drawText(baseText, baseX, y.toFloat(), paint)
    }

    private fun rubyPaint(base: Paint): Paint = Paint(base).apply {
        textSize *= RUBY_SIZE_RATIO
    }
}
