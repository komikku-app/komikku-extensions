package eu.kanade.tachiyomi.extension.all.flamescans

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.source.SourceFactory
import eu.kanade.tachiyomi.source.model.SManga
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class FlameScansFactory : SourceFactory {
    override fun createSources() = listOf(
        FlameScansAr(),
        FlameScansEn()
    )
}

class FlameScansAr : FlameScans("https://ar.flamescans.org", "ar", "/series") {
    override val client: OkHttpClient = super.client.newBuilder()
        .addInterceptor(RateLimitInterceptor(1, 1, TimeUnit.SECONDS))
        .build()

    override val id: Long = 6053688312544266540

    override fun parseStatus(status: String) = when {
        status.contains("مستمر") -> SManga.ONGOING
        status.contains("مكتمل") -> SManga.COMPLETED
        else -> SManga.UNKNOWN
    }
}

class FlameScansEn : FlameScans("https://flamescans.org", "en", "/series") {
    override val client: OkHttpClient = super.client.newBuilder()
        .addInterceptor(RateLimitInterceptor(1, 3, TimeUnit.SECONDS))
        .build()
}
