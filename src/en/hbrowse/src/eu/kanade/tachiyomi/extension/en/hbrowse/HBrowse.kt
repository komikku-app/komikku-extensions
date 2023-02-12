package eu.kanade.tachiyomi.extension.en.hbrowse

import android.app.Application
import android.content.SharedPreferences
import androidx.preference.CheckBoxPreference
import androidx.preference.PreferenceScreen
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.source.ConfigurableSource
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import java.io.IOException

class HBrowse : ParsedHttpSource(), ConfigurableSource {

    override val name = "HBrowse"

    override val baseUrl = "https://www.hbrowse.com"

    override val lang = "en"

    override val supportsLatest = true

    private val json: Json by injectLazy()

    // Clients

    private lateinit var phpSessId: String

    private val searchClient = OkHttpClient().newBuilder()
        .followRedirects(false)
        .cookieJar(CookieJar.NO_COOKIES)
        .build()

    override val client: OkHttpClient = network.cloudflareClient.newBuilder()
        .followRedirects(false)
        .addInterceptor { chain ->
            val originalRequest = chain.request()
            when {
                originalRequest.url.toString() == searchUrl -> {
                    phpSessId = searchClient.newCall(originalRequest).execute()
                        .headers("Set-Cookie")
                        .firstOrNull { it.contains("PHPSESSID") }
                        ?.toString()
                        ?.substringBefore(";")
                        ?: throw IOException("PHPSESSID missing")

                    val newHeaders = headersBuilder()
                        .add("Cookie", phpSessId)

                    val contentLength = originalRequest.body!!.contentLength()

                    searchClient.newCall(GET("$baseUrl/${if (contentLength > 8000) "result" else "search"}/1", newHeaders.build())).execute()
                }
                originalRequest.url.toString().contains(nextSearchPageUrlRegex) -> {
                    searchClient.newCall(originalRequest).execute()
                }
                else -> chain.proceed(originalRequest)
            }
        }
        .build()

    // Popular

    override fun popularMangaRequest(page: Int): Request {
        return GET("$baseUrl/browse/title/rank/DESC/$page", headers)
    }

    override fun popularMangaSelector() = "table.thumbTable tbody"

    override fun popularMangaFromElement(element: Element): SManga {
        return SManga.create().apply {
            element.select("div.thumbDiv a").let {
                setUrlWithoutDomain(it.attr("href"))
                title = it.attr("title").substringAfter("\'").substringBeforeLast("\'")
            }
            thumbnail_url = element.select("img.thumbImg").attr("abs:src")
        }
    }

    override fun popularMangaNextPageSelector() = "a[title^=\"jump to next\"]"

    // Latest

    override fun latestUpdatesRequest(page: Int): Request {
        return GET("$baseUrl/browse/title/date/DESC/$page", headers)
    }

    override fun latestUpdatesSelector() = popularMangaSelector()

    override fun latestUpdatesFromElement(element: Element): SManga = popularMangaFromElement(element)

    override fun latestUpdatesNextPageSelector() = popularMangaNextPageSelector()

    // Search

    private val searchUrl = "$baseUrl/content/process.php"
    private val nextSearchPageUrlRegex = Regex("""(/search/|/result/)""")

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val filterList = if (filters.isEmpty()) getFilterList() else filters

        return if (page == 1) {
            val rBody = FormBody.Builder().apply {
                if (query.isNotBlank()) {
                    add("type", "search")
                    add("needle", query)
                } else {
                    add("type", "advance")
                    filterList.filterIsInstance<AdvancedFilter>()
                        .flatMap { it.vals }
                        .forEach { filter -> add(filter.formName, filter.formValue()) }
                }
            }
            POST(searchUrl, headers, rBody.build())
        } else {
            val url = "$baseUrl/${if (query.isNotBlank()) "search" else "result"}/$page"
            val nextPageHeaders = headersBuilder().add("Cookie", phpSessId).build()
            GET(url, nextPageHeaders)
        }
    }

    override fun searchMangaSelector() = "tbody > tr td.browseTitle a"

    override fun searchMangaFromElement(element: Element): SManga {
        return SManga.create().apply {
            setUrlWithoutDomain(element.attr("href"))
            title = element.text()
            thumbnail_url = "$baseUrl/thumbnails/${url.removePrefix("/").substringBefore("/")}_1.jpg"
        }
    }

    override fun searchMangaNextPageSelector() = popularMangaNextPageSelector()

    // Details

    override fun mangaDetailsRequest(manga: SManga): Request {
        return GET("$baseUrl/thumbnails${manga.url}")
    }

    override fun mangaDetailsParse(document: Document): SManga {
        return SManga.create().apply {
            with(document.select("div#main").first()!!) {
                title = select("td:contains(title) + td.listLong").first()!!.text()
                artist = select("td:contains(artist) + td.listLong").text()
                genre = select("td:contains(genre) + td.listLong").joinToString { it.text() }
                description = select("tr:has(.listLong)")
                    .filterNot { it.select("td:first-child").text().contains(Regex("""(Title|Artist|Genre)""")) }
                    .joinToString("\n") { tr -> tr.select("td").joinToString(": ") { it.text() } }
                thumbnail_url = select("tbody img").first()!!.attr("abs:src")
            }
        }
    }

    // Chapters

    override fun chapterListSelector() = if (!hbrowseOnlyChapters()) "h2:contains(read manga online) + table tr" else "h2:contains(read manga online) + table tr:contains(chapter)"

    override fun chapterListParse(response: Response): List<SChapter> {
        return super.chapterListParse(response).reversed()
    }

    override fun chapterFromElement(element: Element): SChapter {
        return SChapter.create().apply {
            name = element.select("td:first-of-type").text()
            setUrlWithoutDomain(element.select("a.listLink").attr("href"))
        }
    }

    // Pages

    override fun pageListParse(document: Document): List<Page> {
        val script = document.select("script:containsData(imageDir)").first()!!.data()
        val imageDir = Regex("""imageDir\s*=\s*"(.+?)";""").find(script)?.groupValues!![1]
        var images = json.decodeFromString<List<String>>(Regex("""list\s*=\s*(\[.+?]);""").find(script)?.groupValues!![1])
        images = images.subList(0, images.size - 2)
        return images.mapIndexed { index, element -> Page(index, "", baseUrl + imageDir + element) }
    }

    override fun imageUrlParse(document: Document): String = throw UnsupportedOperationException("Not used")

    // Filters

    override fun getFilterList(): FilterList {
        return FilterList(
            listOf(Filter.Header("Can't combine with text search!"), Filter.Separator()) +
                advFilterMap.map { AdvancedFilter(getAdvTriStateList(it.key, it.value.split(", "))) },
        )
    }

    private class AdvTriStateFilter(val groupName: String, name: String) : Filter.TriState(name) {
        val formName = "${groupName[0].lowercase() + groupName.drop(1).replace(" ", "")}_$name"
        fun formValue() = when {
            this.isIncluded() -> "y"
            this.isExcluded() -> "n"
            else -> ""
        }
    }
    private class AdvancedFilter(val vals: List<AdvTriStateFilter>) : Filter.Group<AdvTriStateFilter>(vals.first().groupName, vals)

    private val advFilterMap = mapOf(
        Pair("Genre", "action, adventure, anime, bizarre, comedy, drama, fantasy, gore, historic, horror, medieval, modern, myth, psychological, romance, school_life, scifi, supernatural, video_game, visual_novel"),
        Pair("Type", "anthology, bestiality, dandere, deredere, deviant, fully_colored, furry, futanari, gender_bender, guro, harem, incest, kuudere, lolicon, long_story, netorare, non-con, partly_colored, reverse_harem, ryona, short_story, shotacon, transgender, tsundere, uncensored, vanilla, yandere, yaoi, yuri"),
        Pair("Setting", "amusement_park, attic, automobile, balcony, basement, bath, beach, bedroom, cabin, castle, cave, church, classroom, deck, dining_room, doctors, dojo, doorway, dream, dressing_room, dungeon, elevator, festival, gym, haunted_building, hospital, hotel, hot_springs, kitchen, laboratory, library, living_room, locker_room, mansion, office, other, outdoor, outer_space, park, pool, prison, public, restaurant, restroom, roof, sauna, school, school_nurses_office, shower, shrine, storage_room, store, street, teachers_lounge, theater, tight_space, toilet, train, transit, virtual_reality, warehouse, wilderness"),
        Pair("Fetish", "androphobia, apron, assertive_girl, bikini, bloomers, breast_expansion, business_suit, chastity_device, chinese_dress, christmas, collar, corset, cosplay_(female), cosplay_(male), crossdressing_(female), crossdressing_(male), eye_patch, food, giantess, glasses, gothic_lolita, gyaru, gynophobia, high_heels, hot_pants, impregnation, kemonomimi, kimono, knee_high_socks, lab_coat, latex, leotard, lingerie, maid_outfit, mother_and_daughter, none, nonhuman_girl, olfactophilia, pregnant, rich_girl, school_swimsuit, shy_girl, sisters, sleeping_girl, sporty, stockings, strapon, student_uniform, swimsuit, tanned, tattoo, time_stop, twins_(coed), twins_(female), twins_(male), uniform, wedding_dress"),
        Pair("Role", "alien, android, angel, athlete, bride, bunnygirl, cheerleader, delinquent, demon, doctor, dominatrix, escort, foreigner, ghost, housewife, idol, magical_girl, maid, mamono, massagist, miko, mythical_being, neet, nekomimi, newlywed, ninja, normal, nun, nurse, office_lady, other, police, priest, princess, queen, school_nurse, scientist, sorcerer, student, succubus, teacher, tomboy, tutor, waitress, warrior, witch"),
        Pair("Relationship", "acquaintance, anothers_daughter, anothers_girlfriend, anothers_mother, anothers_sister, anothers_wife, aunt, babysitter, childhood_friend, classmate, cousin, customer, daughter, daughter-in-law, employee, employer, enemy, fiance, friend, friends_daughter, friends_girlfriend, friends_mother, friends_sister, friends_wife, girlfriend, landlord, manager, master, mother, mother-in-law, neighbor, niece, none, older_sister, patient, pet, physician, relative, relatives_friend, relatives_girlfriend, relatives_wife, servant, server, sister-in-law, slave, stepdaughter, stepmother, stepsister, stranger, student, teacher, tutee, tutor, twin, underclassman, upperclassman, wife, workmate, younger_sister"),
        Pair("Male Body", "adult, animal, animal_ears, bald, beard, dark_skin, elderly, exaggerated_penis, fat, furry, goatee, hairy, half_animal, horns, large_penis, long_hair, middle_age, monster, muscular, mustache, none, short, short_hair, skinny, small_penis, tail, tall, tanned, tan_line, teenager, wings, young"),
        Pair("Female Body", "adult, animal_ears, bald, big_butt, chubby, dark_skin, elderly, elf_ears, exaggerated_breasts, fat, furry, hairy, hair_bun, half_animal, halo, hime_cut, horns, large_breasts, long_hair, middle_age, monster_girl, muscular, none, pigtails, ponytail, short, short_hair, skinny, small_breasts, tail, tall, tanned, tan_line, teenager, twintails, wings, young"),
        Pair("Grouping", "foursome_(1_female), foursome_(1_male), foursome_(mixed), foursome_(only_female), one_on_one, one_on_one_(2_females), one_on_one_(2_males), orgy_(1_female), orgy_(1_male), orgy_(mainly_female), orgy_(mainly_male), orgy_(mixed), orgy_(only_female), orgy_(only_male), solo_(female), solo_(male), threesome_(1_female), threesome_(1_male), threesome_(only_female), threesome_(only_male)"),
        Pair("Scene", "adultery, ahegao, anal_(female), anal_(male), aphrodisiac, armpit_sex, asphyxiation, blackmail, blowjob, bondage, breast_feeding, breast_sucking, bukkake, cheating_(female), cheating_(male), chikan, clothed_sex, consensual, cunnilingus, defloration, discipline, dominance, double_penetration, drunk, enema, exhibitionism, facesitting, fingering_(female), fingering_(male), fisting, footjob, grinding, groping, handjob, humiliation, hypnosis, intercrural, interracial_sex, interspecies_sex, lactation, lotion, masochism, masturbation, mind_break, nonhuman, orgy, paizuri, phone_sex, props, rape, reverse_rape, rimjob, sadism, scat, sex_toys, spanking, squirt, submission, sumata, swingers, tentacles, voyeurism, watersports, x-ray_blowjob, x-ray_sex"),
        Pair("Position", "69, acrobat, arch, bodyguard, butterfly, cowgirl, dancer, deck_chair, deep_stick, doggy, drill, ex_sex, jockey, lap_dance, leg_glider, lotus, mastery, missionary, none, other, pile_driver, prison_guard, reverse_piggyback, rodeo, spoons, standing, teaspoons, unusual, victory"),
    )

    private fun getAdvTriStateList(groupName: String, vals: List<String>) = vals.map { AdvTriStateFilter(groupName, it) }

    // Preferences

    private val preferences: SharedPreferences by lazy {
        Injekt.get<Application>().getSharedPreferences("source_$id", 0x0000)
    }

    companion object {
        private const val ONLYCHAPTERS_PREF_KEY = "HBROWSE_ONLYCHAPTERS"
        private const val ONLYCHAPTERS_PREF_TITLE = "Only show chapters"
        private const val ONLYCHAPTERS_PREF_SUMMARY = "Only show chapters and not the cover/final pages"
        private const val ONLYCHAPTERS_PREF_DEFAULT_VALUE = false
    }

    override fun setupPreferenceScreen(screen: PreferenceScreen) {
        val onlychaptersPref = CheckBoxPreference(screen.context).apply {
            key = "${ONLYCHAPTERS_PREF_KEY}_$lang"
            title = ONLYCHAPTERS_PREF_TITLE
            summary = ONLYCHAPTERS_PREF_SUMMARY
            setDefaultValue(ONLYCHAPTERS_PREF_DEFAULT_VALUE)

            setOnPreferenceChangeListener { _, newValue ->
                val checkValue = newValue as Boolean
                preferences.edit().putBoolean("${ONLYCHAPTERS_PREF_KEY}_$lang", checkValue).commit()
            }
        }
        screen.addPreference(onlychaptersPref)
    }

    private fun hbrowseOnlyChapters(): Boolean = preferences.getBoolean("${ONLYCHAPTERS_PREF_KEY}_$lang", ONLYCHAPTERS_PREF_DEFAULT_VALUE)
}
