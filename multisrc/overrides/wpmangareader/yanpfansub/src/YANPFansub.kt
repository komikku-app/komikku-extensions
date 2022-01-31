package eu.kanade.tachiyomi.extension.pt.yanpfansub

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.wpmangareader.WPMangaReader
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class YANPFansub : WPMangaReader(
    "YANP Fansub",
    "https://melhorcasal.com",
    "pt-BR",
    dateFormat = SimpleDateFormat("MMMMM dd, yyyy", Locale("pt", "BR"))
) {

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .addNetworkInterceptor(RateLimitInterceptor(1, 2, TimeUnit.SECONDS))
        .build()

    override val altName = "Nome alternativo: "
}
