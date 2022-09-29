package eu.kanade.tachiyomi.extension.all.comickfun

import android.os.Build
import android.text.Html
import eu.kanade.tachiyomi.source.model.SManga
import org.jsoup.Jsoup

internal fun beautifyDescription(description: String): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        return Html.fromHtml(description, Html.FROM_HTML_MODE_LEGACY).toString()
    }
    return Jsoup.parse(description).text()
}

internal fun parseStatus(status: Int): Int {
    return when (status) {
        1 -> SManga.ONGOING
        2 -> SManga.COMPLETED
        3 -> SManga.CANCELLED
        4 -> SManga.ON_HIATUS
        else -> SManga.UNKNOWN
    }
}

internal fun beautifyChapterName(vol: String, chap: String, title: String): String {
    return buildString {
        if (vol.isNotEmpty()) {
            if (chap.isEmpty()) append("Volume $vol") else append("Vol. $vol")
        }
        if (chap.isNotEmpty()) {
            if (vol.isEmpty()) append("Chapter $chap") else append(", Ch. $chap")
        }
        if (title.isNotEmpty()) {
            if (chap.isEmpty()) append(title) else append(": $title")
        }
    }
}
