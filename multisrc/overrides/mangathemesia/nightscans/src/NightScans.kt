package eu.kanade.tachiyomi.extension.en.nightscans

import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia
import okhttp3.Headers

class NightScans : MangaThemesia("NIGHT SCANS", "https://nightscans.org", "en", "/series") {
    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .add("Referer", baseUrl)
}
