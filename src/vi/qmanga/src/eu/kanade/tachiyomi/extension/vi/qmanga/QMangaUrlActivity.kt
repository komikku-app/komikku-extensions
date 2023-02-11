package eu.kanade.tachiyomi.extension.vi.qmanga

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import kotlin.system.exitProcess

class QMangaUrlActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pathSegments = intent?.data?.pathSegments
        if (pathSegments != null && pathSegments.size > 0 && pathSegments[0].endsWith(".html")) {
            val id = pathSegments[0].removeSuffix(".html")
            try {
                startActivity(
                    Intent().apply {
                        action = "eu.kanade.tachiyomi.SEARCH"
                        putExtra("query", "${QManga.PREFIX_ID_SEARCH}$id")
                        putExtra("filter", packageName)
                    },
                )
            } catch (e: ActivityNotFoundException) {
                Log.e("QMangaUrlActivity", e.toString())
            }
        } else {
            Log.e("QMangaUrlActivity", "Could not parse URI from intent $intent")
        }
        finish()
        exitProcess(0)
    }
}
