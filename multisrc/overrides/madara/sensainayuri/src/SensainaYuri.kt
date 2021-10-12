package eu.kanade.tachiyomi.extension.pt.sensainayuri

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.madara.Madara
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class SensainaYuri : Madara(
    "Sensaina Yuri",
    "https://sensainayuri.dropescan.com",
    "pt-BR",
    SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
) {

    override val client: OkHttpClient = super.client.newBuilder()
        .addInterceptor(RateLimitInterceptor(1, 2, TimeUnit.SECONDS))
        .build()

    override val altName: String = "Nome alternativo: "

    override val useNewChapterEndpoint = true

    override fun popularMangaSelector() = "div.page-item-detail.manga"
}
