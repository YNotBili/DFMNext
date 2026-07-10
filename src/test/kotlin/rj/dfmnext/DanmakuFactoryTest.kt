package rj.dfmnext

import rj.dfmnext.danmaku.model.BaseDanmaku
import rj.dfmnext.danmaku.model.FTDanmaku
import rj.dfmnext.danmaku.model.FBDanmaku
import rj.dfmnext.danmaku.model.R2LDanmaku
import rj.dfmnext.danmaku.model.L2RDanmaku
import rj.dfmnext.danmaku.model.SpecialDanmaku
import rj.dfmnext.danmaku.model.android.DanmakuFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DanmakuFactoryTest {

    private val factory = DanmakuFactory.create()
    private val displayer = StubDisplayer(width = 682, height = 438)

    @Test
    fun `updateViewportState returns true on change`() {
        val changed = factory.updateViewportState(682f, 438f, 1.0f)
        assertTrue(changed)
    }

    @Test
    fun `updateViewportState returns false when unchanged`() {
        factory.updateViewportState(682f, 438f, 1.0f)
        val changed = factory.updateViewportState(682f, 438f, 1.0f)
        assertEquals(false, changed)
    }

    @Test
    fun `REAL_DANMAKU_DURATION clamps to MIN`() {
        factory.updateViewportState(100f, 100f, 0.01f)
        assertTrue(factory.REAL_DANMAKU_DURATION >= DanmakuFactory.MIN_DANMAKU_DURATION)
    }

    @Test
    fun `REAL_DANMAKU_DURATION clamps to MAX`() {
        factory.updateViewportState(99999f, 99999f, 99f)
        assertTrue(factory.REAL_DANMAKU_DURATION <= DanmakuFactory.MAX_DANMAKU_DURATION_HIGH_DENSITY)
    }

    @Test
    fun `createDanmaku type 1 returns R2LDanmaku`() {
        val d = factory.createDanmaku(1, displayer, 1.0f, 1.0f)
        assertNotNull(d)
        assertTrue(d is R2LDanmaku)
        assertEquals(BaseDanmaku.TYPE_SCROLL_RL, d!!.getType())
    }

    @Test
    fun `createDanmaku type 4 returns FBDanmaku`() {
        val d = factory.createDanmaku(4, displayer, 1.0f, 1.0f)
        assertNotNull(d)
        assertTrue(d is FBDanmaku)
        assertEquals(BaseDanmaku.TYPE_FIX_BOTTOM, d!!.getType())
    }

    @Test
    fun `createDanmaku type 5 returns FTDanmaku`() {
        val d = factory.createDanmaku(5, displayer, 1.0f, 1.0f)
        assertNotNull(d)
        assertTrue(d is FTDanmaku)
        assertEquals(BaseDanmaku.TYPE_FIX_TOP, d!!.getType())
    }

    @Test
    fun `createDanmaku type 6 returns L2RDanmaku`() {
        val d = factory.createDanmaku(6, displayer, 1.0f, 1.0f)
        assertNotNull(d)
        assertTrue(d is L2RDanmaku)
        assertEquals(BaseDanmaku.TYPE_SCROLL_LR, d!!.getType())
    }

    @Test
    fun `createDanmaku type 7 returns SpecialDanmaku`() {
        val d = factory.createDanmaku(7, displayer, 1.0f, 1.0f)
        assertNotNull(d)
        assertTrue(d is SpecialDanmaku)
        assertEquals(BaseDanmaku.TYPE_SPECIAL, d!!.getType())
    }

    @Test
    fun `createDanmaku unknown type returns null`() {
        val d = factory.createDanmaku(99, displayer, 1.0f, 1.0f)
        assertEquals(null, d)
    }

    @Test
    fun `getCacheSize returns w times h times 4`() {
        assertEquals(400 * 300 * 4, rj.dfmnext.danmaku.util.DanmakuUtils.getCacheSize(400, 300))
    }

    @Test
    fun `fillLinePathData only processes special danmaku`() {
        val d = createDanmaku(type = BaseDanmaku.TYPE_SCROLL_RL)
        // should not throw
        DanmakuFactory.fillLinePathData(d, arrayOf(floatArrayOf(1f, 2f)), 1f, 1f)
    }

    @Test
    fun `fillLinePathData scales points on special danmaku`() {
        val d = SpecialDanmaku()
        d.paintWidth = 50f; d.paintHeight = 20f
        d.setTranslationData(0f, 0f, 100f, 100f, 3000, 0)
        val points = arrayOf(floatArrayOf(10f, 20f), floatArrayOf(30f, 40f))
        DanmakuFactory.fillLinePathData(d, points, 2f, 3f)
        assertNotNull(d.linePaths)
        assertEquals(20f, d.linePaths!![0].pBegin.x)
        assertEquals(60f, d.linePaths!![0].pBegin.y)
    }
}
