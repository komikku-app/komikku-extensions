package eu.kanade.tachiyomi.extension.tr.liebeschneehiver

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.wpmangastream.WPMangaStream
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class LiebeSchneeHiver : WPMangaStream(
    "Liebe Schnee Hiver",
    "https://lshistoria.com",
    "tr",
    SimpleDateFormat("MMMM dd, yyyy", Locale.forLanguageTag("tr"))
) {
    private val rateLimitInterceptor = RateLimitInterceptor(4)

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addNetworkInterceptor(rateLimitInterceptor)
        .build()
}
