package eu.kanade.tachiyomi.extension.vi.truyentranh8

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import kotlin.system.exitProcess

class TruyenTranh8UrlActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pathSegments = intent?.data?.pathSegments
        if (pathSegments != null && pathSegments.size > 1) {
            val id = pathSegments[1]

            try {
                startActivity(
                    Intent().apply {
                        action = "eu.kanade.tachiyomi.SEARCH"
                        putExtra("query", "${TruyenTranh8.PREFIX_ID_SEARCH}$id")
                        putExtra("filter", packageName)
                    },
                )
            } catch (e: ActivityNotFoundException) {
                Log.e("TruyenTranh8UrlActivity", e.toString())
            }
        } else {
            Log.e("TruyenTranh8UrlActivity", "Could not parse URL from intent $intent")
        }

        finish()
        exitProcess(0)
    }
}
