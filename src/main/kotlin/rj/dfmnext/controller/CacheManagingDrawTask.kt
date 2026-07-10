package rj.dfmnext.controller

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.util.Pair
import rj.dfmnext.danmaku.model.AbsDisplayer
import rj.dfmnext.danmaku.model.BaseDanmaku
import rj.dfmnext.danmaku.model.DanmakuTimer
import rj.dfmnext.danmaku.model.IDanmakus
import rj.dfmnext.danmaku.model.android.DanmakuContext
import rj.dfmnext.danmaku.model.android.DanmakuContext.DanmakuConfigTag
import rj.dfmnext.danmaku.model.android.DanmakuFactory
import rj.dfmnext.danmaku.model.android.Danmakus
import rj.dfmnext.danmaku.model.android.DrawingCache
import rj.dfmnext.danmaku.model.objectpool.Pool
import rj.dfmnext.danmaku.model.objectpool.Pools
import rj.dfmnext.danmaku.renderer.IRenderer.RenderingState
import rj.dfmnext.danmaku.util.DanmakuUtils
import rj.dfmnext.danmaku.util.SystemClock

class CacheManagingDrawTask(
    timer: DanmakuTimer,
    config: DanmakuContext,
    taskListener: IDrawTask.TaskListener?,
    maxCacheSize: Int
) : DrawTask(timer, config, taskListener) {

    private var mMaxCacheSize = maxCacheSize

    private var mCacheManager: CacheManager? = null

    private lateinit var mCacheTimer: DanmakuTimer

    private val mDrawingNotify = Object()

    init {
        // NativeBitmapFactory calls removed: use regular Bitmap allocation
        mCacheManager = CacheManager(mMaxCacheSize, MAX_CACHE_SCREEN_SIZE)
        mRenderer.setCacheManager { mCacheManager?.addDanmaku(it) }
    }

    override fun initTimer(timer: DanmakuTimer) {
        mTimer = timer
        mCacheTimer = DanmakuTimer()
        mCacheTimer.update(timer.currMillisecond)
    }

    override fun addDanmaku(danmaku: BaseDanmaku) {
        super.addDanmaku(danmaku)
        mCacheManager?.addDanmaku(danmaku)
    }

    override fun invalidateDanmaku(item: BaseDanmaku, remeasure: Boolean) {
        if (mCacheManager == null) {
            super.invalidateDanmaku(item, remeasure)
            return
        }
        mCacheManager!!.invalidateDanmaku(item, remeasure)
    }

    override fun removeAllDanmakus(isClearDanmakusOnScreen: Boolean) {
        super.removeAllDanmakus(isClearDanmakusOnScreen)
        mCacheManager?.requestClearAll()
    }

    override fun onDanmakuRemoved(danmaku: BaseDanmaku) {
        super.onDanmakuRemoved(danmaku)
        if (danmaku.hasDrawingCache()) {
            if (danmaku.cache?.hasReferences() == true) {
                danmaku.cache?.decreaseReference()
            } else {
                danmaku.cache?.destroy()
            }
            danmaku.cache = null
        }
    }

    override fun draw(displayer: AbsDisplayer): RenderingState {
        val result = super.draw(displayer)
        synchronized(mDrawingNotify) {
            (mDrawingNotify as Object).notify()
        }
        if (result != null && mCacheManager != null) {
            if (result.incrementCount < -20) {
                mCacheManager!!.requestClearTimeout()
                mCacheManager!!.requestBuild(-mContext.mDanmakuFactory.MAX_DANMAKU_DURATION)
            }
        }
        return result!!
    }

    override fun seek(mills: Long) {
        super.seek(mills)
        if (mCacheManager == null) {
            start()
        }
        mCacheManager!!.seek(mills)
    }

    override fun start() {
        super.start()
        // NativeBitmapFactory.loadLibs() removed
        if (mCacheManager == null) {
            mCacheManager = CacheManager(mMaxCacheSize, MAX_CACHE_SCREEN_SIZE)
            mCacheManager!!.begin()
            mRenderer.setCacheManager { mCacheManager?.addDanmaku(it) }
        } else {
            mCacheManager!!.resume()
        }
    }

    override fun quit() {
        super.quit()
        reset()
        mRenderer.setCacheManager(null)
        if (mCacheManager != null) {
            mCacheManager!!.end()
            mCacheManager = null
        }
        // NativeBitmapFactory.releaseLibs() removed
    }

    override fun prepare() {
        assert(mParser != null)
        loadDanmakus(mParser!!)
        mCacheManager!!.begin()
    }

    inner class CacheManager(private val mMaxSize: Int, private var mScreenSize: Int = 3) {

        val mThread: HandlerThread? get() = _mThread
        private var _mThread: HandlerThread? = null

        val mCaches = Danmakus()

        val mCachePoolManager = DrawingCache.PoolManager

        val mCachePool: Pool<DrawingCache> = Pools.finitePool(mCachePoolManager, 800)

        private var mRealSize: Int = 0

        private var mHandler: CacheHandler? = null

        private var mEndFlag: Boolean = false

        init {
            mEndFlag = false
            mRealSize = 0
        }

        fun seek(mills: Long) {
            if (mHandler == null) return
            mHandler!!.requestCancelCaching()
            mHandler!!.removeMessages(CACHE_BUILD_CACHES)
            mHandler!!.obtainMessage(CACHE_SEEK, mills).sendToTarget()
        }

        fun addDanmaku(danmaku: BaseDanmaku) {
            if (mHandler != null) {
                if (danmaku.isLive) {
                    if (danmaku.forceBuildCacheInSameThread) {
                        if (!danmaku.isTimeOut()) {
                            mHandler!!.createCache(danmaku)
                        }
                    } else {
                        mHandler!!.obtainMessage(CACHE_BIND_CACHE, danmaku).sendToTarget()
                    }
                } else {
                    mHandler!!.obtainMessage(CACHE_ADD_DANMAKKU, danmaku).sendToTarget()
                }
            }
        }

        fun invalidateDanmaku(danmaku: BaseDanmaku, remeasure: Boolean) {
            if (mHandler != null) {
                mHandler!!.requestCancelCaching()
                val pair = Pair(danmaku, remeasure)
                mHandler!!.obtainMessage(CACHE_REBUILD_CACHE, pair).sendToTarget()
            }
        }

        fun begin() {
            mEndFlag = false
            if (_mThread == null) {
                _mThread = HandlerThread("DFM Cache-Building Thread")
                _mThread!!.start()
            }
            if (mHandler == null) mHandler = CacheHandler(_mThread!!.looper)
            mHandler!!.begin()
        }

        fun end() {
            mEndFlag = true
            synchronized(mDrawingNotify) {
                (mDrawingNotify as Object).notifyAll()
            }
            if (mHandler != null) {
                mHandler!!.pause()
                mHandler = null
            }
            if (_mThread != null) {
                try {
                    _mThread!!.join()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                _mThread!!.quit()
                _mThread = null
            }
        }

        fun resume() {
            if (mHandler != null) {
                mHandler!!.resume()
            } else {
                begin()
            }
        }

        fun getPoolPercent(): Float {
            if (mMaxSize == 0) return 0f
            return mRealSize / mMaxSize.toFloat()
        }

        fun isPoolFull(): Boolean {
            return mRealSize + 5120 >= mMaxSize
        }

        private fun evictAll() {
            if (mCaches != null) {
                val it = mCaches.iterator()
                while (it.hasNext()) {
                    val danmaku = it.next() ?: continue
                    entryRemoved(true, danmaku, null)
                }
                mCaches.clear()
            }
            mRealSize = 0
        }

        private fun evictAllNotInScreen() {
            evictAllNotInScreen(false)
        }

        private fun evictAllNotInScreen(removeAllReferences: Boolean) {
            if (mCaches != null) {
                val it = mCaches.iterator()
                while (it.hasNext()) {
                    val danmaku = it.next() ?: continue
                    val cache = danmaku.cache
                    val hasReferences = cache != null && cache.hasReferences()
                    if (removeAllReferences && hasReferences) {
                        if (cache!!.get() != null) {
                            mRealSize -= cache.size()
                            cache.destroy()
                        }
                        entryRemoved(true, danmaku, null)
                        it.remove()
                        continue
                    }
                    if (!danmaku.hasDrawingCache() || danmaku.isOutside()) {
                        entryRemoved(true, danmaku, null)
                        it.remove()
                    }
                }
            }
            mRealSize = 0
        }

        protected fun entryRemoved(evicted: Boolean, oldValue: BaseDanmaku, newValue: BaseDanmaku?) {
            if (oldValue.cache != null) {
                val cache = oldValue.cache
                val releasedSize = clearCache(oldValue)
                if (oldValue.isTimeOut()) {
                    mContext.getDisplayer().getCacheStuffer().releaseResource(oldValue)
                }
                if (releasedSize <= 0) return
                mRealSize -= releasedSize.toInt()
                mCachePool.release(cache!! as DrawingCache)
            }
        }

        private fun clearCache(oldValue: BaseDanmaku): Long {
            if (oldValue.cache!!.hasReferences()) {
                oldValue.cache!!.decreaseReference()
                oldValue.cache = null
                return 0
            }
            val size = sizeOf(oldValue)
            oldValue.cache!!.destroy()
            oldValue.cache = null
            return size.toLong()
        }

        protected fun sizeOf(value: BaseDanmaku): Int {
            val cache = value.cache
            if (cache != null && !cache.hasReferences()) {
                return cache.size()
            }
            return 0
        }

        private fun clearCachePool() {
            var item: DrawingCache?
            while (mCachePool.acquire().also { item = it } != null) {
                item!!.destroy()
            }
        }

        fun push(item: BaseDanmaku, itemSize: Int, forcePush: Boolean): Boolean {
            val size = itemSize
            while (mRealSize + size > mMaxSize && mCaches.size() > 0) {
                val oldValue = mCaches.first() ?: continue
                if (oldValue.isTimeOut()) {
                    entryRemoved(false, oldValue, item)
                    mCaches -= oldValue
                } else {
                    if (forcePush) break
                    return false
                }
            }
            mCaches += item
            mRealSize += size
            return true
        }

        private fun clearTimeOutCaches() {
            clearTimeOutCaches(mTimer.currMillisecond)
        }

        private fun clearTimeOutCaches(time: Long) {
            val it = mCaches.iterator()
            while (it.hasNext() && !mEndFlag) {
                val `val` = it.next() ?: continue
                if (`val`.isTimeOut()) {
                    synchronized(mDrawingNotify) {
                        try {
                            (mDrawingNotify as Object).wait(30)
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                            return
                        }
                    }
                    entryRemoved(false, `val`, null)
                    it.remove()
                } else {
                    break
                }
            }
        }

        private fun findReuseableCache(
            refDanmaku: BaseDanmaku,
            strictMode: Boolean,
            maximumTimes: Int
        ): BaseDanmaku? {
            val it = mCaches.iterator()
            var slopPixel = 0
            if (!strictMode) {
                slopPixel = mDisp.slopPixel * 2
            }
            var count = 0
            while (it.hasNext() && count++ < maximumTimes) {
                val danmaku = it.next() ?: continue
                if (!danmaku.hasDrawingCache()) continue
                if (danmaku.paintWidth == refDanmaku.paintWidth
                    && danmaku.paintHeight == refDanmaku.paintHeight
                    && danmaku.underlineColor == refDanmaku.underlineColor
                    && danmaku.borderColor == refDanmaku.borderColor
                    && danmaku.textColor == refDanmaku.textColor
                    && danmaku.text == refDanmaku.text
                ) {
                    return danmaku
                }
                if (strictMode) continue
                if (!danmaku.isTimeOut()) break
                if (danmaku.cache?.hasReferences() == true) continue
                val widthGap = danmaku.cache!!.width() - refDanmaku.paintWidth
                val heightGap = danmaku.cache!!.height() - refDanmaku.paintHeight
                if (widthGap >= 0 && widthGap <= slopPixel &&
                    heightGap >= 0 && heightGap <= slopPixel
                ) {
                    return danmaku
                }
            }
            return null
        }

        inner class CacheHandler(looper: Looper) : Handler(looper) {

            private var mPause = false
            private var mSeekedFlag = false
            private var mCancelFlag = false

            fun requestCancelCaching() {
                mCancelFlag = true
            }

            override fun handleMessage(msg: Message) {
                when (msg.what) {
                    CACHE_PREPARE -> {
                        evictAllNotInScreen()
                        for (i in 0 until 300) {
                            mCachePool.release(DrawingCache())
                        }
                        // Java fall-through to CACHE_DISPATCH_ACTIONS
                        val delayed0 = dispatchAction()
                        val actualDelayed0 = if (delayed0 <= 0) mContext.mDanmakuFactory.MAX_DANMAKU_DURATION / 2 else delayed0
                        sendEmptyMessageDelayed(CACHE_DISPATCH_ACTIONS, actualDelayed0)
                    }
                    CACHE_DISPATCH_ACTIONS -> {
                        val delayed = dispatchAction()
                        val actualDelayed = if (delayed <= 0) mContext.mDanmakuFactory.MAX_DANMAKU_DURATION / 2 else delayed
                        sendEmptyMessageDelayed(CACHE_DISPATCH_ACTIONS, actualDelayed)
                    }
                    CACHE_BUILD_CACHES -> {
                        removeMessages(CACHE_BUILD_CACHES)
                        val repositioned = (mTaskListener != null && !mReadyState) || mSeekedFlag
                        prepareCaches(repositioned)
                        if (repositioned) mSeekedFlag = false
                        if (mTaskListener != null && !mReadyState) {
                            mTaskListener!!.ready()
                            mReadyState = true
                        }
                    }
                    CACHE_ADD_DANMAKKU -> {
                        val item = msg.obj as BaseDanmaku
                        addDanmakuAndBuildCache(item)
                    }
                    CACHE_BIND_CACHE -> {
                        val danmaku = msg.obj as BaseDanmaku
                        if (!danmaku.isTimeOut()) {
                            createCache(danmaku)
                        }
                    }
                    CACHE_REBUILD_CACHE -> {
                        val pair = msg.obj as Pair<BaseDanmaku, Boolean>?
                        if (pair != null) {
                            val cacheitem = pair.first
                            if (pair.second == true) {
                                cacheitem.requestFlags = cacheitem.requestFlags or BaseDanmaku.FLAG_REQUEST_REMEASURE
                                cacheitem.measureResetFlag++
                            }
                            cacheitem.requestFlags = cacheitem.requestFlags or BaseDanmaku.FLAG_REQUEST_INVALIDATE
                            if (pair.second != true && cacheitem.hasDrawingCache() && cacheitem.cache?.hasReferences() != true) {
                                val cache = DanmakuUtils.buildDanmakuDrawingCache(cacheitem, mDisp, cacheitem.cache as DrawingCache)
                                cacheitem.cache = cache
                                push(cacheitem, 0, true)
                                return
                            }
                            if (cacheitem.isLive) {
                                clearCache(cacheitem)
                                createCache(cacheitem)
                            } else {
                                entryRemoved(true, cacheitem, null)
                                addDanmakuAndBuildCache(cacheitem)
                            }
                        }
                    }
                    CACHE_CLEAR_TIMEOUT_CACHES -> {
                        clearTimeOutCaches()
                    }
                    CACHE_SEEK -> {
                        val seekMills = msg.obj as? Long
                        if (seekMills != null) {
                            val seekCacheTime = seekMills
                            val oldCacheTime = mCacheTimer.currMillisecond
                            mCacheTimer.update(seekCacheTime)
                            mSeekedFlag = true
                            val firstCacheTime = getFirstCacheTime()
                            if (seekCacheTime > oldCacheTime || firstCacheTime - seekCacheTime > mContext.mDanmakuFactory.MAX_DANMAKU_DURATION) {
                                evictAllNotInScreen()
                            } else {
                                clearTimeOutCaches()
                            }
                            prepareCaches(true)
                            resume()
                        }
                    }
                    CACHE_QUIT -> {
                        removeCallbacksAndMessages(null)
                        mPause = true
                        evictAll()
                        clearCachePool()
                        looper.quit()
                    }
                    CACHE_CLEAR_ALL_CACHES -> {
                        evictAll()
                        mCacheTimer.update(mTimer.currMillisecond - mContext.mDanmakuFactory.MAX_DANMAKU_DURATION)
                        mSeekedFlag = true
                    }
                    CACHE_CLEAR_OUTSIDE_CACHES -> {
                        evictAllNotInScreen(true)
                        mCacheTimer.update(mTimer.currMillisecond)
                    }
                    CACHE_CLEAR_OUTSIDE_CACHES_AND_RESET -> {
                        evictAllNotInScreen(true)
                        mCacheTimer.update(mTimer.currMillisecond)
                        requestClear()
                    }
                }
            }

            private fun dispatchAction(): Long {
                if (mCacheTimer.currMillisecond <= mTimer.currMillisecond - mContext.mDanmakuFactory.MAX_DANMAKU_DURATION) {
                    evictAllNotInScreen()
                    mCacheTimer.update(mTimer.currMillisecond)
                    sendEmptyMessage(CACHE_BUILD_CACHES)
                    return 0
                }
                val level = getPoolPercent()
                val firstCache = mCaches.first()
                val gapTime = if (firstCache != null) firstCache.time - mTimer.currMillisecond else 0
                val doubleScreenDuration = mContext.mDanmakuFactory.MAX_DANMAKU_DURATION * 2
                if (level < 0.6f && gapTime > mContext.mDanmakuFactory.MAX_DANMAKU_DURATION) {
                    mCacheTimer.update(mTimer.currMillisecond)
                    removeMessages(CACHE_BUILD_CACHES)
                    sendEmptyMessage(CACHE_BUILD_CACHES)
                    return 0
                } else if (level > 0.4f && gapTime < -doubleScreenDuration) {
                    removeMessages(CACHE_CLEAR_TIMEOUT_CACHES)
                    sendEmptyMessage(CACHE_CLEAR_TIMEOUT_CACHES)
                    return 0
                }

                if (level >= 0.9f) return 0

                val deltaTime = mCacheTimer.currMillisecond - mTimer.currMillisecond
                if (firstCache != null && firstCache.isTimeOut() && deltaTime < -mContext.mDanmakuFactory.MAX_DANMAKU_DURATION) {
                    mCacheTimer.update(mTimer.currMillisecond)
                    sendEmptyMessage(CACHE_CLEAR_OUTSIDE_CACHES)
                    sendEmptyMessage(CACHE_BUILD_CACHES)
                    return 0
                } else if (deltaTime > doubleScreenDuration) {
                    return 0
                }

                removeMessages(CACHE_BUILD_CACHES)
                sendEmptyMessage(CACHE_BUILD_CACHES)
                return 0
            }

            private fun releaseDanmakuCache(item: BaseDanmaku, cache: DrawingCache?) {
                var actualCache = cache
                if (actualCache == null) {
                    actualCache = item.cache as? DrawingCache
                }
                item.cache = null
                if (actualCache == null) return
                actualCache.destroy()
                mCachePool.release(actualCache)
            }

            private fun prepareCaches(repositioned: Boolean): Long {
                val curr = mCacheTimer.currMillisecond
                var end = curr + mContext.mDanmakuFactory.MAX_DANMAKU_DURATION * mScreenSize
                if (end < mTimer.currMillisecond) return 0
                val startTime = SystemClock.uptimeMillis()
                var danmakus: IDanmakus? = null
                var tryCount = 0
                var hasException = false
                do {
                    try {
                        danmakus = danmakuList!!.subnew(curr, end)
                    } catch (e: Exception) {
                        hasException = true
                        SystemClock.sleep(10)
                    }
                } while (++tryCount < 3 && danmakus == null && hasException)
                if (danmakus == null) {
                    mCacheTimer.update(end)
                    return 0
                }
                val first = danmakus.first()
                val last = danmakus.last()
                if (first == null || last == null) {
                    mCacheTimer.update(end)
                    return 0
                }
                val deltaTime = first.time - mTimer.currMillisecond
                var sleepTime = 30 + 10 * deltaTime / mContext.mDanmakuFactory.MAX_DANMAKU_DURATION
                sleepTime = Math.max(0, Math.min(100, sleepTime))
                if (repositioned) sleepTime = 0

                val itr = danmakus.iterator()
                var item: BaseDanmaku? = null
                var consumingTime = 0L
                var orderInScreen = 0
                var currScreenIndex = 0
                val sizeInScreen = danmakus.size()
                while (!mPause && !mCancelFlag) {
                    val hasNext = itr.hasNext()
                    if (!hasNext) break
                    item = itr.next() ?: continue

                    if (last.time < mTimer.currMillisecond) break

                    if (item!!.hasDrawingCache()) continue

                    if (!repositioned && (item!!.isTimeOut() || !item!!.isOutside())) continue

                    if (!item!!.hasPassedFilter()) {
                        mContext.mDanmakuFilters.filter(item!!, orderInScreen, sizeInScreen, null, true, mContext)
                    }

                    if (item!!.priority == 0.toByte() && item!!.isFiltered()) continue

                    if (item!!.getType() == BaseDanmaku.TYPE_SCROLL_RL) {
                        val screenIndex = ((item!!.time - curr) / mContext.mDanmakuFactory.MAX_DANMAKU_DURATION).toInt()
                        if (currScreenIndex == screenIndex) orderInScreen++
                        else {
                            orderInScreen = 0
                            currScreenIndex = screenIndex
                        }
                    }

                    if (!repositioned) {
                        try {
                            synchronized(mDrawingNotify) {
                                (mDrawingNotify as Object).wait(sleepTime.toLong())
                            }
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                            break
                        }
                    }

                    if (buildCache(item!!, false) == RESULT_FAILED) break

                    if (!repositioned) {
                        consumingTime = SystemClock.uptimeMillis() - startTime
                        if (consumingTime >= DanmakuFactory.COMMON_DANMAKU_DURATION * mScreenSize) break
                    }
                }
                consumingTime = SystemClock.uptimeMillis() - startTime
                if (item != null) {
                    mCacheTimer.update(item.time)
                } else {
                    mCacheTimer.update(end)
                }
                return consumingTime
            }

            fun createCache(item: BaseDanmaku): Boolean {
                if (!item.isMeasured()) {
                    item.measure(mDisp, true)
                }
                var cache: DrawingCache? = null
                try {
                    cache = mCachePool.acquire()
                    cache = DanmakuUtils.buildDanmakuDrawingCache(item, mDisp, cache)
                    item.cache = cache
                } catch (e: OutOfMemoryError) {
                    if (cache != null) mCachePool.release(cache)
                    item.cache = null
                    return false
                } catch (e: Exception) {
                    if (cache != null) mCachePool.release(cache)
                    item.cache = null
                    return false
                }
                return true
            }

            private fun buildCache(item: BaseDanmaku, forceInsert: Boolean): Byte {
                if (!item.isMeasured()) {
                    item.measure(mDisp, true)
                }

                var cache: DrawingCache? = null
                try {
                    var danmaku = findReuseableCache(item, true, 20)
                    if (danmaku != null) {
                        cache = danmaku.cache as? DrawingCache
                    }
                    if (cache != null) {
                        cache.increaseReference()
                        item.cache = cache
                        push(item, 0, forceInsert)
                        return RESULT_SUCCESS
                    }

                    danmaku = findReuseableCache(item, false, 50)
                    if (danmaku != null) {
                        cache = danmaku.cache as? DrawingCache
                    }
                    if (cache != null) {
                        danmaku!!.cache = null
                        cache = DanmakuUtils.buildDanmakuDrawingCache(item, mDisp, cache)
                        item.cache = cache
                        push(item, 0, forceInsert)
                        return RESULT_SUCCESS
                    }

                    if (!forceInsert) {
                        val cacheSize = DanmakuUtils.getCacheSize(
                            item.paintWidth.toInt(), item.paintHeight.toInt()
                        )
                        if (mRealSize + cacheSize > mMaxSize) return RESULT_FAILED
                    }

                    cache = mCachePool.acquire()
                    cache = DanmakuUtils.buildDanmakuDrawingCache(item, mDisp, cache)
                    item.cache = cache
                    val pushed = push(item, sizeOf(item), forceInsert)
                    if (!pushed) releaseDanmakuCache(item, cache)
                    return if (pushed) RESULT_SUCCESS else RESULT_FAILED

                } catch (e: OutOfMemoryError) {
                    releaseDanmakuCache(item, cache)
                    return RESULT_FAILED
                } catch (e: Exception) {
                    releaseDanmakuCache(item, cache)
                    return RESULT_FAILED
                }
            }

            private fun addDanmakuAndBuildCache(danmaku: BaseDanmaku) {
                if (danmaku.isTimeOut() || (danmaku.time > mCacheTimer.currMillisecond + mContext.mDanmakuFactory.MAX_DANMAKU_DURATION && !danmaku.isLive)) return
                if (danmaku.priority == 0.toByte() && danmaku.isFiltered()) return
                if (!danmaku.hasDrawingCache()) {
                    buildCache(danmaku, true)
                }
            }

            fun begin() {
                sendEmptyMessage(CACHE_PREPARE)
                sendEmptyMessageDelayed(CACHE_CLEAR_TIMEOUT_CACHES, mContext.mDanmakuFactory.MAX_DANMAKU_DURATION)
            }

            fun pause() {
                mPause = true
                removeCallbacksAndMessages(null)
                sendEmptyMessage(CACHE_QUIT)
            }

            fun resume() {
                mCancelFlag = false
                mPause = false
                removeMessages(CACHE_DISPATCH_ACTIONS)
                sendEmptyMessage(CACHE_DISPATCH_ACTIONS)
                sendEmptyMessageDelayed(CACHE_CLEAR_TIMEOUT_CACHES, mContext.mDanmakuFactory.MAX_DANMAKU_DURATION)
            }

            fun isPause(): Boolean = mPause

            fun requestBuildCacheAndDraw(correctionTime: Long) {
                removeMessages(CACHE_BUILD_CACHES)
                mSeekedFlag = true
                mCancelFlag = false
                mCacheTimer.update(mTimer.currMillisecond + correctionTime)
                sendEmptyMessage(CACHE_BUILD_CACHES)
            }

        }

        fun getFirstCacheTime(): Long {
            if (mCaches != null && mCaches.size() > 0) {
                val firstItem = mCaches.first() ?: return 0
                return firstItem.time
            }
            return 0
        }

        fun requestBuild(correctionTime: Long) {
            mHandler?.requestBuildCacheAndDraw(correctionTime)
        }

        fun requestClearAll() {
            if (mHandler == null) return
            mHandler!!.removeMessages(CACHE_BUILD_CACHES)
            mHandler!!.requestCancelCaching()
            mHandler!!.removeMessages(CACHE_CLEAR_ALL_CACHES)
            mHandler!!.sendEmptyMessage(CACHE_CLEAR_ALL_CACHES)
        }

        fun requestClearUnused() {
            if (mHandler == null) return
            mHandler!!.removeMessages(CACHE_CLEAR_OUTSIDE_CACHES_AND_RESET)
            mHandler!!.sendEmptyMessage(CACHE_CLEAR_OUTSIDE_CACHES_AND_RESET)
        }

        fun requestClearTimeout() {
            if (mHandler == null) return
            mHandler!!.removeMessages(CACHE_CLEAR_TIMEOUT_CACHES)
            mHandler!!.sendEmptyMessage(CACHE_CLEAR_TIMEOUT_CACHES)
        }

        fun post(runnable: Runnable) {
            mHandler?.post(runnable)
        }

    }

    override fun onDanmakuConfigChanged(config: DanmakuContext, tag: DanmakuConfigTag?, vararg values: Any?): Boolean {
        if (super.handleOnDanmakuConfigChanged(config, tag, values)) {
            // do nothing
        } else if (tag != null && tag.isVisibilityRelatedTag()) {
            if (values.isNotEmpty()) {
                if (values[0] != null && (values[0] !is Boolean || (values[0] as Boolean))) {
                    mCacheManager?.requestBuild(0L)
                }
            }
            requestClear()
        } else if (DanmakuConfigTag.TRANSPARENCY == tag || DanmakuConfigTag.SCALE_TEXTSIZE == tag) {
            if (DanmakuConfigTag.SCALE_TEXTSIZE == tag) {
                mDisp.resetSlopPixel(mContext.scaleTextSize)
            }
            mCacheManager?.requestClearAll()
            mCacheManager?.requestBuild(-mContext.mDanmakuFactory.MAX_DANMAKU_DURATION)
        } else {
            mCacheManager?.requestClearUnused()
            mCacheManager?.requestBuild(0L)
        }

        if (mTaskListener != null && mCacheManager != null) {
            mCacheManager!!.post { mTaskListener!!.onDanmakuConfigChanged() }
        }
        return true
    }

    companion object {
        private const val MAX_CACHE_SCREEN_SIZE = 3

        // CacheHandler message IDs
        const val CACHE_PREPARE = 0x1
        const val CACHE_ADD_DANMAKKU = 0x2
        const val CACHE_BUILD_CACHES = 0x3
        const val CACHE_CLEAR_TIMEOUT_CACHES = 0x4
        const val CACHE_SEEK = 0x5
        const val CACHE_QUIT = 0x6
        const val CACHE_CLEAR_ALL_CACHES = 0x7
        const val CACHE_CLEAR_OUTSIDE_CACHES = 0x8
        const val CACHE_CLEAR_OUTSIDE_CACHES_AND_RESET = 0x9
        const val CACHE_DISPATCH_ACTIONS = 0x10
        const val CACHE_REBUILD_CACHE = 0x11
        const val CACHE_BIND_CACHE = 0x12

        // CacheManager result codes
        const val RESULT_SUCCESS: Byte = 0
        const val RESULT_FAILED: Byte = 1
        const val RESULT_FAILED_OVERSIZE: Byte = 2
    }
}
