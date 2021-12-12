package eu.kanade.tachiyomi.extension.zh.comico

import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document

class ComicoFactory : SourceFactory {
    override fun createSources(): List<Source> = listOf(
        ComicoOfficial(),
        ComicoChallenge()
    )
}

class ComicoOfficial : Comico("Comico Official (Limited free chapters)", "", false)
class ComicoChallenge : Comico("Comico Challenge", "/challenge", true) {
    override fun popularMangaRequest(page: Int): Request {
        val body = FormBody.Builder()
            .add("page", page.toString())
            .build()

        return POST("$baseUrl$urlModifier/updateList.nhn?order=new", headers, body)
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val mangas = mutableListOf<SManga>()
        val body = response.body!!.string()

        json.decodeFromString<JsonObject>(body)["result"]!!
            .jsonObject["list"]!!
            .jsonArray
            .forEach {
                val manga = SManga.create()

                manga.thumbnail_url = it.jsonObject["img_url"]!!.jsonPrimitive.content
                manga.title = it.jsonObject["article_title"]!!.jsonPrimitive.content
                manga.author = it.jsonObject["author"]!!.jsonPrimitive.content
                manga.description = it.jsonObject["description"]!!.jsonPrimitive.content
                manga.url = it.jsonObject["article_url"]!!.jsonPrimitive.content.substringAfter(urlModifier)
                manga.status = if (it.jsonObject["is_end"]!!.jsonPrimitive.content == "false") SManga.ONGOING else SManga.COMPLETED

                mangas.add(manga)
            }

        val lastPage = json.decodeFromString<JsonObject>(body)["result"]!!.jsonObject["totalPageCnt"]!!.jsonPrimitive.content
        val currentPage = json.decodeFromString<JsonObject>(body)["result"]!!.jsonObject["currentPageNo"]!!.jsonPrimitive.content

        return MangasPage(mangas, currentPage < lastPage)
    }

    override fun latestUpdatesRequest(page: Int): Request {
        val body = FormBody.Builder()
            .add("page", page.toString())
            .build()

        return POST("$baseUrl$urlModifier/updateList.nhn?order=update", headers, body)
    }

    override fun latestUpdatesParse(response: Response) = popularMangaParse(response)

    override fun searchMangaSelector() = "div#challengeList ul.list-article02__list li.list-article02__item a"

    override fun mangaDetailsParse(document: Document): SManga {
        val infoElement = document.select("div.article-hero03__inner")

        val manga = SManga.create()
        manga.title = infoElement.select("h1").text()
        manga.author = infoElement.select("p.article-hero03__author").text()
        manga.description = infoElement.select("div.article-hero03__description p").text()
        manga.thumbnail_url = infoElement.select("img").attr("src")

        return manga
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val chapters = mutableListOf<SChapter>()

        json.decodeFromString<JsonObject>(response.body!!.string())["result"]!!
            .jsonObject["list"]!!
            .jsonArray
            .forEach {
                chapters.add(chapterFromJson(it.jsonObject))
            }

        return chapters.reversed()
    }

    override fun pageListParse(document: Document): List<Page> {
        val pages = mutableListOf<Page>()

        document.select("img.comic-image__image").forEachIndexed { i, img ->
            pages.add(Page(i, "", img.attr("src")))
        }

        return pages
    }
}
