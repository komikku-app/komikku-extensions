package eu.kanade.tachiyomi.extension.tr.tempestmanga

import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class TempestManga : MangaThemesia(
    "Tempest Manga", "https://manga.tempestfansub.com", "tr",
    dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale("tr"))
) {

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .rateLimit(4)
        .build()
}
