package eu.kanade.tachiyomi.extension.all.reaperscans

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.SourceFactory
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import eu.kanade.tachiyomi.util.asJsoup
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import uy.kohesive.injekt.injectLazy
import java.text.SimpleDateFormat
import java.util.Locale

class ReaperScansFactory : SourceFactory {
    override fun createSources() = listOf(
        ReaperScansEn(),
        ReaperScansTr(),
        ReaperScansId(),
        ReaperScansFr()
    )
}

abstract class ReaperScans(
    override val baseUrl: String,
    lang: String,
    dateFormat: SimpleDateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.US)
) : Madara("Reaper Scans", baseUrl, lang, dateFormat) {

    override fun chapterFromElement(element: Element): SChapter = SChapter.create().apply {
        val urlElement = element.selectFirst(chapterUrlSelector)!!

        name = urlElement.selectFirst("p.chapter-manhwa-title")?.text()
            ?: urlElement.ownText()
        date_upload = urlElement.selectFirst("span.chapter-release-date > i")?.text()
            .let { parseChapterDate(it) }

        val fixedUrl = urlElement.attr("abs:href").toHttpUrl().newBuilder()
            .removeAllQueryParameters("style")
            .addQueryParameter("style", "list")
            .toString()

        setUrlWithoutDomain(fixedUrl)
    }
}

class ReaperScansEn : ParsedHttpSource() {
    override val name = "Reaper Scans"

    override val baseUrl = "https://reaperscans.com"

    override val lang = "en"

    override val id = 5177220001642863679

    override val supportsLatest = false

    private val json: Json by injectLazy()

    // Popular

    override fun popularMangaRequest(page: Int) = GET("$baseUrl/comics?page=$page", headers)

    override fun popularMangaNextPageSelector(): String = "button[wire:click*=nextPage]"

    override fun popularMangaSelector(): String = "li"

    override fun popularMangaFromElement(element: Element): SManga {
        return SManga.create().apply {
            element.select("a.text-white").let {
                title = it.text()
                setUrlWithoutDomain(it.attr("href"))
            }
            thumbnail_url = element.select("img").attr("abs:src")
        }
    }

    // Latest

    override fun latestUpdatesRequest(page: Int): Request = throw UnsupportedOperationException("Not used")

    override fun latestUpdatesSelector() = throw UnsupportedOperationException("Not used")

    override fun latestUpdatesFromElement(element: Element): SManga = throw UnsupportedOperationException("Not used")

    override fun latestUpdatesNextPageSelector() = throw UnsupportedOperationException("Not used")

    // Details

    override fun mangaDetailsParse(document: Document): SManga = SManga.create().apply {
        thumbnail_url = document.select("div > img").first().attr("abs:src")
        title = document.select("h1").first().text()

        status = when (document.select("dt:contains(Source Status)").next().text()) {
            "On hold" -> SManga.ON_HIATUS
            "Complete" -> SManga.COMPLETED
            "Ongoing" -> SManga.ONGOING
            else -> SManga.UNKNOWN
        }
        description = document.select("section > div:nth-child(1) > div > p").first().text()
    }

    // Chapters

    override fun chapterListSelector() = "ul > li"

    /**
     * Recursively merges j2 onto j1 in place
     * If j1 and j2 both contain keys whose values aren't both jsonObjects, j2's value overwrites j1's
     *
     */
    private fun mergeLeft(j1: JsonObject, j2: JsonObject): JsonObject = buildJsonObject {
        j1.keys.forEach { put(it, j1[it]!!) }
        j2.keys.forEach { k ->
            when {
                j1[k] !is JsonObject -> put(k, j2[k]!!)
                j1[k] is JsonObject && j2[k] is JsonObject -> put(k, mergeLeft(j1[k]!!.jsonObject, j2[k]!!.jsonObject))
            }
        }
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        var document = response.asJsoup()
        val chapters = mutableListOf<SChapter>()
        document.select("div.pb-4 > div >" + chapterListSelector()).map { chapters.add(chapterFromElement(it)) }

        val csrfToken = document.selectFirst("meta[name=csrf-token]")?.attr("content")

        val initialProps = document.selectFirst("div[wire:initial-data*=frontend.comic-chapters-list]")?.attr("wire:initial-data")?.let {
            json.parseToJsonElement(it)
        }

        if (csrfToken != null && initialProps is JsonObject) {
            var serverMemo = initialProps["serverMemo"]!!.jsonObject
            val fingerprint = initialProps["fingerprint"]!!

            var nextPage = 2
            while (document.select(popularMangaNextPageSelector()).isNotEmpty()) {
                val payload = buildJsonObject {
                    put("fingerprint", fingerprint)
                    put("serverMemo", serverMemo)
//                    put("updates", json.parseToJsonElement("[{\"type\":\"callMethod\",\"payload\":{\"id\":\"9jhcg\",\"method\":\"gotoPage\",\"params\":[$nextPage,\"page\"]}}]"))
                    putJsonArray("updates") {
                        addJsonObject {
                            put("type", "callMethod")
                            putJsonObject("payload") {
                                put("id", "9jhcg")
                                put("method", "gotoPage")
                                putJsonArray("params") {
                                    add(nextPage)
                                    add("page")
                                }
                            }
                        }
                    }
                }.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

                val request = Request.Builder().url("$baseUrl/livewire/message/frontend.comic-chapters-list").method("POST", payload).addHeader("x-csrf-token", csrfToken).addHeader("x-livewire", "true").build()

                val response1 = client.newCall(request).execute()
                val responseText = response1.body!!.string()

                val responseJson = json.parseToJsonElement(responseText).jsonObject

                // response contains state that we need to preserve
                serverMemo = mergeLeft(serverMemo, responseJson["serverMemo"]!!.jsonObject)

                document = Jsoup.parse(responseJson["effects"]!!.jsonObject.get("html")?.jsonPrimitive?.content)

                document.select(chapterListSelector()).map { chapters.add(chapterFromElement(it)) }
                nextPage++
            }
        }

        return chapters
    }

    override fun chapterFromElement(element: Element): SChapter {
        val chapter = SChapter.create()

        with(element) {
            select("a").first()?.let { urlElement ->
                chapter.setUrlWithoutDomain(urlElement.attr("abs:href"))
                chapter.name = urlElement.select("p").first().text()
            }
        }

        return chapter
    }

    // Search

    override fun searchMangaSelector(): String = "a[href*=/comics/]"

    override fun searchMangaFromElement(element: Element) = SManga.create().apply {
        setUrlWithoutDomain(element.attr("href"))

        element.select("img").first()?.let {
            thumbnail_url = it.attr("abs:src")
        }

        title = element.select("p").first().text()
    }

    override fun searchMangaNextPageSelector(): String? = throw UnsupportedOperationException("Not Used")

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val response = client.newCall(GET(baseUrl)).execute()
        val soup = response.asJsoup()

        val csrfToken = soup.selectFirst("meta[name=csrf-token]")?.attr("content")

        val initialProps = soup.selectFirst("div[wire:initial-data*=frontend.global-search]")?.attr("wire:initial-data")?.let {
            json.parseToJsonElement(it)
        }

        if (csrfToken != null && initialProps is JsonObject) {
            val serverMemo = initialProps["serverMemo"]!!.jsonObject
            val fingerprint = initialProps["fingerprint"]!!

            val payload = buildJsonObject {
                put("fingerprint", fingerprint)
                put("serverMemo", serverMemo)
//                put("updates", json.parseToJsonElement("[{\"type\":\"syncInput\",\"payload\":{\"id\":\"03r6\",\"name\":\"query\",\"value\":\"$query\"}}]"))
                putJsonArray("updates") {
                    addJsonObject {
                        put("type", "syncInput")
                        putJsonObject("payload") {
                            put("id", "03r6")
                            put("name", "query")
                            put("value", query)
                        }
                    }
                }
            }.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

            return Request.Builder().url("$baseUrl/livewire/message/frontend.global-search").method("POST", payload).addHeader("x-csrf-token", csrfToken).addHeader("x-livewire", "true").build()
        }

        throw Exception("search error")
    }

    override fun searchMangaParse(response: Response): MangasPage {
        val responseText = response.body!!.string()

        val responseJson = json.parseToJsonElement(responseText).jsonObject

        val document = Jsoup.parse(responseJson["effects"]!!.jsonObject.get("html")?.jsonPrimitive?.content)

        val mangas = document.select(searchMangaSelector()).map { element ->
            searchMangaFromElement(element)
        }

        return MangasPage(mangas, false)
    }

    // Page

    override fun pageListRequest(chapter: SChapter): Request = GET("$baseUrl${chapter.url}")

    override fun pageListParse(document: Document): List<Page> {
        return document.select("p.py-4 > img").mapIndexed { index, element ->
            Page(index, "", element.attr("src"))
        }
    }

    override fun imageUrlParse(document: Document): String = throw UnsupportedOperationException("Not Used")
}

class ReaperScansTr : ReaperScans(
    "https://reaperscanstr.com",
    "tr",
    SimpleDateFormat("MMMMM dd, yyyy", Locale("tr"))
) {

    // Tags are useless as they are just SEO keywords.
    override val mangaDetailsSelectorTag = ""
}

class ReaperScansId : ReaperScans("https://reaperscans.id", "id") {

    // Tags are useless as they are just SEO keywords.
    override val mangaDetailsSelectorTag = ""
}

class ReaperScansFr : ReaperScans(
    "https://reaperscans.fr",
    "fr",
    SimpleDateFormat("dd/MM/yyyy", Locale.US)
) {

    // Migrated from WpMangaReader to Madara.
    override val versionId = 2
}
