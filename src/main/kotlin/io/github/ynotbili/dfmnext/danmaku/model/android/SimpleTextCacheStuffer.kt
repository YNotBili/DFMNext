package io.github.ynotbili.dfmnext.danmaku.model.android

import android.graphics.Canvas
import android.graphics.Paint
import android.text.TextPaint
import io.github.ynotbili.dfmnext.danmaku.model.BaseDanmaku

open class SimpleTextCacheStuffer : BaseCacheStuffer() {

    companion object {
        private val sTextHeightCache = HashMap<Float, Float>()
    }

    protected fun getCacheHeight(danmaku: BaseDanmaku, paint: Paint): Float {
        val textSize = paint.textSize
        return sTextHeightCache.getOrPut(textSize) {
            val fontMetrics = paint.fontMetrics
            fontMetrics.descent - fontMetrics.ascent + fontMetrics.leading
        }
    }

    override fun measure(danmaku: BaseDanmaku, paint: TextPaint, fromWorkerThread: Boolean) {
        mProxy?.prepareDrawing(danmaku, fromWorkerThread)

        var w = 0f
        var textHeight = 0f
        if (danmaku.lines == null) {
            if (danmaku.text == null) {
                w = 0f
            } else {
                w = paint.measureText(danmaku.text.toString())
                textHeight = getCacheHeight(danmaku, paint)
            }
            danmaku.paintWidth = w
            danmaku.paintHeight = textHeight
        } else {
            textHeight = getCacheHeight(danmaku, paint)
            for (tempStr in danmaku.lines!!) {
                if (tempStr.isNotEmpty()) {
                    val tr = paint.measureText(tempStr)
                    w = Math.max(tr, w)
                }
            }
            danmaku.paintWidth = w
            danmaku.paintHeight = danmaku.lines!!.size * textHeight
        }
    }

    override fun drawStroke(danmaku: BaseDanmaku, lineText: String?, canvas: Canvas, left: Float, top: Float, paint: Paint) {
        if (lineText != null) {
            canvas.drawText(lineText, left, top, paint)
        } else {
            canvas.drawText(danmaku.text.toString(), left, top, paint)
        }
    }

    override fun drawText(danmaku: BaseDanmaku, lineText: String?, canvas: Canvas, left: Float, top: Float, paint: TextPaint, fromWorkerThread: Boolean) {
        if (lineText != null) {
            canvas.drawText(lineText, left, top, paint)
        } else {
            canvas.drawText(danmaku.text.toString(), left, top, paint)
        }
    }

    override fun clearCaches() {
        sTextHeightCache.clear()
    }

    override fun drawBackground(danmaku: BaseDanmaku, canvas: Canvas, left: Float, top: Float) {}
}
