package io.github.ynotbili.dfmnext.danmaku.model.android

import kotlin.reflect.KMutableProperty0
import io.github.ynotbili.dfmnext.controller.DanmakuFilters
import io.github.ynotbili.dfmnext.controller.DanmakuFilters.IDanmakuFilter
import io.github.ynotbili.dfmnext.danmaku.model.AbsDisplayer
import io.github.ynotbili.dfmnext.danmaku.model.BaseDanmaku
import io.github.ynotbili.dfmnext.danmaku.model.GlobalFlagValues

class DanmakuContext {

    companion object {
        fun create(): DanmakuContext = DanmakuContext()
    }

    enum class DanmakuConfigTag {
        FT_DANMAKU_VISIBILITY, FB_DANMAKU_VISIBILITY, L2R_DANMAKU_VISIBILITY, R2L_DANMAKU_VISIBILITY,
        SPECIAL_DANMAKU_VISIBILITY, TRANSPARENCY, SCALE_TEXTSIZE,
        DUPLICATE_MERGING_ENABLED, MAXIMUM_LINES, OVERLAPPING_ENABLE;

        fun isVisibilityRelatedTag(): Boolean {
            return this == FT_DANMAKU_VISIBILITY || this == FB_DANMAKU_VISIBILITY ||
                this == L2R_DANMAKU_VISIBILITY || this == R2L_DANMAKU_VISIBILITY ||
                this == SPECIAL_DANMAKU_VISIBILITY
        }
    }

    var transparency: Int = BaseDanmaku.ALPHA_MAX
    var scaleTextSize: Float = 1.0f
    var FTDanmakuVisibility: Boolean = true
    var FBDanmakuVisibility: Boolean = true
    var L2RDanmakuVisibility: Boolean = true
    var R2LDanmakuVisibility: Boolean = true
    var SpecialDanmakuVisibility: Boolean = true

    val mFilterTypes: MutableList<Int> = ArrayList()
    var refreshRateMS: Int = 15

    private var configChangedCallback: ConfigChangedCallback? = null
    private var mDuplicateMergingEnable: Boolean = false
    private var mIsMaxLinesLimited: Boolean = false
    private var mIsPreventOverlappingEnabled: Boolean = false

    private val mDisplayer: AbsDisplayer by lazy { AndroidDisplayer() }

    val mGlobalFlagValues: GlobalFlagValues = GlobalFlagValues()
    val mDanmakuFilters: DanmakuFilters = DanmakuFilters()
    val mDanmakuFactory: DanmakuFactory = DanmakuFactory.create()

    fun getDisplayer(): AbsDisplayer = mDisplayer

    fun setDanmakuTransparency(p: Float): DanmakuContext {
        val newTransparency = (p * BaseDanmaku.ALPHA_MAX).toInt()
        if (newTransparency != transparency) {
            transparency = newTransparency
            mDisplayer.setTransparency(newTransparency)
            notifyConfigureChanged(DanmakuConfigTag.TRANSPARENCY, p)
        }
        return this
    }

    fun setScaleTextSize(p: Float): DanmakuContext {
        if (scaleTextSize != p) {
            scaleTextSize = p
            mDisplayer.clearTextHeightCache()
            mDisplayer.setScaleTextSizeFactor(p)
            mGlobalFlagValues.updateMeasureFlag()
            mGlobalFlagValues.updateVisibleFlag()
            notifyConfigureChanged(DanmakuConfigTag.SCALE_TEXTSIZE, p)
        }
        return this
    }

    fun setFTDanmakuVisibility(visible: Boolean): DanmakuContext {
        setVisibilityAndUpdate(visible, BaseDanmaku.TYPE_FIX_TOP, ::FTDanmakuVisibility, DanmakuConfigTag.FT_DANMAKU_VISIBILITY)
        return this
    }

    fun setFBDanmakuVisibility(visible: Boolean): DanmakuContext {
        setVisibilityAndUpdate(visible, BaseDanmaku.TYPE_FIX_BOTTOM, ::FBDanmakuVisibility, DanmakuConfigTag.FB_DANMAKU_VISIBILITY)
        return this
    }

    fun setL2RDanmakuVisibility(visible: Boolean): DanmakuContext {
        setVisibilityAndUpdate(visible, BaseDanmaku.TYPE_SCROLL_LR, ::L2RDanmakuVisibility, DanmakuConfigTag.L2R_DANMAKU_VISIBILITY)
        return this
    }

    fun setR2LDanmakuVisibility(visible: Boolean): DanmakuContext {
        setVisibilityAndUpdate(visible, BaseDanmaku.TYPE_SCROLL_RL, ::R2LDanmakuVisibility, DanmakuConfigTag.R2L_DANMAKU_VISIBILITY)
        return this
    }

    fun setSpecialDanmakuVisibility(visible: Boolean): DanmakuContext {
        setVisibilityAndUpdate(visible, BaseDanmaku.TYPE_SPECIAL, ::SpecialDanmakuVisibility, DanmakuConfigTag.SPECIAL_DANMAKU_VISIBILITY)
        return this
    }

    fun setDuplicateMergingEnabled(enable: Boolean): DanmakuContext {
        if (mDuplicateMergingEnable != enable) {
            mDuplicateMergingEnable = enable
            mGlobalFlagValues.updateFilterFlag()
            notifyConfigureChanged(DanmakuConfigTag.DUPLICATE_MERGING_ENABLED, enable)
        }
        return this
    }

    fun isDuplicateMergingEnabled(): Boolean = mDuplicateMergingEnable

    fun setMaximumLines(pairs: Map<Int, Int>?): DanmakuContext {
        mIsMaxLinesLimited = (pairs != null)
        if (pairs == null) {
            mDanmakuFilters.unregisterFilter(DanmakuFilters.TAG_MAXIMUN_LINES_FILTER, false)
        } else {
            setFilterData(DanmakuFilters.TAG_MAXIMUN_LINES_FILTER, pairs, false)
        }
        mGlobalFlagValues.updateFilterFlag()
        notifyConfigureChanged(DanmakuConfigTag.MAXIMUM_LINES, pairs)
        return this
    }

    @Deprecated("Use preventOverlapping", ReplaceWith("preventOverlapping(pairs)"))
    fun setOverlapping(pairs: Map<Int, Boolean>?): DanmakuContext = preventOverlapping(pairs)

    fun preventOverlapping(pairs: Map<Int, Boolean>?): DanmakuContext {
        mIsPreventOverlappingEnabled = (pairs != null)
        if (pairs == null) {
            mDanmakuFilters.unregisterFilter(DanmakuFilters.TAG_OVERLAPPING_FILTER, false)
        } else {
            setFilterData(DanmakuFilters.TAG_OVERLAPPING_FILTER, pairs, false)
        }
        mGlobalFlagValues.updateFilterFlag()
        notifyConfigureChanged(DanmakuConfigTag.OVERLAPPING_ENABLE, pairs)
        return this
    }

    fun isMaxLinesLimited(): Boolean = mIsMaxLinesLimited
    fun isPreventOverlappingEnabled(): Boolean = mIsPreventOverlappingEnabled

    interface ConfigChangedCallback {
        fun onDanmakuConfigChanged(config: DanmakuContext, tag: DanmakuConfigTag, vararg value: Any?): Boolean
    }

    fun registerConfigChangedCallback(listener: ConfigChangedCallback) {
        configChangedCallback = listener
    }

    fun unregisterConfigChangedCallback(listener: ConfigChangedCallback) {
        if (configChangedCallback == listener) configChangedCallback = null
    }

    fun unregisterAllConfigChangedCallbacks() {
        configChangedCallback = null
    }

    private fun setVisibilityAndUpdate(visible: Boolean, type: Int, field: KMutableProperty0<Boolean>, tag: DanmakuConfigTag) {
        setDanmakuVisible(visible, type)
        setFilterData(DanmakuFilters.TAG_TYPE_DANMAKU_FILTER, mFilterTypes)
        mGlobalFlagValues.updateFilterFlag()
        if (field.get() != visible) {
            field.set(visible)
            notifyConfigureChanged(tag, visible)
        }
    }

    private fun <T> setFilterData(tag: String, data: T) {
        setFilterData(tag, data, true)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> setFilterData(tag: String, data: T, primary: Boolean) {
        val filter = mDanmakuFilters.get(tag, primary) as IDanmakuFilter<T>
        filter.setData(data)
    }

    private fun setDanmakuVisible(visible: Boolean, type: Int) {
        if (visible) {
            mFilterTypes.remove(type)
        } else if (!mFilterTypes.contains(type)) {
            mFilterTypes.add(type)
        }
    }

    private fun notifyConfigureChanged(tag: DanmakuConfigTag, vararg values: Any?) {
        configChangedCallback?.onDanmakuConfigChanged(this, tag, *values)
    }
}
