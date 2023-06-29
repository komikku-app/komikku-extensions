package eu.kanade.tachiyomi.extension.zh.vomic

import android.app.Application
import android.util.Base64
import androidx.preference.ListPreference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.text.SimpleDateFormat
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class Vomic : HttpSource(), ConfigurableSource {

    override val name = "vomic"

    override val lang = "zh"

    override val supportsLatest = false

    override val baseUrl: String

    private val apiUrl: String

    init {
        val mirrors = MIRRORS
        val mirrorIndex = Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)
            .getString(MIRROR_PREF, "0")!!.toInt().coerceAtMost(mirrors.size - 1)
        baseUrl = "http://" + mirrors[mirrorIndex]
        apiUrl = "http://" + mirrors[mirrorIndex].replace("www.", "api.")
    }

    override fun headersBuilder() = Headers.Builder().add("User-Agent", System.getProperty("http.agent")!!)

    override fun popularMangaRequest(page: Int) = GET("$apiUrl/api/v1/rank/rank-data?rank_id=1&page=$page", headers)

    override fun popularMangaParse(response: Response): MangasPage {
        val mangaList: RankingDto = response.parseAs()
        val entries = mangaList.result.mapNotNull { it.toSMangaOrNull() }
        val hasNextPage = response.request.url.queryParameter("page") != "4"
        return MangasPage(entries, hasNextPage)
    }

    override fun latestUpdatesRequest(page: Int) = throw UnsupportedOperationException()

    override fun latestUpdatesParse(response: Response) = throw UnsupportedOperationException()

    override fun getFilterList() = getFilterListInternal()

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val searchQuery = parseSearchQuery(query.trim(), filters)
        if (searchQuery.title.isEmpty() && searchQuery.category.isEmpty()) throw Exception("请输入搜索词或分类")

        val url = "$apiUrl/api/v1/search/search-comic-data".toHttpUrl().newBuilder()
            .addQueryParameter("title", searchQuery.title)
            .addQueryParameter("category", searchQuery.category)
            .addEncodedQueryParameter("page", page.toString())
            .build()
        return GET(url, headers)
    }

    override fun searchMangaParse(response: Response): MangasPage {
        val mangaList: MangaListDto = response.parseAs()
        val entries = mangaList.entries.mapNotNull { it.toSMangaOrNull() }
        return MangasPage(entries, mangaList.hasNextPage)
    }

    override fun getMangaUrl(manga: SManga) = "$baseUrl/#/detail?id=${manga.id}"

    override fun mangaDetailsRequest(manga: SManga) =
        GET("$apiUrl/api/v1/detail/get-comic-detail-data?mid=${manga.id}", headers)

    override fun mangaDetailsParse(response: Response) =
        response.parseAs<MangaDto>().toSMangaDetails()

    override fun chapterListRequest(manga: SManga) =
        GET("$apiUrl/api/v1/detail/get-comic-detail-chapter-data?mid=${manga.id}", headers)

    override fun chapterListParse(response: Response): List<SChapter> {
        val chapters: List<ChapterDto> = response.parseAs()
        val mangaId = response.request.url.queryParameter("mid")!!
        val dateFormat = dateFormat
        return chapters.map { it.toSChapter(mangaId, dateFormat) }
    }

    override fun getChapterUrl(chapter: SChapter): String {
        val (mangaId, chapterId) = chapter.id
        return "$baseUrl/#/page/$mangaId/$chapterId"
    }

    override fun pageListRequest(chapter: SChapter): Request {
        val (mangaId, chapterId) = chapter.id
        val key = run {
            val alphanumeric = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
            val chars = CharArray(24) { alphanumeric.random() }
            String(chars)
        }
        val time = System.currentTimeMillis().toString()
        val encrypted = run {
            val keySpec = SecretKeySpec(key.toByteArray(), "DESede")
            val iv = "k8tUyS\$m"
            val ivSpec = IvParameterSpec(iv.toByteArray())
            val payload = key + iv + "cid=" + chapterId + "&mid=" + mangaId + time
            val bytes = Cipher.getInstance("DESede/CBC/PKCS5Padding").run {
                init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
                doFinal(payload.toByteArray())
            }
            Base64.encodeToString(bytes, Base64.DEFAULT)
        }
        val url = "$apiUrl/api/v2/page/get-comic-page-img-data".toHttpUrl().newBuilder()
            .addEncodedQueryParameter("k", key)
            .addEncodedQueryParameter("t", time)
            .addQueryParameter("e", encrypted)
            .build()
        return GET(url, headers)
    }

    override fun pageListParse(response: Response): List<Page> {
        val pageList: List<String> = response.parseAs()
        if (pageList.size == 1 && pageList[0] == "https://cdn.vomicer.com/qiniu/vomic/otherImg/info2.webp") {
            throw Exception("无法阅读此章节")
        }
        return pageList.mapIndexed { index, imageUrl -> Page(index, imageUrl = imageUrl) }
    }

    override fun imageUrlParse(response: Response) = throw UnsupportedOperationException()

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

    private val json: Json by injectLazy()

    private inline fun <reified T> Response.parseAs(): T =
        json.decodeFromString<ResponseDto<T>>(body.string()).data

    companion object {
        private const val MIRROR_PREF = "MIRROR"
        private val MIRRORS get() = arrayOf("www.vomicmh.com", "www.iewoai.com")

        private val dateFormat by lazy { SimpleDateFormat("yyyy-MM-dd hh:mm:ss", Locale.ENGLISH) }
    }
}
