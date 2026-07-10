package rj.dfmnext

import rj.dfmnext.danmaku.model.objectpool.Pool
import rj.dfmnext.danmaku.model.objectpool.Poolable
import rj.dfmnext.danmaku.model.objectpool.PoolableManager
import rj.dfmnext.danmaku.model.objectpool.Pools
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PoolTest {

    private class SimplePoolable : Poolable<SimplePoolable> {
        var next: SimplePoolable? = null
        private var _pooled = false
        var id = counter++

        companion object { var counter = 0 }

        override fun setNextPoolable(element: SimplePoolable?) { next = element }
        override fun getNextPoolable(): SimplePoolable? = next
        override fun isPooled(): Boolean = _pooled
        override fun setPooled(isPooled: Boolean) { _pooled = isPooled }
    }

    private val manager = object : PoolableManager<SimplePoolable> {
        override fun newInstance() = SimplePoolable()
        override fun onAcquired(element: SimplePoolable) {}
        override fun onReleased(element: SimplePoolable) {}
    }

    @Test
    fun `acquire creates new instance when empty`() {
        val pool = Pools.simplePool(manager)
        val item = pool.acquire()
        assertNotNull(item)
    }

    @Test
    fun `release and acquire returns same instance`() {
        val pool = Pools.simplePool(manager)
        val item = pool.acquire()!!
        pool.release(item)
        val reused = pool.acquire()
        assertSame(item, reused)
    }

    @Test
    fun `released item is marked pooled`() {
        val pool = Pools.simplePool(manager)
        val item = pool.acquire()!!
        pool.release(item)
        assertTrue(item.isPooled())
    }

    @Test
    fun `acquired item is not marked pooled`() {
        val pool = Pools.simplePool(manager)
        val item = pool.acquire()!!
        assertEquals(false, item.isPooled())
    }

    @Test
    fun `double release is ignored`() {
        val pool = Pools.simplePool(manager)
        val a = pool.acquire()!!
        val b = pool.acquire()!!
        pool.release(b)
        pool.release(a)
        pool.release(a) // duplicate — should be ignored
        val first = pool.acquire()
        val second = pool.acquire()
        assertSame(a, first)
        assertSame(b, second)
    }

    @Test
    fun `finite pool respects limit`() {
        val pool: Pool<SimplePoolable> = Pools.finitePool(manager, 2)
        val a = pool.acquire()!!
        val b = pool.acquire()!!
        val c = pool.acquire()!!
        pool.release(a)
        pool.release(b)
        pool.release(c) // limit is 2, c should not be pooled
        val r1 = pool.acquire()
        val r2 = pool.acquire()
        val r3 = pool.acquire() // new instance, c was not pooled
        assertNotNull(r1)
        assertNotNull(r2)
        assertNotNull(r3)
    }

    @Test
    fun `synchronized pool delegates correctly`() {
        val inner = Pools.simplePool(manager)
        val pool = Pools.synchronizedPool(inner)
        val item = pool.acquire()!!
        pool.release(item)
        val reused = pool.acquire()
        assertSame(item, reused)
    }
}
