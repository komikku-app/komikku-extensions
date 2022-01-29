package eu.kanade.tachiyomi.extension.all.sevenkingscanlation

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory
import java.text.SimpleDateFormat
import java.util.Locale

class SevenKingScanlationFactory : SourceFactory {
    override fun createSources(): List<Source> = listOf(
        SevenKingScanlationEN(),
        SevenKingScanlationES(),
    )
}

class SevenKingScanlationEN : Madara("Seven King Scanlation", "https://sksubs.net", "en", SimpleDateFormat("MMMMM dd, yyyy", Locale("es"))) {
    override fun searchMangaSelector(): String {
        return "${super.searchMangaSelector()}:contains(English)"
    }
    override fun popularMangaSelector(): String {
        return "${super.popularMangaSelector()}:contains(English)"
    }
}

class SevenKingScanlationES : Madara("Seven King Scanlation", "https://sksubs.net", "es", SimpleDateFormat("MMMMM dd, yyyy", Locale("es"))) {
    override fun searchMangaSelector(): String {
        return "${super.searchMangaSelector()}:not(:contains(English))"
    }
    override fun popularMangaSelector(): String {
        return "${super.popularMangaSelector()}:not(:contains(English))"
    }
}
