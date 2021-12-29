package eu.kanade.tachiyomi.extension.es.mangafenix

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.madara.Madara
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.Locale

class MangaFenix : Madara(
    "Manga Fenix",
    "https://manga-fenix.com",
    "es",
    SimpleDateFormat("dd MMMM, yyyy", Locale("es"))
) {

    override val client: OkHttpClient = super.client.newBuilder()
        .addInterceptor(RateLimitInterceptor(1))
        .build()
}
