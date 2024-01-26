package eu.kanade.tachiyomi.extension.vi.nettruyenx

import eu.kanade.tachiyomi.multisrc.wpcomics.WPComics
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * URL history:
 *   - Original URL & redirecting: https://nettruyenx.com
 *   - Latest: https://nettruyenx.com
 *   - Previous:
 *    - https://nettruyenx.com
 */
class NetTruyenX : WPComics("NetTruyenX", "https://nettruyenx.com", "vi", SimpleDateFormat("dd/MM/yy", Locale.getDefault()), null) {
    override val popularPath = "truyen-tranh-hot"
    override val replaceSearchPathOld = "/tim-truyen?status=2&"
    override val replaceSearchPathNew = "/truyen-full?"

    override fun getGenreList(): Array<Pair<String?, String>> = arrayOf(
        null to "Tất cả",
        "action-95" to "Action",
        "truong-thanh" to "Adult",
        "adventure" to "Adventure",
        "anime" to "Anime",
        "chuyen-sinh-2130" to "Chuyển Sinh",
        "comedy-99" to "Comedy",
        "comic" to "Comic",
        "cooking" to "Cooking",
        "co-dai-207" to "Cổ Đại",
        "doujinshi" to "Doujinshi",
        "drama-103" to "Drama",
        "dam-my" to "Đam Mỹ",
        "ecchi" to "Ecchi",
        "fantasy-105" to "Fantasy",
        "gender-bender" to "Gender Bender",
        "harem-107" to "Harem",
        "historical" to "Historical",
        "horror" to "Horror",
        "josei" to "Josei",
        "live-action" to "Live action",
        "manga-112" to "Manga",
        "manhua" to "Manhua",
        "manhwa-11400" to "Manhwa",
        "martial-arts" to "Martial Arts",
        "mature" to "Mature",
        "mecha-117" to "Mecha",
        "mystery" to "Mystery",
        "ngon-tinh" to "Ngôn Tình",
        "one-shot" to "One shot",
        "psychological" to "Psychological",
        "romance-121" to "Romance",
        "school-life" to "School Life",
        "sci-fi" to "Sci-fi",
        "seinen" to "Seinen",
        "shoujo" to "Shoujo",
        "shoujo-ai-126" to "Shoujo Ai",
        "shounen-127" to "Shounen",
        "shounen-ai" to "Shounen Ai",
        "slice-of-life" to "Slice of Life",
        "smut" to "Smut",
        "soft-yaoi" to "Soft Yaoi",
        "soft-yuri" to "Soft Yuri",
        "sports" to "Sports",
        "supernatural" to "Supernatural",
        "thieu-nhi" to "Thiếu Nhi",
        "tragedy-136" to "Tragedy",
        "trinh-tham" to "Trinh Thám",
        "truyen-scan" to "Truyện scan",
        "truyen-mau" to "Truyện Màu",
        "webtoon" to "Webtoon",
        "xuyen-khong-205" to "Xuyên Không",
        "16" to "16+",
        "18" to "18+",
    )
}
