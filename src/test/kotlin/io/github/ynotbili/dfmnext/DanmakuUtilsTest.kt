package io.github.ynotbili.dfmnext

import io.github.ynotbili.dfmnext.danmaku.util.DanmakuUtils
import io.github.ynotbili.dfmnext.danmaku.model.BaseDanmaku
import io.github.ynotbili.dfmnext.danmaku.model.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DanmakuUtilsTest {

    // --- compare ---

    @Test
    fun `compare same instance returns zero`() {
        val d = createDanmaku()
        assertEquals(0, DanmakuUtils.compare(d, d))
    }

    @Test
    fun `compare by time ascending`() {
        val a = createDanmaku(time = 100)
        val b = createDanmaku(time = 200)
        assertTrue(DanmakuUtils.compare(a, b) < 0)
        assertTrue(DanmakuUtils.compare(b, a) > 0)
    }

    @Test
    fun `compare by index when time equal`() {
        val a = createDanmaku(time = 100).apply { index = 1 }
        val b = createDanmaku(time = 100).apply { index = 2 }
        assertTrue(DanmakuUtils.compare(a, b) < 0)
    }

    @Test
    fun `compare by type when time and index equal`() {
        val a = createDanmaku(type = BaseDanmaku.TYPE_SCROLL_RL, time = 100).apply { index = 0 }
        val b = createDanmaku(type = BaseDanmaku.TYPE_FIX_TOP, time = 100).apply { index = 0 }
        // TYPE_SCROLL_RL=1 < TYPE_FIX_TOP=5
        assertTrue(DanmakuUtils.compare(a, b) < 0)
    }

    @Test
    fun `compare by text when time index type equal`() {
        val a = createDanmaku(time = 100, text = "abc").apply { index = 0 }
        val b = createDanmaku(time = 100, text = "def").apply { index = 0 }
        assertTrue(DanmakuUtils.compare(a, b) < 0)
    }

    // --- isDuplicate ---

    @Test
    fun `isDuplicate true for same text reference`() {
        val shared = "hello"
        val a = createDanmaku(text = shared)
        val b = createDanmaku(text = shared)
        // text === text (same reference since both set to same string constant)
        assertTrue(DanmakuUtils.isDuplicate(a, b))
    }

    @Test
    fun `isDuplicate true for equal text`() {
        val a = createDanmaku(text = String("hello".toCharArray()))
        val b = createDanmaku(text = String("hello".toCharArray()))
        assertTrue(DanmakuUtils.isDuplicate(a, b))
    }

    @Test
    fun `isDuplicate false for different text`() {
        val a = createDanmaku(text = "hello")
        val b = createDanmaku(text = "world")
        assertFalse(DanmakuUtils.isDuplicate(a, b))
    }

    @Test
    fun `isDuplicate false for null text`() {
        val a = createDanmaku()
        val b = createDanmaku()
        assertFalse(DanmakuUtils.isDuplicate(a, b))
    }

    @Test
    fun `isDuplicate false for same instance`() {
        val a = createDanmaku(text = "hi")
        assertFalse(DanmakuUtils.isDuplicate(a, a))
    }

    // --- getCacheSize ---

    @Test
    fun `getCacheSize returns w times h times 4`() {
        assertEquals(400 * 300 * 4, DanmakuUtils.getCacheSize(400, 300))
    }

    // --- fillText ---

    @Test
    fun `fillText sets text`() {
        val d = createDanmaku()
        DanmakuUtils.fillText(d, "hello")
        assertEquals("hello", d.text)
    }

    @Test
    fun `fillText splits on DANMAKU_BR_CHAR`() {
        val d = createDanmaku()
        DanmakuUtils.fillText(d, "line1/nline2/nline3")
        assertEquals("line1/nline2/nline3", d.text)
        assertEquals(3, d.lines!!.size)
        assertEquals("line1", d.lines!![0])
        assertEquals("line2", d.lines!![1])
        assertEquals("line3", d.lines!![2])
    }

    @Test
    fun `fillText does not split when no separator`() {
        val d = createDanmaku()
        DanmakuUtils.fillText(d, "single line")
        assertEquals(null, d.lines)
    }

    @Test
    fun `fillText handles null text`() {
        val d = createDanmaku()
        DanmakuUtils.fillText(d, null)
        assertEquals(null, d.text)
        assertEquals(null, d.lines)
    }
}
