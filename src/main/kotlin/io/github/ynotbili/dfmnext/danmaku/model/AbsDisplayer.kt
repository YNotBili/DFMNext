package io.github.ynotbili.dfmnext.danmaku.model

import android.graphics.Canvas
import android.graphics.Typeface
import io.github.ynotbili.dfmnext.danmaku.model.android.BaseCacheStuffer

abstract class AbsDisplayer : IDisplayer {

    abstract fun getExtraData(): Canvas?
    abstract fun setExtraData(data: Canvas)

    override val isHardwareAccelerated: Boolean get() = false

    abstract fun drawDanmaku(danmaku: BaseDanmaku, canvas: Canvas, left: Float, top: Float, quickly: Boolean)
    abstract fun clearTextHeightCache()
    abstract fun setTypeFace(font: Typeface)
    abstract fun setFakeBoldText(bold: Boolean)
    abstract fun setTransparency(newTransparency: Int)
    abstract fun setScaleTextSizeFactor(factor: Float)
    abstract fun setCacheStuffer(cacheStuffer: BaseCacheStuffer)
    abstract fun getCacheStuffer(): BaseCacheStuffer
}
