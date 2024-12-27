package eu.kanade.tachiyomi.extension.all.hentai3

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import kotlin.system.exitProcess

/**
 * Springboard that accepts https://3hentai.net/d/xxxxxx intents and redirects them to
 * the main Tachiyomi process.
 */
class Hentai3UrlActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pathSegments = intent?.data?.pathSegments
        if (pathSegments != null && pathSegments.size > 1) {
            val id = pathSegments[1]
            val mainIntent = Intent().apply {
                action = "eu.kanade.tachiyomi.SEARCH"
                putExtra("query", "${Hentai3.PREFIX_ID_SEARCH}$id")
                putExtra("filter", packageName)
            }

            try {
                startActivity(mainIntent)
            } catch (e: ActivityNotFoundException) {
                Log.e("Hentai3UrlActivity", e.toString())
            }
        } else {
            Log.e("Hentai3UrlActivity", "could not parse uri from intent $intent")
        }

        finish()
        exitProcess(0)
    }
}
