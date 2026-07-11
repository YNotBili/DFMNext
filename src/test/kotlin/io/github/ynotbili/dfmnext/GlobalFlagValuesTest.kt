package io.github.ynotbili.dfmnext

import io.github.ynotbili.dfmnext.danmaku.model.GlobalFlagValues
import kotlin.test.Test
import kotlin.test.assertEquals

class GlobalFlagValuesTest {

    @Test
    fun `initial flags are zero`() {
        val flags = GlobalFlagValues()
        assertEquals(0, flags.MEASURE_RESET_FLAG)
        assertEquals(0, flags.VISIBLE_RESET_FLAG)
        assertEquals(0, flags.FILTER_RESET_FLAG)
        assertEquals(0, flags.FIRST_SHOWN_RESET_FLAG)
    }

    @Test
    fun `updateMeasureFlag increments independently`() {
        val flags = GlobalFlagValues()
        flags.updateMeasureFlag()
        flags.updateMeasureFlag()
        assertEquals(2, flags.MEASURE_RESET_FLAG)
        assertEquals(0, flags.VISIBLE_RESET_FLAG)
    }

    @Test
    fun `updateVisibleFlag increments independently`() {
        val flags = GlobalFlagValues()
        flags.updateVisibleFlag()
        assertEquals(1, flags.VISIBLE_RESET_FLAG)
    }

    @Test
    fun `updateFilterFlag increments independently`() {
        val flags = GlobalFlagValues()
        flags.updateFilterFlag()
        flags.updateFilterFlag()
        flags.updateFilterFlag()
        assertEquals(3, flags.FILTER_RESET_FLAG)
    }

    @Test
    fun `resetAll zeroes everything`() {
        val flags = GlobalFlagValues()
        flags.updateMeasureFlag()
        flags.updateVisibleFlag()
        flags.updateFilterFlag()
        flags.updateFirstShownFlag()
        flags.resetAll()
        assertEquals(0, flags.MEASURE_RESET_FLAG)
        assertEquals(0, flags.VISIBLE_RESET_FLAG)
        assertEquals(0, flags.FILTER_RESET_FLAG)
        assertEquals(0, flags.FIRST_SHOWN_RESET_FLAG)
    }
}
