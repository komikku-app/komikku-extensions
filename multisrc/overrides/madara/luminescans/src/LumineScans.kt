package eu.kanade.tachiyomi.extension.en.luminescans

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.source.model.SChapter
import okhttp3.Response

class LumineScans : Madara("Lumine Scans", "https://luminescans.xyz/", "en") {
  
    override fun chapterListParse(response: Response): List<SChapter> = super.chapterListParse(response).reversed()
}
