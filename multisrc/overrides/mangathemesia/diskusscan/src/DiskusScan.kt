package eu.kanade.tachiyomi.extension.pt.diskusscan

import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class DiskusScan : MangaThemesia(
    "Diskus Scan",
    "https://diskusscan.com",
    "pt-BR",
    dateFormat = SimpleDateFormat("MMMMM dd, yyyy", Locale("pt", "BR")),
) {

    // Changed their theme from Madara to MangaThemesia.
    override val versionId = 2

    override val client: OkHttpClient = super.client.newBuilder()
        .rateLimit(1, 2, TimeUnit.SECONDS)
        .build()
}
