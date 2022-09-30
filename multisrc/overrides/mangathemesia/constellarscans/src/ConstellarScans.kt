package eu.kanade.tachiyomi.extension.en.constellarscans

import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia

class ConstellarScans : MangaThemesia("Constellar Scans", "https://constellarscans.com", "en") {
    override val seriesStatusSelector = ".status"
}
