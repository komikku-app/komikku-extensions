package eu.kanade.tachiyomi.extension.pt.origamiorpheans

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.wpmangareader.WPMangaReader
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class OrigamiOrpheans : WPMangaReader(
    "Origami Orpheans",
    "https://origami-orpheans.com.br",
    "pt-BR",
    dateFormat = SimpleDateFormat("MMMMM dd, yyyy", Locale("pt", "BR"))
) {

    // Scanlator migrated from Madara to WpMangaReader.
    override val versionId = 2

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .addNetworkInterceptor(RateLimitInterceptor(1, 2, TimeUnit.SECONDS))
        .build()

    override val altName = "Nomes alternativos: "
}
