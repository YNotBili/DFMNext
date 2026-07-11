package rj.dfmnext.danmaku.model.android

import rj.dfmnext.danmaku.model.objectpool.Poolable
import rj.dfmnext.danmaku.model.objectpool.PoolableManager

interface IDrawingCache<T> {
    fun build(w: Int, h: Int, density: Int, checkSizeEquals: Boolean)
    fun erase()
    fun get(): T?
    fun destroy()
    fun size(): Int
    fun width(): Int
    fun height(): Int
    fun hasReferences(): Boolean
    fun increaseReference()
    fun decreaseReference()
}

class DrawingCache : IDrawingCache<DrawingCacheHolder>, Poolable<DrawingCache> {

    private val mHolder = DrawingCacheHolder()
    private var mSize = 0
    private var mNextElement: DrawingCache? = null
    private var mIsPooled = false
    private var referenceCount = 0

    override fun build(w: Int, h: Int, density: Int, checkSizeEquals: Boolean) {
        mHolder.buildCache(w, h, density, checkSizeEquals)
        mSize = mHolder.bitmap!!.rowBytes * mHolder.bitmap!!.height
    }

    override fun erase() {
        mHolder.erase()
    }

    override fun get(): DrawingCacheHolder? {
        if (mHolder.bitmap == null) return null
        return mHolder
    }

    override fun destroy() {
        mHolder.recycle()
        mSize = 0
        referenceCount = 0
    }

    override fun size(): Int = mSize

    override fun setNextPoolable(element: DrawingCache?) {
        mNextElement = element
    }

    override fun getNextPoolable(): DrawingCache? = mNextElement

    override fun isPooled(): Boolean = mIsPooled

    override fun setPooled(isPooled: Boolean) {
        mIsPooled = isPooled
    }

    @Synchronized
    override fun hasReferences(): Boolean = referenceCount > 0

    @Synchronized
    override fun increaseReference() {
        referenceCount++
    }

    @Synchronized
    override fun decreaseReference() {
        referenceCount--
    }

    override fun width(): Int = mHolder.width

    override fun height(): Int = mHolder.height

    companion object {
        val PoolManager = object : PoolableManager<DrawingCache> {
            override fun newInstance(): DrawingCache = DrawingCache()
            override fun onAcquired(element: DrawingCache) {}
            override fun onReleased(element: DrawingCache) {}
        }
    }
}
