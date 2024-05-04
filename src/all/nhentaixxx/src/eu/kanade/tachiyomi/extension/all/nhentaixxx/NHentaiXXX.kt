package eu.kanade.tachiyomi.extension.all.nhentaixxx

import eu.kanade.tachiyomi.multisrc.galleryadults.GalleryAdults
import eu.kanade.tachiyomi.multisrc.galleryadults.SortOrderFilter
import eu.kanade.tachiyomi.multisrc.galleryadults.cleanTag
import eu.kanade.tachiyomi.multisrc.galleryadults.imgAttr
import eu.kanade.tachiyomi.source.model.FilterList
import okhttp3.FormBody
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class NHentaiXXX(
    lang: String = "all",
    override val mangaLang: String = LANGUAGE_MULTI,
) : GalleryAdults(
    "NHentai.xxx",
    "https://nhentai.xxx",
    lang = lang,
) {
    override val supportsLatest = true

    private val languages: List<Pair<String, String>> = listOf(
        Pair(LANGUAGE_ENGLISH, "1"),
        Pair(LANGUAGE_JAPANESE, "2"),
        Pair(LANGUAGE_CHINESE, "3"),
    )
    private val langCode = languages.firstOrNull { lang -> lang.first == mangaLang }?.second

    override fun Element.mangaLang() = when (attr("data-languages")) {
        langCode -> mangaLang
        else -> "other"
    }

    override fun Element.mangaUrl() =
        selectFirst(".gallery_item a")?.attr("abs:href")

    override fun Element.mangaThumbnail() =
        selectFirst(".gallery_item img")?.imgAttr()

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

    override fun popularMangaSelector() = ".galleries_box .gallery_item"

    override val basicSearchKey = "key"

    override val favoritePath = "favorites"

    override fun loginRequired(document: Document, url: String): Boolean {
        return (
            url.contains("/$favoritePath/") &&
                document.select("a[href='/login/']:contains(Sign in)").isNotEmpty()
            )
    }

    override fun Element.getInfo(tag: String): String {
        return select(".tags:contains($tag:) .tag_btn .tag_name")
            .joinToString { it.ownText().cleanTag() }
    }

    override fun pageRequestForm(document: Document, totalPages: String): FormBody {
        val token = document.select("[name=csrf-token]").attr("content")
        val serverNumber = document.serverNumber()

        return FormBody.Builder()
            .add("u_id", document.inputIdValueOf(galleryIdSelector))
            .add("g_id", document.inputIdValueOf(loadIdSelector))
            .add("img_dir", document.inputIdValueOf(loadDirSelector))
            .add("visible_pages", "10")
            .add("total_pages", totalPages)
            .add("type", "2") // 1 would be "more", 2 is "all remaining"
            .apply {
                if (token.isNotBlank()) add("_token", token)
                if (serverNumber != null) add("server", serverNumber)
            }
            .build()
    }

    override fun Element.parseJson() =
        selectFirst("script:containsData(parseJSON)")?.data()
            ?.substringAfter("$.parseJSON('{\"fl\":")
            ?.substringBefore(",\"th\":")?.trim()

    override fun tagsParser(document: Document): List<Pair<String, String>> {
        return document.select(".tags_items a.tag_btn")
            .mapNotNull {
                Pair(
                    it.selectFirst(".tag_name")?.ownText() ?: "",
                    it.attr("href")
                        .removeSuffix("/").substringAfterLast('/'),
                )
            }
    }
}
