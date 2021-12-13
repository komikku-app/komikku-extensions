package eu.kanade.tachiyomi.extension.all.mangadex

import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object MDConstants {

    val uuidRegex =
        Regex("[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")

    const val mangaLimit = 20
    const val latestChapterLimit = 100

    const val manga = "manga"
    const val coverArt = "cover_art"
    const val scanlator = "scanlation_group"
    const val uploader = "user"
    const val author = "author"
    const val artist = "artist"
    const val legacyNoGroupId = "00e03853-1b96-4f41-9542-c71b8692033b"

    const val cdnUrl = "https://uploads.mangadex.org"
    const val apiUrl = "https://api.mangadex.org"
    const val apiMangaUrl = "$apiUrl/manga"
    const val apiChapterUrl = "$apiUrl/chapter"
    const val atHomePostUrl = "https://api.mangadex.network/report"
    val whitespaceRegex = "\\s".toRegex()

    const val mdAtHomeTokenLifespan = 5 * 60 * 1000

    val dateFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss+SSS", Locale.US)
        .apply { timeZone = TimeZone.getTimeZone("UTC") }

    const val prefixIdSearch = "id:"
    const val prefixChSearch = "ch:"
    const val prefixGrpSearch = "grp:"

    const val coverQualityPref = "thumbnailQuality"

    fun getCoverQualityPreferenceKey(dexLang: String): String {
        return "${coverQualityPref}_$dexLang"
    }

    fun getCoverQualityPreferenceEntries() = arrayOf("Original", "Medium", "Low")

    fun getCoverQualityPreferenceEntryValues() = arrayOf("", ".512.jpg", ".256.jpg")

    fun getCoverQualityPreferenceDefaultValue() = getCoverQualityPreferenceEntryValues()[0]

    const val dataSaverPref = "dataSaverV5"

    fun getDataSaverPreferenceKey(dexLang: String): String {
        return "${dataSaverPref}_$dexLang"
    }

    private const val standardHttpsPortPref = "usePort443"

    fun getStandardHttpsPreferenceKey(dexLang: String): String {
        return "${standardHttpsPortPref}_$dexLang"
    }

    private const val contentRatingPref = "contentRating"
    const val contentRatingPrefValSafe = "safe"
    const val contentRatingPrefValSuggestive = "suggestive"
    const val contentRatingPrefValErotica = "erotica"
    const val contentRatingPrefValPornographic = "pornographic"
    val contentRatingPrefDefaults = setOf(contentRatingPrefValSafe, contentRatingPrefValSuggestive)

    fun getContentRatingPrefKey(dexLang: String): String {
        return "${contentRatingPref}_$dexLang"
    }

    private const val originalLanguagePref = "originalLanguage"
    const val originalLanguagePrefValJapanese = "ja"
    const val originalLanguagePrefValChinese = "zh"
    const val originalLanguagePrefValChineseHk = "zh-hk"
    const val originalLanguagePrefValKorean = "ko"

    fun getOriginalLanguagePrefKey(dexLang: String): String {
        return "${originalLanguagePref}_$dexLang"
    }
}
