package eu.kanade.tachiyomi.extension.es.manhwalatino.filters

import android.net.Uri

/**
 * Represents a filter that is able to modify a URI.
 */
interface UriFilter {
    fun addToUri(uri: Uri.Builder)
}
