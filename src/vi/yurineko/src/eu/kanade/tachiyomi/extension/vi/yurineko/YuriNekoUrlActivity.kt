package eu.kanade.tachiyomi.extension.vi.yurineko

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import kotlin.system.exitProcess

class YuriNekoUrlActivity : Activity() {
    private fun prefixDeterminer(path: String): String? = when (path) {
        "manga" -> YuriNeko.PREFIX_ID_SEARCH
        "origin" -> YuriNeko.PREFIX_DOUJIN_SEARCH
        "author" -> YuriNeko.PREFIX_AUTHOR_SEARCH
        "tag" -> YuriNeko.PREFIX_TAG_SEARCH
        "couple" -> YuriNeko.PREFIX_COUPLE_SEARCH
        "team" -> YuriNeko.PREFIX_TEAM_SEARCH
        else -> null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pathSegments = intent?.data?.pathSegments
        if (pathSegments != null &&
            pathSegments.size > 2 &&
            prefixDeterminer(pathSegments[1]) != null
        ) {
            val id = pathSegments[2]
            try {
                startActivity(
                    Intent().apply {
                        action = "eu.kanade.tachiyomi.SEARCH"
                        putExtra("query", "${prefixDeterminer(pathSegments[1])}$id")
                        putExtra("filter", packageName)
                    }
                )
            } catch (e: ActivityNotFoundException) {
                Log.e("YuriNekoUrlActivity", e.toString())
            }
        } else {
            Log.e("YuriNekoUrlActivity", "Could not parse URI from intent $intent")
        }

        finish()
        exitProcess(0)
    }
}
