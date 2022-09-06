package eu.kanade.tachiyomi.extension.en.mangabuddy

import android.app.Application
import android.content.SharedPreferences
import androidx.preference.ListPreference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.multisrc.madtheme.MadTheme
import eu.kanade.tachiyomi.source.ConfigurableSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class MangaBuddy :
    MadTheme(
        "MangaBuddy",
        "https://mangabuddy.com",
        "en"
    ),
    ConfigurableSource {
    private val preferences: SharedPreferences by lazy {
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)
    }

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        val imageServerPref = ListPreference(screen.context).apply {
            key = "${IMAGE_SERVER_PREF_KEY}_$lang"
            title = IMAGE_SERVER_PREF_TITLE
            entries = IMAGE_SERVER_PREF_ENTRIES
            entryValues = IMAGE_SERVER_PREF_ENTRY_VALUES
            setDefaultValue(IMAGE_SERVER_PREF_DEFAULT_VALUE)
            summary = "%s"

            setOnPreferenceChangeListener { _, newValue ->
                val selected = newValue as String
                val index = findIndexOfValue(selected)
                val entry = entryValues[index] as String
                preferences.edit().putString("${IMAGE_SERVER_PREF_KEY}_$lang", entry).commit()
            }
        }
        screen.addPreference(imageServerPref)
    }

    override var CDN_URL: String? = IMAGE_SERVER_PREF_DEFAULT_VALUE
        get() = preferences.getString("${IMAGE_SERVER_PREF_KEY}_$lang", IMAGE_SERVER_PREF_DEFAULT_VALUE)

    companion object {
        /*
         * On the site Server 1 load balances between 5 hosts, and Server 2 uses the last host.
         */
        private const val IMAGE_SERVER_PREF_KEY = "IMAGE_SERVER"
        private const val IMAGE_SERVER_PREF_TITLE = "Image server"
        private val IMAGE_SERVER_PREF_ENTRIES = arrayOf("Server 1", "Server 2")
        private val IMAGE_SERVER_PREF_ENTRY_VALUES = arrayOf("https://s1.mbcdnv1.xyz", "https://s1.mbcdnv5.xyz")
        private val IMAGE_SERVER_PREF_DEFAULT_VALUE = IMAGE_SERVER_PREF_ENTRY_VALUES[0]
    }
}
