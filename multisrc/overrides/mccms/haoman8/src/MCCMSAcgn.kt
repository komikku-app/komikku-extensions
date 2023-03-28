package eu.kanade.tachiyomi.extension.zh.haoman8

import eu.kanade.tachiyomi.multisrc.mccms.MCCMSWeb
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.select.Evaluator
import uy.kohesive.injekt.injectLazy
import java.text.SimpleDateFormat
import java.util.Locale

// TODO: Category page
open class MCCMSAcgn(
    name: String,
    baseUrl: String,
    lang: String = "zh",
    hasCategoryPage: Boolean = true,
) : MCCMSWeb(name, baseUrl, lang, hasCategoryPage) {

    override fun parseListing(document: Document): MangasPage {
        if (document.location().contains("search")) {
            return searchMangaParse(document)
        }
        val list = document.selectFirst(Evaluator.Class("acgn-comic-list"))
            ?: return MangasPage(emptyList(), false)
        val mangas = list.children().map {
            SManga.create().apply {
                val titleElement = it.selectFirst(Evaluator.Tag("h3"))!!.child(0)
                url = titleElement.attr("href")
                title = titleElement.ownText()
                thumbnail_url = it.selectFirst(Evaluator.Tag("img"))!!
                    .attr("style").split("'")[1]
            }.cleanup()
        }
        val hasNextPage = run { // default pagination
            val pagination = document.selectFirst(Evaluator.Class("acgn-pages"))!!
            pagination.children().last()!!.tagName() == "a"
        }
        return MangasPage(mangas, hasNextPage)
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val request = super.searchMangaRequest(page, query, filters)
        return if (query.isNotBlank()) {
            // TODO: Fix Captcha
            throw Exception("暂不支持搜索，请等待后续插件更新")
            // request.newBuilder().headers(headers).build()
        } else {
            request
        }
    }

    private fun searchMangaParse(document: Document): MangasPage {
        val entries = document.select(Evaluator.Class("comic-list-item")).map {
            SManga.create().apply {
                val titleElement = it.selectFirst(Evaluator.Class("comic-name"))!!.child(0)
                url = titleElement.attr("href")
                title = titleElement.ownText()
                author = it.selectFirst(Evaluator.Class("comic-author"))?.ownText()
                genre = it.selectFirst(Evaluator.Class("comic-tags"))?.run {
                    children().joinToString { it.ownText() }
                }
                thumbnail_url = it.selectFirst(Evaluator.Tag("img"))!!.attr("src")
            }.cleanup()
        }
        return MangasPage(entries, false)
    }

    override fun mangaDetailsParse(response: Response) = SManga.create().apply {
        val document = response.asJsoup().selectFirst(Evaluator.Class("acgn-model-detail-frontcover"))!!
        title = document.selectFirst(Evaluator.Tag("h1"))!!.ownText()
        description = document.selectFirst(Evaluator.Class("desc-content"))?.ownText()
        genre = document.select("ul.tags > a[href]").joinToString { it.ownText() }
        thumbnail_url = document.selectFirst(Evaluator.Tag("img"))?.attr("src")
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        val scripts = document.select(Evaluator.Tag("script"))
        val js = scripts[scripts.size - 2].data()
        val start = js.indexOf('[')
        val end = js.lastIndexOf(']') + 1
        val replaced = js.substring(start, end).replace('\'', '"')
        val list: List<AcgnChapter> = json.decodeFromString(replaced)
        val dateFormat = dateFormat
        return list.asReversed().map { it.toSChapter(dateFormat) }
    }

    override val lazyLoadImageAttr get() = "data-echo"

    @Serializable
    class AcgnChapter(
        private val name: String,
        private val url: String,
        private val time: String,
    ) {
        fun toSChapter(dateFormat: SimpleDateFormat) = SChapter.create().apply {
            url = this@AcgnChapter.url
            name = this@AcgnChapter.name
            date_upload = dateFormat.parse(time)?.time ?: 0
        }
    }

    private val json: Json by injectLazy()

    private val dateFormat by lazy {
        SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
    }
}
