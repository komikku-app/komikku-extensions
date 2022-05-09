package eu.kanade.tachiyomi.extension.zh.wuqimanga

import android.app.Application
import android.content.SharedPreferences
import androidx.preference.ListPreference
import androidx.preference.PreferenceScreen
import com.squareup.duktape.Duktape
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Headers
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class WuqiManga : ParsedHttpSource(), ConfigurableSource {

    override val name = "57漫画"
    override val baseUrl = "http://www.wuqimh.net"
    override val lang = "zh"
    override val supportsLatest = true
    // Server list can be found from baseUrl/templates/wuqi/default/scripts/configs.js?v=1.0.3
    private val imageServer = arrayOf("http://imagesold.benzidui.com", "http://images.tingliu.cc")

    private val preferences: SharedPreferences =
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)

    override fun latestUpdatesRequest(page: Int) = GET("$baseUrl/list/order-addtime-p-$page", headers)
    override fun latestUpdatesNextPageSelector() = popularMangaNextPageSelector()
    override fun latestUpdatesSelector() = popularMangaSelector()
    override fun latestUpdatesFromElement(element: Element) = popularMangaFromElement(element)

    override fun popularMangaRequest(page: Int) = GET("$baseUrl/list/order-hits-p-$page", headers)
    override fun popularMangaSelector() = "ul#contList > li"
    override fun popularMangaNextPageSelector(): String? = "span.pager > a.prev:contains(下一页)"

    override fun popularMangaFromElement(element: Element): SManga {
        val coverEl = element.select("a img").first()
        val cover = if (coverEl.hasAttr("data-src")) {
            coverEl.attr("data-src")
        } else {
            coverEl.attr("src")
        }
        val title = element.select("a").attr("title")
        val url = element.select("a").attr("href")

        val manga = SManga.create()

        manga.thumbnail_url = cover
        manga.title = title
        manga.url = url

        return manga
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        return GET("$baseUrl/search/q_$query-p-$page", headers)
    }

    override fun searchMangaNextPageSelector() = popularMangaNextPageSelector()
    override fun searchMangaSelector() = "div.book-result li.cf"
    override fun searchMangaFromElement(element: Element): SManga {
        val manga = SManga.create()
        element.select("div.book-detail").first().let {
            val titleEl = it.select("dl > dt > a")
            manga.setUrlWithoutDomain(titleEl.attr("href"))
            manga.title = titleEl.attr("title").trim()
            manga.description = it.select("dd.intro").text()
            val status = it.select("dd.tags.status")
            manga.status = if (status.select("span.red").first().text().contains("连载中")) {
                SManga.ONGOING
            } else {
                SManga.COMPLETED
            }
            for (el in it.select("dd.tags")) {
                if (el.select("span strong").text().contains("作者")) {
                    manga.author = el.select("span a").text()
                }
            }
        }
        manga.thumbnail_url = element.select("a.bcover > img").attr("src")
        return manga
    }

    override fun chapterListSelector() = throw Exception("Not used")

    override fun headersBuilder() = Headers.Builder().add("Referer", "$baseUrl/")
        .set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/78.0.3904.108 Safari/537.36")

    override fun chapterFromElement(element: Element): SChapter {
        val urlElement = element.select("a")

        val chapter = SChapter.create()
        chapter.url = urlElement.attr("href")
        chapter.name = urlElement.attr("alt").trim()
        return chapter
    }

    override fun mangaDetailsParse(document: Document): SManga {
        val manga = SManga.create()
        manga.description = document.selectFirst("div#intro-all > p").text()
        manga.title = document.select(".book-title h1").text().trim()
        manga.thumbnail_url = document.select(".hcover img").attr("src")
        for (element in document.select("ul.detail-list li span")) {
            if (element.select("strong").text().contains("漫画作者")) {
                manga.author = element.select("a").text()
                break
            }
        }
        return manga
    }

    override fun imageUrlParse(document: Document): String = ""

    override fun chapterListParse(response: Response): List<SChapter> {
        val chapters = mutableListOf<SChapter>()
        response.asJsoup().select("div.chapter div.chapter-list>ul").asReversed().forEach {
            it.select("li a").forEach {
                chapters.add(
                    SChapter.create().apply {
                        url = it.attr("href")
                        name = it.attr("title")
                    }
                )
            }
        }
        return chapters
    }

    override fun pageListParse(document: Document): List<Page> {
        val html = document.html()
        val packed = Regex("eval(.*?)\\n").find(html)?.groups?.get(1)?.value
        val result = Duktape.create().use {
            it.evaluate(packed) as String
        }
        val re2 = Regex("""\{.*\}""")
        val imgJsonStr = re2.find(result)?.groups?.get(0)?.value
        val server = preferences.getString("IMAGE_SERVER", imageServer[0])
        // The website is using single quotes in json, so kotlinx.serialization doesn't work
        return imgJsonStr!!.substringAfter("'fs':['").substringBefore("'],").split("','").mapIndexed { index, it ->
            Page(index, "", if (it.startsWith("http")) it else "$server$it")
        }
    }

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        ListPreference(screen.context).apply {
            key = "IMAGE_SERVER"
            title = "线路"
            summary = "需要清除章节缓存以生效"
            entries = arrayOf("主线", "备用")
            entryValues = imageServer
            setDefaultValue(imageServer[0])

            setOnPreferenceChangeListener { _, newValue ->
                preferences.edit().putString(key, newValue as String).commit()
            }
        }.let(screen::addPreference)
    }
}
