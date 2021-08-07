package eu.kanade.tachiyomi.extension.ja.rawlh

import eu.kanade.tachiyomi.multisrc.fmreader.FMReader
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.Page
import okhttp3.Request
import org.jsoup.nodes.Document

class WeLoveManga : FMReader("WeLoveManga", "https://weloma.net", "ja") {
    // Formerly "RawLH"
    override val id = 7595224096258102519

    override val chapterUrlSelector = ""
    override fun pageListParse(document: Document): List<Page> = base64PageListParse(document)
    // Referer needs to be chapter URL
    override fun imageRequest(page: Page): Request = GET(page.imageUrl!!, headersBuilder().set("Referer", page.url).build())
}
