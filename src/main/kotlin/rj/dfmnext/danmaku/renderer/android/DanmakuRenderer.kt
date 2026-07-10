package rj.dfmnext.danmaku.renderer.android

import rj.dfmnext.danmaku.model.BaseDanmaku
import rj.dfmnext.danmaku.model.DanmakuTimer
import rj.dfmnext.danmaku.model.IDanmakus
import rj.dfmnext.danmaku.model.IDisplayer
import rj.dfmnext.danmaku.model.android.DanmakuContext
import rj.dfmnext.danmaku.renderer.IRenderer
import rj.dfmnext.danmaku.util.SystemClock

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

    private val mLastDrawnDanmakus = arrayOfNulls<BaseDanmaku>(15)
    private var mLastDrawnIndex = 0
    private val mSpecialDanmakusToDraw = ArrayList<BaseDanmaku>()

    private fun isExactDuplicate(a: BaseDanmaku, b: BaseDanmaku): Boolean {
        if (a === b) return false
        if (a.getType() != b.getType()) return false
        if (a.getLeft().compareTo(b.getLeft()) != 0) return false
        if (a.getTop().compareTo(b.getTop()) != 0) return false
        if (a.textColor != b.textColor) return false
        if (a.textSize != b.textSize) return false
        if (a.text == null || a.text != b.text) return false
        return true
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
        val sizeInScreen = danmakus.size()
        var drawItem: BaseDanmaku? = null
        var specialDanmakuCount = 0
        mSpecialDanmakusToDraw.clear()

        for (i in mLastDrawnDanmakus.indices) {
            mLastDrawnDanmakus[i] = null
        }
        mLastDrawnIndex = 0

        while (itr.hasNext()) {

            val item = itr.next() ?: continue
            drawItem = item

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

            if (item.getType() == BaseDanmaku.TYPE_SCROLL_RL
                || item.getType() == BaseDanmaku.TYPE_SCROLL_LR
            ) {
                orderInScreen++
                if (orderInScreen > 150) {
                    continue
                }
            } else if (item.getType() == BaseDanmaku.TYPE_SPECIAL) {
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

                var isDuplicate = false
                for (cachedItem in mLastDrawnDanmakus) {
                    if (cachedItem != null && isExactDuplicate(cachedItem, item)) {
                        isDuplicate = true
                        break
                    }
                }
                if (isDuplicate) {
                    continue
                }

                mLastDrawnDanmakus[mLastDrawnIndex] = item
                mLastDrawnIndex = (mLastDrawnIndex + 1) % mLastDrawnDanmakus.size

                if (item.getType() == BaseDanmaku.TYPE_SPECIAL) {
                    mSpecialDanmakusToDraw.add(item)
                    continue
                }

                val renderingType = item.draw(disp)
                if (renderingType == IRenderer.CACHE_RENDERING) {
                    mRenderingState.cacheHitCount++
                } else if (renderingType == IRenderer.TEXT_RENDERING) {
                    mRenderingState.cacheMissCount++
                    if (mCacheManager != null) {
                        mCacheManager!!.invoke(item)
                    }
                }
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
            val renderingType = specialItem.draw(disp)
            if (renderingType == IRenderer.CACHE_RENDERING) {
                mRenderingState.cacheHitCount++
            } else if (renderingType == IRenderer.TEXT_RENDERING) {
                mRenderingState.cacheMissCount++
                if (mCacheManager != null) {
                    mCacheManager!!.invoke(specialItem)
                }
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
