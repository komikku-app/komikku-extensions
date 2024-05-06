package eu.kanade.tachiyomi.extension.all.hentairox

import eu.kanade.tachiyomi.multisrc.galleryadults.GalleryAdults
import eu.kanade.tachiyomi.multisrc.galleryadults.Genre
import eu.kanade.tachiyomi.multisrc.galleryadults.imgAttr
import eu.kanade.tachiyomi.network.GET
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class HentaiRox(
    lang: String = "all",
    override val mangaLang: String = LANGUAGE_MULTI,
) : GalleryAdults(
    "HentaiRox",
    "https://hentairox.com",
    lang = lang,
) {
    override val supportsLatest = true

    override fun Element.mangaLang() =
        select("a:has(.thumb_flag)").attr("href")
            .removeSuffix("/").substringAfterLast("/")

    override fun Element.mangaTitle(selector: String): String? =
        mangaFullTitle(selector.takeIf { it != ".caption" } ?: ".gallery_title").let {
            if (preferences.shortTitle) it?.shortenTitle() else it
        }

    override fun popularMangaRequest(page: Int): Request {
        return if (mangaLang.isBlank()) {
            val url = baseUrl.toHttpUrl().newBuilder().apply {
                addPathSegments("top-rated")
                addPageUri(page)
            }
            return GET(url.build(), headers)
        } else {
            super.popularMangaRequest(page)
        }
    }

    /**
     * Convert space( ) typed in search-box into plus(+) in URL. Then:
     * - ignore the word preceding by a special character (e.g. 'school-girl' will ignore 'girl')
     *    => replace to plus(+),
     * - use plus(+) for separate terms, as AND condition.
     * - use double quote(") to search for exact match.
     */
    override fun buildQueryString(tags: List<String>, query: String): String {
        val regexSpecialCharacters = Regex("""[^a-zA-Z0-9"]+(?=[a-zA-Z0-9"])""")
        return (tags + query + mangaLang).filterNot { it.isBlank() }.joinToString("+") {
            it.trim().replace(regexSpecialCharacters, "+")
        }
    }

    /* Details */
    override fun Element.getInfo(tag: String): String {
        return select("li:has(.tags_text:contains($tag)) a.tag")
            .joinToString {
                val name = it.selectFirst(".item_name")?.ownText() ?: ""
                if (tag.contains(regexTag)) {
                    genres[name] = it.attr("href")
                        .removeSuffix("/").substringAfterLast('/')
                }
                listOf(
                    name,
                    it.select(".split_tag").text()
                        .removePrefix("| ")
                        .trim(),
                )
                    .filter { s -> s.isNotBlank() }
                    .joinToString()
            }
    }

    override fun Element.getCover() =
        selectFirst(".left_cover img")?.imgAttr()

    override val mangaDetailInfoSelector = ".gallery_first"

    /* Pages */
    override val idPrefixUri = "gallery"
    override val thumbnailSelector = ".gthumb"
    override val pageUri = "view"

    /* Filters */
    override fun tagsParser(document: Document): List<Genre> {
        return document.select(".galleries .gallery_title a")
            .mapNotNull {
                Genre(
                    it.ownText(),
                    it.attr("href")
                        .removeSuffix("/").substringAfterLast('/'),
                )
            }
    }
}
