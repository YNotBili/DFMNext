package rj.dfmnext.controller

import rj.dfmnext.danmaku.model.AbsDisplayer
import rj.dfmnext.danmaku.model.BaseDanmaku
import rj.dfmnext.danmaku.model.IDanmakus
import rj.dfmnext.danmaku.parser.BaseDanmakuParser
import rj.dfmnext.danmaku.renderer.IRenderer

interface IDrawTask {

    interface TaskListener {
        fun ready()
        fun onDanmakuAdd(danmaku: BaseDanmaku)
        fun onDanmakuShown(danmaku: BaseDanmaku)
        fun onDanmakuConfigChanged()
        fun onDanmakusDrawingFinished()
    }

    fun addDanmaku(item: BaseDanmaku)
    fun removeAllDanmakus(isClearDanmakusOnScreen: Boolean)
    fun removeAllLiveDanmakus()
    fun clearDanmakusOnScreen(currMillis: Long)
    fun getVisibleDanmakusOnTime(time: Long): IDanmakus?
    fun draw(displayer: AbsDisplayer): IRenderer.RenderingState
    fun reset()
    fun seek(mills: Long)
    fun start()
    fun quit()
    fun prepare()
    fun requestClear()
    fun setParser(parser: BaseDanmakuParser?)
    fun invalidateDanmaku(item: BaseDanmaku, remeasure: Boolean)
    fun requestHide()
}
