package rj.dfmnext

import rj.dfmnext.danmaku.model.android.Danmakus
import rj.dfmnext.danmaku.model.BaseDanmaku
import rj.dfmnext.danmaku.model.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DanmakusCollectionTest {

    private fun d(time: Long, type: Int = BaseDanmaku.TYPE_SCROLL_RL) =
        createDanmaku(type = type, time = time, durationMs = 5000)

    @Test
    fun `empty collection`() {
        val c = Danmakus()
        assertTrue(c.isEmpty())
        assertEquals(0, c.size())
        assertNull(c.first())
        assertNull(c.last())
    }

    @Test
    fun `add and size`() {
        val c = Danmakus()
        c.addItem(d(100))
        c.addItem(d(200))
        assertEquals(2, c.size())
        assertFalse(c.isEmpty())
    }

    @Test
    fun `remove reduces size`() {
        val item = d(100)
        val c = Danmakus()
        c.addItem(item)
        c.addItem(d(200))
        c.removeItem(item)
        assertEquals(1, c.size())
    }

    @Test
    fun `contains works`() {
        val item = d(100)
        val c = Danmakus()
        c.addItem(item)
        assertTrue(c.contains(item))
        assertFalse(c.contains(d(999)))
    }

    @Test
    fun `first and last by time sort`() {
        val c = Danmakus()
        c.addItem(d(300))
        c.addItem(d(100))
        c.addItem(d(200))
        assertEquals(100, c.first()!!.time)
        assertEquals(300, c.last()!!.time)
    }

    @Test
    fun `sub returns time-windowed subset`() {
        val c = Danmakus()
        c.addItem(d(100))
        c.addItem(d(200))
        c.addItem(d(300))
        c.addItem(d(400))
        val sub = c.sub(150, 350)
        assertEquals(200, sub.first()!!.time)
        assertEquals(300, sub.last()!!.time)
    }

    @Test
    fun `list mode preserves insertion order`() {
        val c = Danmakus(Danmakus.ST_BY_LIST)
        c.addItem(d(300))
        c.addItem(d(100))
        c.addItem(d(200))
        val items = c.iterator().asSequence().toList()
        assertEquals(300, items[0].time)
        assertEquals(100, items[1].time)
        assertEquals(200, items[2].time)
    }

    @Test
    fun `iterator remove decrements size`() {
        val c = Danmakus()
        c.addItem(d(100))
        c.addItem(d(200))
        val it = c.iterator()
        it.next()
        it.remove()
        assertEquals(1, c.size())
    }

    @Test
    fun `clear empties collection`() {
        val c = Danmakus()
        c.addItem(d(100))
        c.addItem(d(200))
        c.clear()
        assertTrue(c.isEmpty())
        assertEquals(0, c.size())
    }

    @Test
    fun `operator plusAssign adds item`() {
        val c = Danmakus()
        c += d(100)
        assertEquals(1, c.size())
    }

    @Test
    fun `operator minusAssign removes item`() {
        val item = d(100)
        val c = Danmakus()
        c += item
        c -= item
        assertEquals(0, c.size())
    }
}
