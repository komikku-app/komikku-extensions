package eu.kanade.tachiyomi.extension.zh.dmzj

import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Response
import uy.kohesive.injekt.injectLazy
import java.net.URLDecoder
import kotlin.math.max

const val PREFIX_ID_SEARCH = "id:"

val json: Json by injectLazy()

inline fun <reified T> Response.parseAs(): T {
    return json.decodeFromString(body.string())
}

fun getMangaUrl(id: String) = "/comic/comic_$id.json?version=2.7.019"

fun String.extractMangaId(): String {
    val start = 13 // length of "/comic/comic_"
    return substring(start, indexOf('.', start))
}

fun String.formatList() = replace("/", ", ")

fun parseStatus(status: String): Int = when (status) {
    "连载中" -> SManga.ONGOING
    "已完结" -> SManga.COMPLETED
    else -> SManga.UNKNOWN
}

private val chapterNameRegex = Regex("""(?:连载版?)?(\d[.\d]*)([话卷])?""")

fun String.formatChapterName(): String {
    val match = chapterNameRegex.matchEntire(this) ?: return this
    val (number, optionalType) = match.destructured
    val type = optionalType.ifEmpty { "话" }
    return "第$number$type"
}

private const val imageSmallUrl = "https://imgsmall.idmzj.com"

fun parsePageList(
    mangaId: Int,
    chapterId: Int,
    images: List<String>,
    lowResImages: List<String>,
): ArrayList<Page> {
    // page count can be messy, see manga ID 55847 chapters 107-109
    val pageCount = max(images.size, lowResImages.size)
    val list = ArrayList<Page>(pageCount + 1) // for comments page
    for (i in 0 until pageCount) {
        val imageUrl = images.getOrNull(i)?.fixFilename()?.toHttps()
        val lowResUrl = lowResImages.getOrElse(i) {
            // this is sometimes different in low-res URLs and might fail, see manga ID 56649
            val initial = imageUrl!!.decodePath().toHttpUrl().pathSegments[0]
            "$imageSmallUrl/$initial/$mangaId/$chapterId/$i.jpg"
        }.toHttps()
        list.add(Page(i, url = lowResUrl, imageUrl = imageUrl ?: lowResUrl))
    }
    return list
}

fun String.toHttps() = "https:" + substringAfter(':')

// see https://github.com/tachiyomiorg/tachiyomi-extensions/issues/3457
fun String.fixFilename() = if (endsWith(".jp")) this + 'g' else this

fun String.decodePath(): String = URLDecoder.decode(this, "UTF-8")

const val COMMENTS_FLAG = "COMMENTS"
