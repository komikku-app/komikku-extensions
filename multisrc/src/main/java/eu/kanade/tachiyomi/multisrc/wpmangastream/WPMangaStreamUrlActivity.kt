package eu.kanade.tachiyomi.multisrc.wpmangastream

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import kotlin.system.exitProcess

class WPMangaStreamUrlActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pathSegments = intent?.data?.pathSegments

        if (pathSegments != null && pathSegments.size >= 1) {

            val mainIntent = Intent().apply {
                action = "eu.kanade.tachiyomi.SEARCH"
                putExtra("query", "${WPMangaStream.URL_SEARCH_PREFIX}${intent?.data?.toString()}")
                putExtra("filter", packageName)
            }
            try {
                startActivity(mainIntent)
            } catch (e: ActivityNotFoundException) {
                Log.e("WPMangaStreamUrl", e.toString())
            }
        } else {
            Log.e("WPMangaStreamUrl", "could not parse uri from intent $intent")
        }

        finish()
        exitProcess(0)
    }
}
