package eu.kanade.tachiyomi.extension.pt.owscan

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.madara.Madara
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class OwScan : Madara("Ow Scan", "https://owscan.com", "pt-BR") {

    override val client: OkHttpClient = super.client.newBuilder()
        .addInterceptor(RateLimitInterceptor(1, 2, TimeUnit.SECONDS))
        .build()

    override val altName = "Nome alternativo: "

    override fun popularMangaSelector() = "div.page-item-detail.manga"
}
