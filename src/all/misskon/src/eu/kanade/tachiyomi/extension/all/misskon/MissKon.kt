package eu.kanade.tachiyomi.extension.all.misskon

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import java.util.Locale

class MissKon : HttpSource() {
    override val name = "MissKon (MrCong)"
    override val lang = "all"
    override val supportsLatest = true

    override val baseUrl = "https://misskon.com"

    override fun popularMangaRequest(page: Int) = GET(baseUrl, headers)

    override fun popularMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()
        val mangas = document.select("div#top-posts-14 a")
            .map { element ->
                SManga.create().apply {
                    url = element.attr("href").removePrefix(baseUrl)
                    title = element.attr("title")
                    thumbnail_url = element.select("img").attr("src")
                        .replace("https://i0.wp.com/", "https://")
                        .replace("\\?resize=.*".toRegex(), "")
                }
            }
        return MangasPage(mangas, hasNextPage = false)
    }

    override fun latestUpdatesRequest(page: Int) = GET("$baseUrl/page/$page", headers)

    override fun latestUpdatesParse(response: Response): MangasPage {
        val document = response.asJsoup()
        val mangas = document.select("div#main-content div.post-listing article.item-list")
            .map { element ->
                SManga.create().apply {
                    val post = element.select("h2.post-box-title a")
                    url = post.attr("href").removePrefix(baseUrl)
                    title = post.text()
                    thumbnail_url = element.select("div.post-thumbnail img").attr("src")
                    val meta = element.select("p.post-meta")
                    description = "View: ${meta.select("span.post-views").text()}"
                    // genre = meta.select("span.post-cats").text()
                    genre = meta.select("span.post-cats a").joinToString { it.text() }
                }
            }
        val isLastPage = document.selectFirst("div#main-content div.pagination span.current + a.page")
        return MangasPage(mangas, hasNextPage = isLastPage != null)
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = "$baseUrl/tag".toHttpUrl().newBuilder()
        (filters.first() as TagsFilter).let { tags ->
            when (tags.state) {
                0 -> {
                    if (query.isNotEmpty()) {
                        url.addPathSegment(
                            query.trim().replace(" ", "-")
                                .lowercase(Locale.getDefault()),
                        )
                    } else {
                        return latestUpdatesRequest(page)
                    }
                }
                else -> tags.toUriPart().let { url.addPathSegment(it) }
            }
        }

        url.addPathSegment("page")
        url.addPathSegment(page.toString())

        return GET(url.toString(), headers)
    }

    override fun searchMangaParse(response: Response) = latestUpdatesParse(response)

    override fun mangaDetailsParse(response: Response): SManga {
        val document = response.asJsoup()
        return SManga.create().apply {
            title = document.select(".post-title span").text()
            val view = document.select("p.post-meta span.post-views").text()
            val info = document.select("div.info div.box-inner-block")

            val password = info.select("input").attr("value")
            val downloadAvailable = document.select("div#fukie2.entry a[href]:has(i.fa-download)")
            val downloadLinks = downloadAvailable.joinToString("\n") {
                "${it.text()}: ${it.attr("href")}"
            }

            description = "View: $view\n" +
                "${info.html()
                    .replace("<.+?>".toRegex(), "")}\n" +
                "Password: $password\n" +
                downloadLinks
            genre = document.select("p.post-tag a").joinToString { it.text() }
            thumbnail_url = document.selectFirst("div#fukie2.entry p img")?.attr("src")
        }
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val postUrl = response.request.url.toString()
        return response.asJsoup()
            .select("div.page-link a")
            .map {
                SChapter.create().apply {
                    url = it.attr("href").removePrefix(baseUrl)
                    name = "Page ${it.text()}"
                }
            }.reversed() + listOf(
            SChapter.create().apply {
                url = postUrl.removePrefix(baseUrl)
                name = "Page 1"
            },

        )
    }

    override fun pageListParse(response: Response): List<Page> = response.asJsoup()
        .select("div#fukie2.entry p img").mapIndexed { index, image ->
            Page(index, imageUrl = image.attr("src"))
        }

    override fun imageUrlParse(response: Response) = throw UnsupportedOperationException()

    override fun getFilterList(): FilterList {
        return FilterList(
            TagsFilter("Tag", getTagsList()),
        )
    }
}
