package eu.kanade.tachiyomi.extension.en.asurascans

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.wpmangastream.WPMangaStream
import eu.kanade.tachiyomi.source.model.Page
import okhttp3.OkHttpClient
import org.jsoup.nodes.Document
import java.util.concurrent.TimeUnit

class AsuraScans : WPMangaStream("AsuraScans", "https://www.asurascans.com", "en") {

    private val rateLimitInterceptor = RateLimitInterceptor(1, 3, TimeUnit.SECONDS)

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addNetworkInterceptor(rateLimitInterceptor)
        .build()

    override val pageSelector = "div.rdminimal img[loading*=lazy]"

    // Skip scriptPages
    override fun pageListParse(document: Document): List<Page> {
        return document.select(pageSelector)
            .filterNot { it.attr("abs:src").isNullOrEmpty() }
            .mapIndexed { i, img -> Page(i, "", img.attr("abs:src")) }
    }
}
