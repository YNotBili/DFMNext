package io.github.ynotbili.dfmnext.danmaku.parser

import android.content.SharedPreferences
import io.github.ynotbili.dfmnext.danmaku.model.DanmakuTimer
import io.github.ynotbili.dfmnext.danmaku.model.IDanmakus
import io.github.ynotbili.dfmnext.danmaku.model.IDisplayer
import io.github.ynotbili.dfmnext.danmaku.model.android.DanmakuContext

abstract class BaseDanmakuParser {

    protected var mDataSource: IDataSource<*>? = null
    protected var mTimer: DanmakuTimer? = null
    protected var mDispWidth: Int = 0
    protected var mDispHeight: Int = 0
    protected var mDispDensity: Float = 0f
    protected var mScaledDensity: Float = 0f
    private var mDanmakus: IDanmakus? = null
    protected var mDisp: IDisplayer? = null
    protected lateinit var mContext: DanmakuContext

    var sharedPreferences: SharedPreferences? = null

    open fun setDisplayer(disp: IDisplayer): BaseDanmakuParser {
        mDisp = disp
        mDispWidth = disp.width
        mDispHeight = disp.height
        mDispDensity = disp.density
        mScaledDensity = disp.scaledDensity
        mContext.mDanmakuFactory.updateViewportState(mDispWidth.toFloat(), mDispHeight.toFloat(), getViewportSizeFactor())
        mContext.mDanmakuFactory.updateMaxDanmakuDuration()
        return this
    }

    protected open fun getViewportSizeFactor(): Float {
        return 1f / (mDispDensity - 0.6f)
    }

    fun getDisplayer(): IDisplayer? = mDisp

    fun load(source: IDataSource<*>): BaseDanmakuParser {
        mDataSource = source
        return this
    }

    fun setTimer(timer: DanmakuTimer): BaseDanmakuParser {
        mTimer = timer
        return this
    }

    fun getTimer(): DanmakuTimer? = mTimer

    fun getDanmakus(): IDanmakus? {
        if (mDanmakus != null) return mDanmakus
        mContext.mDanmakuFactory.resetDurationsData()
        mDanmakus = parse()
        releaseDataSource()
        mContext.mDanmakuFactory.updateMaxDanmakuDuration()
        return mDanmakus
    }

    protected fun releaseDataSource() {
        mDataSource?.release()
        mDataSource = null
    }

    protected abstract fun parse(): IDanmakus?

    fun release() {
        releaseDataSource()
    }

    fun setConfig(config: DanmakuContext): BaseDanmakuParser {
        if (::mContext.isInitialized && mContext !== config) {
            mDanmakus = null // call re-parse() under different context
        }
        mContext = config
        return this
    }
}
