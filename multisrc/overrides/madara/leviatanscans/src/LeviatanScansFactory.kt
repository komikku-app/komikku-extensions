package eu.kanade.tachiyomi.extension.all.leviatanscans

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory
import eu.kanade.tachiyomi.source.model.SChapter
import okhttp3.Response

class LeviatanScansFactory : SourceFactory {
    override fun createSources(): List<Source> = listOf(
        LeviatanScansEN(),
        LeviatanScansES(),
    )
}
class LeviatanScansEN : Madara("Leviatan Scans", "https://leviatanscans.com", "en")
class LeviatanScansES : Madara("Leviatan Scans", "https://es.leviatanscans.com", "es")
