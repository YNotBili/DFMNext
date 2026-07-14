package io.github.ynotbili.dfmnext.controller

import android.graphics.Canvas
import io.github.ynotbili.dfmnext.danmaku.model.AbsDisplayer
import io.github.ynotbili.dfmnext.danmaku.model.BaseDanmaku
import io.github.ynotbili.dfmnext.danmaku.model.DanmakuTimer
import io.github.ynotbili.dfmnext.danmaku.model.IDanmakus
import io.github.ynotbili.dfmnext.danmaku.model.android.DanmakuContext
import io.github.ynotbili.dfmnext.danmaku.model.android.DanmakuContext.ConfigChangedCallback
import io.github.ynotbili.dfmnext.danmaku.model.android.DanmakuContext.DanmakuConfigTag
import io.github.ynotbili.dfmnext.danmaku.model.android.Danmakus
import io.github.ynotbili.dfmnext.danmaku.parser.BaseDanmakuParser
import io.github.ynotbili.dfmnext.danmaku.renderer.IRenderer
import io.github.ynotbili.dfmnext.danmaku.renderer.IRenderer.RenderingState
import io.github.ynotbili.dfmnext.danmaku.renderer.android.DanmakuRenderer
import io.github.ynotbili.dfmnext.danmaku.util.SystemClock
import io.github.ynotbili.dfmnext.danmaku.util.clearCanvas

open class DrawTask(
    timer: DanmakuTimer,
    protected val mContext: DanmakuContext,
    protected var mTaskListener: IDrawTask.TaskListener?
) : IDrawTask {

    protected val mDisp: AbsDisplayer = mContext.getDisplayer()

    protected var danmakuList: IDanmakus? = null

    protected var mParser: BaseDanmakuParser? = null

    val mRenderer: IRenderer = DanmakuRenderer(mContext)

    var mTimer: DanmakuTimer = timer
        protected set

    private var danmakus: IDanmakus = Danmakus(Danmakus.ST_BY_LIST)

    protected var clearRetainerFlag: Boolean = false

    private var mStartRenderTime: Long = 0

    private var mRenderingState = RenderingState()

    protected var mReadyState: Boolean = false

    private var mLastBeginMills: Long = 0

    private var mLastEndMills: Long = 0

    private var mIsHidden: Boolean = false

    private var mLastDanmaku: BaseDanmaku? = null

    private val mLiveDanmakus = Danmakus(Danmakus.ST_BY_LIST)

    private val mConfigChangedCallback = object : ConfigChangedCallback {
        override fun onDanmakuConfigChanged(config: DanmakuContext, tag: DanmakuConfigTag, vararg value: Any?): Boolean {
            return onDanmakuConfigChanged(config, tag, *value)
        }
    }

    init {
        mRenderer.setOnDanmakuShownListener(object : IRenderer.OnDanmakuShownListener {
            override fun onDanmakuShown(danmaku: BaseDanmaku) {
                mTaskListener?.onDanmakuShown(danmaku)
            }
        })
        mRenderer.setVerifierEnabled(mContext.isPreventOverlappingEnabled() || mContext.isMaxLinesLimited())
        initTimer(timer)
        val enable = mContext.isDuplicateMergingEnabled()
        if (enable) {
            mContext.mDanmakuFilters.registerFilter(DanmakuFilters.TAG_DUPLICATE_FILTER)
        } else {
            mContext.mDanmakuFilters.unregisterFilter(DanmakuFilters.TAG_DUPLICATE_FILTER)
        }
    }

    protected open fun initTimer(timer: DanmakuTimer) {
        mTimer = timer
    }

    @Synchronized
    override fun addDanmaku(item: BaseDanmaku) {
        if (danmakuList == null) return
        if (item.isLive) {
            mLiveDanmakus += item
            removeUnusedLiveDanmakusIn(10)
        }
        item.index = danmakuList!!.size()
        var subAdded = true
        if (item.time in mLastBeginMills..mLastEndMills) {
            @Suppress("ReplaceCallWithOperatorAssignment")
            subAdded = danmakus.addItem(item)
        } else if (item.isLive) {
            subAdded = false
        }
        @Suppress("ReplaceCallWithOperatorAssignment")
        val added = danmakuList!!.addItem(item)
        if (!subAdded) {
            mLastBeginMills = 0
            mLastEndMills = 0
        }
        if (added && mTaskListener != null) {
            mTaskListener!!.onDanmakuAdd(item)
        }
        if (mLastDanmaku == null || (item.time > mLastDanmaku!!.time)) {
            mLastDanmaku = item
        }
    }

    override fun invalidateDanmaku(item: BaseDanmaku, remeasure: Boolean) {
        mContext.getDisplayer().getCacheStuffer().clearCache(item)
        if (remeasure) {
            item.paintWidth = -1f
            item.paintHeight = -1f
        }
    }

    @Synchronized
    override fun removeAllDanmakus(isClearDanmakusOnScreen: Boolean) {
        if (danmakuList == null || danmakuList!!.isEmpty()) return
        if (!isClearDanmakusOnScreen) {
            val beginMills = mTimer.currMillisecond - mContext.mDanmakuFactory.MAX_DANMAKU_DURATION - 100
            val endMills = mTimer.currMillisecond + mContext.mDanmakuFactory.MAX_DANMAKU_DURATION
            val tempDanmakus = danmakuList!!.subnew(beginMills, endMills)
            danmakus = tempDanmakus
        }
        danmakuList!!.clear()
    }

    protected open fun onDanmakuRemoved(danmaku: BaseDanmaku) {
        // TODO call callback here
    }

    @Synchronized
    override fun removeAllLiveDanmakus() {
        if (danmakus.isEmpty()) return
        val it = danmakus.iterator()
        while (it.hasNext()) {
            val danmaku = it.next()
            if (danmaku.isLive) {
                it.remove()
                onDanmakuRemoved(danmaku)
            }
        }
    }

    @Synchronized
    protected fun removeUnusedLiveDanmakusIn(msec: Int) {
        if (danmakuList == null || danmakuList!!.isEmpty() || mLiveDanmakus.isEmpty()) return
        val startTime = SystemClock.uptimeMillis()
        val it = mLiveDanmakus.iterator()
        while (it.hasNext()) {
            val danmaku = it.next()
            val isTimeout = danmaku.isTimeOut()
            if (isTimeout) {
                it.remove()
                danmakuList!! -= danmaku
                onDanmakuRemoved(danmaku)
            } else {
                break
            }
            if (SystemClock.uptimeMillis() - startTime > msec) {
                break
            }
        }
    }

    override fun getVisibleDanmakusOnTime(time: Long): IDanmakus {
        val beginMills = time - mContext.mDanmakuFactory.MAX_DANMAKU_DURATION - 100
        val endMills = time + mContext.mDanmakuFactory.MAX_DANMAKU_DURATION
        val subDanmakus = danmakuList?.sub(beginMills, endMills)
        val visibleDanmakus: IDanmakus = Danmakus()
        if (subDanmakus != null && !subDanmakus.isEmpty()) {
            val iterator = subDanmakus.iterator()
            while (iterator.hasNext()) {
                val danmaku = iterator.next()
                if (danmaku.isShown() && !danmaku.isOutside()) {
                    visibleDanmakus += danmaku
                }
            }
        }
        return visibleDanmakus
    }

    @Synchronized
    override fun draw(displayer: AbsDisplayer): RenderingState {
        return drawDanmakus(displayer, mTimer) ?: RenderingState()
    }

    override fun reset() {
        danmakus = Danmakus()
        mRenderer.clear()
    }

    override fun seek(mills: Long) {
        reset()
        requestClear()
        mContext.mGlobalFlagValues.updateVisibleFlag()
        mContext.mGlobalFlagValues.updateFirstShownFlag()
        mStartRenderTime = if (mills < 1000) 0 else mills
        mRenderingState.reset()
        mRenderingState.endTime = mStartRenderTime
        if (danmakuList != null) {
            val last = danmakuList!!.last()
            if (last != null && !last.isTimeOut()) {
                mLastDanmaku = last
            }
        }
    }

    override fun clearDanmakusOnScreen(currMillis: Long) {
        reset()
        mContext.mGlobalFlagValues.updateVisibleFlag()
        mContext.mGlobalFlagValues.updateFirstShownFlag()
        mStartRenderTime = currMillis
    }

    override fun start() {
        mContext.registerConfigChangedCallback(mConfigChangedCallback)
    }

    override fun quit() {
        mContext.unregisterAllConfigChangedCallbacks()
        mRenderer.release()
    }

    override fun prepare() {
        assert(mParser != null)
        loadDanmakus(mParser!!)
        mLastBeginMills = 0
        mLastEndMills = 0
        if (mTaskListener != null) {
            mTaskListener!!.ready()
            mReadyState = true
        }
    }

    protected fun loadDanmakus(parser: BaseDanmakuParser) {
        danmakuList = parser.setConfig(mContext).setDisplayer(mDisp).setTimer(mTimer).getDanmakus()
        if (danmakuList != null && !danmakuList!!.isEmpty()) {
            if (danmakuList!!.first()?.flags == null) {
                val it = danmakuList!!.iterator()
                while (it.hasNext()) {
                    val item = it.next()
                    item.flags = mContext.mGlobalFlagValues
                }
            }
            // Pre-measure first batch of danmakus to avoid per-frame measure overhead
            // Limit to 500 to avoid blocking prepare() for too long on large danmaku sets
            val measureIt = danmakuList!!.iterator()
            var measureCount = 0
            while (measureIt.hasNext() && measureCount < 500) {
                val item = measureIt.next()
                if (!item.isMeasured()) {
                    item.measure(mDisp, true)
                }
                measureCount++
            }
        }
        mContext.mGlobalFlagValues.resetAll()

        if (danmakuList != null) {
            mLastDanmaku = danmakuList!!.last()
        }
    }

    override fun setParser(parser: BaseDanmakuParser?) {
        mParser = parser
        mReadyState = false
    }

    protected fun drawDanmakus(disp: AbsDisplayer, timer: DanmakuTimer): RenderingState? {
        if (clearRetainerFlag) {
            mRenderer.clearRetainer()
            clearRetainerFlag = false
        }
        if (danmakuList != null) {
            val canvas = disp.getExtraData() as Canvas
            canvas.clearCanvas()
            if (mIsHidden) {
                return mRenderingState
            }
            var beginMills = timer.currMillisecond - mContext.mDanmakuFactory.MAX_DANMAKU_DURATION - 100
            var endMills = timer.currMillisecond + mContext.mDanmakuFactory.MAX_DANMAKU_DURATION
            if (mLastBeginMills > beginMills || timer.currMillisecond > mLastEndMills) {
                val subDanmakus = danmakuList!!.sub(beginMills, endMills)
                danmakus = subDanmakus
                mLastBeginMills = beginMills
                mLastEndMills = endMills
            } else {
                beginMills = mLastBeginMills
                endMills = mLastEndMills
            }
            if (!danmakus.isEmpty()) {
                val renderingState = mRenderer.draw(mDisp, danmakus, mStartRenderTime).also { mRenderingState = it }
                if (renderingState.nothingRendered) {
                    if (mLastDanmaku != null && mLastDanmaku!!.isTimeOut()) {
                        mLastDanmaku = null
                        if (mTaskListener != null) {
                            mTaskListener!!.onDanmakusDrawingFinished()
                        }
                    }
                    if (renderingState.beginTime == RenderingState.UNKNOWN_TIME) {
                        renderingState.beginTime = beginMills
                    }
                    if (renderingState.endTime == RenderingState.UNKNOWN_TIME) {
                        renderingState.endTime = endMills
                    }
                }
                return renderingState
            } else {
                mRenderingState.nothingRendered = true
                mRenderingState.beginTime = beginMills
                mRenderingState.endTime = endMills
                return mRenderingState
            }
        }
        return null
    }

    override fun requestClear() {
        mLastBeginMills = 0
        mLastEndMills = 0
        mIsHidden = false
    }

    override fun requestClearRetainer() {
        clearRetainerFlag = true
    }

    open fun onDanmakuConfigChanged(config: DanmakuContext, tag: DanmakuConfigTag?, vararg values: Any?): Boolean {
        val handled = handleOnDanmakuConfigChanged(config, tag, values)
        if (mTaskListener != null) {
            mTaskListener!!.onDanmakuConfigChanged()
        }
        return handled
    }

    protected fun handleOnDanmakuConfigChanged(config: DanmakuContext, tag: DanmakuConfigTag?, values: Array<out Any?>): Boolean {
        var handled = false
        if (tag == null) {
            handled = true
        } else if (DanmakuConfigTag.DUPLICATE_MERGING_ENABLED == tag) {
            val enable = values[0] as? Boolean
            if (enable != null) {
                if (enable) {
                    mContext.mDanmakuFilters.registerFilter(DanmakuFilters.TAG_DUPLICATE_FILTER)
                } else {
                    mContext.mDanmakuFilters.unregisterFilter(DanmakuFilters.TAG_DUPLICATE_FILTER)
                }
                handled = true
            }
        } else if (DanmakuConfigTag.SCALE_TEXTSIZE == tag) {
            requestClearRetainer()
            handled = false
        } else if (DanmakuConfigTag.MAXIMUM_LINES == tag || DanmakuConfigTag.OVERLAPPING_ENABLE == tag) {
            mRenderer.setVerifierEnabled(mContext.isPreventOverlappingEnabled() || mContext.isMaxLinesLimited())
            handled = true
        }
        return handled
    }

    override fun requestHide() {
        mIsHidden = true
    }
}
