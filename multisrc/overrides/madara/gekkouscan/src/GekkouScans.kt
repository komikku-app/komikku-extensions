package eu.kanade.tachiyomi.extension.pt.gekkouscan

import eu.kanade.tachiyomi.annotations.Nsfw
import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.madara.Madara
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

@Nsfw
class GekkouScans : Madara(
    "Gekkou Scans",
    "https://gekkouscans.com.br",
    "pt-BR",
    SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
) {

    // Theme changed from MMRCMS to Madara.
    override val versionId: Int = 2

    override val client: OkHttpClient = super.client.newBuilder()
        .connectTimeout(1, TimeUnit.MINUTES)
        .readTimeout(1, TimeUnit.MINUTES)
        .writeTimeout(1, TimeUnit.MINUTES)
        .addInterceptor(RateLimitInterceptor(1, 2, TimeUnit.SECONDS))
        .build()

    override val altName: String = "Nome alternativo: "

    override val useNewChapterEndpoint: Boolean = true

    override fun popularMangaSelector() = "div.page-item-detail.manga"
}
