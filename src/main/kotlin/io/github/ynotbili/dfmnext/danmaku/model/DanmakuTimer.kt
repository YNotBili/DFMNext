package io.github.ynotbili.dfmnext.danmaku.model

class DanmakuTimer {

    var currMillisecond: Long = 0

    private var lastInterval: Long = 0

    fun update(curr: Long): Long {
        lastInterval = curr - currMillisecond
        currMillisecond = curr
        return lastInterval
    }

    fun add(mills: Long): Long = update(currMillisecond + mills)

    fun lastInterval(): Long = lastInterval
}
