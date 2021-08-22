package eu.kanade.tachiyomi.extension.en.fizmanga

import eu.kanade.tachiyomi.multisrc.madara.Madara
import okhttp3.Headers

class FizManga : Madara("Fiz Manga", "https://fizmanga.com", "en") {
    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .add("Referer", baseUrl)
}
