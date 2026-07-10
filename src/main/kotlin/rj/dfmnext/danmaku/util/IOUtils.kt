package rj.dfmnext.danmaku.util

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

object IOUtils {

    fun getString(input: InputStream): String? {
        val data = getBytes(input) ?: return null
        return String(data)
    }

    fun getBytes(input: InputStream): ByteArray? {
        return try {
            val baos = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var len: Int
            while (input.read(buffer).also { len = it } != -1) {
                baos.write(buffer, 0, len)
            }
            input.close()
            baos.toByteArray()
        } catch (e: Exception) {
            null
        }
    }

    fun closeQuietly(input: InputStream?) {
        try {
            input?.close()
        } catch (_: Exception) {
        }
    }

    fun closeQuietly(output: OutputStream?) {
        try {
            output?.close()
        } catch (_: Exception) {
        }
    }
}
