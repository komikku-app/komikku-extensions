package eu.kanade.tachiyomi.extension.all.mangareaderto

import android.app.Application
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.jsoup.select.Evaluator
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

open class MangaReader(
    override val lang: String,
) : ConfigurableSource, ParsedHttpSource() {
    override val name = "MangaReader"

    override val baseUrl = "https://mangareader.to"

    override val supportsLatest = true

    override val client = network.client.newBuilder()
        .addInterceptor(MangaReaderImageInterceptor)
        .build()

    private fun MangasPage.insertVolumeEntries(): MangasPage {
        if (preferences.showVolume.not()) return this
        val list = mangas.ifEmpty { return this }
        val newList = ArrayList<SManga>(list.size * 2)
        for (manga in list) {
            val volume = SManga.create().apply {
                url = manga.url + VOLUME_URL_SUFFIX
                title = VOLUME_TITLE_PREFIX + manga.title
                thumbnail_url = manga.thumbnail_url
            }
            newList.add(manga)
            newList.add(volume)
        }
        return MangasPage(newList, hasNextPage)
    }

    override fun latestUpdatesParse(response: Response) = super.latestUpdatesParse(response).insertVolumeEntries()
    override fun popularMangaParse(response: Response) = super.popularMangaParse(response).insertVolumeEntries()
    override fun searchMangaParse(response: Response) = super.searchMangaParse(response).insertVolumeEntries()

    override fun latestUpdatesRequest(page: Int) =
        GET("$baseUrl/filter?sort=latest-updated&language=$lang&page=$page", headers)

    override fun latestUpdatesSelector() = searchMangaSelector()

    override fun latestUpdatesNextPageSelector() = searchMangaNextPageSelector()

    override fun latestUpdatesFromElement(element: Element) =
        searchMangaFromElement(element)

    override fun popularMangaRequest(page: Int) =
        GET("$baseUrl/filter?sort=most-viewed&language=$lang&page=$page", headers)

    override fun popularMangaSelector() = searchMangaSelector()

    override fun popularMangaNextPageSelector() = searchMangaNextPageSelector()

    override fun popularMangaFromElement(element: Element) =
        searchMangaFromElement(element)

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val urlBuilder = baseUrl.toHttpUrl().newBuilder()
        if (query.isNotBlank()) {
            urlBuilder.addPathSegment("search").apply {
                addQueryParameter("keyword", query)
                addQueryParameter("page", page.toString())
            }
        } else {
            urlBuilder.addPathSegment("filter").apply {
                addQueryParameter("language", lang)
                addQueryParameter("page", page.toString())
                filters.ifEmpty(::getFilterList).forEach { filter ->
                    when (filter) {
                        is Select -> {
                            addQueryParameter(filter.param, filter.selection)
                        }
                        is DateFilter -> {
                            filter.state.forEach {
                                addQueryParameter(it.param, it.selection)
                            }
                        }
                        is GenresFilter -> {
                            addQueryParameter(filter.param, filter.selection)
                        }
                        else -> {}
                    }
                }
            }
        }
        return Request.Builder().url(urlBuilder.build()).headers(headers).build()
    }

    override fun searchMangaSelector() = ".manga_list-sbs .manga-poster"

    override fun searchMangaNextPageSelector() = ".page-link[title=Next]"

    override fun searchMangaFromElement(element: Element) =
        SManga.create().apply {
            url = element.attr("href")
            element.selectFirst(Evaluator.Tag("img"))!!.let {
                title = it.attr("alt")
                thumbnail_url = it.attr("src")
            }
        }

    private fun Element.parseAuthorsTo(manga: SManga) {
        val authors = select(Evaluator.Tag("a"))
        val text = authors.map { it.ownText().replace(",", "") }
        val count = authors.size
        when (count) {
            0 -> return
            1 -> {
                manga.author = text[0]
                return
            }
        }
        val authorList = ArrayList<String>(count)
        val artistList = ArrayList<String>(count)
        for ((index, author) in authors.withIndex()) {
            val textNode = author.nextSibling() as? TextNode
            val list = if (textNode != null && "(Art)" in textNode.wholeText) artistList else authorList
            list.add(text[index])
        }
        if (authorList.isEmpty().not()) manga.author = authorList.joinToString()
        if (artistList.isEmpty().not()) manga.artist = artistList.joinToString()
    }

    override fun mangaDetailsParse(document: Document) = SManga.create().apply {
        url = document.location().removePrefix(baseUrl)
        val root = document.selectFirst(Evaluator.Id("ani_detail"))!!
        val mangaTitle = root.selectFirst(Evaluator.Tag("h2"))!!.ownText()
        title = if (url.endsWith(VOLUME_URL_SUFFIX)) VOLUME_TITLE_PREFIX + mangaTitle else mangaTitle
        description = root.run {
            val description = selectFirst(Evaluator.Class("description"))!!.ownText()
            when (val altTitle = selectFirst(Evaluator.Class("manga-name-or"))!!.ownText()) {
                "", mangaTitle -> description
                else -> "$description\n\nAlternative Title: $altTitle"
            }
        }
        thumbnail_url = root.selectFirst(Evaluator.Tag("img"))!!.attr("src")
        genre = root.selectFirst(Evaluator.Class("genres"))!!.children().joinToString { it.ownText() }
        for (item in root.selectFirst(Evaluator.Class("anisc-info"))!!.children()) {
            if (item.hasClass("item").not()) continue
            when (item.selectFirst(Evaluator.Class("item-head"))!!.ownText()) {
                "Authors:" -> item.parseAuthorsTo(this)
                "Status:" -> status = when (item.selectFirst(Evaluator.Class("name"))!!.ownText()) {
                    "Finished" -> SManga.COMPLETED
                    "Publishing" -> SManga.ONGOING
                    else -> SManga.UNKNOWN
                }
            }
        }
    }

    override fun chapterListRequest(manga: SManga): Request {
        val url = manga.url
        val id = url.removeSuffix(VOLUME_URL_SUFFIX).substringAfterLast('-')
        val type = if (url.endsWith(VOLUME_URL_SUFFIX)) "vol" else "chap"
        return GET("$baseUrl/ajax/manga/reading-list/$id?readingBy=$type", headers)
    }

    override fun chapterListSelector() = "#$lang-chapters .item"

    override fun chapterListParse(response: Response): List<SChapter> {
        val isVolume = response.request.url.queryParameter("readingBy") == "vol"
        val container = response.parseHtmlProperty().run {
            val type = if (isVolume) "volumes" else "chapters"
            selectFirst(Evaluator.Id("$lang-$type")) ?: return emptyList()
        }
        val abbrPrefix = if (isVolume) "Vol" else "Chap"
        val fullPrefix = if (isVolume) "Volume" else "Chapter"
        return container.children().map { chapterFromElement(it, abbrPrefix, fullPrefix) }
    }

    override fun chapterFromElement(element: Element) =
        throw UnsupportedOperationException("Not used.")

    private fun chapterFromElement(element: Element, abbrPrefix: String, fullPrefix: String) =
        SChapter.create().apply {
            val number = element.attr("data-number")
            chapter_number = number.toFloatOrNull() ?: -1f
            element.selectFirst(Evaluator.Tag("a"))!!.let {
                url = it.attr("href")
                name = run {
                    val name = it.attr("title")
                    val prefix = "$abbrPrefix $number: "
                    if (name.startsWith(prefix).not()) return@run name
                    val realName = name.removePrefix(prefix)
                    if (realName.contains(number)) realName else "$fullPrefix $number: $realName"
                }
            }
        }

    override fun pageListParse(document: Document): List<Page> {
        val ajaxUrl = document.selectFirst(Evaluator.Id("wrapper"))!!.run {
            val readingBy = attr("data-reading-by")
            val readingId = attr("data-reading-id")
            "$baseUrl/ajax/image/list/$readingBy/$readingId?quality=${preferences.quality}"
        }

        val pageDocument = client.newCall(GET(ajaxUrl, headers)).execute().parseHtmlProperty()

        return pageDocument.getElementsByClass("iv-card").mapIndexed { index, img ->
            val url = img.attr("data-url")
            val imageUrl = if (img.hasClass("shuffled")) "$url#${MangaReaderImageInterceptor.SCRAMBLED}" else url
            Page(index, imageUrl = imageUrl)
        }
    }

    override fun imageUrlParse(document: Document) =
        throw UnsupportedOperationException("Not used")

    private val preferences by lazy {
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)!!
    }

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        getPreferences(screen.context).forEach(screen::addPreference)
    }

    override fun getFilterList() =
        FilterList(
            Note,
            TypeFilter(),
            StatusFilter(),
            RatingFilter(),
            ScoreFilter(),
            StartDateFilter(),
            EndDateFilter(),
            SortFilter(),
            GenresFilter(),
        )

    private fun Response.parseHtmlProperty(): Document {
        val html = Json.parseToJsonElement(body.string()).jsonObject["html"]!!.jsonPrimitive.content
        return Jsoup.parseBodyFragment(html)
    }
}
