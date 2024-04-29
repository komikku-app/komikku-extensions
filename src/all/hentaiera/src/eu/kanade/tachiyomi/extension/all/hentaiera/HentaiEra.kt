package eu.kanade.tachiyomi.extension.all.hentaiera

import eu.kanade.tachiyomi.multisrc.galleryadults.GalleryAdults
import eu.kanade.tachiyomi.multisrc.galleryadults.SearchFlagFilter
import eu.kanade.tachiyomi.multisrc.galleryadults.SortOrderFilter
import eu.kanade.tachiyomi.multisrc.galleryadults.cleanTag
import eu.kanade.tachiyomi.multisrc.galleryadults.imgAttr
import eu.kanade.tachiyomi.source.model.FilterList
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
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
        mangaFullTitle(if (selector == ".caption") ".gallery_title" else selector).let {
            if (preferences.shortTitle) it?.shortenTitle() else it
        }

    override val favoritePath = "user/fav_pags.php"

    /* Popular */
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

    override fun Element.getDescription(): String {
        return (
            listOf("Parodies", "Characters", "Languages", "Category")
                .mapNotNull { tag ->
                    getInfo(tag)
                        .let { if (it.isNotBlank()) "$tag: $it" else null }
                } +
                listOfNotNull(
                    selectFirst("#pages_btn")?.ownText(),
                    selectFirst(".subtitle")?.ownText()
                        .let { altTitle -> if (!altTitle.isNullOrBlank()) "Alternate Title: $altTitle" else null },
                )
            )
            .joinToString("\n\n")
            .plus(
                if (preferences.shortTitle) {
                    "\nFull title: ${mangaFullTitle("h1")}"
                } else {
                    ""
                },
            )
    }

    override fun Element.getCover() =
        selectFirst(".left_cover img")?.imgAttr()

    override val mangaDetailInfoSelector = ".gallery_first"

    /* Pages */
    override val pageUri = "view"
    override val pageSelector = ".gthumb"
    private val serverSelector = "load_server"

    private fun serverNumber(document: Document, galleryId: String): String {
        return document.inputIdValueOf(serverSelector).takeIf {
            it.isNotBlank()
        } ?: when (galleryId.toInt()) {
            in 1..274825 -> "1"
            in 274826..403818 -> "2"
            in 403819..527143 -> "3"
            in 527144..632481 -> "4"
            in 632482..816010 -> "5"
            in 816011..970098 -> "6"
            in 970099..1121113 -> "7"
            else -> "8"
        }
    }

    override fun getServer(document: Document, galleryId: String): String {
        val domain = baseUrl.toHttpUrl().host
        return "m${serverNumber(document, galleryId)}.$domain"
    }

    override fun pageRequestForm(document: Document, totalPages: String): FormBody {
        val galleryId = document.inputIdValueOf(galleryIdSelector)

        return FormBody.Builder()
            .add("server", serverNumber(document, galleryId))
            .add("u_id", document.inputIdValueOf(galleryIdSelector))
            .add("g_id", document.inputIdValueOf(loadIdSelector))
            .add("img_dir", document.inputIdValueOf(loadDirSelector))
            .add("visible_pages", "12")
            .add("total_pages", totalPages)
            .add("type", "2") // 1 would be "more", 2 is "all remaining"
            .build()
    }

    /* Filters */
    override fun tagsParser(document: Document): List<Pair<String, String>> {
        return document.select(".galleries .gallery_title a")
            .mapNotNull {
                Pair(
                    it.ownText() ?: "",
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

    override val idPrefixUri = "gallery"
}
