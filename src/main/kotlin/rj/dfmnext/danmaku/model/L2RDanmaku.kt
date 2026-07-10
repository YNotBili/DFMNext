package rj.dfmnext.danmaku.model

class L2RDanmaku(duration: Duration) : R2LDanmaku(duration) {

    override fun getAccurateLeft(displayer: IDisplayer, currTime: Long): Float {
        val elapsedTime = currTime - time
        if (elapsedTime >= (duration?.value ?: 0)) {
            return displayer.width.toFloat()
        }
        return mStepX * elapsedTime - paintWidth
    }

    override fun getType(): Int = TYPE_SCROLL_LR
}
