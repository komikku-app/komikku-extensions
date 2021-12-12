package eu.kanade.tachiyomi.extension.pt.geassscanlator

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.madara.Madara
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class GeassScanlator : Madara(
    "Geass Scanlator",
    "https://geassscan.net",
    "pt-BR",
    SimpleDateFormat("yyyy-MM-dd", Locale("pt", "BR"))
) {

    // Website changed from WpMangaStream to Madara (again).
    override val versionId: Int = 2

    override val client: OkHttpClient = super.client.newBuilder()
        .addInterceptor(RateLimitInterceptor(1, 2, TimeUnit.SECONDS))
        .build()

    override val useNewChapterEndpoint: Boolean = true

    override val altName: String = "Nome alternativo: "

    // Tags are full of garbage, so remove them.
    override val mangaDetailsSelectorTag: String = ""

    override fun popularMangaSelector() = "div.page-item-detail.manga"
}
