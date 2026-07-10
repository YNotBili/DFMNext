package rj.dfmnext.danmaku.parser.android

import android.graphics.Color
import android.text.TextUtils
import org.json.JSONArray
import rj.dfmnext.danmaku.model.BaseDanmaku
import rj.dfmnext.danmaku.model.Duration
import rj.dfmnext.danmaku.model.IDisplayer
import rj.dfmnext.danmaku.model.SpecialDanmaku
import rj.dfmnext.danmaku.model.android.DanmakuFactory
import rj.dfmnext.danmaku.model.android.Danmakus
import rj.dfmnext.danmaku.parser.BaseDanmakuParser
import rj.dfmnext.danmaku.util.DanmakuUtils

class BiliProtobufDanmakuParser : BaseDanmakuParser() {

    private var mDispScaleX: Float = 0f
    private var mDispScaleY: Float = 0f
    private var mDanmakuSegments: List<*>? = null

    fun setDanmakuSegments(segments: List<*>) {
        this.mDanmakuSegments = segments
    }

    override fun parse(): Danmakus {
        val segments = mDanmakuSegments
        if (segments.isNullOrEmpty()) {
            return Danmakus()
        }

        val result = Danmakus()
        var index = 0

        for (segmentObj in segments) {
            try {
                val elemsField = segmentObj!!::class.java.getField("elems")
                val elems = elemsField.get(segmentObj) as? List<*>

                if (elems != null) {
                    for (elemObj in elems) {
                        val danmaku = parseDanmakuElem(elemObj!!, index++)
                        if (danmaku != null) {
                            result += danmaku
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return result
    }

    private fun parseDanmakuElem(elemObj: Any, index: Int): BaseDanmaku? {
        try {
            val elemClass = elemObj.javaClass

            val progress = elemClass.getField("progress").getInt(elemObj)
            var mode = elemClass.getField("mode").getInt(elemObj)
            val fontsize = elemClass.getField("fontsize").getInt(elemObj)
            val color = elemClass.getField("color").getInt(elemObj)
            var content = elemClass.getField("content").get(elemObj) as? String

            if (content.isNullOrEmpty()) return null
            if (mode == 8) return null

            val enableAdvanced = sharedPreferences == null ||
                sharedPreferences!!.getBoolean("player_danmaku_advanced_enable", true)

            if (!enableAdvanced && (mode == 7 || mode == 8)) {
                if (mode == 7 && content.startsWith("[") && content.endsWith("]")) {
                    try {
                        val jsonArray = JSONArray(content)
                        if (jsonArray.length() >= 5) {
                            content = jsonArray.getString(4)
                        }
                    } catch (_: Exception) {}
                }
                mode = 1
            }

            if (sharedPreferences != null && mode != 7 && mode != 8 &&
                sharedPreferences!!.getBoolean("player_danmaku_forceR2L", false)
            ) {
                mode = 1
            }

            val item = mContext.mDanmakuFactory.createDanmaku(mode, mContext) ?: return null

            item.time = progress.toLong()
            DanmakuUtils.fillText(item, content)
            item.index = index
            item.textSize = fontsize * (mDispDensity - 0.6f)
            item.textColor = color or -0x1000000
            item.textShadowColor = if ((color or -0x1000000) <= Color.BLACK) Color.WHITE else Color.BLACK
            item.setTimer(mTimer)

            if (mode == 7 && content.startsWith("[") && content.endsWith("]")) {
                val textArr = SpecialDanmakuParser.parseFromJson(content)
                if (textArr != null && textArr.size >= 5) {
                    SpecialDanmakuParser.parse(item as SpecialDanmaku, textArr, mContext, mDispScaleX, mDispScaleY)
                }
            } else {
                DanmakuUtils.fillText(item, content)
            }

            item.setTimer(mTimer)
            return item
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    override fun setDisplayer(disp: IDisplayer): BaseDanmakuParser {
        super.setDisplayer(disp)
        mDispScaleX = mDispWidth / DanmakuFactory.BILI_PLAYER_WIDTH
        mDispScaleY = mDispHeight / DanmakuFactory.BILI_PLAYER_HEIGHT
        return this
    }
}
