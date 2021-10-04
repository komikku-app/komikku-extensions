package eu.kanade.tachiyomi.extension.pt.silencescan

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.wpmangareader.WPMangaReader
import eu.kanade.tachiyomi.source.model.SManga
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class SilenceScan : WPMangaReader(
    "Silence Scan",
    "https://silencescan.com.br",
    "pt-BR",
    dateFormat = SimpleDateFormat("MMMMM dd, yyyy", Locale("pt", "BR"))
) {

    override val versionId: Int = 2

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .addNetworkInterceptor(RateLimitInterceptor(1, 2, TimeUnit.SECONDS))
        .build()

    override val altName = "Nome alternativo: "

    override fun parseStatus(status: String) = when {
        status.contains("Em Andamento") -> SManga.ONGOING
        status.contains("Completo") -> SManga.COMPLETED
        else -> SManga.UNKNOWN
    }
}
