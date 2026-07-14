package io.github.ynotbili.dfmnext.danmaku.model.objectpool

interface Poolable<T> {
    fun setNextPoolable(element: T?)
    fun getNextPoolable(): T?
    fun isPooled(): Boolean
    fun setPooled(isPooled: Boolean)
}

interface Pool<T : Poolable<T>> {
    fun acquire(): T?
    fun release(element: T)
    fun trimToSize(maxSize: Int)
}

interface PoolableManager<T : Poolable<T>> {
    fun newInstance(): T
    fun onAcquired(element: T)
    fun onReleased(element: T)
}

internal class FinitePool<T : Poolable<T>>(
    private val manager: PoolableManager<T>,
    private val limit: Int = 0
) : Pool<T> {

    private val infinite: Boolean = limit <= 0
    private var root: T? = null
    private var poolCount: Int = 0

    init {
        if (!infinite && limit <= 0) {
            throw IllegalArgumentException("The pool limit must be > 0")
        }
    }

    override fun acquire(): T? {
        val element: T? = if (root != null) {
            val r = root!!
            root = r.getNextPoolable()
            poolCount--
            r
        } else {
            manager.newInstance()
        }
        element?.let {
            it.setNextPoolable(null)
            it.setPooled(false)
            manager.onAcquired(it)
        }
        return element
    }

    override fun release(element: T) {
        if (!element.isPooled()) {
            if (infinite || poolCount < limit) {
                poolCount++
                element.setNextPoolable(root)
                element.setPooled(true)
                root = element
            }
            manager.onReleased(element)
        }
    }

    override fun trimToSize(maxSize: Int) {
        if (maxSize < 0) return
        while (poolCount > maxSize && root != null) {
            val element = root!!
            root = element.getNextPoolable()
            element.setNextPoolable(null)
            element.setPooled(false)
            poolCount--
            manager.onReleased(element)
        }
    }
}

internal class SynchronizedPool<T : Poolable<T>>(
    private val pool: Pool<T>,
    private val lock: Any = Any()
) : Pool<T> {

    override fun acquire(): T? = synchronized(lock) { pool.acquire() }

    override fun release(element: T) = synchronized(lock) { pool.release(element) }

    override fun trimToSize(maxSize: Int) = synchronized(lock) { pool.trimToSize(maxSize) }
}

object Pools {

    fun <T : Poolable<T>> simplePool(manager: PoolableManager<T>): Pool<T> =
        FinitePool(manager)

    fun <T : Poolable<T>> finitePool(manager: PoolableManager<T>, limit: Int): Pool<T> =
        FinitePool(manager, limit)

    fun <T : Poolable<T>> synchronizedPool(pool: Pool<T>): Pool<T> =
        SynchronizedPool(pool)

    fun <T : Poolable<T>> synchronizedPool(pool: Pool<T>, lock: Any): Pool<T> =
        SynchronizedPool(pool, lock)
}
