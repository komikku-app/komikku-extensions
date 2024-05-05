package eu.kanade.tachiyomi.extension.all.hentaiera

import eu.kanade.tachiyomi.multisrc.galleryadults.GalleryAdults
import eu.kanade.tachiyomi.multisrc.galleryadults.SearchFlagFilter
import eu.kanade.tachiyomi.multisrc.galleryadults.SortOrderFilter
import eu.kanade.tachiyomi.multisrc.galleryadults.cleanTag
import eu.kanade.tachiyomi.multisrc.galleryadults.imgAttr
import eu.kanade.tachiyomi.source.model.FilterList
import okhttp3.FormBody
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class HentaiEra(
    lang: String = "all",
    override val mangaLang: String = LANGUAGE_MULTI,
) : GalleryAdults(
    "HentaiEra",
    "https://hentaiera.com",
    lang = lang,
) {
    override val supportsLatest = true
    override val useIntermediateSearch: Boolean = true

    override fun Element.mangaLang() =
        select("a:has(.g_flag)").attr("href")
            .removeSuffix("/").substringAfterLast("/")

    override fun Element.mangaTitle(selector: String): String? =
        mangaFullTitle(selector.takeIf { it != ".caption" } ?: ".gallery_title").let {
            if (preferences.shortTitle) it?.shortenTitle() else it
        }

    override fun popularMangaRequest(page: Int): Request {
        return if (mangaLang.isBlank()) {
            val popularFilter = SortOrderFilter(getSortOrderURIs())
                .apply {
                    state = 0
                }
            searchMangaRequest(page, "", FilterList(popularFilter))
        } else {
            super.popularMangaRequest(page)
        }
    }

    /* Details */
    override fun Element.getInfo(tag: String): String {
        return select("li:has(.tags_text:contains($tag)) .tag .item_name").map {
            it?.run {
                listOf(
                    ownText().cleanTag(),
                    select(".split_tag").text()
                        .trim()
                        .removePrefix("| ")
                        .cleanTag(),
                )
                    .filter { s -> s.isNotBlank() }
                    .joinToString()
            }
        }.joinToString()
    }

    override fun Element.getCover() =
        selectFirst(".left_cover img")?.imgAttr()

    override val mangaDetailInfoSelector = ".gallery_first"

    /* Pages */
    override val idPrefixUri = "gallery"
    override val pageUri = "view"
    override val pageSelector = ".gthumb"

    /* Filters */
    override fun tagsParser(document: Document): List<Pair<String, String>> {
        return document.select(".galleries .gallery_title a")
            .mapNotNull {
                Pair(
                    it.ownText(),
                    it.attr("href")
                        .removeSuffix("/").substringAfterLast('/'),
                )
            }
    }

    override fun getCategoryURIs() = listOf(
        SearchFlagFilter("Manga", "mg"),
        SearchFlagFilter("Doujinshi", "dj"),
        SearchFlagFilter("Western", "ws"),
        SearchFlagFilter("Image Set", "is"),
        SearchFlagFilter("Artist CG", "ac"),
        SearchFlagFilter("Game CG", "gc"),
    )
}
