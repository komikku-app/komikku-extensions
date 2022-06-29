package eu.kanade.tachiyomi.extension.zh.pufei

import eu.kanade.tachiyomi.AppInfo
import eu.kanade.tachiyomi.source.model.SManga
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.text.SimpleDateFormat
import java.util.Locale

internal val GB2312 = charset("GB2312")

internal fun Response.asPufeiJsoup(): Document =
    Jsoup.parse(String(body!!.bytes(), GB2312), request.url.toString())

internal fun SManga.urlWithCheck(): String {
    val result = url
    if (result.endsWith("/index.html")) {
        throw Exception("作品地址格式过期，请迁移更新")
    }
    return result
}

internal val isNewDateLogic = AppInfo.getVersionCode() >= 81

internal val dateFormat by lazy {
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH)
}

internal class ProgressiveParser(private val text: String) {
    private var startIndex = 0
    fun consumeUntil(string: String) = with(text) { startIndex = indexOf(string, startIndex) + string.length }
    fun substringBetween(left: String, right: String): String = with(text) {
        val leftIndex = indexOf(left, startIndex) + left.length
        val rightIndex = indexOf(right, leftIndex)
        startIndex = rightIndex + right.length
        return substring(leftIndex, rightIndex)
    }
}

internal fun unpack(data: String, dictionary: List<String>): String {
    val size = dictionary.size
    return Regex("""\b\w+\b""").replace(data) {
        with(it.value) {
            val key = parseRadix62()
            if (key >= size) return@replace this
            val value = dictionary[key]
            if (value.isEmpty()) return@replace this
            return@replace value
        }
    }
}

private fun String.parseRadix62(): Int {
    var result = 0
    for (char in this) {
        result = result * 62 + when {
            char <= '9' -> char.code - '0'.code
            char >= 'a' -> char.code - 'a'.code + 10
            else -> char.code - 'A'.code + 36
        }
    }
    return result
}
