package eu.kanade.tachiyomi.extension.en.manhuafast

import eu.kanade.tachiyomi.multisrc.madara.Madara

class ManhuaFast : Madara("ManhuaFast", "https://manhuafast.com", "en") {

    // The website does not flag the content.
    override val filterNonMangaItems = false
}
