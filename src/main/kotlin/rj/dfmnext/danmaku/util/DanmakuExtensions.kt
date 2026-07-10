package rj.dfmnext.danmaku.util

import rj.dfmnext.danmaku.model.BaseDanmaku

val BaseDanmaku.isScrollRL: Boolean get() = getType() == BaseDanmaku.TYPE_SCROLL_RL
val BaseDanmaku.isScrollLR: Boolean get() = getType() == BaseDanmaku.TYPE_SCROLL_LR
val BaseDanmaku.isScrolling: Boolean get() = isScrollRL || isScrollLR
val BaseDanmaku.isFixTop: Boolean get() = getType() == BaseDanmaku.TYPE_FIX_TOP
val BaseDanmaku.isFixBottom: Boolean get() = getType() == BaseDanmaku.TYPE_FIX_BOTTOM
val BaseDanmaku.isFixed: Boolean get() = isFixTop || isFixBottom
val BaseDanmaku.isSpecial: Boolean get() = getType() == BaseDanmaku.TYPE_SPECIAL

val BaseDanmaku.isVisible: Boolean get() = visibility == BaseDanmaku.VISIBLE

val BaseDanmaku.actualDuration: Long get() = duration?.value ?: 0
