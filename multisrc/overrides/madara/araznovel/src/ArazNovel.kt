package eu.kanade.tachiyomi.extension.tr.araznovel

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.FormBody
import okhttp3.Response
import java.text.SimpleDateFormat
import java.util.Locale

class ArazNovel : Madara("ArazNovel", "https://www.araznovel.com", "tr", SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())) {
    override fun formBuilder(page: Int, popular: Boolean): FormBody.Builder = super.formBuilder(page, popular)
        .add("vars[meta_query][0][0][value]", "manga")

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        val mangaId = document.select("div#manga-chapters-holder").attr("data-id")

        val xhrRequest = oldXhrChaptersRequest(mangaId)
        val xhrResponse = client.newCall(xhrRequest).execute()

        return xhrResponse.asJsoup().let { xhrDocument ->
            xhrDocument.select("li.parent").let { elements ->
                if (!elements.isNullOrEmpty()) {
                    elements.reversed()
                        .map { volumeElement -> volumeElement.select(chapterListSelector()).map { chapterFromElement(it) } }
                        .flatten()
                } else {
                    xhrDocument.select(chapterListSelector()).map { chapterFromElement(it) }
                }
            }
        }
    }
}
