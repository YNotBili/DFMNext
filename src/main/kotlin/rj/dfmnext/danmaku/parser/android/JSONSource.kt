package rj.dfmnext.danmaku.parser.android

import android.net.Uri
import android.text.TextUtils
import rj.dfmnext.danmaku.parser.IDataSource
import rj.dfmnext.danmaku.util.IOUtils
import org.json.JSONArray
import org.json.JSONException
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.net.URL

class JSONSource : IDataSource<JSONArray> {

    private var mJSONArray: JSONArray? = null
    private var mInput: InputStream? = null

    @Throws(JSONException::class)
    constructor(json: String) {
        init(json)
    }

    @Throws(JSONException::class)
    constructor(input: InputStream) {
        init(input)
    }

    @Throws(JSONException::class, IOException::class)
    constructor(url: URL) {
        init(url.openStream())
    }

    @Throws(FileNotFoundException::class, JSONException::class)
    constructor(file: File) {
        init(FileInputStream(file))
    }

    @Throws(IOException::class, JSONException::class)
    constructor(uri: Uri) {
        val scheme = uri.scheme
        if (IDataSource.SCHEME_HTTP_TAG.equals(scheme, ignoreCase = true) ||
            IDataSource.SCHEME_HTTPS_TAG.equals(scheme, ignoreCase = true)
        ) {
            init(URL(uri.path).openStream())
        } else if (IDataSource.SCHEME_FILE_TAG.equals(scheme, ignoreCase = true)) {
            init(FileInputStream(uri.path))
        }
    }

    @Throws(JSONException::class)
    private fun init(input: InputStream) {
        requireNotNull(input) { "input stream cannot be null!" }
        mInput = input
        val json = IOUtils.getString(mInput!!)
        init(json ?: "")
    }

    @Throws(JSONException::class)
    private fun init(json: String) {
        if (!TextUtils.isEmpty(json)) {
            mJSONArray = JSONArray(json)
        }
    }

    override fun data(): JSONArray? = mJSONArray

    override fun release() {
        IOUtils.closeQuietly(mInput)
        mInput = null
        mJSONArray = null
    }
}
