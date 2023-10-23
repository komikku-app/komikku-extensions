package eu.kanade.tachiyomi.extension.id.mgkomik

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import okhttp3.Headers
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class MGKomik : Madara("MG Komik", "https://mgkomik.id", "id", SimpleDateFormat("dd MMM yy", Locale.US)) {

    override val client: OkHttpClient = super.client.newBuilder()
        .rateLimit(20, 5, TimeUnit.SECONDS)
        .build()

    override val chapterUrlSuffix = ""

    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .add("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
        .add("Accept-Language", "en-US,en;q=0.9,id;q=0.8")
        .add("Referer", "$baseUrl/")
        .add("Sec-Fetch-Dest", "document")
        .add("Sec-Fetch-Mode", "navigate")
        .add("Sec-Fetch-Site", "same-origin")
        .add("Upgrade-Insecure-Requests", "1")
        .add("X-Requested-With", someBrowserName.random())

    private val someBrowserName = arrayOf(
        "org.mozilla.firefox",
        "com.apple.safari",
        "org.chromium.chrome",
    )

    override val mangaSubString = "komik"

    override fun searchMangaNextPageSelector() = "a.page.larger"
}
