package rj.dfmnext.danmaku.util

object SystemClock {

    fun uptimeMillis(): Long = android.os.SystemClock.elapsedRealtime()

    fun sleep(mills: Long) {
        android.os.SystemClock.sleep(mills)
    }
}
