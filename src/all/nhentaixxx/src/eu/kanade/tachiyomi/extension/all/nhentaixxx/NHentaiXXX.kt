package eu.kanade.tachiyomi.extension.all.nhentaixxx

import android.util.Log
import eu.kanade.tachiyomi.multisrc.galleryadults.GalleryAdults
import eu.kanade.tachiyomi.multisrc.galleryadults.cleanTag
import eu.kanade.tachiyomi.multisrc.galleryadults.imgAttr
import eu.kanade.tachiyomi.multisrc.galleryadults.toDate
import eu.kanade.tachiyomi.source.model.Page
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.jsonObject
import okhttp3.FormBody
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class NHentaiXXX(
    lang: String = "all",
    override val mangaLang: String = LANGUAGE_MULTI,
) : GalleryAdults(
    "NHentai.xxx",
    "https://nhentai.xxx",
    lang = lang,
) {
    override val supportsLatest = mangaLang.isNotBlank()

    private val languages: List<Pair<String, String>> = listOf(
        Pair(LANGUAGE_ENGLISH, "1"),
        Pair(LANGUAGE_JAPANESE, "2"),
        Pair(LANGUAGE_CHINESE, "3"),
    )
    private val langCode = languages.firstOrNull { lang -> lang.first == mangaLang }?.second

    override fun Element.mangaLang() = when (attr("data-languages")) {
        langCode -> mangaLang
        else -> "other"
    }

    override fun Element.mangaUrl() =
        selectFirst(".gallery_item a")?.attr("abs:href")

    override fun Element.mangaThumbnail() =
        selectFirst(".gallery_item img")?.imgAttr()

    override fun popularMangaSelector() = ".galleries_box .gallery_item"

    override fun Element.getInfo(tag: String): String {
        return select(".tags:contains($tag:) .tag_btn .tag_name")
            .joinToString { it.ownText().cleanTag() }
    }

    override fun Element.getDescription(): String {
        return (
            listOf("Parodies", "Characters", "Languages", "Categories")
                .mapNotNull { tag ->
                    getInfo(tag)
                        .let { if (it.isNotBlank()) "$tag: $it" else null }
                } +
                listOfNotNull(
                    selectFirst("li:has(.tags .pages)")?.text(),
                    selectFirst(".info h1 + h2")?.ownText()
                        .let { altTitle -> if (!altTitle.isNullOrBlank()) "Alternate Title: $altTitle" else null },
                )
            )
            .joinToString("\n\n")
            .plus(
                if (preferences.shortTitle) {
                    "\nFull title: ${mangaFullTitle("h1")}"
                } else {
                    ""
                },
            )
    }

    override fun Element.getTime(): Long {
        return selectFirst(".uploaded")
            ?.ownText()
            .toDate(simpleDateFormat)
    }

    override val favoritePath = "favorites"

    override fun loginRequired(document: Document, url: String): Boolean {
        return (
            url.contains("/favorites/") &&
                document.select("a[href='/login/']:contains(Sign in)").isNotEmpty()
            )
    }

    override val basicSearchKey = "key"

    private val serverSelector = "load_server"

    private fun serverNumber(document: Document, galleryId: String): String {
        return document.inputIdValueOf(serverSelector).takeIf {
            it.isNotBlank()
        } ?: when (galleryId.toInt()) {
            in 1..274825 -> "1"
            in 274826..403818 -> "2"
            in 403819..527143 -> "3"
            in 527144..632481 -> "4"
            in 632482..816010 -> "5"
            in 816011..970098 -> "6"
            in 970099..1121113 -> "7"
            else -> "8"
        }
    }

    override fun pageRequestForm(document: Document, totalPages: String): FormBody {
        val token = document.select("[name=csrf-token]").attr("content")
        val galleryId = document.inputIdValueOf(galleryIdSelector)

        return FormBody.Builder()
            .add("_token", token)
            .add("server", serverNumber(document, galleryId))
            .add("u_id", document.inputIdValueOf(galleryIdSelector))
            .add("g_id", document.inputIdValueOf(loadIdSelector))
            .add("img_dir", document.inputIdValueOf(loadDirSelector))
            .add("visible_pages", "10")
            .add("total_pages", totalPages)
            .add("type", "2") // 1 would be "more", 2 is "all remaining"
            .build()
    }

    override fun pageListParse(document: Document): List<Page> {
        val json = document.selectFirst("script:containsData(parseJSON)")?.data()
            ?.substringAfter("$.parseJSON('{\"fl\":")
            ?.substringBefore(",\"th\":")?.trim()
        Log.e("TAG", "json: $json")

        if (json != null) {
            val loadDir = document.inputIdValueOf(loadDirSelector)
            val loadId = document.inputIdValueOf(loadIdSelector)
            val galleryId = document.inputIdValueOf(galleryIdSelector)
            val pageUrl = "$baseUrl/$pageUri/$galleryId"

            val randomServer = getServer(document, galleryId)
            val imagesUri = "https://$randomServer/$loadDir/$loadId"

            try {
                val pages = mutableListOf<Page>()
                val images = jsonFormat.parseToJsonElement(json).jsonObject

                // JSON string in this form: {"1":"j,1100,1148","2":"j,728,689",...
                for (image in images) {
                    val ext = image.value.toString().replace("\"", "").split(",")[0]
                    val imageExt = when (ext) {
                        "p" -> "png"
                        "b" -> "bmp"
                        "g" -> "gif"
                        else -> "jpg"
                    }
                    val idx = image.key.toInt()
                    pages.add(
                        Page(
                            index = idx,
                            imageUrl = "$imagesUri/${image.key}.$imageExt",
                            url = "$pageUrl/$idx/",
                        ),
                    )
                }
                return pages
            } catch (e: SerializationException) {
                Log.e("GalleryAdults", "Failed to decode JSON")
                return this.pageListParseAlternative(document)
            }
        } else {
            return this.pageListParseAlternative(document)
        }
    }

    /* Filters */
    override fun tagsParser(document: Document): List<Pair<String, String>> {
        return document.select(".tags_items a.tag_btn")
            .mapNotNull {
                Pair(
                    it.selectFirst(".tag_name")?.ownText() ?: "",
                    it.attr("href")
                        .removeSuffix("/").substringAfterLast('/'),
                )
            }
    }
}
