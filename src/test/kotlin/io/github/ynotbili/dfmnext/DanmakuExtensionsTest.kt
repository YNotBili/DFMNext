package io.github.ynotbili.dfmnext

import io.github.ynotbili.dfmnext.danmaku.model.BaseDanmaku
import io.github.ynotbili.dfmnext.danmaku.util.isFixBottom
import io.github.ynotbili.dfmnext.danmaku.util.isFixTop
import io.github.ynotbili.dfmnext.danmaku.util.isFixed
import io.github.ynotbili.dfmnext.danmaku.util.isScrollLR
import io.github.ynotbili.dfmnext.danmaku.util.isScrollRL
import io.github.ynotbili.dfmnext.danmaku.util.isScrolling
import io.github.ynotbili.dfmnext.danmaku.util.isSpecial
import io.github.ynotbili.dfmnext.danmaku.util.isVisible
import io.github.ynotbili.dfmnext.danmaku.util.actualDuration
import io.github.ynotbili.dfmnext.danmaku.model.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DanmakuExtensionsTest {

    @Test
    fun `isScrollRL true for TYPE_SCROLL_RL`() {
        val d = createDanmaku(BaseDanmaku.TYPE_SCROLL_RL)
        assertTrue(d.isScrollRL)
        assertFalse(d.isScrollLR)
        assertTrue(d.isScrolling)
    }

    @Test
    fun `isScrollLR true for TYPE_SCROLL_LR`() {
        val d = createDanmaku(BaseDanmaku.TYPE_SCROLL_LR)
        assertFalse(d.isScrollRL)
        assertTrue(d.isScrollLR)
        assertTrue(d.isScrolling)
    }

    @Test
    fun `isFixTop true for TYPE_FIX_TOP`() {
        val d = createDanmaku(BaseDanmaku.TYPE_FIX_TOP)
        assertTrue(d.isFixTop)
        assertFalse(d.isFixBottom)
        assertTrue(d.isFixed)
        assertFalse(d.isScrolling)
    }

    @Test
    fun `isFixBottom true for TYPE_FIX_BOTTOM`() {
        val d = createDanmaku(BaseDanmaku.TYPE_FIX_BOTTOM)
        assertFalse(d.isFixTop)
        assertTrue(d.isFixBottom)
        assertTrue(d.isFixed)
    }

    @Test
    fun `isSpecial true for TYPE_SPECIAL`() {
        val d = createDanmaku(BaseDanmaku.TYPE_SPECIAL)
        assertTrue(d.isSpecial)
        assertFalse(d.isScrolling)
        assertFalse(d.isFixed)
    }

    @Test
    fun `isVisible reflects visibility field`() {
        val d = createDanmaku()
        assertFalse(d.isVisible)
        d.visibility = BaseDanmaku.VISIBLE
        assertTrue(d.isVisible)
    }

    @Test
    fun `actualDuration returns duration value`() {
        val d = createDanmaku(durationMs = 4000)
        assertEquals(4000, d.actualDuration)
    }

    @Test
    fun `actualDuration returns zero when duration is null`() {
        val d = TestDanmaku()
        d.duration = null
        assertEquals(0, d.actualDuration)
    }
}
