package io.github.ynotbili.dfmnext.danmaku.renderer

import io.github.ynotbili.dfmnext.danmaku.model.BaseDanmaku
import io.github.ynotbili.dfmnext.danmaku.model.IDanmakus
import io.github.ynotbili.dfmnext.danmaku.model.IDisplayer

interface IRenderer {

    interface OnDanmakuShownListener {
        fun onDanmakuShown(danmaku: BaseDanmaku)
    }

    class Area {
        val mRefreshRect = FloatArray(4)
        private var mMaxHeight = 0
        private var mMaxWidth = 0

        fun setEdge(maxWidth: Int, maxHeight: Int) {
            mMaxWidth = maxWidth
            mMaxHeight = maxHeight
        }

        fun reset() {
            set(mMaxWidth.toFloat(), mMaxHeight.toFloat(), 0f, 0f)
        }

        fun resizeToMax() {
            set(0f, 0f, mMaxWidth.toFloat(), mMaxHeight.toFloat())
        }

        fun set(left: Float, top: Float, right: Float, bottom: Float) {
            mRefreshRect[0] = left
            mRefreshRect[1] = top
            mRefreshRect[2] = right
            mRefreshRect[3] = bottom
        }
    }

    class RenderingState {
        var r2lDanmakuCount: Int = 0
        var l2rDanmakuCount: Int = 0
        var ftDanmakuCount: Int = 0
        var fbDanmakuCount: Int = 0
        var specialDanmakuCount: Int = 0
        var totalDanmakuCount: Int = 0
        var incrementCount: Int = 0
        var consumingTime: Long = 0
        var beginTime: Long = 0
        var endTime: Long = 0
        var nothingRendered: Boolean = false
        var sysTime: Long = 0
        var cacheHitCount: Long = 0
        var cacheMissCount: Long = 0

        fun addTotalCount(count: Int): Int {
            totalDanmakuCount += count
            return totalDanmakuCount
        }

        fun addCount(type: Int, count: Int): Int = when (type) {
            BaseDanmaku.TYPE_SCROLL_RL -> { r2lDanmakuCount += count; r2lDanmakuCount }
            BaseDanmaku.TYPE_SCROLL_LR -> { l2rDanmakuCount += count; l2rDanmakuCount }
            BaseDanmaku.TYPE_FIX_TOP -> { ftDanmakuCount += count; ftDanmakuCount }
            BaseDanmaku.TYPE_FIX_BOTTOM -> { fbDanmakuCount += count; fbDanmakuCount }
            BaseDanmaku.TYPE_SPECIAL -> { specialDanmakuCount += count; specialDanmakuCount }
            else -> 0
        }

        fun reset() {
            r2lDanmakuCount = 0; l2rDanmakuCount = 0; ftDanmakuCount = 0
            fbDanmakuCount = 0; specialDanmakuCount = 0; totalDanmakuCount = 0
            sysTime = 0; beginTime = 0; endTime = 0; consumingTime = 0
            nothingRendered = false
        }

        fun set(other: RenderingState?) {
            other ?: return
            r2lDanmakuCount = other.r2lDanmakuCount
            l2rDanmakuCount = other.l2rDanmakuCount
            ftDanmakuCount = other.ftDanmakuCount
            fbDanmakuCount = other.fbDanmakuCount
            specialDanmakuCount = other.specialDanmakuCount
            totalDanmakuCount = other.totalDanmakuCount
            incrementCount = other.incrementCount
            consumingTime = other.consumingTime
            beginTime = other.beginTime
            endTime = other.endTime
            nothingRendered = other.nothingRendered
            sysTime = other.sysTime
            cacheHitCount = other.cacheHitCount
            cacheMissCount = other.cacheMissCount
        }

        companion object {
            const val UNKNOWN_TIME = -1L
        }
    }

    fun draw(disp: IDisplayer, danmakus: IDanmakus, startRenderTime: Long): RenderingState
    fun clear()
    fun clearRetainer()
    fun release()
    fun setVerifierEnabled(enabled: Boolean)
    fun setCacheManager(addDanmaku: ((BaseDanmaku) -> Unit)?)
    fun setOnDanmakuShownListener(listener: OnDanmakuShownListener)
    fun removeOnDanmakuShownListener()

    companion object {
        const val NOTHING_RENDERING = 0
        const val CACHE_RENDERING = 1
        const val TEXT_RENDERING = 2
    }
}
