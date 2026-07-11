package io.github.ynotbili.dfmnext.danmaku.util

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF

private val clearPaint = Paint().apply {
    xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    color = Color.TRANSPARENT
}

private val fpsPaint by lazy {
    Paint().apply {
        color = Color.RED
        textSize = 30f
    }
}

private val tmpRect = RectF()

var useDrawColorToClear = true
var useDrawColorModeClear = false

fun Canvas.clearCanvas() {
    if (useDrawColorToClear) {
        if (useDrawColorModeClear) {
            drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        } else {
            drawColor(Color.TRANSPARENT)
        }
    } else {
        tmpRect.set(0f, 0f, width.toFloat(), height.toFloat())
        drawRect(tmpRect, clearPaint)
    }
}

fun Canvas.drawFps(text: String) {
    val top = (height - 50).toFloat()
    val textWidth = fpsPaint.measureText(text) + 20
    tmpRect.set(10f, top - 50, textWidth, height.toFloat())
    drawRect(tmpRect, clearPaint)
    drawText(text, 10f, top, fpsPaint)
}
