package eu.kanade.tachiyomi.extension.es.templescanesp

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import okhttp3.FormBody
import okhttp3.Request
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale

class TempleScanEsp : Madara(
    "TempleScan",
    "https://templescanesp.com",
    "es",
    SimpleDateFormat("dd.MM.yyyy", Locale("es")),
) {
    override val mangaSubString = "series"

    override fun popularMangaSelector() = "div:has(> div#series-card)"
    override val popularMangaUrlSelector = "div#series-card a.series-link"
    override fun popularMangaNextPageSelector() = "body:not(:has(.no-posts))"

    override val mangaDetailsSelectorAuthor = "div.post-content_item:contains(Autor) div.summary-content"
    override val mangaDetailsSelectorArtist = "div.post-content_item:contains(Artista) div.summary-content"
    override val mangaDetailsSelectorStatus = "div.post-content_item:contains(Estado) div.summary-content"

    private fun loadMoreRequest(page: Int, metaKey: String): Request {
        val formBody = FormBody.Builder().apply {
            add("action", "madara_load_more")
            add("page", page.toString())
            add("template", "madara-core/content/content-archive")
            add("vars[paged]", "1")
            add("vars[orderby]", "meta_value_num")
            add("vars[template]", "archive")
            add("vars[sidebar]", "full")
            add("vars[meta_query][0][0][key]", "_wp_manga_chapter_type")
            add("vars[meta_query][0][0][value]", "manga")
            add("vars[meta_query][0][relation]", "AND")
            add("vars[meta_query][relation]", "AND")
            add("vars[post_type]", "wp-manga")
            add("vars[post_status]", "publish")
            add("vars[meta_key]", metaKey)
            add("vars[manga_archives_item_layout]", "big_thumbnail")
        }.build()

        val xhrHeaders = headersBuilder()
            .add("Content-Length", formBody.contentLength().toString())
            .add("Content-Type", formBody.contentType().toString())
            .add("X-Requested-With", "XMLHttpRequest")
            .build()

        return POST("$baseUrl/wp-admin/admin-ajax.php", xhrHeaders, formBody)
    }

    override fun popularMangaRequest(page: Int): Request {
        return loadMoreRequest(page - 1, "_wp_manga_views")
    }

    override fun latestUpdatesRequest(page: Int): Request {
        return loadMoreRequest(page - 1, "_latest_update")
    }

    override fun popularMangaFromElement(element: Element): SManga {
        val manga = SManga.create()

        with(element) {
            select(popularMangaUrlSelector).first()?.let {
                manga.setUrlWithoutDomain(it.attr("abs:href"))
            }

            select("div.series-box .series-title").first()?.let {
                manga.title = it.text()
            }

            select("img").first()?.let {
                manga.thumbnail_url = imageFromElement(it)
            }
        }

        return manga
    }

    override fun chapterFromElement(element: Element): SChapter {
        val chapter = SChapter.create()

        with(element) {
            select(chapterUrlSelector).first()?.let { urlElement ->
                chapter.url = urlElement.attr("abs:href").let {
                    it.substringBefore("?style=paged") + if (!it.endsWith(chapterUrlSuffix)) chapterUrlSuffix else ""
                }
                chapter.name = urlElement.select("p").text()
            }

            chapter.date_upload = select("img:not(.thumb)").firstOrNull()?.attr("alt")?.let { parseRelativeDate(it) }
                ?: select("span a").firstOrNull()?.attr("title")?.let { parseRelativeDate(it) }
                ?: parseChapterDate(select(chapterDateSelector()).firstOrNull()?.text())
        }

        return chapter
    }
}
