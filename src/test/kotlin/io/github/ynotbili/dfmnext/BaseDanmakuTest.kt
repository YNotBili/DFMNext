package io.github.ynotbili.dfmnext

import io.github.ynotbili.dfmnext.danmaku.model.BaseDanmaku
import io.github.ynotbili.dfmnext.danmaku.model.DanmakuTimer
import io.github.ynotbili.dfmnext.danmaku.model.Duration
import io.github.ynotbili.dfmnext.danmaku.model.GlobalFlagValues
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BaseDanmakuTest {

    @Test
    fun `isTimeOut true when elapsed exceeds duration`() {
        val d = createDanmaku(time = 100, durationMs = 500)
        val timer = DanmakuTimer()
        timer.update(700) // elapsed = 600 >= 500
        d.setTimer(timer)
        assertTrue(d.isTimeOut())
    }

    @Test
    fun `isTimeOut false when within duration`() {
        val d = createDanmaku(time = 100, durationMs = 500)
        val timer = DanmakuTimer()
        timer.update(400) // elapsed = 300 < 500
        d.setTimer(timer)
        assertFalse(d.isTimeOut())
    }

    @Test
    fun `isTimeOut with explicit ctime`() {
        val d = createDanmaku(time = 100, durationMs = 500)
        assertTrue(d.isTimeOut(700))  // 600 >= 500
        assertFalse(d.isTimeOut(500)) // 400 < 500
    }

    @Test
    fun `isOutside true before start time`() {
        val d = createDanmaku(time = 500, durationMs = 1000)
        val timer = DanmakuTimer()
        timer.update(200) // 200 < 500
        d.setTimer(timer)
        assertTrue(d.isOutside())
    }

    @Test
    fun `isOutside true after end time`() {
        val d = createDanmaku(time = 100, durationMs = 500)
        val timer = DanmakuTimer()
        timer.update(700) // 600 >= 500
        d.setTimer(timer)
        assertTrue(d.isOutside())
    }

    @Test
    fun `isOutside false during playback`() {
        val d = createDanmaku(time = 100, durationMs = 500)
        val timer = DanmakuTimer()
        timer.update(300) // 200 in [0, 500)
        d.setTimer(timer)
        assertFalse(d.isOutside())
    }

    @Test
    fun `isLate true when timer before start`() {
        val d = createDanmaku(time = 500)
        val timer = DanmakuTimer()
        timer.update(200)
        d.setTimer(timer)
        assertTrue(d.isLate())
    }

    @Test
    fun `isLate false when timer at or after start`() {
        val d = createDanmaku(time = 500)
        val timer = DanmakuTimer()
        timer.update(500)
        d.setTimer(timer)
        assertFalse(d.isLate())
    }

    @Test
    fun `isMeasured true when paint dimensions set and flags match`() {
        val d = createDanmaku()
        val flags = GlobalFlagValues()
        flags.updateMeasureFlag()
        d.flags = flags
        d.paintWidth = 100f
        d.paintHeight = 20f
        d.measureResetFlag = flags.MEASURE_RESET_FLAG
        assertTrue(d.isMeasured())
    }

    @Test
    fun `isMeasured false when flags mismatch`() {
        val d = createDanmaku()
        val flags = GlobalFlagValues()
        d.flags = flags
        d.paintWidth = 100f
        d.paintHeight = 20f
        d.measureResetFlag = 999
        assertFalse(d.isMeasured())
    }

    @Test
    fun `isShown true when visible and flag matches`() {
        val d = createDanmaku()
        val flags = GlobalFlagValues()
        flags.updateVisibleFlag()
        d.flags = flags
        d.setVisibility(true)
        assertTrue(d.isShown())
    }

    @Test
    fun `isShown false when invisible`() {
        val d = createDanmaku()
        val flags = GlobalFlagValues()
        flags.updateVisibleFlag()
        d.flags = flags
        d.setVisibility(false)
        assertFalse(d.isShown())
    }

    @Test
    fun `setVisibility true sets VISIBLE and reset flag`() {
        val d = createDanmaku()
        val flags = GlobalFlagValues()
        flags.updateVisibleFlag()
        d.flags = flags
        d.setVisibility(true)
        assertEquals(BaseDanmaku.VISIBLE, d.visibility)
    }

    @Test
    fun `setVisibility false sets INVISIBLE`() {
        val d = createDanmaku()
        d.visibility = BaseDanmaku.VISIBLE
        d.setVisibility(false)
        assertEquals(BaseDanmaku.INVISIBLE, d.visibility)
    }

    @Test
    fun `isFiltered and isFilteredBy with flag matching`() {
        val d = createDanmaku()
        val flags = GlobalFlagValues()
        flags.updateFilterFlag()
        d.flags = flags
        d.filterResetFlag = flags.FILTER_RESET_FLAG
        d.mFilterParam = 0x10
        assertTrue(d.isFiltered())
        assertTrue(d.isFilteredBy(0x10))
        assertFalse(d.isFilteredBy(0x20))
    }

    @Test
    fun `hasPassedFilter false when flag mismatch`() {
        val d = createDanmaku()
        val flags = GlobalFlagValues()
        d.flags = flags
        d.filterResetFlag = 999
        assertFalse(d.hasPassedFilter())
        assertEquals(0, d.mFilterParam)
    }

    @Test
    fun `getDuration returns duration value`() {
        val d = createDanmaku(durationMs = 3000)
        assertEquals(3000, d.getDuration())
    }

    @Test
    fun `getDuration returns zero when duration is null`() {
        val d = TestDanmaku()
        d.duration = null
        assertEquals(0, d.getDuration())
    }
}
