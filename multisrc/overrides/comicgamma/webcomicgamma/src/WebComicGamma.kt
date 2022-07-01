package eu.kanade.tachiyomi.extension.ja.webcomicgamma

import eu.kanade.tachiyomi.multisrc.comicgamma.ComicGamma
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Evaluator

class WebComicGamma : ComicGamma("Web Comic Gamma", "https://webcomicgamma.takeshobo.co.jp") {
    override val supportsLatest = false

    override fun popularMangaSelector() = ".tab_panel.active .manga_item"
    override fun popularMangaFromElement(element: Element) = SManga.create().apply {
        url = element.selectFirst(Evaluator.Tag("a")).attr("href")
        title = element.selectFirst(Evaluator.Class("manga_title")).text()
        author = element.selectFirst(Evaluator.Class("manga_author")).text()
        val genreList = element.select(Evaluator.Tag("li")).map { it.text() }
        genre = genreList.joinToString()
        status = when {
            genreList.contains("完結") && !genreList.contains("リピート配信") -> SManga.COMPLETED
            else -> SManga.ONGOING
        }
        thumbnail_url = element.selectFirst(Evaluator.Tag("img")).absUrl("src")
    }

    override fun latestUpdatesRequest(page: Int) = throw UnsupportedOperationException("Not used.")
    override fun latestUpdatesNextPageSelector() = throw UnsupportedOperationException("Not used.")
    override fun latestUpdatesSelector() = throw UnsupportedOperationException("Not used.")
    override fun latestUpdatesFromElement(element: Element) = throw UnsupportedOperationException("Not used.")

    override fun mangaDetailsParse(document: Document): SManga {
        val titleElement = document.selectFirst(Evaluator.Class("manga__title"))
        val titleName = titleElement.child(0).text()
        val desc = document.selectFirst(".detail__item > p")?.run {
            select(Evaluator.Tag("br")).prepend("\\n")
            this.text().replace("\\n", "\n").replace("\n ", "\n")
        }
        val listResponse = client.newCall(popularMangaRequest(0)).execute()
        val manga = popularMangaParse(listResponse).mangas.find { it.title == titleName }
        return manga?.apply { description = desc } ?: SManga.create().apply {
            author = titleElement.child(1).text()
            description = desc
            status = SManga.UNKNOWN
            val slug = document.location().removeSuffix("/").substringAfterLast("/")
            thumbnail_url = "$baseUrl/img/manga_thumb/${slug}_list.jpg"
        }
    }

    override fun chapterListSelector() = ".read__area > .read__outer > a"
    override fun chapterFromElement(element: Element) = SChapter.create().apply {
        url = element.attr("href").toOldChapterUrl()
        val number = url.removeSuffix("/").substringAfterLast('/').replace('_', '.')
        val list = element.selectFirst(Evaluator.Class("read__contents")).children()
        name = "[$number] ${list[0].text()}"
        if (list.size >= 3) {
            date_upload = dateFormat.parse(list[2].text())?.time ?: 0L
        }
        if (date_upload <= 0L) date_upload = -1L // hide unknown ones
    }

    override fun pageListRequest(chapter: SChapter) =
        GET(baseUrl + chapter.url.toNewChapterUrl(), headers)

    companion object {
        private val dateFormat by lazy { getJSTFormat("yyyy年M月dd日") }

        private fun String.toOldChapterUrl(): String {
            // ../../../_files/madeinabyss/063_2/
            val segments = split('/')
            val size = segments.size
            val slug = segments[size - 3]
            val number = segments[size - 2]
            return "/manga/$slug/_files/$number/"
        }

        private fun String.toNewChapterUrl(): String {
            val segments = split('/')
            return "/_files/${segments[2]}/${segments[4]}/"
        }
    }
}
