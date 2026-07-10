package rj.dfmnext.controller

import rj.dfmnext.danmaku.model.BaseDanmaku
import rj.dfmnext.danmaku.model.DanmakuTimer
import rj.dfmnext.danmaku.model.IDanmakus
import rj.dfmnext.danmaku.model.android.DanmakuContext
import rj.dfmnext.danmaku.model.android.Danmakus
import rj.dfmnext.danmaku.util.SystemClock
import java.util.Collections
import java.util.LinkedHashMap
import java.util.TreeMap

class DanmakuFilters {

    interface IDanmakuFilter<T> {
        fun filter(
            danmaku: BaseDanmaku, index: Int, totalsizeInScreen: Int,
            timer: DanmakuTimer?, fromCachingTask: Boolean, config: DanmakuContext
        ): Boolean

        fun setData(data: T?)
        fun reset()
        fun clear()
    }

    abstract class BaseDanmakuFilter<T> : IDanmakuFilter<T> {
        override fun clear() {}
    }

    class TypeDanmakuFilter : BaseDanmakuFilter<List<Int>>() {

        val mFilterTypes: MutableList<Int> = Collections.synchronizedList(ArrayList())

        fun enableType(type: Int) {
            if (!mFilterTypes.contains(type)) mFilterTypes.add(type)
        }

        fun disableType(type: Int) {
            if (mFilterTypes.contains(type)) mFilterTypes.remove(type)
        }

        override fun filter(
            danmaku: BaseDanmaku, index: Int, totalsizeInScreen: Int,
            timer: DanmakuTimer?, fromCachingTask: Boolean, config: DanmakuContext
        ): Boolean {
            val filtered = mFilterTypes.contains(danmaku.getType())
            if (filtered) {
                danmaku.mFilterParam = danmaku.mFilterParam or FILTER_TYPE_TYPE
            }
            return filtered
        }

        override fun setData(data: List<Int>?) {
            reset()
            data?.forEach { enableType(it) }
        }

        override fun reset() {
            mFilterTypes.clear()
        }
    }

    class DuplicateMergingFilter : BaseDanmakuFilter<Void?>() {

        protected val blockedDanmakus: IDanmakus = Danmakus(Danmakus.ST_BY_LIST)

        protected val currentDanmakus: LinkedHashMap<String, BaseDanmaku> = LinkedHashMap()
        private val passedDanmakus: IDanmakus = Danmakus(Danmakus.ST_BY_LIST)

        private fun removeTimeoutDanmakus(danmakus: IDanmakus, limitTime: Long) {
            val it = danmakus.iterator()
            val startTime = SystemClock.uptimeMillis()
            while (it.hasNext()) {
                try {
            val item = it.next()
                    if (item?.isTimeOut() == true) {
                        it.remove()
                    } else {
                        break
                    }
                } catch (_: Exception) {
                    break
                }
                if (SystemClock.uptimeMillis() - startTime > limitTime) break
            }
        }

        private fun removeTimeoutDanmakus(danmakus: LinkedHashMap<String, BaseDanmaku>, limitTime: Int) {
            val it = danmakus.entries.iterator()
            val startTime = SystemClock.uptimeMillis()
            while (it.hasNext()) {
                try {
                    val entry = it.next()
                    if (entry.value.isTimeOut()) {
                        it.remove()
                    } else {
                        break
                    }
                } catch (_: Exception) {
                    break
                }
                if (SystemClock.uptimeMillis() - startTime > limitTime) break
            }
        }

        @Synchronized
        fun needFilter(
            danmaku: BaseDanmaku, index: Int, totalsizeInScreen: Int,
            timer: DanmakuTimer?, fromCachingTask: Boolean
        ): Boolean {
            removeTimeoutDanmakus(blockedDanmakus, 2)
            removeTimeoutDanmakus(passedDanmakus, 2)
            removeTimeoutDanmakus(currentDanmakus, 3)
            if (danmaku in blockedDanmakus && !danmaku.isOutside()) return true
            if (danmaku in passedDanmakus) return false

            val textStr = danmaku.text.toString()
            if (currentDanmakus.containsKey(textStr)) {
                val original = currentDanmakus[textStr]
                if (original != null && !original.isTimeOut()) {
                    original.mMergeCount++
                    original.text = original.mOriginalText.toString() + " (x" + (original.mMergeCount + 1) + ")"

                    val scale = 1.0f + (original.mMergeCount * 0.1f).coerceAtMost(0.5f)
                    original.textSize = original.mOriginalTextSize * scale

                    original.measureResetFlag++
                    original.requestFlags =
                        original.requestFlags or BaseDanmaku.FLAG_REQUEST_REMEASURE or BaseDanmaku.FLAG_REQUEST_INVALIDATE
                    if (original.cache != null) {
                        original.cache!!.destroy()
                        original.cache = null
                    }

                    blockedDanmakus -= danmaku
                    blockedDanmakus += danmaku
                    return true
                }
            }
            danmaku.mOriginalText = danmaku.text
            danmaku.mOriginalTextSize = danmaku.textSize
            currentDanmakus[textStr] = danmaku
            passedDanmakus += danmaku
            return false
        }

        override fun filter(
            danmaku: BaseDanmaku, index: Int, totalsizeInScreen: Int,
            timer: DanmakuTimer?, fromCachingTask: Boolean, config: DanmakuContext
        ): Boolean {
            val filtered = needFilter(danmaku, index, totalsizeInScreen, timer, fromCachingTask)
            if (filtered) {
                danmaku.mFilterParam = danmaku.mFilterParam or FILTER_TYPE_DUPLICATE_MERGE
            }
            return filtered
        }

        override fun setData(data: Void?) {}

        @Synchronized
        override fun reset() {
            passedDanmakus.clear()
            blockedDanmakus.clear()
            currentDanmakus.clear()
        }

        override fun clear() {
            reset()
        }
    }

    class MaximumLinesFilter : BaseDanmakuFilter<Map<Int, Int>>() {

        private var mMaximumLinesPairs: Map<Int, Int>? = null

        override fun filter(
            danmaku: BaseDanmaku, index: Int, totalsizeInScreen: Int,
            timer: DanmakuTimer?, fromCachingTask: Boolean, config: DanmakuContext
        ): Boolean {
            var filtered = false
            if (mMaximumLinesPairs != null) {
                val maxLines = mMaximumLinesPairs!![danmaku.getType()]
                filtered = maxLines != null && index >= maxLines
                if (filtered) {
                    danmaku.mFilterParam = danmaku.mFilterParam or FILTER_TYPE_MAXIMUM_LINES
                }
            }
            return filtered
        }

        override fun setData(data: Map<Int, Int>?) {
            mMaximumLinesPairs = data
        }

        override fun reset() {
            mMaximumLinesPairs = null
        }
    }

    class OverlappingFilter : BaseDanmakuFilter<Map<Int, Boolean>>() {

        private var mEnabledPairs: Map<Int, Boolean>? = null

        override fun filter(
            danmaku: BaseDanmaku, index: Int, totalsizeInScreen: Int,
            timer: DanmakuTimer?, fromCachingTask: Boolean, config: DanmakuContext
        ): Boolean {
            var filtered = false
            if (mEnabledPairs != null) {
                val enabledValue = mEnabledPairs!![danmaku.getType()]
                filtered = enabledValue != null && enabledValue && fromCachingTask
                if (filtered) {
                    danmaku.mFilterParam = danmaku.mFilterParam or FILTER_TYPE_OVERLAPPING
                }
            }
            return filtered
        }

        override fun setData(data: Map<Int, Boolean>?) {
            mEnabledPairs = data
        }

        override fun reset() {
            mEnabledPairs = null
        }
    }

    fun filter(
        danmaku: BaseDanmaku, index: Int, totalsizeInScreen: Int,
        timer: DanmakuTimer?, fromCachingTask: Boolean, context: DanmakuContext
    ) {
        for (f in mFilterArray) {
            if (f != null) {
                val filtered = f.filter(danmaku, index, totalsizeInScreen, timer, fromCachingTask, context)
                danmaku.filterResetFlag = context.mGlobalFlagValues.FILTER_RESET_FLAG
                if (filtered) break
            }
        }
    }

    fun filterSecondary(
        danmaku: BaseDanmaku, index: Int, totalsizeInScreen: Int,
        timer: DanmakuTimer?, willHit: Boolean, context: DanmakuContext
    ): Boolean {
        for (f in mFilterArraySecondary) {
            if (f != null) {
                val filtered = f.filter(danmaku, index, totalsizeInScreen, timer, willHit, context)
                danmaku.filterResetFlag = context.mGlobalFlagValues.FILTER_RESET_FLAG
                if (filtered) return true
            }
        }
        return false
    }

    private val filters: MutableMap<String, IDanmakuFilter<*>> = Collections.synchronizedSortedMap(TreeMap())
    private val filtersSecondary: MutableMap<String, IDanmakuFilter<*>> = Collections.synchronizedSortedMap(TreeMap())

    var mFilterArray: Array<IDanmakuFilter<*>?> = arrayOfNulls(0)

    var mFilterArraySecondary: Array<IDanmakuFilter<*>?> = arrayOfNulls(0)

    operator fun get(tag: String): IDanmakuFilter<*>? = get(tag, true)

    operator fun get(tag: String, primary: Boolean): IDanmakuFilter<*> {
        val f = if (primary) filters[tag] else filtersSecondary[tag]
        return f ?: registerFilter(tag, primary)!!
    }

    fun registerFilter(tag: String): IDanmakuFilter<*>? = registerFilter(tag, true)

    fun registerFilter(tag: String, primary: Boolean): IDanmakuFilter<*>? {
        var filter = filters[tag]
        if (filter == null) {
            filter = when (tag) {
                TAG_TYPE_DANMAKU_FILTER -> TypeDanmakuFilter()
                TAG_DUPLICATE_FILTER -> DuplicateMergingFilter()
                TAG_MAXIMUN_LINES_FILTER -> MaximumLinesFilter()
                TAG_OVERLAPPING_FILTER -> OverlappingFilter()
                else -> null
            }
        }
        if (filter == null) {
            return null
        }
        filter.setData(null)
        if (primary) {
            filters[tag] = filter
            mFilterArray = filters.values.toTypedArray()
        } else {
            filtersSecondary[tag] = filter
            mFilterArraySecondary = filtersSecondary.values.toTypedArray()
        }
        return filter
    }

    fun unregisterFilter(tag: String) {
        unregisterFilter(tag, true)
    }

    fun unregisterFilter(tag: String, primary: Boolean) {
        val f = if (primary) filters.remove(tag) else filtersSecondary.remove(tag)
        if (f != null) {
            f.clear()
            if (primary) {
                mFilterArray = filters.values.toTypedArray()
            } else {
                mFilterArraySecondary = filtersSecondary.values.toTypedArray()
            }
        }
    }

    fun clear() {
        mFilterArray.forEach { it?.clear() }
        mFilterArraySecondary.forEach { it?.clear() }
    }

    fun reset() {
        mFilterArray.forEach { it?.reset() }
        mFilterArraySecondary.forEach { it?.reset() }
    }

    fun release() {
        clear()
        filters.clear()
        mFilterArray = arrayOfNulls(0)
        filtersSecondary.clear()
        mFilterArraySecondary = arrayOfNulls(0)
    }

    companion object {
        const val FILTER_TYPE_TYPE = 1
        const val FILTER_TYPE_DUPLICATE_MERGE = 128
        const val FILTER_TYPE_MAXIMUM_LINES = 256
        const val FILTER_TYPE_OVERLAPPING = 512

        const val TAG_TYPE_DANMAKU_FILTER = "1010_Filter"
        const val TAG_DUPLICATE_FILTER = "1017_Filter"
        const val TAG_MAXIMUN_LINES_FILTER = "1018_Filter"
        const val TAG_OVERLAPPING_FILTER = "1019_Filter"
    }
}
