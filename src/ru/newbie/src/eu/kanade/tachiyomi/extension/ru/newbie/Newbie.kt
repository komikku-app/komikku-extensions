package eu.kanade.tachiyomi.extension.ru.newbie

import BookDto
import BranchesDto
import LibraryDto
import MangaDetDto
import PageDto
import PageWrapperDto
import SearchLibraryDto
import SearchWrapperDto
import SeriesWrapperDto
import SubSearchDto
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Application
import android.content.SharedPreferences
import android.os.Build
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.asObservableSuccess
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.jsoup.Jsoup
import rx.Observable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Newbie : ConfigurableSource, HttpSource() {
    override val name = "NewManga(Newbie)"

    override val id: Long = 8033757373676218584

    override val baseUrl = "https://newmanga.org"

    override val lang = "ru"

    private val preferences: SharedPreferences by lazy {
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)
    }

    override val supportsLatest = true

    private var branches = mutableMapOf<String, List<BranchesDto>>()

    override fun headersBuilder(): Headers.Builder = Headers.Builder()
        .add("User-Agent", "Tachiyomi " + System.getProperty("http.agent"))
        .add("Referer", baseUrl)

    private fun imageContentTypeIntercept(chain: Interceptor.Chain): Response {
        if (chain.request().url.queryParameter("slice").isNullOrEmpty()) {
            return chain.proceed(chain.request())
        }

        val response = chain.proceed(chain.request())
        val image = response.body?.byteString()?.toResponseBody("image/*".toMediaType())
        return response.newBuilder().body(image).build()
    }

    override val client: OkHttpClient =
        network.client.newBuilder()
            .addInterceptor { imageContentTypeIntercept(it) }
            .build()

    private val count = 30

    override fun popularMangaRequest(page: Int) = GET("$API_URL/projects/popular?scale=month&size=$count&page=$page", headers)

    override fun popularMangaParse(response: Response): MangasPage {
        val page = json.decodeFromString<PageWrapperDto<LibraryDto>>(response.body!!.string())
        val mangas = page.items.map {
            it.toSManga()
        }
        return MangasPage(mangas, mangas.isNotEmpty())
    }

    private fun LibraryDto.toSManga(): SManga {
        val o = this
        return SManga.create().apply {
            // Do not change the title name to ensure work with a multilingual catalog!
            title = o.title.en
            url = "$id"
            thumbnail_url = if (image.srcset.large.isNotEmpty()) {
                "$IMAGE_URL/${image.srcset.large}"
            } else "$IMAGE_URL/${image.srcset.small}"
        }
    }

    override fun latestUpdatesRequest(page: Int): Request = GET("$API_URL/projects/updates?only_bookmarks=false&size=$count&page=$page", headers)

    override fun latestUpdatesParse(response: Response): MangasPage = popularMangaParse(response)

    override fun searchMangaParse(response: Response): MangasPage {
        val page = json.decodeFromString<SearchWrapperDto<SubSearchDto<SearchLibraryDto>>>(response.body!!.string())
        val mangas = page.result.hits.map {
            it.toSearchManga()
        }
        return MangasPage(mangas, mangas.isNotEmpty())
    }

    private fun SearchLibraryDto.toSearchManga(): SManga {
        return SManga.create().apply {
            // Do not change the title name to ensure work with a multilingual catalog!
            title = document.title_en
            url = document.id
            thumbnail_url = if (document.image_large.isNotEmpty()) {
                "$IMAGE_URL/${document.image_large}"
            } else "$IMAGE_URL/${document.image_small}"
        }
    }

    private val simpleDateFormat by lazy { SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US) }

    private fun parseDate(date: String?): Long {
        date ?: return 0L
        return try {
            simpleDateFormat.parse(date)!!.time
        } catch (_: Exception) {
            Date().time
        }
    }

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val mutableGenre = mutableListOf<String>()
        val mutableExGenre = mutableListOf<String>()
        val mutableTag = mutableListOf<String>()
        val mutableExTag = mutableListOf<String>()
        val mutableType = mutableListOf<String>()
        val mutableStatus = mutableListOf<String>()
        val mutableTitleStatus = mutableListOf<String>()
        val mutableAge = mutableListOf<String>()
        var orderBy = "MATCH"
        var ascEnd = "DESC"
        var requireChapters = true
        (if (filters.isEmpty()) getFilterList() else filters).forEach { filter ->
            when (filter) {
                is OrderBy -> {
                    if (query.isEmpty()) {
                        orderBy = arrayOf("RATING", "VIEWS", "HEARTS", "COUNT_CHAPTERS", "CREATED_AT", "UPDATED_AT")[filter.state!!.index]
                        ascEnd = if (filter.state!!.ascending) "ASC" else "DESC"
                    }
                }
                is GenreList -> filter.state.forEach { genre ->
                    if (genre.state != Filter.TriState.STATE_IGNORE) {
                        if (genre.isIncluded()) mutableGenre += '"' + genre.name + '"' else mutableExGenre += '"' + genre.name + '"'
                    }
                }
                is TagsList -> filter.state.forEach { tag ->
                    if (tag.state != Filter.TriState.STATE_IGNORE) {
                        if (tag.isIncluded()) mutableTag += '"' + tag.name + '"' else mutableExTag += '"' + tag.name + '"'
                    }
                }
                is TypeList -> filter.state.forEach { type ->
                    if (type.state) {
                        mutableType += '"' + type.id + '"'
                    }
                }
                is StatusList -> filter.state.forEach { status ->
                    if (status.state) {
                        mutableStatus += '"' + status.id + '"'
                    }
                }
                is StatusTitleList -> filter.state.forEach { status ->
                    if (status.state) {
                        mutableTitleStatus += '"' + status.id + '"'
                    }
                }
                is AgeList -> filter.state.forEach { age ->
                    if (age.state) {
                        mutableAge += '"' + age.id + '"'
                    }
                }
                is RequireChapters -> {
                    if (filter.state == 1) {
                        requireChapters = false
                    }
                }
            }
        }

        return POST(
            "https://neo.newmanga.org/catalogue",
            body = """{"query":"$query","sort":{"kind":"$orderBy","dir":"$ascEnd"},"filter":{"hidden_projects":[],"genres":{"excluded":$mutableExGenre,"included":$mutableGenre},"tags":{"excluded":$mutableExTag,"included":$mutableTag},"type":{"allowed":$mutableType},"translation_status":{"allowed":$mutableStatus},"released_year":{"min":null,"max":null},"require_chapters":$requireChapters,"original_status":{"allowed":$mutableTitleStatus},"adult":{"allowed":$mutableAge}},"pagination":{"page":$page,"size":$count}}""".toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()),
            headers = headers
        )
    }

    private fun parseStatus(status: String): Int {
        return when (status) {
            "completed" -> SManga.COMPLETED
            "on_going" -> SManga.ONGOING
            else -> SManga.UNKNOWN
        }
    }

    private fun parseType(type: String): String {
        return when (type) {
            "manga" -> "Манга"
            "manhwa" -> "Манхва"
            "manhya" -> "Маньхуа"
            "single" -> "Сингл"
            "comics" -> "Комикс"
            "russian" -> "Руманга"
            else -> type
        }
    }
    private fun parseAge(adult: String): String {
        return when (adult) {
            "" -> "0+"
            else -> "$adult+"
        }
    }

    private fun MangaDetDto.toSManga(): SManga {
        val ratingValue = DecimalFormat("#,###.##").format(rating * 2).replace(",", ".").toFloat()
        val ratingStar = when {
            ratingValue > 9.5 -> "★★★★★"
            ratingValue > 8.5 -> "★★★★✬"
            ratingValue > 7.5 -> "★★★★☆"
            ratingValue > 6.5 -> "★★★✬☆"
            ratingValue > 5.5 -> "★★★☆☆"
            ratingValue > 4.5 -> "★★✬☆☆"
            ratingValue > 3.5 -> "★★☆☆☆"
            ratingValue > 2.5 -> "★✬☆☆☆"
            ratingValue > 1.5 -> "★☆☆☆☆"
            ratingValue > 0.5 -> "✬☆☆☆☆"
            else -> "☆☆☆☆☆"
        }
        val o = this
        return SManga.create().apply {
            // Do not change the title name to ensure work with a multilingual catalog!
            title = o.title.en
            url = "$id"
            thumbnail_url = "$IMAGE_URL/${image.srcset.large}"
            author = o.author?.name
            artist = o.artist?.name
            description = o.title.ru + "\n" + ratingStar + " " + ratingValue + " [♡" + hearts + "]\n" + Jsoup.parse(o.description).text()
            genre = parseType(type) + ", " + adult?.let { parseAge(it) } + ", " + genres.joinToString { it.title.ru.capitalize() }
            status = parseStatus(o.status)
        }
    }

    private fun titleDetailsRequest(manga: SManga): Request {
        return GET(API_URL + "/projects/" + manga.url, headers)
    }

    // Workaround to allow "Open in browser" use the real URL.
    override fun fetchMangaDetails(manga: SManga): Observable<SManga> {
        return client.newCall(titleDetailsRequest(manga))
            .asObservableSuccess()
            .map { response ->
                mangaDetailsParse(response).apply { initialized = true }
            }
    }

    override fun mangaDetailsRequest(manga: SManga): Request {
        return GET(baseUrl + "/p/" + manga.url, headers)
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val series = json.decodeFromString<MangaDetDto>(response.body!!.string())
        branches[series.title.en] = series.branches
        return series.toSManga()
    }

    @SuppressLint("DefaultLocale")
    private fun chapterName(book: BookDto): String {
        var chapterName = "${book.tom}. Глава ${DecimalFormat("#,###.##").format(book.number).replace(",", ".")}"
        if (!book.is_available)
            chapterName += " \uD83D\uDCB2 "
        if (book.name?.isNotBlank() == true) {
            chapterName += " ${book.name.capitalize()}"
        }
        return chapterName
    }

    private fun mangaBranches(manga: SManga): List<BranchesDto> {
        val response = client.newCall(titleDetailsRequest(manga)).execute()
        val series = json.decodeFromString<MangaDetDto>(response.body!!.string())
        branches[series.title.en] = series.branches
        return series.branches
    }

    private fun selector(b: BranchesDto): Boolean = b.is_default
    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> {
        val branch = branches.getOrElse(manga.title) { mangaBranches(manga) }
        return when {
            branch.isEmpty() -> {
                return Observable.just(listOf())
            }
            manga.status == SManga.LICENSED -> {
                Observable.error(Exception("Лицензировано - Нет глав"))
            }
            else -> {
                val branchId = branch.first { selector(it) }.id
                client.newCall(chapterListRequest(branchId))
                    .asObservableSuccess()
                    .map { response ->
                        chapterListParse(response)
                    }
            }
        }
    }

    override fun chapterListParse(response: Response): List<SChapter> {
        var chapters = json.decodeFromString<SeriesWrapperDto<List<BookDto>>>(response.body!!.string()).items
        if (!preferences.getBoolean(PAID_PREF, false)) {
            chapters = chapters.filter { it.is_available }
        }
        return chapters.map { chapter ->
            SChapter.create().apply {
                chapter_number = chapter.number
                name = chapterName(chapter)
                url = "/chapters/${chapter.id}/pages"
                date_upload = parseDate(chapter.created_at)
                scanlator = chapter.translator
            }
        }
    }
    override fun chapterListRequest(manga: SManga): Request = throw NotImplementedError("Unused")
    private fun chapterListRequest(branch: Long): Request {
        return GET(
            "$API_URL/branches/$branch/chapters?reverse=true&size=1000000",
            headers
        )
    }

    @TargetApi(Build.VERSION_CODES.N)
    override fun pageListRequest(chapter: SChapter): Request {
        return GET(API_URL + chapter.url, headers)
    }

    private fun pageListParse(response: Response, chapter: SChapter): List<Page> {
        val pages = json.decodeFromString<List<PageDto>>(response.body?.string()!!)
        val result = mutableListOf<Page>()
        pages.forEach { page ->
            (1..page.slices!!).map { i ->
                result.add(Page(result.size, API_URL + chapter.url + "/${page.id}?slice=$i"))
            }
        }
        return result
    }

    override fun pageListParse(response: Response): List<Page> = throw Exception("Not used")
    override fun fetchPageList(chapter: SChapter): Observable<List<Page>> {
        return client.newCall(pageListRequest(chapter))
            .asObservableSuccess()
            .map { response ->
                pageListParse(response, chapter)
            }
    }

    override fun fetchImageUrl(page: Page): Observable<String> {
        val bodyLength = client.newCall(GET(page.url, headers)).execute().body!!.contentLength()
        return if (bodyLength > 320)
            Observable.just(page.url)
        else
            Observable.just("$baseUrl/error-page/img/logo-fullsize.png")
    }

    override fun imageUrlRequest(page: Page): Request = throw NotImplementedError("Unused")

    override fun imageUrlParse(response: Response): String = throw NotImplementedError("Unused")

    override fun imageRequest(page: Page): Request {
        val refererHeaders = headersBuilder().build()
        return GET(page.imageUrl!!, refererHeaders)
    }

    private class CheckFilter(name: String, val id: String) : Filter.CheckBox(name)
    private class SearchFilter(name: String) : Filter.TriState(name)

    private class TypeList(types: List<CheckFilter>) : Filter.Group<CheckFilter>("Типы", types)
    private class StatusList(statuses: List<CheckFilter>) : Filter.Group<CheckFilter>("Статус перевода", statuses)
    private class StatusTitleList(titles: List<CheckFilter>) : Filter.Group<CheckFilter>("Статус оригинала", titles)
    private class GenreList(genres: List<SearchFilter>) : Filter.Group<SearchFilter>("Жанры", genres)
    private class TagsList(tags: List<SearchFilter>) : Filter.Group<SearchFilter>("Теги", tags)
    private class AgeList(ages: List<CheckFilter>) : Filter.Group<CheckFilter>("Возрастное ограничение", ages)

    override fun getFilterList() = FilterList(
        OrderBy(),
        GenreList(getGenreList()),
        TagsList(getTagsList()),
        TypeList(getTypeList()),
        StatusList(getStatusList()),
        StatusTitleList(getStatusTitleList()),
        AgeList(getAgeList()),
        RequireChapters()
    )

    private class OrderBy : Filter.Sort(
        "Сортировка",
        arrayOf("По рейтингу", "По просмотрам", "По лайкам", "По кол-ву глав", "По дате создания", "По дате обновления"),
        Selection(0, false)
    )

    private class RequireChapters : Filter.Select<String>(
        "Только проекты с главами",
        arrayOf("Да", "Все")
    )

    private fun getTypeList() = listOf(
        CheckFilter("Манга", "MANGA"),
        CheckFilter("Манхва", "MANHWA"),
        CheckFilter("Маньхуа", "MANHYA"),
        CheckFilter("Сингл", "SINGLE"),
        CheckFilter("OEL-манга", "OEL"),
        CheckFilter("Комикс", "COMICS"),
        CheckFilter("Руманга", "RUSSIAN")
    )

    private fun getStatusList() = listOf(
        CheckFilter("Выпускается", "ON_GOING"),
        CheckFilter("Заброшен", "ABANDONED"),
        CheckFilter("Завершён", "COMPLETED"),
    )

    private fun getStatusTitleList() = listOf(
        CheckFilter("Выпускается", "ON_GOING"),
        CheckFilter("Приостановлен", "SUSPENDED"),
        CheckFilter("Завершён", "COMPLETED"),
        CheckFilter("Анонс", "ANNOUNCEMENT"),
    )

    private fun getGenreList() = listOf(
        SearchFilter("cёнэн-ай"),
        SearchFilter("боевик"),
        SearchFilter("боевые искусства"),
        SearchFilter("гарем"),
        SearchFilter("гендерная интрига"),
        SearchFilter("героическое фэнтези"),
        SearchFilter("детектив"),
        SearchFilter("дзёсэй"),
        SearchFilter("додзинси"),
        SearchFilter("драма"),
        SearchFilter("ёнкома"),
        SearchFilter("игра"),
        SearchFilter("драма"),
        SearchFilter("ёнкома"),
        SearchFilter("игра"),
        SearchFilter("исекай"),
        SearchFilter("история"),
        SearchFilter("киберпанк"),
        SearchFilter("кодомо"),
        SearchFilter("комедия"),
        SearchFilter("махо-сёдзё"),
        SearchFilter("меха"),
        SearchFilter("мистика"),
        SearchFilter("научная фантастика"),
        SearchFilter("омегаверс"),
        SearchFilter("повседневность"),
        SearchFilter("постапокалиптика"),
        SearchFilter("приключения"),
        SearchFilter("психология"),
        SearchFilter("романтика"),
        SearchFilter("самурайский боевик"),
        SearchFilter("сверхъестественное"),
        SearchFilter("сёдзё"),
        SearchFilter("сёдзё-ай"),
        SearchFilter("сёнэн"),
        SearchFilter("спорт"),
        SearchFilter("сэйнэн"),
        SearchFilter("трагедия"),
        SearchFilter("триллер"),
        SearchFilter("ужасы"),
        SearchFilter("фантастика"),
        SearchFilter("фэнтези"),
        SearchFilter("школа"),
        SearchFilter("элементы юмора"),
        SearchFilter("эротика"),
        SearchFilter("этти"),
        SearchFilter("юри"),
        SearchFilter("яой"),
    )

    private fun getTagsList() = listOf(
        SearchFilter("веб"),
        SearchFilter("в цвете"),
        SearchFilter("сборник"),
        SearchFilter("хентай"),
        SearchFilter("азартные игры"),
        SearchFilter("алхимия"),
        SearchFilter("амнезия"),
        SearchFilter("ангелы"),
        SearchFilter("антигерой"),
        SearchFilter("антиутопия"),
        SearchFilter("апокалипсис"),
        SearchFilter("аристократия"),
        SearchFilter("армия"),
        SearchFilter("артефакты"),
        SearchFilter("боги"),
        SearchFilter("бои на мечах"),
        SearchFilter("борьба за власть"),
        SearchFilter("брат и сестра"),
        SearchFilter("будущее"),
        SearchFilter("вампиры"),
        SearchFilter("ведьма"),
        SearchFilter("вестерн"),
        SearchFilter("видеоигры"),
        SearchFilter("виртуальная реальность"),
        SearchFilter("военные"),
        SearchFilter("война"),
        SearchFilter("волшебники"),
        SearchFilter("волшебные существа"),
        SearchFilter("воспоминания из другого мира"),
        SearchFilter("врачи / доктора"),
        SearchFilter("выживание"),
        SearchFilter("гг женщина"),
        SearchFilter("гг имба"),
        SearchFilter("гг мужчина"),
        SearchFilter("гг не человек"),
        SearchFilter("геймеры"),
        SearchFilter("гильдии"),
        SearchFilter("глупый гг"),
        SearchFilter("гоблины"),
        SearchFilter("горничные"),
        SearchFilter("грузовик-сан"),
        SearchFilter("гяру"),
        SearchFilter("демоны"),
        SearchFilter("драконы"),
        SearchFilter("дружба"),
        SearchFilter("ёнкома"),
        SearchFilter("жестокий мир"),
        SearchFilter("животные компаньоны"),
        SearchFilter("завоевание мира"),
        SearchFilter("зверолюди"),
        SearchFilter("злые духи"),
        SearchFilter("зомби"),
        SearchFilter("игровые элементы"),
        SearchFilter("империи"),
        SearchFilter("исекай"),
        SearchFilter("квесты"),
        SearchFilter("космос"),
        SearchFilter("кулинария"),
        SearchFilter("культивация"),
        SearchFilter("лгбт"),
        SearchFilter("легендарное оружие"),
        SearchFilter("лоли"),
        SearchFilter("магическая академия"),
        SearchFilter("магия"),
        SearchFilter("мафия"),
        SearchFilter("медицина"),
        SearchFilter("месть"),
        SearchFilter("монстродевушки"),
        SearchFilter("монстры"),
        SearchFilter("музыка"),
        SearchFilter("навыки / способности"),
        SearchFilter("наёмники"),
        SearchFilter("насилие / жестокость"),
        SearchFilter("нежить"),
        SearchFilter("ниндзя"),
        SearchFilter("обмен телами"),
        SearchFilter("оборотни"),
        SearchFilter("обратный гарем"),
        SearchFilter("огнестрельное оружие"),
        SearchFilter("офисные работники"),
        SearchFilter("пародия"),
        SearchFilter("пираты"),
        SearchFilter("подземелье"),
        SearchFilter("политика"),
        SearchFilter("полиция"),
        SearchFilter("преступники / криминал"),
        SearchFilter("призраки / духи"),
        SearchFilter("прокачка"),
        SearchFilter("психодел"),
        SearchFilter("путешествия во времени"),
        SearchFilter("рабы"),
        SearchFilter("разумные расы"),
        SearchFilter("ранги силы"),
        SearchFilter("реинкарнация"),
        SearchFilter("роботы"),
        SearchFilter("рыцари"),
        SearchFilter("самураи"),
        SearchFilter("система"),
        SearchFilter("скрытие личности"),
        SearchFilter("спасение мира"),
        SearchFilter("спортивное тело"),
        SearchFilter("средневековье"),
        SearchFilter("стимпанк"),
        SearchFilter("супергерои"),
        SearchFilter("традиционные игры"),
        SearchFilter("умный гг"),
        SearchFilter("управление территорией"),
        SearchFilter("учитель / ученик"),
        SearchFilter("философия"),
        SearchFilter("хикикомори"),
        SearchFilter("холодное оружие"),
        SearchFilter("шантаж"),
        SearchFilter("эльфы"),
        SearchFilter("якудза"),
        SearchFilter("япония"),
    )

    private fun getAgeList() = listOf(
        CheckFilter("13+", "ADULT_13"),
        CheckFilter("16+", "ADULT_16"),
        CheckFilter("18+", "ADULT_18")
    )

    override fun setupPreferenceScreen(screen: androidx.preference.PreferenceScreen) {
        val paidChapterShow = androidx.preference.CheckBoxPreference(screen.context).apply {
            key = PAID_PREF
            title = PAID_PREF_Title
            summary = "Показывает не купленные\uD83D\uDCB2 главы(может вызвать ошибки при обновлении/автозагрузке)"
            setDefaultValue(false)

            setOnPreferenceChangeListener { _, newValue ->
                val checkValue = newValue as Boolean
                preferences.edit().putBoolean(key, checkValue).commit()
            }
        }
        screen.addPreference(paidChapterShow)
    }

    companion object {
        private const val API_URL = "https://api.newmanga.org/v2"
        private const val IMAGE_URL = "https://storage.newmanga.org"

        private const val PAID_PREF = "PaidChapter"
        private const val PAID_PREF_Title = "Показывать платные главы"
    }

    private val json: Json by injectLazy()
}
