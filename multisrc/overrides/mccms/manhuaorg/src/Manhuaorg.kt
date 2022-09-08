package eu.kanade.tachiyomi.extension.zh.manhuaorg

import eu.kanade.tachiyomi.multisrc.mccms.MCCMS
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SManga
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request

class Manhuaorg : MCCMS("朴朴漫画", "https://app.manhuaorg.com", hasCategoryPage = false) {

    override fun mangaDetailsRequest(manga: SManga) = throw UnsupportedOperationException()

    override fun imageRequest(page: Page): Request {
        val url = page.imageUrl!!
        val host = url.toHttpUrl().host
        val headers = headers.newBuilder().set("Referer", "https://$host/").build()
        return GET(url, headers)
    }
}
