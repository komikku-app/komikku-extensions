package eu.kanade.tachiyomi.extension.en.asurascansus

import eu.kanade.tachiyomi.multisrc.madara.Madara

class AsuraScansUs : Madara("Asura Scans.us", "https://asurascans.us", "en") {
    // Redirected from: https://mangagoyaoi.com
    override val useNewChapterEndpoint = true

    override fun searchPage(page: Int): String = if (page == 1) "" else "page/$page/"
}
