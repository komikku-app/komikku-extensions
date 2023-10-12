package eu.kanade.tachiyomi.extension.pt.ladyestelarscan

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.asObservable
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Response
import rx.Observable
import java.lang.IllegalStateException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class LadyEstelarScan : Madara(
    "Lady Estelar Scan",
    "https://ladyestelarscan.com.br",
    "pt-BR",
    SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")),
) {

    override val client: OkHttpClient = super.client.newBuilder()
        .rateLimit(1, 2, TimeUnit.SECONDS)
        .build()

    override val useNewChapterEndpoint = true

    override fun fetchPopularManga(page: Int): Observable<MangasPage> {
        return client.newCall(popularMangaRequest(page))
            .asObservableSuccessIgnoreCode(404)
            .map(::popularMangaParse)
    }

    override fun fetchLatestUpdates(page: Int): Observable<MangasPage> {
        return client.newCall(latestUpdatesRequest(page))
            .asObservableSuccessIgnoreCode(404)
            .map(::latestUpdatesParse)
    }

    override fun fetchSearchManga(
        page: Int,
        query: String,
        filters: FilterList,
    ): Observable<MangasPage> {
        return client.newCall(searchMangaRequest(page, query, filters))
            .asObservableSuccessIgnoreCode(404)
            .map(::searchMangaParse)
    }

    /**
     * Their site have some issues and is returning 404 in some pages even if they exist.
     */
    private fun Call.asObservableSuccessIgnoreCode(code: Int): Observable<Response> {
        return asObservable().doOnNext { response ->
            if (!response.isSuccessful && response.code != code) {
                response.close()
                throw IllegalStateException("HTTP error ${response.code}")
            }
        }
    }
}
