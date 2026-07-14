package io.github.ynotbili.dfmnext.controller

import io.github.ynotbili.dfmnext.danmaku.model.AbsDisplayer
import io.github.ynotbili.dfmnext.danmaku.model.BaseDanmaku
import io.github.ynotbili.dfmnext.danmaku.model.IDanmakus
import io.github.ynotbili.dfmnext.danmaku.parser.BaseDanmakuParser
import io.github.ynotbili.dfmnext.danmaku.renderer.IRenderer

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
    fun requestClearRetainer()
}
