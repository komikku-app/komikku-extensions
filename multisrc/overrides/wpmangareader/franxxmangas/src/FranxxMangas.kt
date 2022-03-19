package eu.kanade.tachiyomi.extension.pt.franxxmangas

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.wpmangareader.WPMangaReader
import eu.kanade.tachiyomi.source.model.SManga
import okhttp3.OkHttpClient
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class FranxxMangas : WPMangaReader(
    "Franxx Mangás",
    "https://franxxmangas.net",
    "pt-BR",
    dateFormat = SimpleDateFormat("MMMMM dd, yyyy", Locale("pt", "BR"))
) {

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .addNetworkInterceptor(RateLimitInterceptor(1, 2, TimeUnit.SECONDS))
        .build()

    override val altName = "Nomes alternativos: "

    override fun searchMangaFromElement(element: Element) = SManga.create().apply {
        thumbnail_url = element.select("img").attr("abs:data-src")
        title = element.select("a").attr("title")
        setUrlWithoutDomain(element.select("a").attr("href"))
    }
}
