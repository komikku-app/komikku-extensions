package eu.kanade.tachiyomi.extension.en.firstkissmanga

import eu.kanade.tachiyomi.multisrc.madara.Madara
import okhttp3.Headers

class FirstKissManga : Madara("1st Kiss", "https://1stkissmanga.com", "en") {
    override fun headersBuilder(): Headers.Builder = super.headersBuilder().add("Referer", baseUrl)
}
