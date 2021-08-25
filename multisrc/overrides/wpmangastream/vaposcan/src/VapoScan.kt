package eu.kanade.tachiyomi.extension.pt.vaposcan

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.wpmangastream.WPMangaStream
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class VapoScan : WPMangaStream(
    "Vapo Scan",
    "https://vaposcans.com",
    "pt-BR",
    SimpleDateFormat("MMMMM dd, yyyy", Locale("pt", "BR"))
) {

    // Theme changed from Madara to WpMangaStream.
    override val versionId = 2

    override val client: OkHttpClient = super.client.newBuilder()
        .addInterceptor(RateLimitInterceptor(1, 2, TimeUnit.SECONDS))
        .build()

    override val altName: String = "Nome alternativo: "
}
