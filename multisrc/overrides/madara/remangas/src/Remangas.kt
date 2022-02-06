package eu.kanade.tachiyomi.extension.pt.remangas

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.madara.Madara
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class Remangas : Madara(
    "Remangas",
    "https://remangas.net",
    "pt-BR",
    SimpleDateFormat("dd 'de' MMM 'de' yyy", Locale("pt", "BR"))
) {
    override val versionId = 2

    override val useNewChapterEndpoint = true

    override val client: OkHttpClient = super.client.newBuilder()
        .addInterceptor(RateLimitInterceptor(1, 2, TimeUnit.SECONDS))
        .build()
}
