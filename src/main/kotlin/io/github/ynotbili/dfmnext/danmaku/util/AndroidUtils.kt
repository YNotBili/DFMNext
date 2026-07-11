package io.github.ynotbili.dfmnext.danmaku.util

import android.app.ActivityManager
import android.content.Context

object AndroidUtils {

    fun getMemoryClass(context: Context): Int {
        return (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).memoryClass
    }
}
