package rj.dfmnext.danmaku.model

open class FTDanmaku(duration: Duration) : BaseDanmaku() {

    private var x: Float = 0f
    protected var y: Float = -1f
    private var RECT: FloatArray? = null
    private var mLastLeft: Float = 0f
    private var mLastPaintWidth: Float = 0f
    private var mLastDispWidth: Int = 0

    init {
        this.duration = duration
    }

    override fun layout(displayer: IDisplayer, x: Float, y: Float) {
        if (mTimer != null) {
            val deltaDuration = mTimer!!.currMillisecond - time
            if (deltaDuration > 0 && deltaDuration < (duration?.value ?: 0)) {
                if (!isShown()) {
                    this.x = getLeft(displayer)
                    this.y = y
                    setVisibility(true)
                }
                return
            }
            setVisibility(false)
            this.y = -1f
            this.x = displayer.width.toFloat()
        }
    }

    protected open fun getLeft(displayer: IDisplayer): Float {
        if (mLastDispWidth == displayer.width && mLastPaintWidth == paintWidth) {
            return mLastLeft
        }
        val left = (displayer.width - paintWidth) / 2
        mLastDispWidth = displayer.width
        mLastPaintWidth = paintWidth
        mLastLeft = left
        return left
    }

    override fun getRectAtTime(displayer: IDisplayer, currTime: Long): FloatArray? {
        if (!isMeasured()) return null
        val left = getLeft(displayer)
        if (RECT == null) RECT = FloatArray(4)
        RECT!![0] = left
        RECT!![1] = y
        RECT!![2] = left + paintWidth
        RECT!![3] = y + paintHeight
        return RECT
    }

    override fun getLeft(): Float = x
    override fun getTop(): Float = y
    override fun getRight(): Float = x + paintWidth
    override fun getBottom(): Float = y + paintHeight
    override fun getType(): Int = TYPE_FIX_TOP
}

class FBDanmaku(duration: Duration) : FTDanmaku(duration) {
    override fun getType(): Int = TYPE_FIX_BOTTOM
}
