package eu.kanade.tachiyomi.extension.zh.pufei

import android.app.Application
import android.content.SharedPreferences
import android.util.Base64
import androidx.preference.ListPreference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Evaluator
import rx.Observable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

// Uses www733dm/IMH/dm456 theme
class Pufei : ParsedHttpSource(), ConfigurableSource {

    override val name = "扑飞漫画"
    override val lang = "zh"
    override val supportsLatest = true

    private val preferences: SharedPreferences =
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)

    private val domain = preferences.getString(MIRROR_PREF, "0")!!.toInt()
        .coerceIn(0, MIRRORS.size - 1).let { MIRRORS[it] }

    override val baseUrl = "http://m.$domain"
    private val pcUrl = "http://www.$domain"

    override val client = network.client.newBuilder()
        .addInterceptor(NonblockingRateLimiter(2))
        .addInterceptor(OctetStreamInterceptor)
        .build()

    private val searchClient = network.client.newBuilder()
        .followRedirects(false)
        .build()

    override fun popularMangaRequest(page: Int) = GET("$baseUrl/manhua/paihang.html", headers)
    override fun popularMangaNextPageSelector() = throw UnsupportedOperationException("Not used.")
    override fun popularMangaSelector() = "ul#detail > li > a"
    override fun popularMangaFromElement(element: Element) = SManga.create().apply {
        url = element.attr("href").removeSuffix("/index.html")
        title = element.selectFirst(Evaluator.Tag("h3")).text()
        thumbnail_url = element.selectFirst(Evaluator.Tag("img")).attr("data-src")
    }

    override fun popularMangaParse(response: Response): MangasPage {
        val document = response.asPufeiJsoup()
        val mangas = document.select(popularMangaSelector()).map { popularMangaFromElement(it) }
        return MangasPage(mangas, false)
    }

    override fun latestUpdatesRequest(page: Int) = GET("$baseUrl/manhua/update.html", headers)
    override fun latestUpdatesNextPageSelector() = throw UnsupportedOperationException("Not used.")
    override fun latestUpdatesSelector() = popularMangaSelector()
    override fun latestUpdatesFromElement(element: Element) = popularMangaFromElement(element)

    override fun latestUpdatesParse(response: Response): MangasPage {
        val document = response.asPufeiJsoup()
        val mangas = document.select(latestUpdatesSelector()).map { latestUpdatesFromElement(it) }
        return MangasPage(mangas, false)
    }

    private val searchCache = HashMap<String, String>(0)

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        return if (query.isNotBlank()) {
            val path = searchCache.getOrPut(query) {
                val formBody = FormBody.Builder(GB2312)
                    .addEncoded("tempid", "4")
                    .addEncoded("show", "title,player,playadmin,bieming,pinyin")
                    .add("keyboard", query)
                    .build()
                val request = POST("$baseUrl/e/search/index.php", headers, formBody)
                searchClient.newCall(request).execute().header("location")!!
            }
            val sortQuery = parseSearchSort(filters)
            GET("$baseUrl/e/search/$path$sortQuery&page=${page - 1}")
        } else {
            val path = parseFilters(page, filters)
            if (path.isEmpty())
                popularMangaRequest(page)
            else
                GET("$baseUrl$path", headers)
        }
    }

    override fun searchMangaNextPageSelector() = throw UnsupportedOperationException("Not used.")
    override fun searchMangaSelector() = popularMangaSelector()
    override fun searchMangaFromElement(element: Element) = popularMangaFromElement(element)

    override fun searchMangaParse(response: Response): MangasPage {
        val document = response.asPufeiJsoup()
        val mangas = document.select(searchMangaSelector()).map { searchMangaFromElement(it) }
        val hasNextPage = run {
            for (element in document.body().children().asReversed()) {
                if (element.tagName() == "a") return@run true
                else if (element.tagName() == "b") return@run false
            }
            false
        }
        return MangasPage(mangas, hasNextPage)
    }

    override fun getFilterList() = getFilters()

    // 让 WebView 显示移动端页面
    override fun mangaDetailsRequest(manga: SManga) = GET(baseUrl + manga.url, headers)

    override fun fetchMangaDetails(manga: SManga): Observable<SManga> =
        client.newCall(GET(pcUrl + manga.urlWithCheck(), headers)).asObservableSuccess()
            .map { mangaDetailsParse(it.asPufeiJsoup()) }

    override fun mangaDetailsParse(document: Document) = SManga.create().apply {
        val details = document.selectFirst(Evaluator.Class("detailInfo")).children()
        title = details[0].child(0).text() // div.titleInfo > h1
        val genreList = mutableListOf<String>()
        for (item in details[1].children()) { // ul > li
            when (item.child(0).text()) { // span
                "作者：" -> author = item.ownText()
                "类别：" -> item.ownText().let { if (it.isNotEmpty()) genreList.add(it) }
                "关键词：" -> item.ownText().let { if (it.isNotEmpty()) genreList.addAll(it.split(',')) }
            }
        }
        author = author ?: details[0].ownText().removePrefix("作者：")
        if (genreList.isEmpty()) {
            genreList.add(document.selectFirst(Evaluator.Class("position")).child(1).text())
        }
        genre = genreList.joinToString()
        description = document.selectFirst("div.introduction")?.text() ?: details[2].ownText()
        status = SManga.UNKNOWN // 所有漫画的标记都是连载，所以没有意义，参见 baseUrl/manhua/wanjie.html
        thumbnail_url = document.selectFirst("img.pic").attr("src")
    }

    override fun chapterListRequest(manga: SManga) = GET(pcUrl + manga.urlWithCheck(), headers)

    override fun chapterListSelector() = "div.plistBox ul > li > a"

    override fun chapterFromElement(element: Element) = SChapter.create().apply {
        url = element.attr("href")
        name = element.attr("title")
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asPufeiJsoup()
        val list = document.select(chapterListSelector()).map { chapterFromElement(it) }
        if (isNewDateLogic && list.isNotEmpty()) {
            val date = document.selectFirst("li.twoCol:contains(更新时间)").text().removePrefix("更新时间：").trim()
            list[0].date_upload = dateFormat.parse(date)?.time ?: 0
        }
        return list
    }

    override fun pageListRequest(chapter: SChapter) = GET(baseUrl + chapter.url, headers)

    // Reference: https://github.com/evanw/packer/blob/master/packer.js
    override fun pageListParse(response: Response): List<Page> {
        val html = String(response.body!!.bytes(), GB2312).let(::ProgressiveParser)
        val base64 = html.substringBetween("cp=\"", "\"")
        val packed = String(Base64.decode(base64, Base64.DEFAULT)).let(::ProgressiveParser)
        packed.consumeUntil("p}('")
        val imageList = packed.substringBetween("[", "]").replace("\\", "")
        if (imageList.isEmpty()) return emptyList()
        packed.consumeUntil("',")
        val dictionary = packed.substringBetween("'", "'").split('|')
        val result = unpack(imageList, dictionary).removeSurrounding("'").split("','")
        // baseUrl/skin/2014mh/view.js (imgserver), mobileUrl/skin/main.js (IMH.reader)
        return result.mapIndexed { i, image ->
            val imageUrl = if (image.startsWith("http")) image else IMAGE_SERVER + image
            Page(i, imageUrl = imageUrl)
        }
    }

    override fun pageListParse(document: Document) = throw UnsupportedOperationException("Not used.")

    override fun imageUrlParse(document: Document) = throw UnsupportedOperationException("Not used.")

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        ListPreference(screen.context).apply {
            key = MIRROR_PREF
            title = "使用镜像网站"
            summary = "选择要使用的镜像网站，重启生效"
            entries = MIRRORS_DESCRIPTION
            entryValues = MIRROR_VALUES
            setDefaultValue("0")
            setOnPreferenceChangeListener { _, newValue ->
                preferences.edit().putString(MIRROR_PREF, newValue as String).apply()
                true
            }
        }.let { screen.addPreference(it) }
    }

    companion object {
        private const val MIRROR_PREF = "MIRROR"
        private val MIRROR_VALUES = arrayOf("0", "1", "2", "3", "4")
        private val MIRRORS = arrayOf(
            "pufei.cc",
            "pfmh.net",
            "alimanhua.com",
            "8nfw.com",
            "pufei5.com",
        )
        private val MIRRORS_DESCRIPTION = arrayOf(
            "pufei.cc",
            "pfmh.net",
            "alimanhua.com (阿狸漫画)",
            "8nfw.com (风之动漫)",
            "pufei5.com (不推荐)",
        )

        private const val IMAGE_SERVER = "http://res.img.tueqi.com/"
    }
}
