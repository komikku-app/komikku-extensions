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
