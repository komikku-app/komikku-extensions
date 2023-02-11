package eu.kanade.tachiyomi.extension.pt.gekkouscan

import eu.kanade.tachiyomi.multisrc.mmrcms.MMRCMS
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.TimeUnit

class GekkouScans : MMRCMS(
    "Gekkou Scans",
    "https://gekkou.com.br",
    "pt-BR",
) {

    override val client: OkHttpClient = super.client.newBuilder()
        .rateLimit(1, 2, TimeUnit.SECONDS)
        .build()

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        val chapters = document.select(chapterListSelector())
            .mapNotNull(::nullableChapterFromElement)

        val isExternal = document
            .select("ul.domaintld > li.li:has(a[href*='hentai.gekkouscans.com.br'])")
            .firstOrNull() != null

        if (chapters.isEmpty() && isExternal) {
            throw Exception(EXTERNAL_SERIES_ERROR)
        }

        return chapters
    }

    override fun chapterListSelector() = "ul.domaintld > li.li:has(a[href^='$baseUrl'])"

    override fun imageRequest(page: Page): Request {
        val newHeaders = headersBuilder()
            .add("Accept", ACCEPT_IMAGE)
            .add("Referer", page.url)
            .build()

        return GET(page.imageUrl!!, newHeaders)
    }

    companion object {
        private const val ACCEPT_IMAGE = "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8"

        private const val EXTERNAL_SERIES_ERROR =
            "Migre esta série para a extensão Gekkou Hentai para continuar lendo."
    }
}
