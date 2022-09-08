package eu.kanade.tachiyomi.extension.zh.pphanman

import android.app.Application
import androidx.preference.ListPreference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.multisrc.mccms.MCCMS
import eu.kanade.tachiyomi.source.ConfigurableSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

// it has a category page with no filter
class PPHanman : MCCMS("PP韩漫", "", hasCategoryPage = false), ConfigurableSource {

    override val baseUrl: String

    init {
        val mirrors = MIRRORS
        val mirrorIndex = Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)
            .getString(MIRROR_PREF, "0")!!.toInt().coerceAtMost(mirrors.size - 1)
        baseUrl = "https://" + mirrors[mirrorIndex]
    }

    // .../comic_{id}.html
    override fun getMangaId(url: String) = url.substringAfterLast('_').substringBeforeLast('.')

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        ListPreference(screen.context).apply {
            val mirrors = MIRRORS
            key = MIRROR_PREF
            title = "镜像站点"
            summary = "%s\n重启生效"
            entries = mirrors
            entryValues = Array(mirrors.size) { it.toString() }
            setDefaultValue("0")
        }.let(screen::addPreference)
    }

    companion object {
        private const val MIRROR_PREF = "MIRROR"

        // https://bitbucket.org/fabuye/pphanman
        private val MIRRORS get() = arrayOf("pphm.xyz", "pphm2.xyz", "krhentai.com")
    }
}
