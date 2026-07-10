package rj.dfmnext.danmaku.parser.android

import android.graphics.Color
import android.text.TextUtils
import rj.dfmnext.danmaku.model.BaseDanmaku
import rj.dfmnext.danmaku.model.Duration
import rj.dfmnext.danmaku.model.SpecialDanmaku
import rj.dfmnext.danmaku.model.android.DanmakuContext
import rj.dfmnext.danmaku.model.android.DanmakuFactory
import rj.dfmnext.danmaku.util.DanmakuUtils
import org.json.JSONArray

object SpecialDanmakuParser {

    fun parse(
        danmaku: SpecialDanmaku,
        textArr: Array<String>,
        context: DanmakuContext,
        dispScaleX: Float,
        dispScaleY: Float
    ) {
        if (textArr.size < 5) return
        DanmakuUtils.fillText(danmaku, textArr[4])
        var beginX = textArr[0].toFloat()
        var beginY = textArr[1].toFloat()
        var endX = beginX
        var endY = beginY
        val alphaArr = textArr[2].split("-")
        val beginAlpha = (BaseDanmaku.ALPHA_MAX * alphaArr[0].toFloat()).toInt()
        var endAlpha = beginAlpha
        if (alphaArr.size > 1) {
            endAlpha = (BaseDanmaku.ALPHA_MAX * alphaArr[1].toFloat()).toInt()
        }
        val alphaDuration = (textArr[3].toFloat() * 1000).toLong()
        var translationDuration = alphaDuration
        var translationStartDelay = 0L
        var rotateY = 0f
        var rotateZ = 0f
        if (textArr.size >= 7 && textArr[5].isNotEmpty()) {
            rotateZ = textArr[5].toFloat()
            rotateY = textArr[6].toFloat()
        }
        if (textArr.size >= 11 && textArr[7].isNotEmpty()) {
            endX = textArr[7].toFloat()
            endY = textArr[8].toFloat()
            if (textArr[9] != "") translationDuration = textArr[9].toInt().toLong()
            if (textArr[10] != "") translationStartDelay = textArr[10].toFloat().toLong()
        }
        if (beginX in 0f..1f) beginX *= DanmakuFactory.BILI_PLAYER_WIDTH
        if (beginY in 0f..1f) beginY *= DanmakuFactory.BILI_PLAYER_HEIGHT
        if (endX in 0f..1f) endX *= DanmakuFactory.BILI_PLAYER_WIDTH
        if (endY in 0f..1f) endY *= DanmakuFactory.BILI_PLAYER_HEIGHT
        danmaku.duration = Duration(alphaDuration)
        danmaku.rotationZ = rotateZ
        danmaku.rotationY = rotateY
        context.mDanmakuFactory.fillTranslationData(
            danmaku, beginX, beginY, endX, endY,
            translationDuration, translationStartDelay, dispScaleX, dispScaleY
        )
        context.mDanmakuFactory.fillAlphaData(danmaku, beginAlpha, endAlpha, alphaDuration)

        if (textArr.size >= 12) {
            if (!TextUtils.isEmpty(textArr[11]) && "true" == textArr[11]) {
                danmaku.textShadowColor = Color.TRANSPARENT
            }
        }
        if (textArr.size >= 15 && textArr[14] != "") {
            val motionPathString = textArr[14].substring(1)
            val pointStrArray = motionPathString.split("L")
            if (pointStrArray.isNotEmpty()) {
                val points = Array(pointStrArray.size) { FloatArray(2) }
                for (i in pointStrArray.indices) {
                    val pointArray = pointStrArray[i].split(",")
                    points[i][0] = pointArray[0].toFloat()
                    points[i][1] = pointArray[1].toFloat()
                }
                DanmakuFactory.fillLinePathData(danmaku, points, dispScaleX, dispScaleY)
            }
        }
    }

    fun parseFromJson(text: String): Array<String>? {
        if (!text.startsWith("[") || !text.endsWith("]")) return null
        return try {
            val jsonArray = JSONArray(text)
            Array(jsonArray.length()) { i -> jsonArray.optString(i, "") }
        } catch (_: Exception) {
            null
        }
    }
}
