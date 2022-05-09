package eu.kanade.tachiyomi.extension.id.mangkomik

import eu.kanade.tachiyomi.multisrc.wpmangareader.WPMangaReader

class MangKomik : WPMangaReader("MangKomik", "https://mangkomik.com", "id") {
    override val hasProjectPage = true
}
