package eu.kanade.tachiyomi.extension.es.inarimanga

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.source.model.SManga
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale

class InariManga : Madara(
    "InariManga",
    "https://inarimanga.com",
    "es",
    dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale("es")),
) {
    override fun popularMangaSelector() = "div.page-listing-item div.post"
    override val popularMangaUrlSelector = "div.p-2 > h6 > a"

    override fun searchMangaSelector() = "div.page-listing-item div.post"
    private val searchMangaUrlSelector = "div.p-2 > h6 > a"

    override val mangaDetailsSelectorDescription = "div.card-body:has(h5:contains(Sinopsis))"
    override val mangaDetailsSelectorThumbnail = "div.col-sticky-top > img"
    override val mangaDetailsSelectorStatus = "div.card-body tr:has(th:contains(Estatus)) > td"
    override val mangaDetailsSelectorGenre = "div.my-auto > div.inline-block > a"

    override val useNewChapterEndpoint = true

    override fun chapterListSelector() = "tr.wp-manga-chapter"
    override fun chapterDateSelector() = "time.chapter-release-date"

    override fun searchMangaFromElement(element: Element): SManga {
        val manga = SManga.create()

        with(element) {
            select(searchMangaUrlSelector).first()?.let {
                manga.setUrlWithoutDomain(it.attr("abs:href"))
                manga.title = it.ownText()
            }
            select("img").first()?.let {
                manga.thumbnail_url = imageFromElement(it)
            }
        }

        return manga
    }
}
