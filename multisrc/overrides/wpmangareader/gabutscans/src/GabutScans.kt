package eu.kanade.tachiyomi.extension.id.gabutscans

import eu.kanade.tachiyomi.multisrc.wpmangareader.WPMangaReader
import eu.kanade.tachiyomi.source.model.Page
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale

class GabutScans : WPMangaReader(
    "Gabut Scans", "https://gabutscans.com", "id",
    dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale("id"))
) {

    override val hasProjectPage = true

    override fun searchMangaFromElement(element: Element) =
        super.searchMangaFromElement(element).apply {
            thumbnail_url = element.select("img").attr("abs:data-src")
        }

    override fun mangaDetailsParse(document: Document) = super.mangaDetailsParse(document).apply {
        thumbnail_url = document.select(seriesThumbnailSelector).attr("abs:data-src")
    }

    override fun pageListParse(document: Document): List<Page> {
        // Prefer using sources loaded from javascript
        // because current CDN (Statically) can't load old images.
        // Example: https://gabutscans.com/cinderella-wa-sagasanai-chapter-41-end-bahasa-indonesia/

        val docString = document.toString()
        val imageListRegex = Regex("""\"images.*?:.*?(\[.*?])""")
        val (imageListJson) = imageListRegex.find(docString)!!.destructured

        val imageList = json.parseToJsonElement(imageListJson).jsonArray

        return imageList.mapIndexed { i, jsonEl ->
            Page(i, "", jsonEl.jsonPrimitive.content)
        }
    }
}
