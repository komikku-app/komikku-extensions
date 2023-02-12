package eu.kanade.tachiyomi.extension.zh.vomic

import android.app.Application
import androidx.preference.ListPreference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.multisrc.mangabz.MangabzTheme
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.jsoup.select.Evaluator
import rx.Observable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.text.SimpleDateFormat
import java.util.Locale

class Vomic : MangabzTheme("vomic", ""), ConfigurableSource {

    override val supportsLatest = false

    override val baseUrl: String

    init {
        val mirrors = MIRRORS
        val mirrorIndex = Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)
            .getString(MIRROR_PREF, "0")!!.toInt().coerceAtMost(mirrors.size - 1)
        baseUrl = "http://" + mirrors[mirrorIndex]
    }

    override fun headersBuilder() = super.headersBuilder().removeAll("Referer")

    override fun popularMangaRequest(page: Int) = GET(baseUrl, headers)

    // original credit: https://github.com/tachiyomiorg/tachiyomi-extensions/pull/5628
    override fun popularMangaParse(response: Response): MangasPage {
        val document = response.asJsoup()

        val link = Evaluator.Tag("a")
        val image = Evaluator.Tag("img")
        val paragraph = Evaluator.Tag("p")

        /* top banner - no thumbnail
        document.selectFirst(Evaluator.Class("banner-con")).select(link).mapTo(mangas) { element ->
            SManga.create().apply {
                title = element.attr("title")
                url = element.attr("href")
                thumbnail_url = element.selectFirst(image).attr("src")
                    .takeIf { !it.endsWith("/static/images/bg/banner_info_a.jpg") }
            }
        } */

        val mangas = buildList {
            // ranking sidebar
            addAll(document.selectFirst(Evaluator.Class("rank-list"))!!.children())
            // carousel list
            addAll(document.selectFirst(Evaluator.Class("carousel-right-list"))!!.children())
            // recommend list
            addAll(document.select(Evaluator.Class("index-manga-item"))!!)
        }.map { element ->
            SManga.create().apply {
                title = element.selectFirst(paragraph)!!.text()
                url = element.selectFirst(link)!!.attr("href")
                thumbnail_url = element.selectFirst(image)!!.attr("src")
            }
        }

        return MangasPage(mangas.distinctBy { it.url }, false)
    }

    override fun parseDescription(element: Element, title: String, details: Elements): String {
        val text = element.ownText()
        val collapsed = element.selectFirst(Evaluator.Tag("span"))?.ownText() ?: ""
        val source = details[3].text()
        return "$source\n\n$text$collapsed"
    }

    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> {
        val chapterId = manga.url.removePrefix("/").removeSuffix("_c/")
        return super.fetchChapterList(manga).doOnNext {
            for (chapter in it) chapter.url = chapter.url + "chapterimage.ashx?mid=" + chapterId
        }
    }

    override fun getChapterElements(document: Document): Elements {
        val chapterId = document.location().removeSuffix("_c/").substringAfterLast('/')
        val response = client.newCall(GET("$baseUrl/chapter-$chapterId-s2/", headers)).execute()
        return Jsoup.parseBodyFragment(response.body.string()).body().children()
    }

    override val needPageCount = false

    override fun parseDate(listTitle: String): Long {
        val date = listTitle.split("|")[2].trim()
        return dateFormat.parse(date)!!.time
    }

    override fun pageListParse(response: Response): List<Page> {
        val urls = response.body.string().run {
            val left = indexOf('[')
            val right = lastIndexOf(']')
            if (left + 1 == right) return emptyList()
            substring(left + 1, right).split(", ")
        }
        return urls.mapIndexed { index, rawUrl ->
            val url = rawUrl.trim('"')
            val imageUrl = when {
                url.startsWith("http://127.0.0.1") -> url.toHttpUrl().queryParameter("url")
                else -> url
            }
            Page(index, imageUrl = imageUrl)
        }
    }

    override fun imageRequest(page: Page): Request {
        val url = page.imageUrl!!
        val host = url.toHttpUrl().host
        val headers = headersBuilder().set("Referer", "https://$host/").build()
        return GET(url, headers)
    }

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        ListPreference(screen.context).apply {
            val mirrors = MIRRORS
            key = MIRROR_PREF
            title = "镜像网址"
            summary = "%s\n重启生效"
            entries = mirrors
            entryValues = Array(mirrors.size) { it.toString() }
            setDefaultValue("0")
        }.let(screen::addPreference)
    }

    companion object {
        private const val MIRROR_PREF = "MIRROR"
        private val MIRRORS get() = arrayOf("www.vomicmh.com", "www.iewoai.com")

        private val dateFormat by lazy { SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.ENGLISH) }
    }
}
