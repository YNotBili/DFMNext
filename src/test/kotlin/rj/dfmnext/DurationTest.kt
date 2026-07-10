package rj.dfmnext

import rj.dfmnext.danmaku.model.Duration
import kotlin.test.Test
import kotlin.test.assertEquals

class DurationTest {

    @Test
    fun `initial value matches constructor`() {
        val d = Duration(3000)
        assertEquals(3000, d.value)
    }

    @Test
    fun `setFactor scales duration`() {
        val d = Duration(3000)
        d.setFactor(2.0f)
        assertEquals(1500, d.value)
    }

    @Test
    fun `setFactor with zero is treated as one`() {
        val d = Duration(3000)
        d.setFactor(0f)
        assertEquals(3000, d.value)
    }

    @Test
    fun `setFactor with negative is treated as one`() {
        val d = Duration(3000)
        d.setFactor(-1.5f)
        assertEquals(3000, d.value)
    }

    @Test
    fun `setFactor same value is no-op`() {
        val d = Duration(3000)
        d.setFactor(1.0f) // same as initial
        assertEquals(3000, d.value)
    }

    @Test
    fun `setValue recalculates with current factor`() {
        val d = Duration(3000)
        d.setFactor(2.0f)
        d.setValue(6000)
        assertEquals(3000, d.value) // 6000 / 2.0
    }
}
