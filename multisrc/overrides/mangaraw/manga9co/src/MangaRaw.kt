package eu.kanade.tachiyomi.extension.ja.manga9co

import android.app.Application
import androidx.preference.ListPreference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.multisrc.mangaraw.MangaRawTheme
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.FilterList
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Evaluator
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MangaRaw : MangaRawTheme("MangaRaw", ""), ConfigurableSource {
    // See https://github.com/tachiyomiorg/tachiyomi-extensions/commits/master/src/ja/mangaraw
    override val versionId = 2
    override val id = 4572869149806246133

    override val supportsLatest = true
    override val baseUrl: String
    private val selectors: Selectors
    private val needUrlSanitize: Boolean

    init {
        val mirrors = MIRRORS
        val mirrorIndex = Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)
            .getString(MIRROR_PREF, "0")!!.toInt().coerceAtMost(mirrors.size - 1)
        baseUrl = "https://" + mirrors[mirrorIndex]
        selectors = getSelectors(mirrorIndex)
        needUrlSanitize = needUrlSanitize(mirrorIndex)
    }

    override fun String.sanitizeTitle() = substringBeforeLast('(').trimEnd()

    override fun popularMangaRequest(page: Int) = GET("$baseUrl/top/?page=$page", headers)
    override fun popularMangaSelector() = selectors.listMangaSelector
    override fun popularMangaNextPageSelector() = ".nextpostslink"

    override fun popularMangaFromElement(element: Element) = super.popularMangaFromElement(element).apply {
        if (needUrlSanitize) url = mangaSlugRegex.replaceFirst(url, "/")
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList) =
        GET("$baseUrl/?s=$query&page=$page", headers)

    override fun Document.getSanitizedDetails(): Element =
        selectFirst(selectors.detailsSelector).apply {
            val recommendClass = selectors.recommendClass
            children().find { it.hasClass(recommendClass) }?.remove()
            selectFirst(Evaluator.Class("list-scoll")).remove()
        }

    override fun chapterListSelector() = ".list-scoll a"
    override fun String.sanitizeChapter() = substring(lastIndexOf('【') + 1, length - 1)

    override fun pageSelector() = Evaluator.Class("card-wrap")

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        ListPreference(screen.context).apply {
            key = MIRROR_PREF
            title = "Mirror"
            summary = "%s\n" +
                "Requires app restart to take effect\n" +
                "Note: 'mangaraw.to' might fail to load images because of Cloudflare protection"
            entries = MIRRORS
            entryValues = MIRRORS.indices.map { it.toString() }.toTypedArray()
            setDefaultValue("0")
        }.let { screen.addPreference(it) }
    }
}
