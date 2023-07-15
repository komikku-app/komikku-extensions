package eu.kanade.tachiyomi.lib.randomua

import android.content.SharedPreferences
import android.widget.Toast
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference
import androidx.preference.PreferenceScreen
import okhttp3.Headers

class RandomUserAgentPreference(
    private val preferences: SharedPreferences,
) {
    /**
     * Helper function to return UserAgentType based on SharedPreference value
     */
    fun getPrefUAType(): UserAgentType {
        return when (preferences.getString(PREF_KEY_RANDOM_UA, "off")) {
            "mobile" -> UserAgentType.MOBILE
            "desktop" -> UserAgentType.DESKTOP
            else -> UserAgentType.OFF
        }
    }

    /**
     * Helper function to return custom UserAgent from SharedPreference
     */
    fun getPrefCustomUA(): String? {
        return preferences.getString(PREF_KEY_CUSTOM_UA, null)
    }

    /**
     * Helper function to add Random User-Agent settings to SharedPreference
     *
     * @param screen, PreferenceScreen from `setupPreferenceScreen`
     */
    fun addPreferenceToScreen(
        screen: PreferenceScreen,
    ) {
        val customUA = EditTextPreference(screen.context).apply {
            key = PREF_KEY_CUSTOM_UA
            title = TITLE_CUSTOM_UA
            summary = CUSTOM_UA_SUMMARY
            setVisible(getPrefUAType() == UserAgentType.OFF)
            setOnPreferenceChangeListener { _, newValue ->
                try {
                    Headers.Builder().add("User-Agent", newValue as String).build()
                    true
                } catch (e: IllegalArgumentException) {
                    Toast.makeText(screen.context, "User Agent invalid：${e.message}", Toast.LENGTH_LONG).show()
                    false
                }
            }
        }

        val randomUA = ListPreference(screen.context).apply {
            key = PREF_KEY_RANDOM_UA
            title = TITLE_RANDOM_UA
            entries = RANDOM_UA_ENTRIES
            entryValues = RANDOM_UA_VALUES
            summary = "%s"
            setDefaultValue("off")
            setOnPreferenceChangeListener { _, newVal ->
                val showCustomUAPref = newVal as String == "off"
                customUA.setVisible(showCustomUAPref)
                true
            }
        }

        screen.addPreference(randomUA)
        screen.addPreference(customUA)
    }

    companion object {
        const val TITLE_RANDOM_UA = "Random User-Agent (Requires Restart)"
        const val PREF_KEY_RANDOM_UA = "pref_key_random_ua_"
        val RANDOM_UA_ENTRIES = arrayOf("OFF", "Desktop", "Mobile")
        val RANDOM_UA_VALUES = arrayOf("off", "desktop", "mobile")

        const val TITLE_CUSTOM_UA = "Custom User-Agent"
        const val PREF_KEY_CUSTOM_UA = "pref_key_custom_ua_"
        const val CUSTOM_UA_SUMMARY = "Leave blank to use application default user-agent. (Requires Restart)"
    }
}
