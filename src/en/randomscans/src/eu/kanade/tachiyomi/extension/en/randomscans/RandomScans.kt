package eu.kanade.tachiyomi.extension.en.randomscans

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import rx.Observable

class RandomScans : HttpSource() {
    override val name = "Random Scans"

    override val baseUrl = "https://wearerandomscans.wordpress.com"

    override val lang = "en"

    override val supportsLatest = false

    override val client: OkHttpClient = network.cloudflareClient

    companion object {
        const val SLUG_SEARCH_PREFIX = "slug:"
    }

    // == Main site functions ==

    override fun latestUpdatesRequest(page: Int): Request = throw(UnsupportedOperationException("Not used"))

    override fun latestUpdatesParse(response: Response): MangasPage = throw(UnsupportedOperationException("Not used"))

    override fun popularMangaRequest(page: Int): Request = GET("$baseUrl/projects", headers)

    override fun popularMangaParse(response: Response): MangasPage {
        val mangaList: List<SManga> = response.asJsoup()
            .select("main#main div.wp-block-media-text")
            .map { element ->
                SManga.create().apply {
                    // url is absolute
                    url = element.selectFirst("a[href]:containsOwn(Chapter List), a[href]:containsOwn(Read Here)").attr("href")

                    // thumbnail_url is absolute
                    thumbnail_url = element.selectFirst("img[src]").attr("src")

                    title = element.selectFirst("p.has-large-font-size > strong").text()

                    description = element.selectFirst("p:containsOwn(Author/Artist) + p")?.text()

                    val details = element.selectFirst("p:containsOwn(Author/Artist)").html().split("<br>")

                    author = details.find { it.contains("Author/Artist: ") }
                        ?.substringAfterLast("Author/Artist: ")

                    artist = author

                    genre = details.find { it.contains("Genres: ") }
                        ?.substringAfterLast("Genres: ")
                        ?.replace(",", ", ")

                    status = when (element.selectFirst("p.has-large-font-size:containsOwn(Status: )")?.text()?.substringAfterLast("Status: ")) {
                        "Ongoing" -> SManga.ONGOING
                        "Completed" -> SManga.COMPLETED
                        "Hiatus" -> {
                            description = "Status: Hiatus\n\n$description"
                            SManga.ONGOING
                        }
                        "Dropped" -> {
                            description = "Status: Dropped\n\n$description"
                            SManga.COMPLETED // Not sure what the best status is for "Dropped"
                        }
                        else -> SManga.UNKNOWN
                    }

                    initialized = true // we have all of the fields
                }
            }
        return MangasPage(mangaList, false)
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request = throw UnsupportedOperationException("Not used")

    override fun searchMangaParse(response: Response): MangasPage = throw UnsupportedOperationException("Not used")

    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> {
        if (query.startsWith(SLUG_SEARCH_PREFIX)) {
            var slug = query.removePrefix(SLUG_SEARCH_PREFIX)

            // remove trailing digits from chapter links
            while (slug.substringAfterLast('-', "").toIntOrNull() != null) {
                slug = slug.substringBeforeLast('-')
            }

            val manga = SManga.create().apply {
                url = "$baseUrl/projects/$slug"
            }

            return fetchMangaDetails(manga).map {
                MangasPage(listOf(it), false)
            }
        }
        return fetchPopularManga(page).map {
            mangasPage ->
            MangasPage(
                mangasPage.mangas.filter {
                    it.title.contains(query, true)
                },
                mangasPage.hasNextPage
            )
        }
    }

    // == Manga functions ==

    override fun fetchMangaDetails(manga: SManga): Observable<SManga> {
        return client.newCall(mangaDetailsRequest(manga))
            .asObservableSuccess()
            .map { response ->
                if (isOneshot(manga)) {
                    oneshotDetailsParse(response).copyFromCustom(manga)
                } else {
                    mangaDetailsParse(response)
                }.apply {
                    initialized = true
                    url = manga.url
                }
            }
    }

    override fun mangaDetailsRequest(manga: SManga): Request = GET(manga.url, headers)

    override fun mangaDetailsParse(response: Response): SManga {
        return SManga.create().apply {
            val element = response.asJsoup().selectFirst("div.wp-block-media-text")

            // thumbnail_url is absolute
            thumbnail_url = element.selectFirst("figure > img[src]").attr("src")

            title = element.selectFirst("p.has-huge-font-size").text()

            genre = element.selectFirst("p.has-huge-font-size + p")?.text()?.replace(" • ", ", ")

            description = element.selectFirst("p.has-huge-font-size + p + p")?.text()

            author = element.selectFirst("p:containsOwn(Author/Artist: )").text().removePrefix("Author/Artist: ")

            artist = author

            status = when (element.selectFirst("p:containsOwn(Format: ) + p")?.text()?.substringAfterLast("• ")) {
                "Ongoing" -> SManga.ONGOING
                "Completed" -> SManga.COMPLETED
                "Hiatus" -> {
                    description = "Status: Hiatus\n\n$description"
                    SManga.ONGOING
                }
                "Dropped" -> {
                    description = "Status: Dropped\n\n$description"
                    SManga.COMPLETED // Not sure what the best status is for "Dropped"
                }
                else -> SManga.UNKNOWN
            }
        }
    }

    // oneshots have manga pages which directly link to the chapter page, and have dates in their url like a chapter page
    private fun isOneshot(manga: SManga): Boolean = manga.url.contains(Regex("/20\\d\\d/\\d\\d/\\d\\d"))

    // We can only get the title
    private fun oneshotDetailsParse(response: Response): SManga {
        return SManga.create().apply {
            val responseJson = response.asJsoup()
            title = responseJson.selectFirst("h1.entry-title").text()
            thumbnail_url = responseJson.selectFirst("img[src]").attr("src")
            status = SManga.COMPLETED
            description = "Add this manga from search to fetch details properly"
        }
    }

    // the same as SManga.copyFrom, which we can't access, except it doesn't copy status
    private fun SManga.copyFromCustom(other: SManga): SManga {
        return this.apply {

            if (other.author != null) {
                author = other.author
            }

            if (other.artist != null) {
                artist = other.artist
            }

            if (other.description != null) {
                description = other.description
            }

            if (other.genre != null) {
                genre = other.genre
            }

            if (other.thumbnail_url != null) {
                thumbnail_url = other.thumbnail_url
            }

            if (!initialized) {
                initialized = other.initialized
            }
        }
    }

    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> {
        if (isOneshot(manga)) {
            return Observable.just(
                listOf(
                    SChapter.create().apply {
                        // the "manga" page for a oneshot is really more like a chapter page
                        url = manga.url

                        name = "Oneshot"

                        date_upload = System.currentTimeMillis() - 1000
                    }
                )
            )
        }

        return super.fetchChapterList(manga)
    }

    override fun chapterListRequest(manga: SManga): Request = GET(manga.url, headers)

    override fun chapterListParse(response: Response): List<SChapter> {
        return response.asJsoup()
            .select("main#main li > a[href]")
            .mapIndexed { index, element ->
                SChapter.create().apply {
                    // url is absolute
                    url = element.attr("href")

                    name = element.text()

                    // chapters are set as uploaded 1 millisecond apart,
                    // so that users can still sort by date uploaded
                    date_upload = System.currentTimeMillis() - 1000 + index
                }
            }.reversed()
    }

    // == Chapter functions ==

    override fun pageListRequest(chapter: SChapter): Request = GET(chapter.url, headers)

    override fun pageListParse(response: Response): List<Page> {
        val reponseJson = response.asJsoup()

        val list = reponseJson.select("figure > img").mapIndexed { index, element ->
            Page(index, "", element.attr("src"))
        }.toMutableList()

        if (reponseJson.selectFirst("iframe[src*=drive.google.com/file]") != null) {
            list.add(Page(list.last().index + 1, "", "https://fakeimg.pl/1800x2252/FFFFFF/000000/?font_size=110&text=Some%20images%20in%20this%20chapter%20could%20not%20be%20loaded%20%0Abecause%20they%20are%20stored%20in%20a%20Google%20Drive%20PDF.%20%0APlease%20open%20the%20chapter%20in%20web%20view%20to%20view%20them."))
        }

        return list
    }

    // == Page functions ==

    override fun fetchImageUrl(page: Page): Observable<String> {
        return Observable.just(page.imageUrl)
    }

    override fun imageUrlRequest(page: Page): Request = throw UnsupportedOperationException("Not used")

    override fun imageUrlParse(response: Response): String = throw UnsupportedOperationException("Not used")
}
