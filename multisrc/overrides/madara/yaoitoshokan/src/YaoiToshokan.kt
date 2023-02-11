package eu.kanade.tachiyomi.extension.pt.yaoitoshokan

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.Page
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.nodes.Document
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class YaoiToshokan : Madara(
    "Yaoi Toshokan",
    "https://www.yaoitoshokan.net",
    "pt-BR",
    SimpleDateFormat("dd MMM yyyy", Locale("pt", "BR")),
) {

    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .removeAll("User-Agent")

    override val client: OkHttpClient = network.client.newBuilder()
        .rateLimit(1, 2, TimeUnit.SECONDS)
        .build()

    // Page has custom link to scan website.
    override val popularMangaUrlSelector = "div.post-title a:not([target])"

    override fun pageListParse(document: Document): List<Page> {
        countViews(document)

        return document.select(pageListParseSelector)
            .mapIndexed { index, element ->
                // Had to add trim because of white space in source.
                val imageUrl = element.select("img").attr("data-src").trim()
                Page(index, "$baseUrl/", imageUrl)
            }
    }

    override fun imageRequest(page: Page): Request {
        val newHeaders = headersBuilder()
            .add("Accept", ACCEPT_IMAGE)
            .add("Accept-Language", ACCEPT_LANGUAGE)
            .set("Referer", page.url)
            .build()

        return GET(page.imageUrl!!, newHeaders)
    }

    companion object {
        private const val ACCEPT_IMAGE = "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8"
        private const val ACCEPT_LANGUAGE = "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7,es;q=0.6,gl;q=0.5"
    }
}
