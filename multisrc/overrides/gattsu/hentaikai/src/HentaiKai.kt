package eu.kanade.tachiyomi.extension.pt.hentaikai

import eu.kanade.tachiyomi.lib.ratelimit.RateLimitInterceptor
import eu.kanade.tachiyomi.multisrc.gattsu.Gattsu
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.SManga
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.nodes.Element
import java.util.concurrent.TimeUnit

class HentaiKai : Gattsu(
    "Hentai Kai",
    "https://hentaikai.com",
    "pt-BR"
) {

    override val client: OkHttpClient = super.client.newBuilder()
        .addInterceptor(RateLimitInterceptor(1, 2, TimeUnit.SECONDS))
        .build()

    override fun latestUpdatesRequest(page: Int): Request {
        val pagePath = if (page > 1) "page/$page" else ""

        return GET("$baseUrl/mangas-e-hqs-recentes/$pagePath", headers)
    }

    override fun latestUpdatesSelector() =
        "div.meio div#fotos div.listaFotoConteudo > a[href^=$baseUrl]:not([title*=Episódio]):first-of-type"

    override fun latestUpdatesFromElement(element: Element): SManga = SManga.create().apply {
        title = element.attr("title")
        thumbnail_url =
            element.select("span.listaFotoThumb img.wp-post-image").first()!!.attr("src")
        setUrlWithoutDomain(element.attr("href"))
    }

    override fun latestUpdatesNextPageSelector() = "div.meio div#fotos ul.paginacao li.next > a"

    override fun searchMangaSelector() = "div.meio div.lista div.listaFotoConteudo > a[href^=$baseUrl]:not([title*=Episódio]):first-of-type"
}
