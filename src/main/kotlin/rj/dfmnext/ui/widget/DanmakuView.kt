package rj.dfmnext.ui.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.os.HandlerThread
import android.os.Looper
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import java.util.LinkedList
import java.util.Locale
import rj.dfmnext.controller.DrawHandler
import rj.dfmnext.controller.DrawHandler.Callback
import rj.dfmnext.controller.IDanmakuView
import rj.dfmnext.danmaku.model.BaseDanmaku
import rj.dfmnext.danmaku.util.clearCanvas
import rj.dfmnext.danmaku.util.drawFps
import rj.dfmnext.danmaku.util.useDrawColorToClear
import rj.dfmnext.danmaku.model.IDanmakus
import rj.dfmnext.danmaku.model.android.DanmakuContext
import rj.dfmnext.danmaku.parser.BaseDanmakuParser
import rj.dfmnext.danmaku.renderer.IRenderer.RenderingState
import rj.dfmnext.danmaku.util.SystemClock

class DanmakuView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle), IDanmakuView {

    companion object {
        const val TAG = "DanmakuView"
        private const val MAX_RECORD_SIZE = 50
        private const val ONE_SECOND = 1000
    }

    private var mCallback: Callback? = null
    private var mHandlerThread: HandlerThread? = null
    private var mHandler: DrawHandler? = null
    private var isSurfaceCreated = false
    private var enableDanmakuDrawingCache = true
    private var mOnDanmakuClickListener: IDanmakuView.OnDanmakuClickListener? = null
    private var mTouchHelper: DanmakuTouchHelper? = null
    private var mShowFps = false
    private var mDanmakuVisible = true
    protected var mDrawingThreadType = IDanmakuView.THREAD_TYPE_NORMAL_PRIORITY
    private val mDrawMonitor = Object()
    private var mDrawFinished = false
    private var mRequestRender = false
    private var mUiThreadId = 0L
    private var mDrawTimes: LinkedList<Long>? = null
    private var mClearFlag = false
    private var mResumeTryCount = 0

    private val mResumeRunnable: Runnable = Runnable {
        if (mHandler == null) return@Runnable
        mResumeTryCount++
        if (mResumeTryCount > 4 || super.isShown()) {
            mHandler!!.resume()
        } else {
            mHandler!!.postDelayed(mResumeRunnable, (100 * mResumeTryCount).toLong())
        }
    }

    init {
        mUiThreadId = Thread.currentThread().id
        setBackgroundColor(Color.TRANSPARENT)
        setDrawingCacheBackgroundColor(Color.TRANSPARENT)
        useDrawColorToClear = true
        mTouchHelper = DanmakuTouchHelper.instance(this)
    }

    override fun addDanmaku(item: BaseDanmaku) {
        mHandler?.addDanmaku(item)
    }

    override fun invalidateDanmaku(item: BaseDanmaku, remeasure: Boolean) {
        mHandler?.invalidateDanmaku(item, remeasure)
    }

    override fun removeAllDanmakus(isClearDanmakusOnScreen: Boolean) {
        mHandler?.removeAllDanmakus(isClearDanmakusOnScreen)
    }

    override fun removeAllLiveDanmakus() {
        mHandler?.removeAllLiveDanmakus()
    }

    override fun getCurrentVisibleDanmakus(): IDanmakus? {
        return mHandler?.getCurrentVisibleDanmakus()
    }

    override fun setCallback(callback: Callback?) {
        mCallback = callback
        mHandler?.setCallback(callback)
    }

    override fun getHandler(): DrawHandler? = mHandler

    override fun release() {
        stop()
        mDrawTimes?.clear()
    }

    override fun stop() {
        stopDraw()
    }

    private fun stopDraw() {
        val h = mHandler
        mHandler = null
        unlockCanvasAndPost()
        h?.quit()
        val handlerThread = mHandlerThread
        if (handlerThread != null) {
            mHandlerThread = null
            try {
                handlerThread.join()
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            handlerThread.quit()
        }
    }

    protected fun getLooper(type: Int): Looper {
        mHandlerThread?.let {
            it.quit()
            mHandlerThread = null
        }

        val priority: Int
        when (type) {
            IDanmakuView.THREAD_TYPE_MAIN_THREAD -> return Looper.getMainLooper()
            IDanmakuView.THREAD_TYPE_HIGH_PRIORITY -> priority = android.os.Process.THREAD_PRIORITY_URGENT_DISPLAY
            IDanmakuView.THREAD_TYPE_LOW_PRIORITY -> priority = android.os.Process.THREAD_PRIORITY_LOWEST
            IDanmakuView.THREAD_TYPE_NORMAL_PRIORITY -> priority = android.os.Process.THREAD_PRIORITY_DEFAULT
            else -> priority = android.os.Process.THREAD_PRIORITY_DEFAULT
        }
        val threadName = "DFM Handler Thread #$priority"
        val ht = HandlerThread(threadName, priority)
        mHandlerThread = ht
        ht.start()
        return ht.looper
    }

    private fun prepare() {
        if (mHandler == null) {
            mHandler = DrawHandler(getLooper(mDrawingThreadType), this, mDanmakuVisible)
        }
    }

    override fun prepare(parser: BaseDanmakuParser?, config: DanmakuContext) {
        prepare()
        mHandler!!.setConfig(config)
        mHandler!!.setParser(parser!!)
        mHandler!!.setCallback(mCallback)
        mHandler!!.prepare()
    }

    override fun isPrepared(): Boolean {
        return mHandler != null && mHandler!!.isPrepared()
    }

    override fun getConfig(): DanmakuContext? {
        return mHandler?.getConfig()
    }

    override fun showFPS(show: Boolean) {
        mShowFps = show
    }

    private fun fps(): Float {
        val lastTime = SystemClock.uptimeMillis()
        mDrawTimes!!.addLast(lastTime)
        val dtime = (lastTime - mDrawTimes!!.first).toFloat()
        val frames = mDrawTimes!!.size
        if (frames > MAX_RECORD_SIZE) {
            mDrawTimes!!.removeFirst()
        }
        return if (dtime > 0) mDrawTimes!!.size * ONE_SECOND / dtime else 0.0f
    }

    override fun drawDanmakus(): Long {
        if (!isSurfaceCreated) return 0
        if (!isShown()) return -1
        val stime = SystemClock.uptimeMillis()
        lockCanvas()
        return SystemClock.uptimeMillis() - stime
    }

    private fun postInvalidateCompat() {
        mRequestRender = true
        postInvalidateOnAnimation()
    }

    private fun lockCanvas() {
        if (!mDanmakuVisible) return
        postInvalidateCompat()
        synchronized(mDrawMonitor) {
            while (!mDrawFinished && mHandler != null) {
                try {
                    mDrawMonitor.wait(200)
                } catch (e: InterruptedException) {
                    if (!mDanmakuVisible || mHandler == null || mHandler!!.isStop()) {
                        break
                    } else {
                        Thread.currentThread().interrupt()
                    }
                }
            }
            mDrawFinished = false
        }
    }

    private fun lockCanvasAndClear() {
        mClearFlag = true
        lockCanvas()
    }

    private fun unlockCanvasAndPost() {
        synchronized(mDrawMonitor) {
            mDrawFinished = true
            mDrawMonitor.notifyAll()
        }
    }

    override fun onDraw(canvas: Canvas) {
        if (!mDanmakuVisible && !mRequestRender) {
            super.onDraw(canvas)
            return
        }
        if (mClearFlag) {
            canvas.clearCanvas()
            mClearFlag = false
        } else {
            if (mHandler != null) {
                val rs = mHandler!!.draw(canvas)
                if (mShowFps) {
                    if (mDrawTimes == null) {
                        mDrawTimes = LinkedList()
                    }
                    val fpsText = String.format(
                        Locale.getDefault(),
                        "fps %.2f,time:%d s,cache:%d,miss:%d",
                        fps(), getCurrentTime() / 1000,
                        rs.cacheHitCount, rs.cacheMissCount
                    )
                    canvas.drawFps(fpsText)
                }
            }
        }
        mRequestRender = false
        unlockCanvasAndPost()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        mHandler?.notifyDispSizeChanged(right - left, bottom - top)
        isSurfaceCreated = true
    }

    override fun toggle() {
        if (isSurfaceCreated) {
            if (mHandler == null) start()
            else if (mHandler!!.isStop()) resume()
            else pause()
        }
    }

    override fun pause() {
        mHandler?.let {
            it.removeCallbacks(mResumeRunnable)
            it.pause()
        }
    }

    override fun resume() {
        if (mHandler != null && mHandler!!.isPrepared()) {
            mHandler!!.removeCallbacks(mResumeRunnable)
            mResumeTryCount = 0
            mHandler!!.postDelayed(mResumeRunnable, 50)
        } else if (mHandler == null) {
            restart()
        }
    }

    override fun isPaused(): Boolean {
        return mHandler?.isStop() ?: false
    }

    fun restart() {
        stop()
        start()
    }

    override fun start() {
        start(0)
    }

    override fun start(position: Long) {
        if (mHandler == null) {
            prepare()
        } else {
            mHandler!!.removeCallbacks(mResumeRunnable)
        }
        mHandler?.obtainMessage(DrawHandler.START, position)?.sendToTarget()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        mTouchHelper?.onTouchEvent(event)
        return super.onTouchEvent(event)
    }

    override fun seekTo(ms: Long?) {
        if (mHandler != null && mHandler!!.isPrepared()) {
            mHandler!!.seekTo(ms)
        }
    }

    override fun setSpeed(speed: Float) {
        if (mHandler != null && mHandler!!.isPrepared()) {
            mHandler!!.setSpeed(speed)
        }
    }

    override fun adjust() {
        mHandler?.syncTimerIfNeeded()
    }

    override fun enableDanmakuDrawingCache(enable: Boolean) {
        enableDanmakuDrawingCache = enable
    }

    override fun isDanmakuDrawingCacheEnabled(): Boolean = enableDanmakuDrawingCache

    override fun isViewReady(): Boolean = isSurfaceCreated

    override fun getView(): View = this

    override fun show() {
        showAndResumeDrawTask(null)
    }

    override fun showAndResumeDrawTask(position: Long?) {
        mDanmakuVisible = true
        mClearFlag = false
        mHandler ?: return
        mHandler!!.showDanmakus(position)
    }

    override fun hide() {
        mDanmakuVisible = false
        mHandler ?: return
        mHandler!!.hideDanmakus(false)
    }

    override fun hideAndPauseDrawTask(): Long {
        mDanmakuVisible = false
        return mHandler?.hideDanmakus(true) ?: 0
    }

    override fun clear() {
        if (!isViewReady()) return
        if (!mDanmakuVisible || Thread.currentThread().id == mUiThreadId) {
            mClearFlag = true
            postInvalidateCompat()
        } else {
            lockCanvasAndClear()
        }
    }

    override fun isShown(): Boolean {
        return mDanmakuVisible && super.isShown()
    }

    override fun setDrawingThreadType(type: Int) {
        mDrawingThreadType = type
    }

    override fun getCurrentTime(): Long {
        return mHandler?.getCurrentTime() ?: 0
    }

    override fun isHardwareAccelerated(): Boolean = super.isHardwareAccelerated()

    override fun clearDanmakusOnScreen() {
        mHandler?.clearDanmakusOnScreen()
    }

    override fun setOnDanmakuClickListener(listener: IDanmakuView.OnDanmakuClickListener?) {
        mOnDanmakuClickListener = listener
        isClickable = listener != null
    }

    override fun getOnDanmakuClickListener(): IDanmakuView.OnDanmakuClickListener? = mOnDanmakuClickListener

    override fun getLayoutParams(): ViewGroup.LayoutParams? = super.getLayoutParams()

    override fun setLayoutParams(params: ViewGroup.LayoutParams?) {
        super.setLayoutParams(params)
    }
}
