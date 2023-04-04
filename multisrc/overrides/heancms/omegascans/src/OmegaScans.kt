package eu.kanade.tachiyomi.extension.en.omegascans

import eu.kanade.tachiyomi.multisrc.heancms.HeanCms
import eu.kanade.tachiyomi.network.interceptor.rateLimitHost
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SManga
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Response

class OmegaScans : HeanCms("Omega Scans", "https://omegascans.org", "en") {
    override val client: OkHttpClient = super.client.newBuilder()
        .rateLimitHost(apiUrl.toHttpUrl(), 1)
        .build()

    // Site changed from Mangathemesia to HeanCms.
    override val versionId = 2
    override val fetchAllTitles = false
    override val coverPath = ""

    override fun popularMangaParse(response: Response): MangasPage {
        return super.popularMangaParse(response).apply {
            this.mangas.forEach {
                it.thumbnail_url = it.thumbnail_url?.substringAfter("$apiUrl/")
            }
        }
    }

    override fun searchMangaParse(response: Response): MangasPage {
        return super.searchMangaParse(response).apply {
            this.mangas.forEach {
                it.thumbnail_url = it.thumbnail_url?.substringAfter("$apiUrl/")
            }
        }
    }

    override fun mangaDetailsParse(response: Response): SManga {
        return super.mangaDetailsParse(response).apply {
            thumbnail_url = thumbnail_url?.substringAfter("$apiUrl/")
        }
    }
}
