package eu.kanade.tachiyomi.extension.en.hiperdex

import android.app.Application
import android.content.SharedPreferences
import android.widget.Toast
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.extension.BuildConfig
import eu.kanade.tachiyomi.multisrc.madara.Madara
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class Hiperdex : Madara("Hiperdex", "https://1sthiperdex.com", "en") {
    override val useNewChapterEndpoint: Boolean = true

    private val defaultBaseUrl = "https://1sthiperdex.com"

    override val baseUrl by lazy { getPrefBaseUrl() }

    private val preferences: SharedPreferences by lazy {
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)
    }

    companion object {
        private const val RESTART_TACHIYOMI = "Restart Tachiyomi to apply new setting."
        private const val BASE_URL_PREF_TITLE = "Override BaseUrl"
        private const val BASE_URL_PREF = "overrideBaseUrl_v${BuildConfig.VERSION_CODE}"
        private const val BASE_URL_PREF_SUMMARY = "For temporary uses. Updating the extension will erase this setting."
    }

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        val baseUrlPref = androidx.preference.EditTextPreference(screen.context).apply {
            key = BASE_URL_PREF
            title = BASE_URL_PREF_TITLE
            summary = BASE_URL_PREF_SUMMARY
            this.setDefaultValue(defaultBaseUrl)
            dialogTitle = BASE_URL_PREF_TITLE

            setOnPreferenceChangeListener { _, _ ->
                Toast.makeText(screen.context, RESTART_TACHIYOMI, Toast.LENGTH_LONG).show()
                true
            }
        }
        screen.addPreference(baseUrlPref)

        super.setupPreferenceScreen(screen)
    }

    private fun getPrefBaseUrl(): String = preferences.getString(BASE_URL_PREF, defaultBaseUrl)!!
}
