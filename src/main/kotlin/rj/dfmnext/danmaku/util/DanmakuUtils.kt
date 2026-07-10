package rj.dfmnext.danmaku.util

import rj.dfmnext.danmaku.model.AbsDisplayer
import rj.dfmnext.danmaku.model.BaseDanmaku
import rj.dfmnext.danmaku.model.IDisplayer
import rj.dfmnext.danmaku.model.android.DrawingCache
import rj.dfmnext.danmaku.model.android.DrawingCacheHolder

object DanmakuUtils {

    fun willHitInDuration(
        disp: IDisplayer, d1: BaseDanmaku, d2: BaseDanmaku,
        duration: Long, currTime: Long
    ): Boolean {
        val type1 = d1.getType()
        val type2 = d2.getType()
        // allow hit if different type
        if (type1 != type2) return false

        if (d1.isOutside()) return false

        val dTime = d2.time - d1.time
        if (dTime <= 0) return true
        if (Math.abs(dTime) >= duration || d1.isTimeOut() || d2.isTimeOut()) return false

        if (type1 == BaseDanmaku.TYPE_FIX_TOP || type1 == BaseDanmaku.TYPE_FIX_BOTTOM) return true

        return checkHitAtTime(disp, d1, d2, currTime) ||
                checkHitAtTime(disp, d1, d2, d1.time + d1.getDuration())
    }

    private fun checkHitAtTime(
        disp: IDisplayer, d1: BaseDanmaku, d2: BaseDanmaku, time: Long
    ): Boolean {
        val rectArr1 = d1.getRectAtTime(disp, time) ?: return false
        val rectArr2 = d2.getRectAtTime(disp, time) ?: return false
        return checkHit(d1.getType(), d2.getType(), rectArr1, rectArr2)
    }

    private fun checkHit(type1: Int, type2: Int, rectArr1: FloatArray, rectArr2: FloatArray): Boolean {
        if (type1 != type2) return false
        if (type1 == BaseDanmaku.TYPE_SCROLL_RL) {
            return rectArr2[0] < rectArr1[2]
        }
        if (type1 == BaseDanmaku.TYPE_SCROLL_LR) {
            return rectArr2[2] > rectArr1[0]
        }
        return false
    }

    fun buildDanmakuDrawingCache(
        danmaku: BaseDanmaku, disp: IDisplayer, cache: DrawingCache?
    ): DrawingCache {
        val drawingCache = cache ?: DrawingCache()
        drawingCache.build(
            Math.ceil(danmaku.paintWidth.toDouble()).toInt(),
            Math.ceil(danmaku.paintHeight.toDouble()).toInt(),
            disp.densityDpi,
            false
        )
        val holder = drawingCache.get()
        (disp as AbsDisplayer).drawDanmaku(danmaku, holder.canvas!!, 0f, 0f, true)
        if (disp.isHardwareAccelerated) {
            holder.splitWith(
                disp.width, disp.height,
                disp.maximumCacheWidth, disp.maximumCacheHeight
            )
        }
        return drawingCache
    }

    fun getCacheSize(w: Int, h: Int): Int = w * h * 4

    fun isDuplicate(obj1: BaseDanmaku, obj2: BaseDanmaku): Boolean {
        if (obj1 === obj2) return false
        if (obj1.text == null || obj2.text == null) return false
        return obj1.text === obj2.text || obj1.text == obj2.text
    }

    fun compare(obj1: BaseDanmaku, obj2: BaseDanmaku): Int {
        if (obj1 === obj2) return 0

        val timeVal = obj1.time - obj2.time
        if (timeVal > 0) return 1 else if (timeVal < 0) return -1

        val indexResult = obj1.index - obj2.index
        if (indexResult > 0) return 1 else if (indexResult < 0) return -1

        val typeResult = obj1.getType() - obj2.getType()
        if (typeResult > 0) return 1 else if (typeResult < 0) return -1

        if (obj1.text == null) return -1
        if (obj2.text == null) return 1

        val r = obj1.text.toString().compareTo(obj2.text.toString())
        if (r != 0) return r

        val colorDiff = obj1.textColor - obj2.textColor
        if (colorDiff != 0) return if (colorDiff < 0) -1 else 1

        val idxDiff = obj1.index - obj2.index
        if (idxDiff != 0) return if (idxDiff < 0) -1 else 1

        return obj1.hashCode() - obj2.hashCode()
    }

    fun isOverSize(disp: IDisplayer, item: BaseDanmaku): Boolean {
        return disp.isHardwareAccelerated &&
                (item.paintWidth > disp.maximumCacheWidth || item.paintHeight > disp.maximumCacheHeight)
    }

    fun fillText(danmaku: BaseDanmaku, text: CharSequence?) {
        danmaku.text = text
        if (text.isNullOrEmpty() || !text.toString().contains(BaseDanmaku.DANMAKU_BR_CHAR)) return

        val lines = danmaku.text.toString().split(BaseDanmaku.DANMAKU_BR_CHAR).toTypedArray()
        if (lines.size > 1) {
            danmaku.lines = lines
        }
    }
}
