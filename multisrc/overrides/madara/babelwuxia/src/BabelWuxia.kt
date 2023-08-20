package eu.kanade.tachiyomi.extension.en.babelwuxia

import eu.kanade.tachiyomi.multisrc.madara.Madara
import eu.kanade.tachiyomi.network.POST
import okhttp3.FormBody
import okhttp3.Request

class BabelWuxia : Madara("Babel Wuxia", "https://read.babelwuxia.com", "en") {

    // moved from MangaThemesia
    override val versionId = 2

    override val useNewChapterEndpoint = true

    override fun popularMangaNextPageSelector() = "body:not(:has(.no-posts))"

    private fun madaraLoadMoreRequest(page: Int, metaKey: String): Request {
        val formBody = FormBody.Builder().apply {
            add("action", "madara_load_more")
            add("page", page.toString())
            add("template", "madara-core/content/content-archive")
            add("vars[paged]", "1")
            add("vars[orderby]", "meta_value_num")
            add("vars[template]", "archive")
            add("vars[sidebar]", "right")
            add("vars[post_type]", "wp-manga")
            add("vars[post_status]", "publish")
            add("vars[meta_key]", metaKey)
            add("vars[meta_query][0][paged]", "1")
            add("vars[meta_query][0][orderby]", "meta_value_num")
            add("vars[meta_query][0][template]", "archive")
            add("vars[meta_query][0][sidebar]", "right")
            add("vars[meta_query][0][post_type]", "wp-manga")
            add("vars[meta_query][0][post_status]", "publish")
            add("vars[meta_query][0][meta_key]", metaKey)
            add("vars[meta_query][relation]", "AND")
            add("vars[manga_archives_item_layout]", "default")
        }.build()

        val xhrHeaders = headersBuilder()
            .add("Content-Length", formBody.contentLength().toString())
            .add("Content-Type", formBody.contentType().toString())
            .add("X-Requested-With", "XMLHttpRequest")
            .build()

        return POST("$baseUrl/wp-admin/admin-ajax.php", xhrHeaders, formBody)
    }

    override fun popularMangaRequest(page: Int): Request {
        return madaraLoadMoreRequest(page - 1, "_wp_manga_views")
    }

    override fun latestUpdatesRequest(page: Int): Request {
        return madaraLoadMoreRequest(page - 1, "_latest_update")
    }
}
