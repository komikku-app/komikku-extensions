package eu.kanade.tachiyomi.extension.en.mangahero

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.source.model.SChapter
import okhttp3.Response

class MangaHero : Madara("Manga Hero", "https://mangahero.xyz", "en") {

    override fun chapterListParse(response: Response): List<SChapter> = super.chapterListParse(response).reversed()
}
