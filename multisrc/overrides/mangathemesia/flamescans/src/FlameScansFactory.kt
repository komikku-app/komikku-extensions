package eu.kanade.tachiyomi.extension.all.flamescans

import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.SourceFactory
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class FlameScansFactory : SourceFactory {
    override fun createSources() = listOf(
        FlameScansEn(),
    )
}

class FlameScansEn : FlameScans("https://flamescans.org", "en", "/series") {
    override val client: OkHttpClient = super.client.newBuilder()
        .rateLimit(2, 7, TimeUnit.SECONDS)
        .build()
}
