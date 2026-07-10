package rj.dfmnext.danmaku.model

import rj.dfmnext.danmaku.model.android.IDrawingCache

abstract class BaseDanmaku {

    companion object {
        const val DANMAKU_BR_CHAR = "/n"
        const val TYPE_SCROLL_RL = 1
        const val TYPE_SCROLL_LR = 6
        const val TYPE_FIX_TOP = 5
        const val TYPE_FIX_BOTTOM = 4
        const val TYPE_SPECIAL = 7
        const val TYPE_MOVEABLE_XXX = 0
        const val INVISIBLE = 0
        const val VISIBLE = 1
        const val FLAG_REQUEST_REMEASURE = 0x1
        const val FLAG_REQUEST_INVALIDATE = 0x2
        const val ALPHA_MAX = 255
        const val ALPHA_TRANSPARENT = 0
    }

    var time: Long = 0
    var text: CharSequence? = null
    var lines: Array<String>? = null
    var obj: Any? = null
    var textColor: Int = 0
    var rotationZ: Float = 0f
    var rotationY: Float = 0f
    var textShadowColor: Int = 0
    var backgroundColor: Int = 0
    var backgroundRadius: Int = 10
    var underlineColor: Int = 0
    var textSize: Float = -1f
    var borderColor: Int = 0
    var padding: Int = 0
    var priority: Byte = 0
    var paintWidth: Float = -1f
    var paintHeight: Float = -1f
    var duration: Duration? = null
    var index: Int = 0
    var visibility: Int = 0
    private var visibleResetFlag: Int = 0
    var mMergeCount: Int = 0
    var mOriginalText: CharSequence? = null
    var mOriginalTextSize: Float = -1f
    var measureResetFlag: Int = 0
    var cache: IDrawingCache<*>? = null
    var isLive: Boolean = false
    var forceBuildCacheInSameThread: Boolean = false
    val userId: Int = 0
    var userHash: String? = null
    var isGuest: Boolean = false
    protected var mTimer: DanmakuTimer? = null
    @JvmField protected var alpha: Int = ALPHA_MAX
    var mFilterParam: Int = 0
    var filterResetFlag: Int = -1
    var flags: GlobalFlagValues? = null
    var requestFlags: Int = 0
    var firstShownFlag: Int = -1

    fun getDuration(): Long = duration?.value ?: 0

    fun draw(displayer: IDisplayer): Int = displayer.draw(this)

    fun isMeasured(): Boolean =
        paintWidth > -1 && paintHeight > -1 && measureResetFlag == flags?.MEASURE_RESET_FLAG

    open fun measure(displayer: IDisplayer, fromWorkerThread: Boolean) {
        displayer.measure(this, fromWorkerThread)
        this.measureResetFlag = flags?.MEASURE_RESET_FLAG ?: 0
    }

    fun hasDrawingCache(): Boolean = cache != null && cache?.get() != null

    fun isShown(): Boolean =
        this.visibility == VISIBLE && visibleResetFlag == flags?.VISIBLE_RESET_FLAG

    fun isTimeOut(): Boolean = mTimer == null || isTimeOut(mTimer!!.currMillisecond)

    fun isTimeOut(ctime: Long): Boolean = ctime - time >= (duration?.value ?: 0)

    fun isOutside(): Boolean = mTimer == null || isOutside(mTimer!!.currMillisecond)

    fun isOutside(ctime: Long): Boolean {
        val dtime = ctime - time
        return dtime <= 0 || dtime >= (duration?.value ?: 0)
    }

    fun isLate(): Boolean = mTimer == null || mTimer!!.currMillisecond < time

    fun hasPassedFilter(): Boolean {
        if (filterResetFlag != flags?.FILTER_RESET_FLAG) {
            mFilterParam = 0
            return false
        }
        return true
    }

    fun isFiltered(): Boolean =
        filterResetFlag == flags?.FILTER_RESET_FLAG && mFilterParam != 0

    fun isFilteredBy(flag: Int): Boolean =
        filterResetFlag == flags?.FILTER_RESET_FLAG && (mFilterParam and flag) == flag

    fun setVisibility(b: Boolean) {
        if (b) {
            this.visibleResetFlag = flags?.VISIBLE_RESET_FLAG ?: 0
            this.visibility = VISIBLE
        } else {
            this.visibility = INVISIBLE
        }
    }

    abstract fun layout(displayer: IDisplayer, x: Float, y: Float)
    abstract fun getRectAtTime(displayer: IDisplayer, currTime: Long): FloatArray?
    abstract fun getLeft(): Float
    abstract fun getTop(): Float
    abstract fun getRight(): Float
    abstract fun getBottom(): Float
    abstract fun getType(): Int

    fun getTimer(): DanmakuTimer? = mTimer

    fun setTimer(timer: DanmakuTimer?) {
        mTimer = timer
    }

    fun getAlpha(): Int = alpha
}
