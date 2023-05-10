package eu.kanade.tachiyomi.extension.all.asurascans

import android.app.Application
import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import rx.Observable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

open class AsuraScans(
    override val baseUrl: String,
    override val lang: String,
    dateFormat: SimpleDateFormat,
) : MangaThemesia(
    "Asura Scans",
    baseUrl,
    lang,
    dateFormat = dateFormat,
) {
    private val preferences = Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .addInterceptor(::urlChangeInterceptor)
        .addInterceptor(uaIntercept)
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .rateLimit(1, 3, TimeUnit.SECONDS)
        .build()

    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> {
        val newManga = manga.apply {
            url = "$url#chapters#${title.toSearchQuery()}"
        }
        return super.fetchChapterList(newManga)
    }

    override fun fetchMangaDetails(manga: SManga): Observable<SManga> {
        val newManga = try {
            manga.apply {
                url = "$url#details#${title.toSearchQuery()}"
            }
        } catch (e: UninitializedPropertyAccessException) {
            // when called from deep link, title is not present
            manga
        }

        return super.fetchMangaDetails(newManga)
    }

    // use updated url for webView
    override fun getMangaUrl(manga: SManga): String {
        val dbSlug = manga.url
            .removeSuffix("/")
            .substringAfterLast("/")

        val storedSlug = getSlugMap()[dbSlug] ?: dbSlug

        return "$baseUrl$mangaUrlDirectory/$storedSlug/"
    }

    // Skip scriptPages
    override fun pageListParse(document: Document): List<Page> {
        return document.select(pageSelector)
            .filterNot { it.attr("src").isNullOrEmpty() }
            .mapIndexed { i, img -> Page(i, "", img.attr("abs:src")) }
    }

    override fun Element.imgAttr(): String = when {
        hasAttr("data-lazy-src") -> attr("abs:data-lazy-src")
        hasAttr("data-src") -> attr("abs:data-src")
        hasAttr("data-cfsrc") -> attr("abs:data-cfsrc")
        else -> attr("abs:src")
    }

    private fun urlChangeInterceptor(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val frag = request.url.fragment

        if (frag.isNullOrEmpty()) {
            return chain.proceed(request)
        }

        val search = frag.substringAfter("#")

        val dbSlug = request.url.toString()
            .substringBefore("#")
            .removeSuffix("/")
            .substringAfterLast("/")

        val slugMap = getSlugMap().toMutableMap()

        // make sure db slug key is present in the slugMap
        val storedSlug = slugMap[dbSlug] ?: dbSlug

        val response = chain.proceed(newRequest(frag, storedSlug))

        if (!response.isSuccessful && response.code == 404) {
            response.close()

            val newSlug = getNewSlug(storedSlug, search)
                ?: throw IOException("Migrate from Asura to Asura")

            slugMap[dbSlug] = newSlug
            putSlugMap(slugMap)

            return chain.proceed(newRequest(frag, newSlug))
        }

        return response
    }

    private fun getNewSlug(existingSlug: String, search: String): String? {
        val permaSlug = existingSlug
            .replaceFirst(TEMP_TO_PERM_REGEX, "")

        val mangas = client.newCall(searchMangaRequest(1, search, FilterList()))
            .execute()
            .use {
                searchMangaParse(it)
            }

        return mangas.mangas.firstOrNull { newManga ->
            newManga.url.contains(permaSlug, true)
        }
            ?.url
            ?.removeSuffix("/")
            ?.substringAfterLast("/")
    }

    private fun newRequest(frag: String, slug: String): Request {
        val manga = SManga.create().apply {
            this.url = "$mangaUrlDirectory/$slug/"
        }

        return when (frag.substringBefore("#")) {
            "chapters" -> chapterListRequest(manga)
            "details" -> mangaDetailsRequest(manga)
            else -> throw IOException("unknown url fragment for urlChangeInterceptor")
        }
    }

    private fun putSlugMap(slugMap: MutableMap<String, String>) {
        val serialized = json.encodeToString(slugMap)

        preferences.edit().putString(PREF_URL_MAP, serialized).commit()
    }

    private fun getSlugMap(): Map<String, String> {
        val serialized = preferences.getString(PREF_URL_MAP, null) ?: return emptyMap()

        return try {
            json.decodeFromString(serialized)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun String.toSearchQuery(): String {
        return this.trim()
            .lowercase()
            .replace(titleSpecialCharactersRegex, "+")
            .replace(trailingPlusRegex, "")
    }

    companion object {
        private const val PREF_URL_MAP = "pref_url_map"
        private val TEMP_TO_PERM_REGEX = Regex("""^\d+-""")
        private val titleSpecialCharactersRegex = Regex("""[^a-z0-9]+""")
        private val trailingPlusRegex = Regex("""\++$""")
    }
}
