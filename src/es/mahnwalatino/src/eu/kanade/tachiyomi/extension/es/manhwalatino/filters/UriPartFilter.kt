package eu.kanade.tachiyomi.extension.es.manhwalatino.filters

import android.net.Uri
import eu.kanade.tachiyomi.source.model.Filter

/**
 * Class that creates a select filter. Each entry in the dropdown has a name and a display name.
 * If an entry is selected it is appended as a query parameter onto the end of the URI.
 * If `firstIsUnspecified` is set to true, if the first entry is selected, nothing will be appended on the the URI.
 */
// vals: <name, display>
open class UriPartFilter(
    displayName: String,
    private val uriParam: String,
    private val vals: Array<Pair<String, String>>,
    private val firstIsUnspecified: Boolean = true,
    defaultValue: Int = 0,
) :
    Filter.Select<String>(displayName, vals.map { it.second }.toTypedArray(), defaultValue),
    UriFilter {
    override fun addToUri(uri: Uri.Builder) {
        if (state != 0 || !firstIsUnspecified) {
            val filter = vals[state].first
            uri.appendPath(filter)
        }
    }
}
