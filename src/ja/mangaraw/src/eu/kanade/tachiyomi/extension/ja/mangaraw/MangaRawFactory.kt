package eu.kanade.tachiyomi.extension.ja.mangaraw

import eu.kanade.tachiyomi.extension.ja.mangaraw.sources.Comick
import eu.kanade.tachiyomi.extension.ja.mangaraw.sources.MangaPro
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory

class MangaRawFactory : SourceFactory {
    override fun createSources(): List<Source> = listOf(
        Comick(),
        MangaPro()
    )
}
