package rj.dfmnext.danmaku.parser.android

import android.net.Uri
import rj.dfmnext.danmaku.parser.IDataSource
import rj.dfmnext.danmaku.util.IOUtils
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.net.MalformedURLException
import java.net.URL

class AndroidFileSource : IDataSource<InputStream> {

    private var inStream: InputStream? = null

    constructor(filepath: String) {
        fillStreamFromFile(File(filepath))
    }

    constructor(uri: Uri) {
        fillStreamFromUri(uri)
    }

    constructor(file: File) {
        fillStreamFromFile(file)
    }

    constructor(stream: InputStream) {
        this.inStream = stream
    }

    fun fillStreamFromFile(file: File) {
        try {
            inStream = BufferedInputStream(FileInputStream(file))
        } catch (e: FileNotFoundException) {
            // File not found
        }
    }

    fun fillStreamFromUri(uri: Uri) {
        val scheme = uri.scheme
        if (IDataSource.SCHEME_HTTP_TAG.equals(scheme, ignoreCase = true) ||
            IDataSource.SCHEME_HTTPS_TAG.equals(scheme, ignoreCase = true)
        ) {
            fillStreamFromHttpFile(uri)
        } else if (IDataSource.SCHEME_FILE_TAG.equals(scheme, ignoreCase = true)) {
            fillStreamFromFile(File(uri.path))
        }
    }

    fun fillStreamFromHttpFile(uri: Uri) {
        try {
            val url = URL(uri.path)
            url.openConnection()
            inStream = BufferedInputStream(url.openStream())
        } catch (e: MalformedURLException) {
            // Invalid URL
        } catch (e: IOException) {
            // IO error
        }
    }

    override fun release() {
        IOUtils.closeQuietly(inStream)
        inStream = null
    }

    override fun data(): InputStream? = inStream
}
