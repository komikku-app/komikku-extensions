package eu.kanade.tachiyomi.extension.pt.geassscanlator

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.madara.Madara
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class OkamishiScans : Madara(
    "Okamishi Scans",
    "https://geassscan.net",
    "pt-BR",
    SimpleDateFormat("yyyy-MM-dd", Locale("pt", "BR"))
) {

    // The scanlator changed their name.
    override val id: Long = 1228448816486487111

    override val client: OkHttpClient = super.client.newBuilder()
        .addInterceptor(RateLimitInterceptor(1, 2, TimeUnit.SECONDS))
        .build()

    override val useNewChapterEndpoint: Boolean = true

    override val altName: String = "Nome alternativo: "
    
    override val mangaDetailsSelectorTitle: String = "div.post-title h1"

    // Tags are full of garbage, so remove them.
    override val mangaDetailsSelectorTag: String = ""

    override fun popularMangaSelector() = "div.page-item-detail.manga"
}
