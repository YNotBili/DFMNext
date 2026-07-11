package io.github.ynotbili.dfmnext.controller

import io.github.ynotbili.dfmnext.danmaku.util.SystemClock

open class UpdateThread(name: String) : Thread(name) {

    @Volatile
    var mIsQuited: Boolean = false

    fun quit() {
        mIsQuited = true
    }

    fun isQuited(): Boolean = mIsQuited

    override fun run() {
        if (mIsQuited) return
    }
}
