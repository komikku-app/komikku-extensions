package eu.kanade.tachiyomi.extension.all.komiklab

import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia
import eu.kanade.tachiyomi.source.SourceFactory

class KomikLabFactory : SourceFactory {
    override fun createSources() = listOf(
        KomikLabEn(),
        KomikLabId(),
    )
}

class KomikLabEn : MangaThemesia("KomikLab Scans", "https://scans.komiklab.com", "en") {
    override val hasProjectPage = true
}

class KomikLabId : MangaThemesia("Komik Lab", "https://komiklab.com", "id") {
    override val hasProjectPage = true

    override val seriesDetailsSelector = ".seriestucon"
}
