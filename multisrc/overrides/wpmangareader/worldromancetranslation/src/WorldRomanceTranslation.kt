package eu.kanade.tachiyomi.extension.id.worldromancetranslation

import eu.kanade.tachiyomi.multisrc.wpmangareader.WPMangaReader
import okhttp3.Headers
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.Locale

class WorldRomanceTranslation : WPMangaReader("World Romance Translation", "https://wrt.my.id", "id", dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale("id"))) {
    override val projectPageString = "/project-wrt"

    override val hasProjectPage = true

    override fun headersBuilder(): Headers.Builder {
        return super.headersBuilder().add("Referer", baseUrl)
    }
}
