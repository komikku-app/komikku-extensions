package eu.kanade.tachiyomi.extension.ar.mangaswat

import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.Page
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import okhttp3.Headers
import okhttp3.OkHttpClient
import org.jsoup.nodes.Document
import java.text.SimpleDateFormat
import java.util.Locale

class MangaSwat : MangaThemesia(
    "MangaSwat",
    "https://swatmanga.net",
    "ar",
    dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US),
) {
    override val client: OkHttpClient = super.client.newBuilder()
        .rateLimit(1)
        .build()

    override fun headersBuilder(): Headers.Builder = super.headersBuilder()
        .add("Referer", "$baseUrl/")

    override val seriesArtistSelector = "span:contains(الناشر) i"
    override val seriesAuthorSelector = "span:contains(المؤلف) i"
    override val seriesGenreSelector = "span:contains(التصنيف) a, .mgen a"
    override val seriesTypeSelector = "span:contains(النوع) a"
    override val seriesStatusSelector = "span:contains(الحالة)"

    override fun pageListParse(document: Document): List<Page> {
        val scriptContent = document.selectFirst("script:containsData(ts_reader)")!!.data()
        val jsonString = scriptContent.substringAfter("ts_reader.run(").substringBefore(");")
        val tsReader = json.decodeFromString<TSReader>(jsonString)
        val imageUrls = tsReader.sources.firstOrNull()?.images ?: return emptyList()
        return imageUrls.mapIndexed { index, imageUrl -> Page(index, imageUrl = imageUrl) }
    }

    @Serializable
    data class TSReader(
        val sources: List<ReaderImageSource>,
    )

    @Serializable
    data class ReaderImageSource(
        val source: String,
        val images: List<String>,
    )
}
