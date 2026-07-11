package rj.dfmnext.danmaku.model.android

import android.graphics.*
import android.graphics.Paint.Style
import android.text.TextPaint
import rj.dfmnext.danmaku.model.AbsDisplayer
import rj.dfmnext.danmaku.model.BaseDanmaku
import rj.dfmnext.danmaku.model.IDisplayer
import rj.dfmnext.danmaku.renderer.IRenderer
import rj.dfmnext.danmaku.util.isSpecial

class AndroidDisplayer : AbsDisplayer() {

    private val camera = Camera()
    private val matrix = Matrix()
    private var sLastScaleTextSize = 0f
    private val sCachedScaleSize = HashMap<Float, Float>(10)

    val UNDERLINE_HEIGHT = 4

    val PAINT: TextPaint = TextPaint().apply { strokeWidth = STROKE_WIDTH_F }
    val PAINT_DUPLICATE: TextPaint = TextPaint(PAINT)
    private val ALPHA_PAINT = Paint()
    private val UNDERLINE_PAINT = Paint().apply {
        strokeWidth = UNDERLINE_HEIGHT.toFloat()
        style = Style.STROKE
    }
    private val BORDER_PAINT = Paint().apply {
        style = Style.STROKE
        strokeWidth = BORDER_WIDTH.toFloat()
    }
    private val BACKGROUND_PAINT = Paint().apply { style = Style.FILL }

    var CONFIG_HAS_SHADOW = false
    private var HAS_SHADOW = false
    var CONFIG_HAS_STROKE = true
    private var HAS_STROKE = true
    var CONFIG_HAS_PROJECTION = false
    private var HAS_PROJECTION = false
    val CONFIG_ANTI_ALIAS = true
    private var ANTI_ALIAS = true

    private var sStuffer: BaseCacheStuffer = SimpleTextCacheStuffer()
    private var isTranslucent = false
    private var transparency = BaseDanmaku.ALPHA_MAX
    private var scaleTextSize = 1.0f
    private var isTextScaled = false

    private var SHADOW_RADIUS = 4.0f
    private var STROKE_WIDTH = 3.5f
    private var sProjectionOffsetX = 1.0f
    private var sProjectionOffsetY = 1.0f
    private var sProjectionAlpha = 0xCC

    private var _canvas: Canvas? = null
    private var _width = 0
    private var _height = 0
    private var _density = 1f
    private var _densityDpi = 160
    private var _scaledDensity = 1f
    private var mSlopPixel = 0
    private var mIsHardwareAccelerated = true
    private var mMaximumBitmapWidth = 2048
    private var mMaximumBitmapHeight = 2048

    private fun update(c: Canvas?) {
        _canvas = c
        if (c != null) {
            _width = c.width
            _height = c.height
            if (mIsHardwareAccelerated) {
                mMaximumBitmapWidth = c.maximumBitmapWidth
                mMaximumBitmapHeight = c.maximumBitmapHeight
            }
        }
    }

    override fun setTypeFace(font: Typeface) {
        PAINT.typeface = font
    }

    fun setShadowRadius(s: Float) { SHADOW_RADIUS = s }

    fun setPaintStorkeWidth(s: Float) {
        PAINT.strokeWidth = s
        STROKE_WIDTH = s
    }

    fun setProjectionConfig(offsetX: Float, offsetY: Float, alpha: Int) {
        sProjectionOffsetX = if (offsetX > 1.0f) offsetX else 1.0f
        sProjectionOffsetY = if (offsetY > 1.0f) offsetY else 1.0f
        sProjectionAlpha = alpha.coerceIn(0, 255)
    }

    override fun setFakeBoldText(bold: Boolean) { PAINT.isFakeBoldText = bold }

    override fun setTransparency(newTransparency: Int) {
        isTranslucent = newTransparency != BaseDanmaku.ALPHA_MAX
        transparency = newTransparency
    }

    override fun setScaleTextSizeFactor(factor: Float) {
        isTextScaled = factor != 1f
        scaleTextSize = factor
    }

    override fun setCacheStuffer(cacheStuffer: BaseCacheStuffer) {
        if (cacheStuffer !== sStuffer) sStuffer = cacheStuffer
    }

    override fun getCacheStuffer(): BaseCacheStuffer = sStuffer

    override val width: Int get() = _width
    override val height: Int get() = _height
    override val density: Float get() = _density
    override val densityDpi: Int get() = _densityDpi
    override val scaledDensity: Float get() = _scaledDensity
    override val slopPixel: Int get() = mSlopPixel
    override val isHardwareAccelerated: Boolean get() = mIsHardwareAccelerated
    override val maximumCacheWidth: Int get() = mMaximumBitmapWidth
    override val maximumCacheHeight: Int get() = mMaximumBitmapHeight

    override fun draw(danmaku: BaseDanmaku): Int {
        val top = danmaku.getTop()
        val left = danmaku.getLeft()
        val c = _canvas ?: return IRenderer.NOTHING_RENDERING

        var alphaPaint: Paint? = null
        var needRestore = false
        if (danmaku.isSpecial) {
            if (danmaku.getAlpha() == BaseDanmaku.ALPHA_TRANSPARENT) return IRenderer.NOTHING_RENDERING
            if (danmaku.rotationZ != 0f || danmaku.rotationY != 0f) {
                saveCanvas(danmaku, c, left, top)
                needRestore = true
            }
            val alpha = danmaku.getAlpha()
            if (alpha != BaseDanmaku.ALPHA_MAX) {
                alphaPaint = ALPHA_PAINT
                alphaPaint.alpha = alpha
            }
        }

        if (alphaPaint != null && alphaPaint.alpha == BaseDanmaku.ALPHA_TRANSPARENT) {
            return IRenderer.NOTHING_RENDERING
        }

        var cacheDrawn = false
        var result = IRenderer.CACHE_RENDERING
        if (danmaku.hasDrawingCache()) {
            val holder = (danmaku.cache as? DrawingCache)?.get()
            if (holder != null) {
                cacheDrawn = holder.draw(c, left, top, alphaPaint)
            }
        }
        if (!cacheDrawn) {
            if (alphaPaint != null) {
                PAINT.alpha = alphaPaint.alpha
            } else {
                resetPaintAlpha(PAINT)
            }
            drawDanmaku(danmaku, c, left, top, false)
            result = IRenderer.TEXT_RENDERING
        }

        if (needRestore) c.restore()
        return result
    }

    private fun resetPaintAlpha(paint: Paint) {
        if (paint.alpha != BaseDanmaku.ALPHA_MAX) paint.alpha = BaseDanmaku.ALPHA_MAX
    }

    private fun saveCanvas(danmaku: BaseDanmaku, canvas: Canvas, left: Float, top: Float): Int {
        camera.save()
        camera.rotateY(-danmaku.rotationY)
        camera.rotateZ(-danmaku.rotationZ)
        camera.getMatrix(matrix)
        matrix.preTranslate(-left, -top)
        matrix.postTranslate(left, top)
        camera.restore()
        val count = canvas.save()
        canvas.concat(matrix)
        return count
    }

    @Synchronized
    override fun drawDanmaku(danmaku: BaseDanmaku, canvas: Canvas, left: Float, top: Float, quickly: Boolean) {
        var _left = left
        var _top = top
        var adjLeft = left + danmaku.padding
        var adjTop = top + danmaku.padding
        if (danmaku.borderColor != 0) {
            adjLeft += BORDER_WIDTH
            adjTop += BORDER_WIDTH
        }

        HAS_STROKE = CONFIG_HAS_STROKE
        HAS_SHADOW = CONFIG_HAS_SHADOW
        HAS_PROJECTION = CONFIG_HAS_PROJECTION
        ANTI_ALIAS = quickly && CONFIG_ANTI_ALIAS
        val paint = getPaint(danmaku, quickly)
        sStuffer.drawBackground(danmaku, canvas, _left, _top)

        if (danmaku.backgroundColor != 0) {
            val bgPaint = getBackgroundPaint(danmaku)
            val rect = RectF(_left, _top, _left + danmaku.paintWidth, _top + danmaku.paintHeight)
            canvas.drawRoundRect(rect, danmaku.backgroundRadius.toFloat(), danmaku.backgroundRadius.toFloat(), bgPaint)
        }

        val lines = danmaku.lines
        if (lines != null) {
            if (lines.size == 1) {
                if (hasStroke(danmaku)) {
                    applyPaintConfig(danmaku, paint, true)
                    var strokeLeft = adjLeft
                    var strokeTop = adjTop - paint.ascent()
                    if (HAS_PROJECTION) {
                        strokeLeft += sProjectionOffsetX
                        strokeTop += sProjectionOffsetY
                    }
                    sStuffer.drawStroke(danmaku, lines[0], canvas, strokeLeft, strokeTop, paint)
                }
                applyPaintConfig(danmaku, paint, false)
                sStuffer.drawText(danmaku, lines[0], canvas, adjLeft, adjTop - paint.ascent(), paint, quickly)
            } else {
                val textHeight = (danmaku.paintHeight - 2 * danmaku.padding) / lines.size
                for (t in lines.indices) {
                    if (lines[t].isNullOrEmpty()) continue
                    if (hasStroke(danmaku)) {
                        applyPaintConfig(danmaku, paint, true)
                        var strokeLeft = adjLeft
                        var strokeTop = t * textHeight + adjTop - paint.ascent()
                        if (HAS_PROJECTION) {
                            strokeLeft += sProjectionOffsetX
                            strokeTop += sProjectionOffsetY
                        }
                        sStuffer.drawStroke(danmaku, lines[t], canvas, strokeLeft, strokeTop, paint)
                    }
                    applyPaintConfig(danmaku, paint, false)
                    sStuffer.drawText(danmaku, lines[t], canvas, adjLeft, t * textHeight + adjTop - paint.ascent(), paint, quickly)
                }
            }
        } else {
            if (hasStroke(danmaku)) {
                applyPaintConfig(danmaku, paint, true)
                var strokeLeft = adjLeft
                var strokeTop = adjTop - paint.ascent()
                if (HAS_PROJECTION) {
                    strokeLeft += sProjectionOffsetX
                    strokeTop += sProjectionOffsetY
                }
                sStuffer.drawStroke(danmaku, null, canvas, strokeLeft, strokeTop, paint)
            }
            applyPaintConfig(danmaku, paint, false)
            sStuffer.drawText(danmaku, null, canvas, adjLeft, adjTop - paint.ascent(), paint, quickly)
        }

        if (danmaku.underlineColor != 0) {
            val linePaint = getUnderlinePaint(danmaku)
            val bottom = _top + danmaku.paintHeight - UNDERLINE_HEIGHT
            canvas.drawLine(_left, bottom, _left + danmaku.paintWidth, bottom, linePaint)
        }

        if (danmaku.borderColor != 0) {
            val borderPaint = getBorderPaint(danmaku)
            canvas.drawRect(_left, _top, _left + danmaku.paintWidth, _top + danmaku.paintHeight, borderPaint)
        }
    }

    private fun hasStroke(danmaku: BaseDanmaku): Boolean =
        (HAS_STROKE || HAS_PROJECTION) && STROKE_WIDTH > 0 && danmaku.textShadowColor != 0

    private fun getBorderPaint(danmaku: BaseDanmaku): Paint {
        BORDER_PAINT.color = danmaku.borderColor
        return BORDER_PAINT
    }

    private fun getUnderlinePaint(danmaku: BaseDanmaku): Paint {
        UNDERLINE_PAINT.color = danmaku.underlineColor
        return UNDERLINE_PAINT
    }

    private fun getBackgroundPaint(danmaku: BaseDanmaku): Paint {
        BACKGROUND_PAINT.color = danmaku.backgroundColor
        return BACKGROUND_PAINT
    }

    @Synchronized
    private fun getPaint(danmaku: BaseDanmaku, fromWorkerThread: Boolean): TextPaint {
        val paint = if (fromWorkerThread) {
            PAINT
        } else {
            PAINT_DUPLICATE.apply { set(PAINT) }
        }
        paint.textSize = danmaku.textSize
        applyTextScaleConfig(danmaku, paint)
        if (!HAS_SHADOW || SHADOW_RADIUS <= 0 || danmaku.textShadowColor == 0) {
            paint.clearShadowLayer()
        } else {
            paint.setShadowLayer(SHADOW_RADIUS, 0f, 0f, danmaku.textShadowColor)
        }
        paint.isAntiAlias = ANTI_ALIAS
        return paint
    }

    private fun applyPaintConfig(danmaku: BaseDanmaku, paint: Paint, stroke: Boolean) {
        if (isTranslucent) {
            if (stroke) {
                paint.style = if (HAS_PROJECTION) Style.FILL else Style.STROKE
                paint.color = danmaku.textShadowColor and 0x00FFFFFF
                paint.alpha = if (HAS_PROJECTION) {
                    (sProjectionAlpha * (transparency.toFloat() / BaseDanmaku.ALPHA_MAX)).toInt()
                } else transparency
            } else {
                paint.style = Style.FILL
                paint.color = danmaku.textColor and 0x00FFFFFF
                paint.alpha = transparency
            }
        } else {
            if (stroke) {
                paint.style = if (HAS_PROJECTION) Style.FILL else Style.STROKE
                paint.color = danmaku.textShadowColor and 0x00FFFFFF
                paint.alpha = if (HAS_PROJECTION) sProjectionAlpha else BaseDanmaku.ALPHA_MAX
            } else {
                paint.style = Style.FILL
                paint.color = danmaku.textColor and 0x00FFFFFF
                paint.alpha = BaseDanmaku.ALPHA_MAX
            }
        }
    }

    private fun applyTextScaleConfig(danmaku: BaseDanmaku, paint: Paint) {
        if (!isTextScaled) return
        var size = sCachedScaleSize[danmaku.textSize]
        if (size == null || sLastScaleTextSize != scaleTextSize) {
            sLastScaleTextSize = scaleTextSize
            size = danmaku.textSize * scaleTextSize
            sCachedScaleSize[danmaku.textSize] = size
        }
        paint.textSize = size
    }

    override fun measure(danmaku: BaseDanmaku, fromWorkerThread: Boolean) {
        val paint = getPaint(danmaku, fromWorkerThread)
        if (HAS_STROKE) applyPaintConfig(danmaku, paint, true)
        sStuffer.measure(danmaku, paint, fromWorkerThread)
        setDanmakuPaintWidthAndHeight(danmaku, danmaku.paintWidth, danmaku.paintHeight)
        if (HAS_STROKE) applyPaintConfig(danmaku, paint, false)
    }

    private fun setDanmakuPaintWidthAndHeight(danmaku: BaseDanmaku, w: Float, h: Float) {
        var pw = w + 2 * danmaku.padding
        var ph = h + 2 * danmaku.padding
        if (danmaku.borderColor != 0) {
            pw += 2 * BORDER_WIDTH
            ph += 2 * BORDER_WIDTH
        }
        danmaku.paintWidth = pw + strokeWidth
        danmaku.paintHeight = ph
    }

    override fun clearTextHeightCache() {
        sStuffer.clearCaches()
        sCachedScaleSize.clear()
    }

    override fun resetSlopPixel(factor: Float) {
        val d = maxOf(factor, width / DanmakuFactory.BILI_PLAYER_WIDTH)
        val slop = d * DanmakuFactory.DANMAKU_MEDIUM_TEXTSIZE
        mSlopPixel = if (factor > 1f) (slop * factor).toInt() else slop.toInt()
    }

    override fun setDensities(density: Float, densityDpi: Int, scaledDensity: Float) {
        this._density = density
        this._densityDpi = densityDpi
        this._scaledDensity = scaledDensity
    }

    override fun setSize(width: Int, height: Int) {
        this._width = width
        this._height = height
    }

    override fun setDanmakuStyle(style: Int, data: FloatArray) {
        when (style) {
            IDisplayer.DANMAKU_STYLE_NONE -> {
                CONFIG_HAS_SHADOW = false; CONFIG_HAS_STROKE = false; CONFIG_HAS_PROJECTION = false
            }
            IDisplayer.DANMAKU_STYLE_SHADOW -> {
                CONFIG_HAS_SHADOW = true; CONFIG_HAS_STROKE = false; CONFIG_HAS_PROJECTION = false
                setShadowRadius(data[0])
            }
            IDisplayer.DANMAKU_STYLE_DEFAULT, IDisplayer.DANMAKU_STYLE_STROKEN -> {
                CONFIG_HAS_SHADOW = false; CONFIG_HAS_STROKE = true; CONFIG_HAS_PROJECTION = false
                setPaintStorkeWidth(data[0])
            }
            IDisplayer.DANMAKU_STYLE_PROJECTION -> {
                CONFIG_HAS_SHADOW = false; CONFIG_HAS_STROKE = false; CONFIG_HAS_PROJECTION = true
                setProjectionConfig(data[0], data[1], data[2].toInt())
            }
        }
    }

    override fun setExtraData(data: Canvas) { update(data) }
    override fun getExtraData(): Canvas? = _canvas

    override val strokeWidth: Float
        get() = when {
            HAS_SHADOW && HAS_STROKE -> maxOf(SHADOW_RADIUS, STROKE_WIDTH)
            HAS_SHADOW -> SHADOW_RADIUS
            HAS_STROKE -> STROKE_WIDTH
            else -> 0f
        }

    override fun setHardwareAccelerated(enable: Boolean) { mIsHardwareAccelerated = enable }

    companion object {
        const val BORDER_WIDTH = 4
        private const val STROKE_WIDTH_F = 3.5f
    }
}
