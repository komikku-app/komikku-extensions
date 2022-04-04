package eu.kanade.tachiyomi.extension.all.mangaplus

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import kotlin.system.exitProcess

class MangaPlusUrlActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pathSegments = intent?.data?.pathSegments
        if (pathSegments != null && pathSegments.size > 1) {
            val titleId = if (!pathSegments[1].equals("sns_share")) pathSegments[1] else
                intent?.data?.getQueryParameter("title_id")

            if (titleId != null) {
                val mainIntent = Intent().apply {
                    action = "eu.kanade.tachiyomi.SEARCH"
                    putExtra("query", MangaPlus.PREFIX_ID_SEARCH + titleId)
                    putExtra("filter", packageName)
                }

                try {
                    startActivity(mainIntent)
                } catch (e: ActivityNotFoundException) {
                    Log.e("MangaPlusUrlActivity", e.toString())
                }
            } else {
                Log.e("MangaPlusUrlActivity", "Missing title ID from the URL")
            }
        } else {
            Log.e("MangaPlusUrlActivity", "Could not parse URI from intent $intent")
        }

        finish()
        exitProcess(0)
    }
}
