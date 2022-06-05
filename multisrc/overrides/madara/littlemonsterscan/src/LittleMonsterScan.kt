package eu.kanade.tachiyomi.extension.pt.littlemonsterscan

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class LittleMonsterScan : Madara(
    "Little Monster Scan",
    "https://littlemonsterscan.com.br",
    "pt-BR",
    SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale("pt", "BR"))
) {

    override val client: OkHttpClient = super.client.newBuilder()
        .rateLimit(1, 2, TimeUnit.SECONDS)
        .build()

    override val altName: String = "Nome alternativo: "

    override val useNewChapterEndpoint = true

    override fun popularMangaSelector() = "div.page-item-detail.manga"
}
