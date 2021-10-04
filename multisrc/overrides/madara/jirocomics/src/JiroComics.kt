package eu.kanade.tachiyomi.extension.en.jirocomics

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.source.model.SChapter
import okhttp3.Response

class JiroComics : Madara("Jiro Comics", "https://jirocomics.com", "en") {
    override fun chapterListParse(response: Response): List<SChapter> = super.chapterListParse(response).reversed()
}
