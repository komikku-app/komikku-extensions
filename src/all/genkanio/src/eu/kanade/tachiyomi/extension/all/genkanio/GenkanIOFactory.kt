package eu.kanade.tachiyomi.extension.all.genkanio

import eu.kanade.tachiyomi.source.SourceFactory

class GenkanIOFactory : SourceFactory {
    override fun createSources() = listOf(
        GenkanIO("all"),
        GenkanIO("ar"),
        GenkanIO("en"),
        GenkanIO("fr"),
        GenkanIO("pl"),
        GenkanIO("pt-BR"),
        GenkanIO("ru"),
        GenkanIO("es"),
        GenkanIO("tr"),
    )
}
