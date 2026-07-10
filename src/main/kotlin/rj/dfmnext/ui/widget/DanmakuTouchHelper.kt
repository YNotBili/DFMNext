package rj.dfmnext.ui.widget

import android.graphics.RectF
import android.view.MotionEvent
import rj.dfmnext.controller.IDanmakuView
import rj.dfmnext.danmaku.model.BaseDanmaku
import rj.dfmnext.danmaku.model.IDanmakus
import rj.dfmnext.danmaku.model.android.Danmakus

class DanmakuTouchHelper private constructor(
    private val danmakuView: IDanmakuView
) {

    private val mDanmakuBounds = RectF()

    fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_UP -> {
                val clickDanmakus = touchHitDanmaku(event.x, event.y)
                var newestDanmaku: BaseDanmaku? = null
                if (clickDanmakus != null && !clickDanmakus.isEmpty()) {
                    performClick(clickDanmakus)
                    newestDanmaku = fetchLatestOne(clickDanmakus)
                }
                if (newestDanmaku != null) {
                    performClickWithLatest(newestDanmaku)
                }
            }
        }
        return false
    }

    private fun performClickWithLatest(newest: BaseDanmaku) {
        danmakuView.getOnDanmakuClickListener()?.onDanmakuClick(newest)
    }

    private fun performClick(danmakus: IDanmakus) {
        danmakuView.getOnDanmakuClickListener()?.onDanmakuClick(danmakus)
    }

    private fun touchHitDanmaku(x: Float, y: Float): IDanmakus {
        val hitDanmakus: IDanmakus = Danmakus()
        mDanmakuBounds.setEmpty()

        val danmakus = danmakuView.getCurrentVisibleDanmakus()
        if (danmakus != null && !danmakus.isEmpty()) {
            val iterator = danmakus.iterator()
            while (iterator.hasNext()) {
                val danmaku = iterator.next()
                if (danmaku != null) {
                    mDanmakuBounds.set(danmaku.getLeft(), danmaku.getTop(), danmaku.getRight(), danmaku.getBottom())
                    if (mDanmakuBounds.contains(x, y)) {
                        hitDanmakus += danmaku
                    }
                }
            }
        }

        return hitDanmakus
    }

    private fun fetchLatestOne(danmakus: IDanmakus): BaseDanmaku? {
        if (danmakus.isEmpty()) return null
        return danmakus.last()
    }

    companion object {
        @Synchronized
        fun instance(danmakuView: IDanmakuView): DanmakuTouchHelper {
            return DanmakuTouchHelper(danmakuView)
        }
    }
}
