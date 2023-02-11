package eu.kanade.tachiyomi.extension.es.chunchunmarutranslations

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.SChapter
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Element
import java.text.SimpleDateFormat
import java.util.Locale

class ChunchunmaruTranslations : Madara(
    "Chunchunmaru Translations",
    "https://chunchunmarutl.com",
    "es",
    SimpleDateFormat("dd 'de' MMMMM 'de' yyyy", Locale("es")),
) {

    override fun popularMangaRequest(page: Int): Request = GET("$baseUrl/manga/page/$page/?m_orderby=views", headers)
    override fun latestUpdatesRequest(page: Int): Request = GET("$baseUrl/manga/page/$page/?m_orderby=latest", headers)

    override fun chapterFromElement(element: Element): SChapter {
        return super.chapterFromElement(element).apply {
            chapter_number = name.split(" ").getOrNull(1)?.toFloatOrNull() ?: -1f
        }
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        // Sorting list because they are in packs
        return super.chapterListParse(response).sortedBy {
            it.chapter_number
        }
    }
}
