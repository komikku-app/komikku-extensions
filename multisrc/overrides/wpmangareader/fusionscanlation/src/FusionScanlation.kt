package eu.kanade.tachiyomi.extension.es.fusionscanlation

import eu.kanade.tachiyomi.multisrc.wpmangareader.WPMangaReader
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class FusionScanlation : WPMangaReader("Fusion Scanlation", "https://fusionscanlation.com", "es", dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale("es"))) {

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .rateLimit(1, 2)
        .build()
}
