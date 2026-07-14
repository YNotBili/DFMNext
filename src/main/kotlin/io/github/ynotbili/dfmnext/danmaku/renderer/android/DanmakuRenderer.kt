package io.github.ynotbili.dfmnext.danmaku.renderer.android

import io.github.ynotbili.dfmnext.danmaku.model.BaseDanmaku
import io.github.ynotbili.dfmnext.danmaku.model.DanmakuTimer
import io.github.ynotbili.dfmnext.danmaku.model.IDanmakus
import io.github.ynotbili.dfmnext.danmaku.model.IDisplayer
import io.github.ynotbili.dfmnext.danmaku.model.android.DanmakuContext
import io.github.ynotbili.dfmnext.danmaku.renderer.IRenderer
import io.github.ynotbili.dfmnext.danmaku.util.SystemClock
import io.github.ynotbili.dfmnext.danmaku.util.isScrolling
import io.github.ynotbili.dfmnext.danmaku.util.isSpecial

class DanmakuRenderer(private val mContext: DanmakuContext) : IRenderer {

    private val mStartTimer = DanmakuTimer()
    private val mRenderingState = IRenderer.RenderingState()
    private var mVerifier: DanmakusRetainer.Verifier? = null
    private val verifier = object : DanmakusRetainer.Verifier {
        override fun skipLayout(danmaku: BaseDanmaku, fixedTop: Float, lines: Int, willHit: Boolean): Boolean {
            if (danmaku.priority == 0.toByte() && mContext.mDanmakuFilters.filterSecondary(danmaku, lines, 0, mStartTimer, willHit, mContext)) {
                danmaku.setVisibility(false)
                return true
            }
            return false
        }
    }
    private val mDanmakusRetainer = DanmakusRetainer()
    private var mCacheManager: ((BaseDanmaku) -> Unit)? = null
    private var mOnDanmakuShownListener: IRenderer.OnDanmakuShownListener? = null

    private val mDrawnSet = HashSet<Long>(64)
    private val mSpecialDanmakusToDraw = ArrayList<BaseDanmaku>()

    /**
     * Encode danmaku identity into a Long to avoid per-frame object allocation.
     * Uses type (4 bits) + left (16 bits) + top (16 bits) + color hash (16 bits) + size hash (12 bits).
     */
    private fun encodeKey(item: BaseDanmaku): Long {
        val type = item.getType().toLong() and 0xF
        val left = (item.getLeft().toLong() and 0xFFFF) shl 4
        val top = (item.getTop().toLong() and 0xFFFF) shl 20
        val color = (item.textColor.toLong() and 0xFFFF) shl 36
        val size = (java.lang.Float.floatToIntBits(item.textSize).toLong() and 0xFFF) shl 52
        return type or left or top or color or size
    }

    override fun clear() {
        clearRetainer()
        mContext.mDanmakuFilters.clear()
    }

    override fun clearRetainer() {
        mDanmakusRetainer.clear()
    }

    override fun release() {
        mDanmakusRetainer.release()
        mContext.mDanmakuFilters.clear()
    }

    override fun setVerifierEnabled(enabled: Boolean) {
        mVerifier = if (enabled) verifier else null
    }

    override fun draw(disp: IDisplayer, danmakus: IDanmakus, startRenderTime: Long): IRenderer.RenderingState {
        val lastTotalDanmakuCount = mRenderingState.totalDanmakuCount
        mRenderingState.reset()
        val itr = danmakus.iterator()
        var orderInScreen = 0
        mStartTimer.update(SystemClock.uptimeMillis())
        val frameStartMs = SystemClock.uptimeMillis()
        val sizeInScreen = danmakus.size()
        var drawItem: BaseDanmaku? = null
        var specialDanmakuCount = 0
        var drawnCount = 0
        mSpecialDanmakusToDraw.clear()
        mDrawnSet.clear()

        while (itr.hasNext()) {

            val item = itr.next()
            drawItem = item

            val elapsed = SystemClock.uptimeMillis() - frameStartMs
            // Hard budget: stop completely after 12ms
            if (elapsed > 12) {
                break
            }

            if (!item.hasPassedFilter()) {
                mContext.mDanmakuFilters.filter(item, orderInScreen, sizeInScreen, mStartTimer, false, mContext)
            }

            if (item.time < startRenderTime
                || (item.priority == 0.toByte() && item.isFiltered())
            ) {
                continue
            }

            if (item.isLate()) {
                if (mCacheManager != null && !item.hasDrawingCache()) {
                    mCacheManager!!.invoke(item)
                }
                break
            }

            if (item.isScrolling) {
                orderInScreen++
            } else if (item.isSpecial) {
                if (item.isOutside()) {
                    continue
                }
                specialDanmakuCount++
                if (specialDanmakuCount > 50) {
                    continue
                }
            }

            if (!item.isMeasured()) {
                item.measure(disp, false)
            }

            mDanmakusRetainer.fix(item, disp, mVerifier)

            if (!item.isOutside() && item.isShown()) {
                if (item.lines == null && item.getBottom() > disp.height) {
                    continue
                }

                // O(1) Long-based dedup (no object allocation)
                val key = encodeKey(item)
                if (!mDrawnSet.add(key)) {
                    continue
                }

                if (item.isSpecial) {
                    mSpecialDanmakusToDraw.add(item)
                    continue
                }




                try {
                    val renderingType = item.draw(disp)
                    if (renderingType == IRenderer.CACHE_RENDERING) {
                        mRenderingState.cacheHitCount++
                    } else if (renderingType == IRenderer.TEXT_RENDERING) {
                        mRenderingState.cacheMissCount++
                        if (mCacheManager != null) {
                            mCacheManager!!.invoke(item)
                        }
                    }
                } catch (_: Exception) {
                    // Skip bad danmaku, continue rendering rest
                    continue
                }
                drawnCount++
                mRenderingState.addCount(item.getType(), 1)
                mRenderingState.addTotalCount(1)

                if (mOnDanmakuShownListener != null
                    && item.firstShownFlag != mContext.mGlobalFlagValues.FIRST_SHOWN_RESET_FLAG
                ) {
                    item.firstShownFlag = mContext.mGlobalFlagValues.FIRST_SHOWN_RESET_FLAG
                    mOnDanmakuShownListener!!.onDanmakuShown(item)
                }
            }

        }

        for (i in 0 until mSpecialDanmakusToDraw.size) {
            val specialItem = mSpecialDanmakusToDraw[i]
            try {
                val renderingType = specialItem.draw(disp)
                if (renderingType == IRenderer.CACHE_RENDERING) {
                    mRenderingState.cacheHitCount++
                } else if (renderingType == IRenderer.TEXT_RENDERING) {
                    mRenderingState.cacheMissCount++
                    if (mCacheManager != null) {
                        mCacheManager!!.invoke(specialItem)
                    }
                }
            } catch (_: Exception) {
                continue
            }
            mRenderingState.addCount(specialItem.getType(), 1)
            mRenderingState.addTotalCount(1)

            if (mOnDanmakuShownListener != null
                && specialItem.firstShownFlag != mContext.mGlobalFlagValues.FIRST_SHOWN_RESET_FLAG
            ) {
                specialItem.firstShownFlag = mContext.mGlobalFlagValues.FIRST_SHOWN_RESET_FLAG
                mOnDanmakuShownListener!!.onDanmakuShown(specialItem)
            }
        }

        mRenderingState.nothingRendered = (mRenderingState.totalDanmakuCount == 0)
        mRenderingState.endTime = drawItem?.time ?: IRenderer.RenderingState.UNKNOWN_TIME
        if (mRenderingState.nothingRendered) {
            mRenderingState.beginTime = IRenderer.RenderingState.UNKNOWN_TIME
        }
        mRenderingState.incrementCount = mRenderingState.totalDanmakuCount - lastTotalDanmakuCount
        mRenderingState.consumingTime = mStartTimer.update(SystemClock.uptimeMillis())
        return mRenderingState
    }

    override fun setCacheManager(addDanmaku: ((BaseDanmaku) -> Unit)?) {
        mCacheManager = addDanmaku
    }

    override fun setOnDanmakuShownListener(listener: IRenderer.OnDanmakuShownListener) {
        mOnDanmakuShownListener = listener
    }

    override fun removeOnDanmakuShownListener() {
        mOnDanmakuShownListener = null
    }
}
