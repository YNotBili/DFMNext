package io.github.ynotbili.dfmnext

import io.github.ynotbili.dfmnext.danmaku.model.DanmakuTimer
import kotlin.test.Test
import kotlin.test.assertEquals

class DanmakuTimerTest {

    @Test
    fun `initial state is zero`() {
        val timer = DanmakuTimer()
        assertEquals(0, timer.currMillisecond)
        assertEquals(0, timer.lastInterval())
    }

    @Test
    fun `update sets time and returns delta`() {
        val timer = DanmakuTimer()
        val delta = timer.update(1000)
        assertEquals(1000, timer.currMillisecond)
        assertEquals(1000, delta)
    }

    @Test
    fun `sequential updates return correct deltas`() {
        val timer = DanmakuTimer()
        timer.update(1000)
        val delta = timer.update(1500)
        assertEquals(500, delta)
        assertEquals(1500, timer.currMillisecond)
    }

    @Test
    fun `add advances time`() {
        val timer = DanmakuTimer()
        timer.update(1000)
        val delta = timer.add(300)
        assertEquals(1300, timer.currMillisecond)
        assertEquals(300, delta)
    }

    @Test
    fun `lastInterval stores most recent delta`() {
        val timer = DanmakuTimer()
        timer.update(100)
        timer.update(250)
        assertEquals(150, timer.lastInterval())
    }
}
