package eu.kanade.tachiyomi.extension.en.mangasee

import eu.kanade.tachiyomi.multisrc.nepnep.NepNep
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class MangaSee : NepNep("MangaSee", "https://mangasee123.com", "en") {

    override val id: Long = 9

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .rateLimit(1, 2)
        .connectTimeout(1, TimeUnit.MINUTES)
        .readTimeout(1, TimeUnit.MINUTES)
        .writeTimeout(1, TimeUnit.MINUTES)
        .build()
}
