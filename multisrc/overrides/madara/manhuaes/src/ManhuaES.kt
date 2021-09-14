package eu.kanade.tachiyomi.extension.en.manhuaes

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Response

class ManhuaES : Madara("Manhua ES", "https://manhuaes.com", "en") {
    override val pageListParseSelector = ".reading-content div.text-left :has(>img)"

    override fun chapterListParse(response: Response): List<SChapter> {
        var chapterList = super.chapterListParse(response)

        // the site adds a ghost chapter for some entries
        chapterList.firstOrNull()?.let {
            if (!isReleasedChapter(it)) {
                chapterList = chapterList.subList(1, chapterList.size)
            }
        }

        return chapterList
    }

    private fun isReleasedChapter(chapter: SChapter): Boolean {
        val document = client.newCall(
            GET(chapter.url, headersBuilder().build())
        ).execute().asJsoup()

        return document.select(pageListParseSelector).isNotEmpty()
    }
}
