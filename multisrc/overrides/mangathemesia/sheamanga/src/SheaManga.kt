package eu.kanade.tachiyomi.extension.id.sheamanga

import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import okhttp3.Dns
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class SheaManga : MangaThemesia(
    "Shea Manga",
    "https://sheakomik.com",
    "id",
    dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.forLanguageTag("id")),
) {

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .rateLimit(4)
        .dns(Dns.SYSTEM)
        .build()

    override val hasProjectPage = true
}
