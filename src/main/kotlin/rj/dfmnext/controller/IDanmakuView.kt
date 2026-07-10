package rj.dfmnext.controller

import android.content.Context
import android.view.View
import android.view.ViewGroup
import rj.dfmnext.danmaku.model.BaseDanmaku
import rj.dfmnext.danmaku.model.IDanmakus
import rj.dfmnext.danmaku.model.android.DanmakuContext
import rj.dfmnext.danmaku.parser.BaseDanmakuParser

interface IDanmakuView {

    interface OnDanmakuClickListener {
        fun onDanmakuClick(latest: BaseDanmaku)
        fun onDanmakuClick(danmakus: IDanmakus)
    }

    fun isPrepared(): Boolean
    fun isPaused(): Boolean
    fun isHardwareAccelerated(): Boolean
    fun setDrawingThreadType(type: Int)
    fun enableDanmakuDrawingCache(enable: Boolean)
    fun isDanmakuDrawingCacheEnabled(): Boolean
    fun showFPS(show: Boolean)
    fun addDanmaku(item: BaseDanmaku)
    fun invalidateDanmaku(item: BaseDanmaku, remeasure: Boolean)
    fun removeAllDanmakus(isClearDanmakusOnScreen: Boolean)
    fun removeAllLiveDanmakus()
    fun getCurrentVisibleDanmakus(): IDanmakus?
    fun setCallback(callback: DrawHandler.Callback?)
    fun getCurrentTime(): Long
    fun getConfig(): DanmakuContext?
    fun getView(): View?
    fun getWidth(): Int
    fun getHeight(): Int
    fun setVisibility(visibility: Int)
    fun isShown(): Boolean
    fun prepare(parser: BaseDanmakuParser?, config: DanmakuContext)
    fun seekTo(ms: Long?)
    fun setSpeed(speed: Float)
    fun adjust()
    fun start()
    fun start(position: Long)
    fun getHandler(): DrawHandler?
    fun stop()
    fun pause()
    fun resume()
    fun release()
    fun toggle()
    fun show()
    fun hide()
    fun showAndResumeDrawTask(position: Long?)
    fun hideAndPauseDrawTask(): Long
    fun clearDanmakusOnScreen()
    fun setOnDanmakuClickListener(listener: OnDanmakuClickListener?)
    fun getOnDanmakuClickListener(): OnDanmakuClickListener?
    fun getLayoutParams(): ViewGroup.LayoutParams?
    fun setLayoutParams(params: ViewGroup.LayoutParams?)
    fun isViewReady(): Boolean
    fun drawDanmakus(): Long
    fun clear()
    fun getContext(): Context

    companion object {
        const val THREAD_TYPE_NORMAL_PRIORITY = 0x0
        const val THREAD_TYPE_MAIN_THREAD = 0x1
        const val THREAD_TYPE_HIGH_PRIORITY = 0x2
        const val THREAD_TYPE_LOW_PRIORITY = 0x3
    }
}
