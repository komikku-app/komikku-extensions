package eu.kanade.tachiyomi.extension.all.projectsuki

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.nodes.Element

typealias NormalizedURL = HttpUrl

val NormalizedURL.rawAbsolute: String
    get() = toString()

private val psDomainURI = """https://projectsuki.com/""".toHttpUrl().toUri()

val NormalizedURL.rawRelative: String?
    get() {
        val uri = toUri()
        return psDomainURI
            .relativize(uri)
            .takeIf { it != uri }
            ?.let { """/$it""" }
    }

private val protocolMatcher = """^https?://""".toRegex()
private val domainMatcher = """^https?://(?:[a-zA-Z\d\-]+\.)+[a-zA-Z\d\-]+""".toRegex()
fun String.toNormalURL(): NormalizedURL? {
    if (contains(':') && !contains(protocolMatcher)) {
        return null
    }

    val toParse = StringBuilder()

    if (!contains(domainMatcher)) {
        toParse.append("https://projectsuki.com")
        if (!this.startsWith("/")) toParse.append('/')
    }

    toParse.append(this)

    return toParse.toString().toHttpUrlOrNull()
}

fun NormalizedURL.pathStartsWith(other: Iterable<String>): Boolean = pathSegments.zip(other).all { (l, r) -> l == r }

fun NormalizedURL.isPSUrl() = host.endsWith("${PS.identifier}.com")

fun NormalizedURL.isBookURL() = isPSUrl() && pathSegments.first() == "book"
fun NormalizedURL.isReadURL() = isPSUrl() && pathStartsWith(PS.chapterPath)
fun NormalizedURL.isImagesGalleryURL() = isPSUrl() && pathStartsWith(PS.pagePath)

fun Element.attrNormalizedUrl(attrName: String): NormalizedURL? {
    val attrValue = attr("abs:$attrName").takeIf { it.isNotBlank() } ?: return null
    return attrValue.toNormalURL()
}
