package eu.kanade.tachiyomi.extension.id.mgkomik

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class MGKomik : Madara("MG Komik", "https://mgkomik.com", "id", SimpleDateFormat("dd MMM yy", Locale.US)) {

    override val chapterUrlSuffix = ""
    override val mangaSubString = "komik"

    override fun searchMangaNextPageSelector() = "a.page.larger"

    override fun searchPage(page: Int): String = "halaman/$page/"
}
