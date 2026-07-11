package io.github.ynotbili.dfmnext

import io.github.ynotbili.dfmnext.danmaku.model.SpecialDanmaku
import io.github.ynotbili.dfmnext.danmaku.model.Duration
import io.github.ynotbili.dfmnext.danmaku.model.GlobalFlagValues
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SpecialDanmakuTest {

    @Test
    fun `Point getDistance euclidean`() {
        val a = SpecialDanmaku.Point(0f, 0f)
        val b = SpecialDanmaku.Point(3f, 4f)
        assertEquals(5f, a.getDistance(b), 0.001f)
    }

    @Test
    fun `Point getDistance zero for same point`() {
        val a = SpecialDanmaku.Point(7f, 12f)
        assertEquals(0f, a.getDistance(a), 0.001f)
    }

    @Test
    fun `setTranslationData computes deltas`() {
        val d = SpecialDanmaku()
        d.setTranslationData(10f, 20f, 110f, 220f, 5000, 100)
        assertEquals(100f, d.deltaX)
        assertEquals(200f, d.deltaY)
        assertEquals(5000L, d.translationDuration)
        assertEquals(100L, d.translationStartDelay)
    }

    @Test
    fun `setAlphaData computes delta`() {
        val d = SpecialDanmaku()
        d.setAlphaData(100, 200, 3000)
        assertEquals(100, d.deltaAlpha)
        assertEquals(3000L, d.alphaDuration)
    }

    @Test
    fun `setLinePathData creates proportional segments`() {
        val d = SpecialDanmaku()
        d.translationDuration = 1000
        val points = arrayOf(
            floatArrayOf(0f, 0f),
            floatArrayOf(100f, 0f),   // distance 100
            floatArrayOf(100f, 100f)  // distance 100
        )
        d.setLinePathData(points)
        assertNotNull(d.linePaths)
        assertEquals(2, d.linePaths!!.size)
        // each segment has equal distance, so each gets 50% of duration
        assertEquals(500L, d.linePaths!![0].duration)
        assertEquals(500L, d.linePaths!![1].duration)
        // begin/end times are contiguous
        assertEquals(0L, d.linePaths!![0].beginTime)
        assertEquals(500L, d.linePaths!![0].endTime)
        assertEquals(500L, d.linePaths!![1].beginTime)
        assertEquals(1000L, d.linePaths!![1].endTime)
    }

    @Test
    fun `setLinePathData sets begin and end coordinates`() {
        val d = SpecialDanmaku()
        d.translationDuration = 1000
        val points = arrayOf(floatArrayOf(5f, 10f), floatArrayOf(50f, 100f))
        d.setLinePathData(points)
        assertEquals(5f, d.beginX)
        assertEquals(10f, d.beginY)
        assertEquals(50f, d.endX)
        assertEquals(100f, d.endY)
    }

    @Test
    fun `getRectAtTime returns null when not measured`() {
        val d = SpecialDanmaku()
        d.setTranslationData(0f, 0f, 100f, 0f, 1000, 0)
        d.time = 0; d.duration = Duration(1000)
        val disp = StubDisplayer()
        assertNull(d.getRectAtTime(disp, 500))
    }

    @Test
    fun `getRectAtTime computes position with translation`() {
        val d = SpecialDanmaku()
        d.paintWidth = 10f; d.paintHeight = 10f
        d.setTranslationData(0f, 0f, 100f, 0f, 1000, 0)
        d.time = 0; d.duration = Duration(1000)
        d.flags = GlobalFlagValues().also { it.updateMeasureFlag() }
        d.measureResetFlag = d.flags!!.MEASURE_RESET_FLAG
        val disp = StubDisplayer()
        val rect = d.getRectAtTime(disp, 500)
        assertNotNull(rect)
        // at 50% progress, x should be ~50
        assertEquals(50f, rect[0], 1f)
        assertEquals(60f, rect[2], 1f) // x + paintWidth
    }

    @Test
    fun `getRectAtTime applies alpha interpolation`() {
        val d = SpecialDanmaku()
        d.paintWidth = 10f; d.paintHeight = 10f
        d.setTranslationData(0f, 0f, 0f, 0f, 1000, 0)
        d.setAlphaData(0, 255, 1000)
        d.time = 0; d.duration = Duration(1000)
        d.flags = GlobalFlagValues().also { it.updateMeasureFlag() }
        d.measureResetFlag = d.flags!!.MEASURE_RESET_FLAG
        val disp = StubDisplayer()
        d.getRectAtTime(disp, 500)
        assertEquals(127.0, d.getAlpha().toDouble(), 2.0)
    }
}
