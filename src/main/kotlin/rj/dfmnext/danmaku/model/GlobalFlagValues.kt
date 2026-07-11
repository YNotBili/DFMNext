package rj.dfmnext.danmaku.model

class GlobalFlagValues {
    @Volatile var MEASURE_RESET_FLAG: Int = 0
    @Volatile var VISIBLE_RESET_FLAG: Int = 0
    @Volatile var FILTER_RESET_FLAG: Int = 0
    @Volatile var FIRST_SHOWN_RESET_FLAG: Int = 0

    fun resetAll() {
        VISIBLE_RESET_FLAG = 0
        MEASURE_RESET_FLAG = 0
        FILTER_RESET_FLAG = 0
        FIRST_SHOWN_RESET_FLAG = 0
    }

    fun updateVisibleFlag() { VISIBLE_RESET_FLAG++ }
    fun updateMeasureFlag() { MEASURE_RESET_FLAG++ }
    fun updateFilterFlag() { FILTER_RESET_FLAG++ }
    fun updateFirstShownFlag() { FIRST_SHOWN_RESET_FLAG++ }
}
