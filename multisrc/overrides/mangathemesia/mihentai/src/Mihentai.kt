package eu.kanade.tachiyomi.extension.all.mihentai

import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.jsoup.nodes.Document
import java.lang.IllegalArgumentException

class Mihentai : MangaThemesia("Mihentai", "https://mihentai.com", "all") {
    override fun pageListParse(document: Document): List<Page> {
        val htmlPages = document.select(pageSelector)
            .filterNot { it.attr("abs:src").isNullOrEmpty() }
            .mapIndexed { i, img ->
                val pageUrl = img.attr("abs:src").substringAfter(baseUrl).prependIndent(baseUrl)
                Page(i, "", pageUrl)
            }
            .toMutableList()

        countViews(document)

        if (htmlPages.isNotEmpty()) { return htmlPages }

        val docString = document.toString()
        val imageListJson = JSON_IMAGE_LIST_REGEX.find(docString)?.destructured?.toList()?.get(0).orEmpty()
        val imageList = try {
            json.parseToJsonElement(imageListJson).jsonArray
        } catch (_: IllegalArgumentException) {
            emptyList()
        }
        val baseResolver = baseUrl.toHttpUrl()

        val scriptPages = imageList.mapIndexed { i, jsonEl ->
            val imageUrl = jsonEl.jsonPrimitive.content
            Page(i, "", baseResolver.resolve(imageUrl).toString())
        }

        return scriptPages
    }

    private class StatusFilter : SelectFilter(
        "Status",
        arrayOf(
            Pair("All", ""),
            Pair("Publishing", "publishing"),
            Pair("Finished", "finished"),
            Pair("Dropped", "drop")
        )
    )

    private class TypeFilter : SelectFilter(
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
            OrderByFilter(),
            GenreListFilter(getGenreList())
        )
    )
}
