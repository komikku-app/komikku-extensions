package eu.kanade.tachiyomi.extension.ar.mangaproz

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.wpmangastream.WPMangaStream
import eu.kanade.tachiyomi.source.model.SChapter
import okhttp3.OkHttpClient
import org.jsoup.nodes.Element
import java.util.concurrent.TimeUnit

class MangaProZ : WPMangaStream("Manga Pro Z", "https://mangaproz.com", "ar") {
    private val rateLimitInterceptor = RateLimitInterceptor(4)

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addNetworkInterceptor(rateLimitInterceptor)
        .build()

    override fun chapterFromElement(element: Element): SChapter = super.chapterFromElement(element).apply { name = name.removeSuffix(" free") }
}
