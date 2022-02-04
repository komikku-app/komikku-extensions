package eu.kanade.tachiyomi.extension.en.madtheme

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.madtheme.MadTheme
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class MangaBuddy : MadTheme(
    "MangaBuddy",
    "https://mangabuddy.com",
    "en",
    SimpleDateFormat("MMM dd, yyy", Locale.US)
) {
    override val client: OkHttpClient = super.client.newBuilder()
        .addInterceptor(RateLimitInterceptor(1, 2, TimeUnit.SECONDS))
        .build()
}
