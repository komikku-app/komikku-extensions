package eu.kanade.tachiyomi.extension.ar.mangaae

import android.app.Application
import android.content.SharedPreferences
import android.widget.Toast
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.extension.BuildConfig
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MangaAe : ParsedHttpSource(), ConfigurableSource {

    override val name = "مانجا العرب"

    private val defaultBaseUrl = "https://manga.ae"
    private val defaultUserAgent = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/114.0.0.0 Mobile Safari/537.3"

    override val baseUrl by lazy { getPrefBaseUrl() }
    private val userAgent by lazy { getPrefUserAgent() }

    private val preferences: SharedPreferences by lazy {
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)
    }

    override val lang = "ar"

    override val supportsLatest = true

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .rateLimit(2)
        .build()

    override fun headersBuilder() = Headers.Builder().apply {
        set("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
        set("Accept-Language", "en-US,en;q=0.9,ar-MA;q=0.8,ar;q=0.7")
        set("Connection", "keep-alive")
        set("Sec-Fetch-Dest", "document")
        set("Sec-Fetch-Mode", "navigate")
        set("Sec-Fetch-Site", "same-origin")
        set("Sec-Fetch-User", "?1")
        set("Referer", baseUrl)
    }

    // Popular
    override fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/manga/page:$page", headers)
    }

    override fun popularMangaNextPageSelector() = "div.pagination a:last-child:not(.active)"

    override fun popularMangaSelector() = "div.mangacontainer"

    override fun popularMangaFromElement(element: Element): SManga = SManga.create().apply {
        val lazysrc = element.select("img").attr("data-pagespeed-lazy-src")
        thumbnail_url = if (lazysrc.isNullOrEmpty()) {
            element.select("img").attr("src")
        } else {
            lazysrc
        }
        element.select("div.mangacontainer a.manga")[0].let {
            title = it.text()
            setUrlWithoutDomain(it.attr("abs:href"))
        }
    }

    // Latest
    override fun latestUpdatesRequest(page: Int): Request {
        return GET(baseUrl, headers)
    }

    override fun latestUpdatesSelector(): String = "div.popular-manga-container"

    override fun latestUpdatesFromElement(element: Element): SManga = SManga.create().apply {
        val lazysrc = element.select("img").attr("data-pagespeed-lazy-src")
        thumbnail_url = if (lazysrc.isNullOrEmpty()) {
            element.select("img").attr("src")
        } else {
            lazysrc
        }
        setUrlWithoutDomain(element.select("a:has(img)").attr("href"))
        title = element.select("a").last()!!.text()
    }

    override fun latestUpdatesNextPageSelector(): String? = null

    // Search
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        var url = "$baseUrl/manga/search:$query|page:$page"
        filters.forEach { filter ->
            when (filter) {
                is OrderByFilter -> {
                    if (filter.state != 0) {
                        url += "|order:${filter.toUriPart()}"
                    }
                }
                else -> {}
            }
        }
        url += "|arrange:minus"
        return GET(url.toHttpUrlOrNull()!!.newBuilder().build().toString(), headers)
    }

    override fun searchMangaSelector(): String = popularMangaSelector()

    override fun searchMangaFromElement(element: Element): SManga = popularMangaFromElement(element)

    override fun searchMangaNextPageSelector() = popularMangaNextPageSelector()

    // Manga summary page
    override fun mangaDetailsParse(document: Document): SManga = SManga.create().apply {
        val infoElement = document.select("div.indexcontainer").first()!!
        title = infoElement.select("h1.EnglishName").text().removeSurrounding("(", ")")
        author = infoElement.select("div.manga-details-author h4")[0].text()
        artist = author
        status = parseStatus(infoElement.select("div.manga-details-extended td h4")[0].text().trim())
        genre = infoElement.select("div.manga-details-extended a[href*=tag]").joinToString(", ") { it.text() }
        description = infoElement.select("div.manga-details-extended h4[style*=overflow-y]")[0].text()
        thumbnail_url = infoElement.select("img.manga-cover").attr("src")
    }

    private fun parseStatus(status: String?) = when {
        status == null -> SManga.UNKNOWN
        status.contains("مستمرة") -> SManga.ONGOING
        status.contains("مكتملة") -> SManga.COMPLETED
        else -> SManga.UNKNOWN
    }

    // Chapters
    override fun chapterListSelector() = "ul.new-manga-chapters > li"

    override fun chapterFromElement(element: Element): SChapter {
        val chapter = SChapter.create()
        element.select("a").let {
            // use full pages for easier links
            chapter.setUrlWithoutDomain(it.attr("href").removeSuffix("/1/") + "/0/allpages")
            chapter.name = "\u061C" + it.text() // Add unicode ARABIC LETTER MARK to ensure all titles are right to left
        }
        return chapter
    }

    // Pages
    override fun pageListParse(document: Document): List<Page> {
        val pages = mutableListOf<Page>()
        document.select("div#showchaptercontainer img").forEach {
            pages.add(Page(pages.size, "", it.attr("src")))
        }
        return pages
    }

    override fun imageUrlParse(document: Document): String = throw Exception("Not used")

    override fun imageRequest(page: Page): Request {
        val imgHeaders = headersBuilder().apply {
            set("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
            set("Referer", baseUrl)
        }.build()
        return GET(page.imageUrl!!, imgHeaders)
    }

    private open class UriPartFilter(displayName: String, val vals: Array<Pair<String, String>>) :
        Filter.Select<String>(displayName, vals.map { it.first }.toTypedArray()) {
        fun toUriPart() = vals[state].second
    }

    private class OrderByFilter : UriPartFilter(
        "الترتيب حسب",
        arrayOf(
            Pair("اختيار", ""),
            Pair("اسم المانجا", "english_name"),
            Pair("تاريخ النشر", "release_date"),
            Pair("عدد الفصول", "chapter_count"),
            Pair("الحالة", "status"),
        ),
    )

    override fun getFilterList() = FilterList(
        OrderByFilter(),
    )

    companion object {
        private const val RESTART_TACHIYOMI = ".لتطبيق الإعدادات الجديدة Tachiyomi أعد تشغيل"
        private const val BASE_URL_PREF_TITLE = "تعديل الرابط"
        private const val BASE_URL_PREF = "overrideBaseUrl_v${BuildConfig.VERSION_CODE}"
        private const val USER_AGENT_PREF_TITLE = "تعديل وكيل المستخدم"
        private const val USER_AGENT_PREF = "overrideUserAgent_v${BuildConfig.VERSION_CODE}"
        private const val PREF_SUMMARY = ".للاستخدام المؤقت. تحديث التطبيق سيؤدي الى حذف الإعدادات"
    }

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        val baseUrlPref = androidx.preference.EditTextPreference(screen.context).apply {
            key = BASE_URL_PREF
            title = BASE_URL_PREF_TITLE
            summary = PREF_SUMMARY
            this.setDefaultValue(defaultBaseUrl)
            dialogTitle = BASE_URL_PREF_TITLE

            setOnPreferenceChangeListener { _, _ ->
                Toast.makeText(screen.context, RESTART_TACHIYOMI, Toast.LENGTH_LONG).show()
                true
            }
        }
        val userAgentPref = androidx.preference.EditTextPreference(screen.context).apply {
            key = USER_AGENT_PREF
            title = USER_AGENT_PREF_TITLE
            summary = PREF_SUMMARY
            this.setDefaultValue(defaultUserAgent)
            dialogTitle = USER_AGENT_PREF_TITLE

            setOnPreferenceChangeListener { _, _ ->
                Toast.makeText(screen.context, RESTART_TACHIYOMI, Toast.LENGTH_LONG).show()
                true
            }
        }
        screen.addPreference(baseUrlPref)
        screen.addPreference(userAgentPref)
    }

    private fun getPrefBaseUrl(): String = preferences.getString(BASE_URL_PREF, defaultBaseUrl)!!
    private fun getPrefUserAgent(): String = preferences.getString(USER_AGENT_PREF, defaultUserAgent)!!
}
