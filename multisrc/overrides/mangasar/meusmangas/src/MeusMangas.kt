package eu.kanade.tachiyomi.extension.pt.meusmangas

import eu.kanade.tachiyomi.annotations.Nsfw
import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.mangasar.MangaSar
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.OkHttpClient
import okhttp3.Response
import org.jsoup.nodes.Element
import java.util.concurrent.TimeUnit

@Nsfw
class MeusMangas : MangaSar(
    "Meus Mangás",
    "https://meusmangas.net",
    "pt-BR"
) {

    override val client: OkHttpClient = super.client.newBuilder()
        .addInterceptor(::searchIntercept)
        .addInterceptor(RateLimitInterceptor(1, 2, TimeUnit.SECONDS))
        .build()

    override fun popularMangaSelector() = "ul.sidebar-popular li.popular-treending"

    override fun popularMangaFromElement(element: Element): SManga = SManga.create().apply {
        title = element.selectFirst("h4.title").text()
        thumbnail_url = element.selectFirst("div.tumbl img").attr("src")
        setUrlWithoutDomain(element.selectFirst("a").attr("abs:href"))
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val document = response.asJsoup()
        val infoElement = document.selectFirst("div.box-single:has(div.mangapage)")

        return SManga.create().apply {
            title = infoElement.selectFirst("h1.kw-title").text()
            author = infoElement.selectFirst("div.mdq.author").text().trim()
            description = infoElement.selectFirst("div.sinopse-page").text()
            genre = infoElement.select("div.touchcarousel a.widget-btn").joinToString { it.text() }
            status = infoElement.selectFirst("span.mdq").text().toStatus()
            thumbnail_url = infoElement.selectFirst("div.thumb img").attr("abs:src")
        }
    }

    private fun String.toStatus(): Int = when (this) {
        "Em andamento" -> SManga.ONGOING
        "Completo" -> SManga.COMPLETED
        else -> SManga.UNKNOWN
    }
}
