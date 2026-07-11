package io.github.ynotbili.dfmnext

import io.github.ynotbili.dfmnext.danmaku.util.IOUtils
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IOUtilsTest {

    @Test
    fun `getBytes reads all bytes`() {
        val data = byteArrayOf(1, 2, 3, 4, 5)
        val result = IOUtils.getBytes(ByteArrayInputStream(data))
        assertContentEquals(data, result)
    }

    @Test
    fun `getBytes returns empty array for empty stream`() {
        val result = IOUtils.getBytes(ByteArrayInputStream(byteArrayOf()))
        assertContentEquals(byteArrayOf(), result)
    }

    @Test
    fun `getBytes returns null on exception`() {
        val broken = object : InputStream() {
            override fun read(): Int = throw IOException("broken")
        }
        assertNull(IOUtils.getBytes(broken))
    }

    @Test
    fun `getString reads text`() {
        val data = "hello world".toByteArray()
        assertEquals("hello world", IOUtils.getString(ByteArrayInputStream(data)))
    }

    @Test
    fun `getString returns null on exception`() {
        val broken = object : InputStream() {
            override fun read(): Int = throw IOException("broken")
        }
        assertNull(IOUtils.getString(broken))
    }

    @Test
    fun `closeQuietly handles null input stream`() {
        IOUtils.closeQuietly(null as InputStream?) // should not throw
    }

    @Test
    fun `closeQuietly handles null output stream`() {
        IOUtils.closeQuietly(null as ByteArrayOutputStream?) // should not throw
    }

    @Test
    fun `closeQuietly closes stream`() {
        var closed = false
        val stream = object : InputStream() {
            override fun read(): Int = -1
            override fun close() { closed = true }
        }
        IOUtils.closeQuietly(stream)
        assertEquals(true, closed)
    }
}
