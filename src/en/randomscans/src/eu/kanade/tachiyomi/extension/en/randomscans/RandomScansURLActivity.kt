package eu.kanade.tachiyomi.extension.en.randomscans

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import kotlin.system.exitProcess

class RandomScansURLActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pathSegments: List<String>? = intent?.data?.pathSegments
        if (!pathSegments.isNullOrEmpty()) {

            val mainIntent = Intent().apply {
                action = "eu.kanade.tachiyomi.SEARCH"
                // pathSegments.last() seemed to be crashing, it's beyond me why
                putExtra("query", RandomScans.SLUG_SEARCH_PREFIX + pathSegments[pathSegments.size - 1])
                putExtra("filter", packageName)
            }

            try {
                startActivity(mainIntent)
            } catch (e: ActivityNotFoundException) {
                Log.e("RandomScansURLActivity", "failed to start activity with error: $e")
            }
        } else {
            Log.e("RandomScansURLActivity", "could not parse uri from intent $intent")
        }

        finish()
        exitProcess(0)
    }
}
