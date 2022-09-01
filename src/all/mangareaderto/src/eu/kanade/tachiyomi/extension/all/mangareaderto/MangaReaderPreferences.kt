package eu.kanade.tachiyomi.extension.all.mangareaderto

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.ListPreference
import androidx.preference.SwitchPreferenceCompat

fun getPreferences(context: Context) = arrayOf(

    ListPreference(context).apply {
        key = QUALITY_PREF
        title = "Image quality"
        summary = "Selected: %s\n" +
            "Changes will not be applied to chapters that are already loaded or read " +
            "until you clear the chapter cache."
        entries = arrayOf("Low", "Medium", "High")
        entryValues = arrayOf("low", QUALITY_MEDIUM, "high")
        setDefaultValue(QUALITY_MEDIUM)
    },

    SwitchPreferenceCompat(context).apply {
        key = SHOW_VOLUME_PREF
        title = "Show manga in volumes in search result"
        setDefaultValue(false)
    },
)

val SharedPreferences.quality
    get() =
        getString(QUALITY_PREF, QUALITY_MEDIUM)!!

val SharedPreferences.showVolume
    get() =
        getBoolean(SHOW_VOLUME_PREF, false)

private const val QUALITY_PREF = "quality"
private const val QUALITY_MEDIUM = "medium"
private const val SHOW_VOLUME_PREF = "show_volume"

const val VOLUME_URL_SUFFIX = "#vol"
const val VOLUME_TITLE_PREFIX = "[VOL] "
