package eu.kanade.tachiyomi.extension.en.alphascans

import android.app.Application
import android.content.SharedPreferences
import androidx.preference.PreferenceScreen
import androidx.preference.SwitchPreferenceCompat
import eu.kanade.tachiyomi.multisrc.wpmangareader.WPMangaReader
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import rx.Observable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

open class AlphaScans : WPMangaReader("Alpha Scans", "https://alpha-scans.org", "en"), ConfigurableSource {

    private val preferences: SharedPreferences by lazy {
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)
    }

    // Permanent Url start
    override fun fetchPopularManga(page: Int): Observable<MangasPage> {
        return super.fetchPopularManga(page).tempUrlToPermIfNeeded()
    }

    override fun fetchLatestUpdates(page: Int): Observable<MangasPage> {
        return super.fetchLatestUpdates(page).tempUrlToPermIfNeeded()
    }

    override fun fetchSearchManga(page: Int, query: String, filters: FilterList): Observable<MangasPage> {
        return super.fetchSearchManga(page, query, filters).tempUrlToPermIfNeeded()
    }

    private fun Observable<MangasPage>.tempUrlToPermIfNeeded(): Observable<MangasPage> {
        return this.map { mangasPage ->
            MangasPage(
                mangasPage.mangas.map { it.tempUrlToPermIfNeeded() },
                mangasPage.hasNextPage
            )
        }
    }

    private fun SManga.tempUrlToPermIfNeeded(): SManga {
        val turnTempUrlToPerm = preferences.getBoolean(getPermanentMangaUrlPreferenceKey(), true)
        if (!turnTempUrlToPerm) return this

        val sMangaTitleFirstWord = this.title.split(" ")[0]
        if (!this.url.contains("/$sMangaTitleFirstWord", ignoreCase = true)) {
            this.url = this.url.replaceFirst(TEMP_TO_PERM_URL_REGEX, "$1")
        }
        return this
    }

    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> {
        val sManga = manga.tempUrlToPermIfNeeded()
        return super.fetchChapterList(sManga).map { sChapterList ->
            sChapterList.map { it.tempUrlToPermIfNeeded(sManga) }
        }
    }

    private fun SChapter.tempUrlToPermIfNeeded(manga: SManga): SChapter {
        val turnTempUrlToPerm = preferences.getBoolean(getPermanentChapterUrlPreferenceKey(), true)
        if (!turnTempUrlToPerm) return this

        val sChapterNameFirstWord = this.name.split(" ")[0]
        val sMangaTitleFirstWord = manga.title.split(" ")[0]
        if (
            !this.url.contains("/$sChapterNameFirstWord", ignoreCase = true) &&
            !this.url.contains("/$sMangaTitleFirstWord", ignoreCase = true)
        ) {
            this.url = this.url.replaceFirst(TEMP_TO_PERM_URL_REGEX, "$1")
        }
        return this
    }

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        val permanentMangaUrlPref = SwitchPreferenceCompat(screen.context).apply {
            key = getPermanentMangaUrlPreferenceKey()
            title = PREF_PERM_MANGA_URL_TITLE
            summary = PREF_PERM_MANGA_URL_SUMMARY
            setDefaultValue(true)

            setOnPreferenceChangeListener { _, newValue ->
                val checkValue = newValue as Boolean
                preferences.edit()
                    .putBoolean(getPermanentMangaUrlPreferenceKey(), checkValue)
                    .commit()
            }
        }
        val permanentChapterUrlPref = SwitchPreferenceCompat(screen.context).apply {
            key = getPermanentChapterUrlPreferenceKey()
            title = PREF_PERM_CHAPTER_URL_TITLE
            summary = PREF_PERM_CHAPTER_URL_SUMMARY
            setDefaultValue(true)

            setOnPreferenceChangeListener { _, newValue ->
                val checkValue = newValue as Boolean
                preferences.edit()
                    .putBoolean(getPermanentChapterUrlPreferenceKey(), checkValue)
                    .commit()
            }
        }
        screen.addPreference(permanentMangaUrlPref)
        screen.addPreference(permanentChapterUrlPref)
    }

    private fun getPermanentMangaUrlPreferenceKey(): String {
        return PREF_PERM_MANGA_URL_KEY_PREFIX + lang
    }

    private fun getPermanentChapterUrlPreferenceKey(): String {
        return PREF_PERM_CHAPTER_URL_KEY_PREFIX + lang
    }
    // Permanent Url for Manga/Chapter End

    companion object {
        private const val PREF_PERM_MANGA_URL_KEY_PREFIX = "pref_permanent_manga_url_"
        private const val PREF_PERM_MANGA_URL_TITLE = "Permanent Manga URL"
        private const val PREF_PERM_MANGA_URL_SUMMARY = "Turns all manga urls into permanent ones."

        private const val PREF_PERM_CHAPTER_URL_KEY_PREFIX = "pref_permanent_chapter_url"
        private const val PREF_PERM_CHAPTER_URL_TITLE = "Permanent Chapter URL"
        private const val PREF_PERM_CHAPTER_URL_SUMMARY = "Turns all chapter urls into permanent one."

        private val TEMP_TO_PERM_URL_REGEX = Regex("""(/)\d+-""")
    }
}
