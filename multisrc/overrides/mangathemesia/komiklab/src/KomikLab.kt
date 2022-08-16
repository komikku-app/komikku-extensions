package eu.kanade.tachiyomi.extension.id.komiklab

import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia

class KomikLab : MangaThemesia("Komik Lab", "https://komiklab.com", "id") {
    override val hasProjectPage = true

    override val seriesDetailsSelector = ".seriestucon"
}
