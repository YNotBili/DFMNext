package rj.dfmnext.danmaku.parser.android

import android.graphics.Color
import rj.dfmnext.danmaku.model.BaseDanmaku
import rj.dfmnext.danmaku.model.IDisplayer
import rj.dfmnext.danmaku.model.SpecialDanmaku
import rj.dfmnext.danmaku.model.android.DanmakuFactory
import rj.dfmnext.danmaku.model.android.Danmakus
import rj.dfmnext.danmaku.parser.BaseDanmakuParser
import rj.dfmnext.danmaku.util.DanmakuUtils
import rj.dfmnext.danmaku.util.isSpecial
import org.json.JSONArray
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import org.xml.sax.helpers.XMLReaderFactory
import java.util.Locale

class BiliDanmukuParser : BaseDanmakuParser() {

    private var mDispScaleX: Float = 0f
    private var mDispScaleY: Float = 0f

    companion object {
        init {
            System.setProperty("org.xml.sax.driver", "org.xmlpull.v1.sax2.Driver")
        }
    }

    override fun parse(): Danmakus? {
        if (mDataSource != null) {
            val source = mDataSource as AndroidFileSource
            try {
                val xmlReader = XMLReaderFactory.createXMLReader()
                val contentHandler = XmlContentHandler()
                xmlReader.contentHandler = contentHandler
                xmlReader.parse(InputSource(source.data()))
                return contentHandler.result
            } catch (e: Exception) {
                // XML parse error, skip
            }
        }
        return null
    }

    private inner class XmlContentHandler : DefaultHandler() {

        var result: Danmakus? = null
        var item: BaseDanmaku? = null
        var completed: Boolean = false
        var index: Int = 0

        override fun startDocument() {
            result = Danmakus()
        }

        override fun endDocument() {
            completed = true
        }

        override fun startElement(uri: String, localName: String, qName: String, attributes: Attributes) {
            var tagName = if (localName.isNotEmpty()) localName else qName
            tagName = tagName.lowercase(Locale.getDefault()).trim()
            if (tagName == "d") {
                val pValue = attributes.getValue("p")
                val values = pValue.split(",")
                if (values.isNotEmpty()) {
                    val time = (values[0].toFloat() * 1000).toLong()
                    var type = values[1].toInt()

                    val enableAdvanced = sharedPreferences == null ||
                        sharedPreferences!!.getBoolean("player_danmaku_advanced_enable", true)
                    if (!enableAdvanced && (type == 7 || type == 8)) {
                        type = 1
                    }
                    if (sharedPreferences != null && type != 7 && type != 8 &&
                        sharedPreferences!!.getBoolean("player_danmaku_forceR2L", false)
                    ) {
                        type = 1
                    }

                    val textSize = values[2].toFloat()
                    val color = values[3].toInt() or -0x1000000
                    item = mContext.mDanmakuFactory.createDanmaku(type, mContext)
                    item?.apply {
                        this.time = time
                        this.textSize = textSize * (mDispDensity - 0.6f)
                        textColor = color
                        textShadowColor = if (color <= Color.BLACK) Color.WHITE else Color.BLACK
                    }
                }
            }
        }

        override fun endElement(uri: String, localName: String, qName: String) {
            item?.let { danmaku ->
                if (danmaku.duration != null) {
                    val tagName = if (localName.isNotEmpty()) localName else qName
                    if (tagName.equals("d", ignoreCase = true)) {
                        danmaku.setTimer(mTimer)
                        result?.let { it += danmaku }
                    }
                }
                item = null
            }
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            item?.let { danmaku ->
                var rawText = decodeXmlString(String(ch, start, length))
                val enableAdvanced = sharedPreferences == null ||
                    sharedPreferences!!.getBoolean("player_danmaku_advanced_enable", true)
                if (!enableAdvanced && rawText.startsWith("[") && rawText.endsWith("]")) {
                    try {
                        val jsonArray = JSONArray(rawText)
                        if (jsonArray.length() >= 5) {
                            rawText = jsonArray.getString(4)
                        }
                    } catch (_: Exception) {}
                }
                DanmakuUtils.fillText(danmaku, rawText)
                danmaku.index = index++

                val text = danmaku.text?.toString()?.trim() ?: ""
                if (danmaku.isSpecial) {
                    val textArr = SpecialDanmakuParser.parseFromJson(text)
                    if (textArr != null && textArr.size >= 5) {
                        SpecialDanmakuParser.parse(danmaku as SpecialDanmaku, textArr, mContext, mDispScaleX, mDispScaleY)
                    } else {
                        item = null
                        return
                    }
                }
            }
        }

        private fun decodeXmlString(title: String): String {
            var result = title
            if (result.contains("&amp;")) result = result.replace("&amp;", "&")
            if (result.contains("&quot;")) result = result.replace("&quot;", "\"")
            if (result.contains("&gt;")) result = result.replace("&gt;", ">")
            if (result.contains("&lt;")) result = result.replace("&lt;", "<")
            return result
        }
    }

    private fun isPercentageNumber(number: Float): Boolean {
        return number in 0f..1f
    }

    override fun setDisplayer(disp: IDisplayer): BaseDanmakuParser {
        super.setDisplayer(disp)
        mDispScaleX = mDispWidth / DanmakuFactory.BILI_PLAYER_WIDTH
        mDispScaleY = mDispHeight / DanmakuFactory.BILI_PLAYER_HEIGHT
        return this
    }
}
