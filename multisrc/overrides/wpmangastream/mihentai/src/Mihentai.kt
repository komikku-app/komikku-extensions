package eu.kanade.tachiyomi.extension.en.mihentai

import eu.kanade.tachiyomi.multisrc.wpmangastream.WPMangaStream
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import org.jsoup.nodes.Document
import uy.kohesive.injekt.injectLazy
import java.util.Locale

class Mihentai : WPMangaStream("Mihentai", "https://mihentai.com", "en") {
    override fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/manga/page/$page/?order=popular", headers)
    }

    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$baseUrl/manga/page/$page/?order=update", headers)
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = "$baseUrl/manga/page/$page/".toHttpUrlOrNull()!!.newBuilder()
        url.addQueryParameter("title", query)
        filters.forEach { filter ->
            when (filter) {
                is StatusFilter -> url.addQueryParameter("status", filter.toUriPart())
                is TypeFilter -> url.addQueryParameter("type", filter.toUriPart())
                is SortByFilter -> url.addQueryParameter("order", filter.toUriPart())
                is GenreListFilter -> {
                    filter.state
                        .filter { it.state != Filter.TriState.STATE_IGNORE }
                        .forEach { url.addQueryParameter("genre[]", it.id) }
                }
            }
        }
        return GET(url.build().toString(), headers)
    }

    override fun mangaDetailsParse(document: Document): SManga {
        return SManga.create().apply {
            document.select("div.bigcontent, div.animefull, div.main-info").firstOrNull()?.let { infoElement ->
                status = parseStatus(infoElement.select("span:contains(Status:), .imptdt:contains(Status) i").firstOrNull()?.ownText())
                thumbnail_url = infoElement.select("div.thumb img").imgAttr()

                val genres = infoElement.select("span:contains(Tag) a")
                    .map { element -> element.text().toLowerCase(Locale.ROOT) }
                    .toMutableSet()

                // add series type(manga/manhwa/manhua/other) thinggy to genre
                document.select("span:contains(Type)").firstOrNull()?.ownText()?.let {
                    if (it.isEmpty().not() && genres.contains(it).not()) {
                        genres.add(it.toLowerCase(Locale.ROOT))
                    }
                }

                genre = genres.toList().joinToString(", ") { it.capitalize(Locale.ROOT) }
            }
        }
    }

    override fun parseStatus(element: String?): Int = when {
        element == null -> SManga.UNKNOWN
        listOf("ongoing", "publishing").any { it.contains(element, ignoreCase = true) } -> SManga.ONGOING
        listOf("finished").any { it.contains(element, ignoreCase = true) } -> SManga.COMPLETED
        else -> SManga.UNKNOWN
    }

    private val json: Json by injectLazy()

    override fun pageListParse(document: Document): List<Page> {
        val htmlPages = document.select(pageSelector)
            .filterNot { it.attr("abs:src").isNullOrEmpty() }
            .mapIndexed { i, img ->
                val pageUrl = img.attr("abs:src").substringAfter(baseUrl).prependIndent(baseUrl)
                Page(i, "", pageUrl)
            }
            .toMutableList()

        val docString = document.toString()
        val imageListRegex = Regex("\\\"images.*?:.*?(\\[.*?\\])")
        val imageListJson = imageListRegex.find(docString)?.destructured?.toList()?.get(0)
        if (imageListJson != null) {
            val imageList = json.parseToJsonElement(imageListJson).jsonArray
            val baseResolver = baseUrl.toHttpUrl()

            val scriptPages = imageList.mapIndexed { i, jsonEl ->
                val imageUrl = jsonEl.jsonPrimitive.content
                Page(i, "", baseResolver.resolve(imageUrl).toString())
            }

            if (htmlPages.size < scriptPages.size) {
                htmlPages += scriptPages
            }
        }

        countViews(document)

        return htmlPages.distinctBy { it.imageUrl }
    }

    private class StatusFilter : UriPartFilter(
        "Status",
        arrayOf(
            Pair("All", ""),
            Pair("Publishing", "publishing"),
            Pair("Finished", "finished"),
            Pair("Dropped", "drop")
        )
    )

    private class TypeFilter : UriPartFilter(
        "Type",
        arrayOf(
            Pair("Default", ""),
            Pair("Manga", "Manga"),
            Pair("Manhwa", "Manhwa"),
            Pair("Manhua", "Manhua"),
            Pair("Webtoon", "webtoon"),
            Pair("One-Shot", "One-Shot"),
            Pair("Doujin", "doujin")
        )
    )

    override fun getFilterList(): FilterList = FilterList(
        listOf(
            StatusFilter(),
            TypeFilter(),
            SortByFilter(),
            GenreListFilter(getGenreList())
        )
    )

    override fun getGenreList(): List<Genre> = listOf(
        Genre("Adventure", "adventure"),
        Genre("Ahego", "ahego"),
        Genre("Anal", "anal"),
        Genre("Battle", "battle"),
        Genre("Big Breasts", "big-breasts"),
        Genre("Blowjob", "blowjob"),
        Genre("Comic 3D", "comic-3d"),
        Genre("Doujin", "doujin"),
        Genre("Dragon ball", "dragon-ball"),
        Genre("Fingering", "fingering"),
        Genre("Full color", "full-color"),
        Genre("Futanari", "futanari"),
        Genre("Girlfriend", "girlfriend"),
        Genre("Grouped", "grouped"),
        Genre("Handjob", "handjob"),
        Genre("Hijab", "hijab"),
        Genre("Incest", "incest"),
        Genre("Kissing", "kissing"),
        Genre("Mama", "mama"),
        Genre("Manga", "manga"),
        Genre("Masturbation", "masturbation"),
        Genre("Milf", "milf"),
        Genre("Mom & Son", "mom-son"),
        Genre("Naruto", "naruto"),
        Genre("One Piece", "one-piece"),
        Genre("Pregnancy", "pregnancy"),
        Genre("Rape", "rape"),
        Genre("Romance", "romance"),
        Genre("School", "school"),
        Genre("Scooby-Doo", "scooby-doo"),
        Genre("Sister", "sister"),
        Genre("Stocking", "stocking"),
        Genre("Sub Indo", "sub-indo"),
        Genre("Threesome", "threesome"),
        Genre("Uncensored", "uncensored"),
        Genre("Western", "western"),
        Genre("Yuri", "yuri")
    )
}
