package eu.kanade.tachiyomi.extension.en.realmscans

import eu.kanade.tachiyomi.multisrc.mangathemesia.MangaThemesia
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.source.model.Page
import okhttp3.OkHttpClient
import org.jsoup.nodes.Document
import java.util.concurrent.TimeUnit

class RealmScans : MangaThemesia(
    "Realm Scans",
    "https://realmscans.com",
    "en",
    "/series"
) {

    override val client: OkHttpClient = super.client.newBuilder()
        .rateLimit(1, 1, TimeUnit.SECONDS)
        .build()

    override fun pageListParse(document: Document): List<Page> {
        return super.pageListParse(document)
            .distinctBy { it.imageUrl }
            .mapIndexed { i, page -> Page(i, imageUrl = page.imageUrl) }
    }
}
