package eu.kanade.tachiyomi.extension.en.grrlpower

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Request
import okhttp3.Response
import rx.Observable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.collections.ArrayList

@Suppress("unused")
class GrrlPower(
    override val baseUrl: String = "https://www.grrlpowercomic.com",
    override val lang: String = "en",
    override val name: String = "Grrl Power Comic",
    override val supportsLatest: Boolean = false,
) : HttpSource() {
    private val startingYear = 2010
    private val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private val dateFormat = SimpleDateFormat("MMM dd yyyy", Locale.US)

    override fun fetchPopularManga(page: Int): Observable<MangasPage> = Observable.just(
        MangasPage(
            listOf(
                SManga.create().apply {
                    artist = "David Barrack"
                    author = "David Barrack"
                    description = "Grrl Power is a comic about a crazy nerdette that becomes a" +
                        " superheroine. Humor, action, cheesecake, beefcake, 'splosions," +
                        " and maybe some drama. Possibly ninjas. "
                    genre = "superhero, humor, action"
                    initialized = true
                    status = SManga.ONGOING
                    // TODO: Find Proper Thumbnail and Extension Icon
                    thumbnail_url = "https://fakeimg.pl/550x780/cc3333/1b2a82/?font=museo&text=Grrl%0APower"
                    title = "Grrl Power"
                    url = "/archive"
                },
            ),
            false,
        ),
    )!!

    /**
     There are separate pages for each year.
     A Separate call needs to be made for each year since publication
     After we get the response send on like normal and collect all the chapters.
     */
    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> {
        val ret: ArrayList<SChapter> = ArrayList()
        for (i in startingYear..currentYear) {
            client
                .newCall(GET("$baseUrl/archive/?archive_year=$i"))
                .asObservableSuccess()
                .map { chapterListParse(it) }
                .subscribe { ret.addAll(it) }
            // Using A List of Observables and calling .from() won't work due to the number of
            // observables active at once. error shown below for reference in case someone knows a fix.
            // java.lang.IllegalArgumentException: Sequence contains too many elements
        }
        return Observable.just(ret)
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val year = response.request.url.toString().substringAfter('=').toInt()
        var num = 0
        return response.asJsoup().getElementsByClass("archive-date").map {
            val date = dateFormat.parse("${it.text()} $year")
            val link = it.nextElementSibling()!!.child(0)
            SChapter.create().apply {
                name = link.text()
                setUrlWithoutDomain(link.attr("href"))
                chapter_number = num++.toFloat()
                date_upload = date?.time ?: 0L
            }
        }
    }

    override fun pageListParse(response: Response): List<Page> {
        return listOf(
            Page(
                0,
                response.request.url.toString(),
                response.asJsoup().selectFirst("div#comic img")!!.absUrl("src"),
            ),
        )
    }

    // This can be called when the user refreshes the comic even if initialized is true
    override fun fetchMangaDetails(manga: SManga): Observable<SManga> = Observable.just(manga)

    override fun popularMangaRequest(page: Int): Request = throw UnsupportedOperationException("Not Used")
    override fun searchMangaParse(response: Response): MangasPage = throw UnsupportedOperationException("Not Used")
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request = throw UnsupportedOperationException("Not Used")
    override fun imageUrlParse(response: Response): String = throw UnsupportedOperationException("Not Used")
    override fun latestUpdatesParse(response: Response): MangasPage = throw UnsupportedOperationException("Not Used")
    override fun latestUpdatesRequest(page: Int): Request = throw UnsupportedOperationException("Not Used")
    override fun mangaDetailsParse(response: Response): SManga = throw UnsupportedOperationException("Not Used")
    override fun popularMangaParse(response: Response): MangasPage = throw UnsupportedOperationException("Not Used")
}
