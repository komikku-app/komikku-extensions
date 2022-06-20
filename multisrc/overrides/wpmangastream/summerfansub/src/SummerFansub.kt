package eu.kanade.tachiyomi.extension.pt.summerfansub

import eu.kanade.tachiyomi.multisrc.wpmangastream.WPMangaStream
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class SummerFansub : WPMangaStream(
    "Summer Fansub",
    "https://smmr.in",
    "pt-BR",
    dateFormat = SimpleDateFormat("MMMMM dd, yyyy", Locale("pt", "BR"))
) {

    override val client: OkHttpClient = super.client.newBuilder()
        .rateLimit(1, 2, TimeUnit.SECONDS)
        .build()
}
