package eu.kanade.tachiyomi.extension.ja.rawlh

import android.util.Base64
import eu.kanade.tachiyomi.multisrc.fmreader.FMReader
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.Page
import okhttp3.Request
import org.jsoup.nodes.Attribute
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.nio.charset.Charset

class WeLoveManga : FMReader("WeLoveManga", "https://weloma.art", "ja") {
    // Formerly "RawLH"
    override val id = 7595224096258102519

    override val chapterUrlSelector = ""
    override fun pageListParse(document: Document): List<Page> {
        fun Element.decoded(): String {
            val attr = this.attributes().map(Attribute::key).maxByOrNull(kotlin.String::length) ?: "src"
            return if (!this.attr(attr).contains(".")) {
                Base64.decode(this.attr(attr), Base64.DEFAULT).toString(Charset.defaultCharset())
            } else {
                this.attr("abs:$attr")
            }
        }
        return document.select(pageListImageSelector).mapIndexed { i, img ->
            Page(i, document.location(), img.decoded())
        }
    }

    // Referer needs to be chapter URL
    override fun imageRequest(page: Page): Request = GET(page.imageUrl!!, headersBuilder().set("Referer", page.url).build())
}
