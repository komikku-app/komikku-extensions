package eu.kanade.tachiyomi.extension.en.soushiyofamiliar

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import rx.Observable
import java.text.SimpleDateFormat
import java.util.Locale

class SoushiyoFamiliar : ParsedHttpSource() {

    override val name = "Soushiyo: Familiar"

    override val baseUrl = "https://familiar.soushiyo.com"

    override val lang = "en"

    override val supportsLatest = false

    override fun fetchPopularManga(page: Int): Observable<MangasPage> {
        val manga = SManga.create().apply {
            title = "Familiar"
            artist = "Soushiyo"
            author = "Soushiyo"
            status = SManga.ONGOING
            url = "/familiar-chapter-list/"
            description = "a witch. a cat. a contract.\n\nWhen career-driven editor Diana Vallejo accidentally summons a familiar whose specialty is soft domination, her life takes a turn for the better – but for how long?\n\nFamiliar is a modern-day, slice-of-life romcomic about magick, work/life balance, BDSM, and relationships. It is kinky, queer, sex-positive, and free to read online. It is also erotic, sexually explicit, and written for adult audiences only."
            thumbnail_url = "https://i.redd.it/wbe6ewkjtz291.jpg"
        }

        return Observable.just(MangasPage(arrayListOf(manga), false))
    }

    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> = Observable.just(MangasPage(emptyList(), false))

    override fun fetchMangaDetails(manga: SManga): Observable<SManga> = Observable.just(manga)

    override fun chapterListParse(response: Response): List<SChapter> {
        val chapterList = super.chapterListParse(response).distinct()
        val chapterListNew = mutableListOf<SChapter>()

        for (i in chapterList.indices) {
            /* Some chapters are already listed on the site but are not yet out (have no link) --> Skip them */
            if (chapterList[i].url.isNotEmpty()) {
                /* Add chapter to front of new list --> Reverse order so that newest chapter is first */
                chapterListNew.add(0, chapterList[i])
            }
        }

        return chapterListNew
    }

    override fun chapterListSelector() = "tbody > tr[class^='row-']"

    override fun chapterFromElement(element: Element): SChapter {
        val textAct = element.select(".column-1").text()
        val textChapNum = element.select(".column-2").text()
        val textChapName = element.select(".column-3").text()
        var textDate = ""

        /* The date is in column 6 after a globe symbol --> Find/select the symbol */
        val dateSymbolElements = element.select(".column-6 > .fa-globe")
        /* Unreleased entries don't have a symbol and date */
        if (dateSymbolElements.size != 0) {
            /* Get the text after the symbol (if there are more occurrences then take the last, so the latest date is used in the app) */
            textDate = dateSymbolElements.last()!!.nextSibling().toString()
            /* Filter out any characters that do not belong to the valid date format "M/d/yy" */
            textDate = textDate.replace("[^0-9/]".toRegex(), "")
        }

        val chapter = SChapter.create()
        chapter.url = element.select(".column-3 > a").attr("href").substringAfter(baseUrl) // This is empty if there is no link (e.g. for unreleased chapters) --> This is then handled in chapterListParse
        chapter.name = "Act $textAct - Chapter $textChapNum: $textChapName"
        chapter.date_upload = parseDate(textDate)

        return chapter
    }

    override fun pageListParse(document: Document): List<Page> {
        val pages = mutableListOf<Page>()

        val elements = document.select(".esg-entry-media > img")
        for (i in 0 until elements.size) {
            pages.add(Page(pages.size, "", elements[i].attr("abs:src")))
        }

        return pages
    }

    override fun imageUrlParse(document: Document) = throw UnsupportedOperationException("Not used.")

    override fun popularMangaSelector(): String = throw UnsupportedOperationException("Not used.")

    override fun searchMangaFromElement(element: Element): SManga = throw UnsupportedOperationException("Not used.")

    override fun searchMangaNextPageSelector(): String = throw UnsupportedOperationException("Not used.")

    override fun searchMangaSelector(): String = throw UnsupportedOperationException("Not used.")

    override fun popularMangaRequest(page: Int): Request = throw UnsupportedOperationException("Not used.")

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request = throw UnsupportedOperationException("Not used.")

    override fun popularMangaNextPageSelector(): String = throw UnsupportedOperationException("Not used.")

    override fun popularMangaFromElement(element: Element): SManga = throw UnsupportedOperationException("Not used.")

    override fun mangaDetailsParse(document: Document): SManga = throw UnsupportedOperationException("Not used.")

    override fun latestUpdatesNextPageSelector(): String? = throw UnsupportedOperationException("Not used.")

    override fun latestUpdatesFromElement(element: Element): SManga = throw UnsupportedOperationException("Not used.")

    override fun latestUpdatesRequest(page: Int): Request = throw UnsupportedOperationException("Not used.")

    override fun latestUpdatesSelector(): String = throw UnsupportedOperationException("Not used.")

    private fun parseDate(dateStr: String): Long {
        return runCatching { DATE_FORMATTER.parse(dateStr)?.time }
            .getOrNull() ?: 0L
    }

    companion object {
        private val DATE_FORMATTER by lazy {
            SimpleDateFormat("M/d/yy", Locale.ENGLISH)
        }
    }
}
