package rj.dfmnext.danmaku.model

class Duration(initialDuration: Long) {

    private var mInitialDuration: Long = initialDuration
    private var factor: Float = 1.0f

    @JvmField var value: Long = initialDuration

    fun setValue(initialDuration: Long) {
        mInitialDuration = initialDuration
        value = (mInitialDuration / normalizeFactor(factor)).toLong()
    }

    fun setFactor(f: Float) {
        if (factor != f) {
            factor = f
            value = (mInitialDuration / normalizeFactor(f)).toLong()
        }
    }

    private fun normalizeFactor(f: Float): Float = if (f <= 0f) 1.0f else f
}
