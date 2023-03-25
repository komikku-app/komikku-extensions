package eu.kanade.tachiyomi.extension.zh.wnacg

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.EditTextPreference

private const val BASE_URL = "http://www.htmanga3.top"

fun getPreferencesInternal(context: Context, preferences: SharedPreferences) = arrayOf(
    EditTextPreference(context).apply {
        key = OVERRIDE_BASE_URL_PREF
        title = "网址"
        summary = "默认网址为：$BASE_URL\n" +
            "可以尝试的网址有：\n" +
            "    http://www.htmanga4.top\n" +
            "    http://www.htmanga5.top\n" +
            "可以到 www.wnacglink.top 查看最新网址。\n" +
            "如果默认网址失效，可以在此填写新的网址。重启生效。"

        setOnPreferenceChangeListener { _, _ ->
            preferences.edit().putString(DEFAULT_BASE_URL_PREF, BASE_URL).apply()
            true
        }
    },
)

val SharedPreferences.baseUrl: String
    get() {
        val overrideBaseUrl = getString(OVERRIDE_BASE_URL_PREF, "")!!
        if (overrideBaseUrl.isNotEmpty()) {
            val defaultBaseUrl = getString(DEFAULT_BASE_URL_PREF, "")!!
            if (defaultBaseUrl == BASE_URL) return overrideBaseUrl
            // Outdated
            edit()
                .remove(OVERRIDE_BASE_URL_PREF)
                .remove(DEFAULT_BASE_URL_PREF)
                .apply()
        }
        return BASE_URL
    }

private const val OVERRIDE_BASE_URL_PREF = "overrideBaseUrl"
private const val DEFAULT_BASE_URL_PREF = "defaultBaseUrl"
