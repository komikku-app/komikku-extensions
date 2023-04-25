package eu.kanade.tachiyomi.extension.all.atlantisscan

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.SourceFactory
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class AtlantisScanFactory : SourceFactory {
    override fun createSources() = listOf(
        AtlantisScanPortuguese(),
        AtlantisScanSpanish(),
    )
}

open class AtlantisScan(
    baseUrl: String,
    lang: String,
    dateFormat: SimpleDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US),
) : Madara("Atlantis Scan", baseUrl, lang, dateFormat)

class AtlantisScanSpanish : AtlantisScan("https://atlantisscan.com", "es") {

    // Name was not capitalized.
    override val id: Long = 2237642340381856331

    override val useNewChapterEndpoint = true
}

class AtlantisScanPortuguese : AtlantisScan("https://br.atlantisscan.com", "pt-BR") {

    override val client: OkHttpClient = super.client.newBuilder()
        .rateLimit(1, 2, TimeUnit.SECONDS)
        .build()
}
