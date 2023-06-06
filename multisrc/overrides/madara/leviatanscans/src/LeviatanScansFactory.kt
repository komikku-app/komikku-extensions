package eu.kanade.tachiyomi.extension.all.leviatanscans

import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.Source
import eu.kanade.tachiyomi.source.SourceFactory
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.Locale

class LeviatanScansFactory : SourceFactory {
    override fun createSources(): List<Source> = listOf(
        LeviatanScansEN(),
        LeviatanScansES(),
    )
}

class LeviatanScansEN : LeviatanScans(
    "https://en.leviatanscans.com",
    "en",
    SimpleDateFormat("MMM dd, yyyy", Locale.US),
) {
    override val client: OkHttpClient = super.client.newBuilder()
        .rateLimit(1, 2)
        .build()

    override val mangaDetailsSelectorDescription = "div.manga-summary"
    override val mangaDetailsSelectorAuthor = "div.manga-authors"
}

class LeviatanScansES : LeviatanScans(
    "https://es.leviatanscans.com",
    "es",
    SimpleDateFormat("MMM dd, yy", Locale("es")),
) {
    override val mangaDetailsSelectorStatus = ".post-content_item:contains(Status) .summary-content"
}
