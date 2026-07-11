package io.github.ynotbili.dfmnext

import io.github.ynotbili.dfmnext.danmaku.model.BaseDanmaku
import io.github.ynotbili.dfmnext.danmaku.model.DanmakuTimer
import io.github.ynotbili.dfmnext.danmaku.model.android.DanmakuContext
import io.github.ynotbili.dfmnext.controller.DanmakuFilters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DanmakuFilterTest {

    // --- TypeDanmakuFilter ---

    @Test
    fun `TypeDanmakuFilter filters enabled type`() {
        val f = DanmakuFilters.TypeDanmakuFilter()
        f.enableType(BaseDanmaku.TYPE_FIX_TOP)
        val d = createDanmaku(type = BaseDanmaku.TYPE_FIX_TOP)
        val config = DanmakuContext.create()
        assertTrue(f.filter(d, 0, 10, null, false, config))
        assertEquals(DanmakuFilters.FILTER_TYPE_TYPE, d.mFilterParam and DanmakuFilters.FILTER_TYPE_TYPE)
    }

    @Test
    fun `TypeDanmakuFilter does not filter disabled type`() {
        val f = DanmakuFilters.TypeDanmakuFilter()
        f.enableType(BaseDanmaku.TYPE_FIX_TOP)
        val d = createDanmaku(type = BaseDanmaku.TYPE_SCROLL_RL)
        assertFalse(f.filter(d, 0, 10, null, false, DanmakuContext.create()))
    }

    @Test
    fun `TypeDanmakuFilter disableType stops filtering`() {
        val f = DanmakuFilters.TypeDanmakuFilter()
        f.enableType(BaseDanmaku.TYPE_FIX_TOP)
        f.disableType(BaseDanmaku.TYPE_FIX_TOP)
        val d = createDanmaku(type = BaseDanmaku.TYPE_FIX_TOP)
        assertFalse(f.filter(d, 0, 10, null, false, DanmakuContext.create()))
    }

    @Test
    fun `TypeDanmakuFilter setData sets multiple types`() {
        val f = DanmakuFilters.TypeDanmakuFilter()
        f.setData(listOf(BaseDanmaku.TYPE_FIX_TOP, BaseDanmaku.TYPE_FIX_BOTTOM))
        assertTrue(f.filter(createDanmaku(type = BaseDanmaku.TYPE_FIX_TOP), 0, 10, null, false, DanmakuContext.create()))
        assertTrue(f.filter(createDanmaku(type = BaseDanmaku.TYPE_FIX_BOTTOM), 0, 10, null, false, DanmakuContext.create()))
        assertFalse(f.filter(createDanmaku(type = BaseDanmaku.TYPE_SCROLL_RL), 0, 10, null, false, DanmakuContext.create()))
    }

    @Test
    fun `TypeDanmakuFilter reset clears all types`() {
        val f = DanmakuFilters.TypeDanmakuFilter()
        f.enableType(BaseDanmaku.TYPE_FIX_TOP)
        f.reset()
        assertFalse(f.filter(createDanmaku(type = BaseDanmaku.TYPE_FIX_TOP), 0, 10, null, false, DanmakuContext.create()))
    }

    // --- MaximumLinesFilter ---

    @Test
    fun `MaximumLinesFilter filters when index exceeds limit`() {
        val f = DanmakuFilters.MaximumLinesFilter()
        f.setData(mapOf(BaseDanmaku.TYPE_SCROLL_RL to 3))
        val d = createDanmaku(type = BaseDanmaku.TYPE_SCROLL_RL)
        assertTrue(f.filter(d, 3, 10, null, false, DanmakuContext.create()))
        assertEquals(DanmakuFilters.FILTER_TYPE_MAXIMUM_LINES, d.mFilterParam and DanmakuFilters.FILTER_TYPE_MAXIMUM_LINES)
    }

    @Test
    fun `MaximumLinesFilter allows when index below limit`() {
        val f = DanmakuFilters.MaximumLinesFilter()
        f.setData(mapOf(BaseDanmaku.TYPE_SCROLL_RL to 3))
        assertFalse(f.filter(createDanmaku(type = BaseDanmaku.TYPE_SCROLL_RL), 2, 10, null, false, DanmakuContext.create()))
    }

    @Test
    fun `MaximumLinesFilter ignores unconfigured types`() {
        val f = DanmakuFilters.MaximumLinesFilter()
        f.setData(mapOf(BaseDanmaku.TYPE_SCROLL_RL to 3))
        assertFalse(f.filter(createDanmaku(type = BaseDanmaku.TYPE_FIX_TOP), 100, 10, null, false, DanmakuContext.create()))
    }

    @Test
    fun `MaximumLinesFilter reset clears config`() {
        val f = DanmakuFilters.MaximumLinesFilter()
        f.setData(mapOf(BaseDanmaku.TYPE_SCROLL_RL to 1))
        f.reset()
        assertFalse(f.filter(createDanmaku(type = BaseDanmaku.TYPE_SCROLL_RL), 100, 10, null, false, DanmakuContext.create()))
    }

    // --- OverlappingFilter ---

    @Test
    fun `OverlappingFilter filters when enabled and willHit`() {
        val f = DanmakuFilters.OverlappingFilter()
        f.setData(mapOf(BaseDanmaku.TYPE_SCROLL_RL to true))
        val d = createDanmaku(type = BaseDanmaku.TYPE_SCROLL_RL)
        assertTrue(f.filter(d, 0, 10, null, true, DanmakuContext.create()))
        assertEquals(DanmakuFilters.FILTER_TYPE_OVERLAPPING, d.mFilterParam and DanmakuFilters.FILTER_TYPE_OVERLAPPING)
    }

    @Test
    fun `OverlappingFilter does not filter when willHit is false`() {
        val f = DanmakuFilters.OverlappingFilter()
        f.setData(mapOf(BaseDanmaku.TYPE_SCROLL_RL to true))
        assertFalse(f.filter(createDanmaku(type = BaseDanmaku.TYPE_SCROLL_RL), 0, 10, null, false, DanmakuContext.create()))
    }

    @Test
    fun `OverlappingFilter does not filter when type disabled`() {
        val f = DanmakuFilters.OverlappingFilter()
        f.setData(mapOf(BaseDanmaku.TYPE_SCROLL_RL to false))
        assertFalse(f.filter(createDanmaku(type = BaseDanmaku.TYPE_SCROLL_RL), 0, 10, null, true, DanmakuContext.create()))
    }
}
