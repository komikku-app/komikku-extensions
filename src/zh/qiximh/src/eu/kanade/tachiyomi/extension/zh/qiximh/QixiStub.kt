package eu.kanade.tachiyomi.extension.zh.qiximh

import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import okhttp3.Response

class QixiStub : HttpSource() {

    private val migratePrompt = Exception("请迁移到“6漫画”插件，可以在该插件的设置中修改镜像站点")

    override val id get() = 418374491144859437
    override val name get() = "七夕漫画 (废弃,请使用6漫画)"
    override val lang get() = "zh"
    override val supportsLatest get() = false

    override val baseUrl get() = ""

    override fun popularMangaRequest(page: Int) = throw migratePrompt
    override fun popularMangaParse(response: Response) = throw migratePrompt
    override fun latestUpdatesRequest(page: Int) = throw migratePrompt
    override fun latestUpdatesParse(response: Response) = throw migratePrompt
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList) = throw migratePrompt
    override fun searchMangaParse(response: Response) = throw migratePrompt
    override fun mangaDetailsRequest(manga: SManga) = throw migratePrompt
    override fun mangaDetailsParse(response: Response) = throw migratePrompt
    override fun chapterListRequest(manga: SManga) = throw migratePrompt
    override fun chapterListParse(response: Response) = throw migratePrompt
    override fun pageListRequest(chapter: SChapter) = throw migratePrompt
    override fun pageListParse(response: Response) = throw migratePrompt
    override fun imageUrlParse(response: Response) = throw migratePrompt
}
