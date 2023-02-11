package eu.kanade.tachiyomi.extension.vi.cuutruyen

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import kotlin.system.exitProcess

/*
    Springboard that accepts https://cuutruyen.net/mangas/xxxx intents
 */
class CuuTruyenUrlActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pathSegments = intent?.data?.pathSegments
        if (pathSegments != null && pathSegments.size > 1) {
            val id = pathSegments[1]
            try {
                startActivity(
                    Intent().apply {
                        action = "eu.kanade.tachiyomi.SEARCH"
                        putExtra("query", "${CuuTruyen.PREFIX_ID_SEARCH}$id")
                        putExtra("filter", packageName)
                    },
                )
            } catch (e: ActivityNotFoundException) {
                Log.e("CuuTruyenUrlActivity", e.toString())
            }
        } else {
            Log.e("CuuTruyenUrlActivity", "Could not parse URI from intent $intent")
        }

        finish()
        exitProcess(0)
    }
}
