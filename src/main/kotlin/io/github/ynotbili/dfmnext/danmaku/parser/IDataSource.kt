package io.github.ynotbili.dfmnext.danmaku.parser

interface IDataSource<T> {
    fun data(): T?
    fun release()

    companion object {
        const val SCHEME_HTTP_TAG = "http"
        const val SCHEME_HTTPS_TAG = "https"
        const val SCHEME_FILE_TAG = "file"
    }
}
