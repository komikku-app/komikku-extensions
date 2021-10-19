package eu.kanade.tachiyomi.extension.all.erocool

import eu.kanade.tachiyomi.source.SourceFactory

class EroCoolFactory : SourceFactory {
    override fun createSources() = listOf(
        EroCool("en", "english", 1),
        EroCool("ja", "japanese", 2),
        EroCool("zh", "chinese", 3),
    )
}
