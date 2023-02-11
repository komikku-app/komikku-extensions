package eu.kanade.tachiyomi.extension.all.olympusscanlation

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.SourceFactory
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class OlympusScanlationFactory : SourceFactory {
    override fun createSources() = listOf(
        OlympusScanlationBr(),
        OlympusScanlationEs(),
    )
}

abstract class OlympusScanlation(
    override val baseUrl: String,
    lang: String,
    dateFormat: SimpleDateFormat = SimpleDateFormat("MMMMM dd, yyyy", Locale.US),
) : Madara("Olympus Scanlation", baseUrl, lang, dateFormat)

class OlympusScanlationEs : OlympusScanlation("https://olympusscanlation.com", "es")

class OlympusScanlationBr : OlympusScanlation(
    "https://br.olympusscanlation.com",
    "pt-BR",
    SimpleDateFormat("MMMMM dd, yyyy", Locale("pt", "BR")),
) {

    override val client: OkHttpClient = super.client.newBuilder()
        .rateLimit(1, 2, TimeUnit.SECONDS)
        .build()
}
