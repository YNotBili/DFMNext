package rj.dfmnext.danmaku.model

interface IDisplayer {

    val width: Int
    val height: Int
    val density: Float
    val densityDpi: Int
    val scaledDensity: Float
    val slopPixel: Int
    val strokeWidth: Float
    val maximumCacheWidth: Int
    val maximumCacheHeight: Int
    val isHardwareAccelerated: Boolean

    fun draw(danmaku: BaseDanmaku): Int
    fun measure(danmaku: BaseDanmaku, fromWorkerThread: Boolean)
    fun setHardwareAccelerated(enable: Boolean)
    fun resetSlopPixel(factor: Float)
    fun setDensities(density: Float, densityDpi: Int, scaledDensity: Float)
    fun setSize(width: Int, height: Int)
    fun setDanmakuStyle(style: Int, data: FloatArray)

    companion object {
        const val DANMAKU_STYLE_DEFAULT = -1
        const val DANMAKU_STYLE_NONE = 0
        const val DANMAKU_STYLE_SHADOW = 1
        const val DANMAKU_STYLE_STROKEN = 2
        const val DANMAKU_STYLE_PROJECTION = 3
    }
}
