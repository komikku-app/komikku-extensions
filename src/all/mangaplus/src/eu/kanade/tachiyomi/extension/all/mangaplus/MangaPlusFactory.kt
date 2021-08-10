package eu.kanade.tachiyomi.extension.all.mangaplus

import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory

class MangaPlusFactory : SourceFactory {
    override fun createSources(): List<Source> = listOf(
        MangaPlusEnglish(),
        MangaPlusIndonesian(),
        MangaPlusPortuguese(),
        MangaPlusRussian(),
        MangaPlusSpanish(),
        MangaPlusThai()
    )
}

class MangaPlusEnglish : MangaPlus("en", "eng", Language.ENGLISH)
class MangaPlusIndonesian : MangaPlus("id", "ind", Language.INDONESIAN)
class MangaPlusPortuguese : MangaPlus("pt-BR", "ptb", Language.PORTUGUESE_BR)
class MangaPlusRussian : MangaPlus("ru", "rus", Language.RUSSIAN)
class MangaPlusSpanish : MangaPlus("es", "esp", Language.SPANISH)
class MangaPlusThai : MangaPlus("th", "tha", Language.THAI)
