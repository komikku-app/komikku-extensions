package eu.kanade.tachiyomi.extension.all.ravensscans

import eu.kanade.tachiyomi.multisrc.readerfront.ReaderFront
import eu.kanade.tachiyomi.source.SourceFactory

class RavensScansFactory : SourceFactory {
    override fun createSources() = listOf(
        RavensScans("es", 1),
        RavensScans("en", 2)
    )

        class RavensScans(override val lang: String, override val langId: Int) :
        ReaderFront("Ravens Scans", "https://ravens-scans.com/", lang, langId) {
        override fun getImageCDN(path: String, width: Int) =
            "https://i${(0..2).random()}.wp.com/img-cdn1.ravens-scans.com" +
                "$path?strip=all&quality=100&w=$width"
    }
}
