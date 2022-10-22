package eu.kanade.tachiyomi.extension.ar.mangaproz

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class MangaPro : Madara("Manga Pro", "https://manga-pro.com", "ar") {
    // Theme changed from MangaThemesia to Madara.
    override val versionId = 2

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .rateLimit(4)
        .build()
}
