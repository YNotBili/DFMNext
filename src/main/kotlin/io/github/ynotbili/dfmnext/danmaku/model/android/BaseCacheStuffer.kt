package io.github.ynotbili.dfmnext.danmaku.model.android

import android.graphics.Canvas
import android.graphics.Paint
import android.text.TextPaint
import io.github.ynotbili.dfmnext.danmaku.model.BaseDanmaku

abstract class BaseCacheStuffer {

    abstract class Proxy {
        abstract fun prepareDrawing(danmaku: BaseDanmaku, fromWorkerThread: Boolean)
        abstract fun releaseResource(danmaku: BaseDanmaku)
    }

    protected var mProxy: Proxy? = null

    abstract fun measure(danmaku: BaseDanmaku, paint: TextPaint, fromWorkerThread: Boolean)

    abstract fun drawStroke(danmaku: BaseDanmaku, lineText: String?, canvas: Canvas, left: Float, top: Float, paint: Paint)

    abstract fun drawText(danmaku: BaseDanmaku, lineText: String?, canvas: Canvas, left: Float, top: Float, paint: TextPaint, fromWorkerThread: Boolean)

    abstract fun clearCaches()

    abstract fun drawBackground(danmaku: BaseDanmaku, canvas: Canvas, left: Float, top: Float)

    open fun clearCache(danmaku: BaseDanmaku) {}

    fun setProxy(adapter: Proxy?) {
        mProxy = adapter
    }

    open fun releaseResource(danmaku: BaseDanmaku) {
        mProxy?.releaseResource(danmaku)
    }
}
