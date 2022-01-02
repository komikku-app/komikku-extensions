package eu.kanade.tachiyomi.extension.pt.geasshentai

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.wpmangastream.WPMangaStream
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class GeassHentai : WPMangaStream(
    "Geass Hentai",
    "https://geassscan.xyz",
    "pt-BR",
    dateFormat = SimpleDateFormat("MMMMM d, yyyy", Locale("pt", "BR"))
) {

    // Source changed from Madara to WpMangaStream.
    override val versionId: Int = 2

    override val client: OkHttpClient = super.client.newBuilder()
        .addInterceptor(RateLimitInterceptor(1, 2, TimeUnit.SECONDS))
        .build()

    override val altName: String = "Nome alternativo: "
}
