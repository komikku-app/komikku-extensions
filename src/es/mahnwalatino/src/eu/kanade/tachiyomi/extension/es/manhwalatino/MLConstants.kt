package eu.kanade.tachiyomi.extension.es.manhwalatino

object MLConstants {

    const val PREFIX_MANGA_ID_SEARCH = "id:"

    const val searchMangaNextPageSelector = "link[rel=next]"
    const val latestUpdatesSelector = "div.slider__item"
    const val searchMangaSelector = "div.page-item-detail.manga"
    const val popularMangaNextPageSelector = "a.nextpostslink"
    const val latestUpdatesNextPageSelector = "div[role=navigation] a.last"

    const val popularMangaSelector = "div.page-item-detail.manga"
    const val popularGenreTitleHTMLSelector: String = "div.item-summary div.post-title h3"
    const val popularGenreUrlHTMLSelector: String = "div.item-summary div.post-title h3 a"
    const val popularGenreThumbnailUrlMangaHTMLSelector: String = "div.item-thumb.c-image-hover img"

    const val searchPageTitleHTMLSelector: String = "div.tab-summary div.post-title h3"
    const val searchPageUrlHTMLSelector: String = "div.tab-summary div.post-title h3 a"
    const val searchPageThumbnailUrlMangaHTMLSelector: String = "div.tab-thumb.c-image-hover img"

    const val mangaDetailsThumbnailUrlHTMLSelector: String = "div.summary_image img"
    const val mangaDetailsAuthorHTMLSelector: String = "div.author-content"
    const val mangaDetailsArtistHTMLSelector: String = "div.artist-content"
    const val mangaDetailsDescriptionHTMLSelector: String = "div.post-content_item > div > p"
    const val mangaDetailsGenreHTMLSelector: String = "div.genres-content a"
    const val mangaDetailsTagsHTMLSelector: String = "div.tags-content a"
    const val mangaDetailsAttributes: String = "div.summary_content div.post-content_item"
    const val searchSiteMangasHTMLSelector = "div.c-tabs-item__content"
    const val genreSiteMangasHTMLSelector = "div.page-item-detail.manga"
    const val latestUpdatesSelectorUrl = "div.slider__thumb_item > a"
    const val latestUpdatesSelectorThumbnailUrl = "div.slider__thumb_item > a > img"
    const val latestUpdatesSelectorTitle = "div.slider__content h4"
    const val chapterListParseSelector = "li.wp-manga-chapter"
    const val chapterLinkParser = "a"
    const val chapterReleaseDateLinkParser = "span.chapter-release-date a"
    const val chapterReleaseDateIParser = "span.chapter-release-date i"
    const val pageListParseSelector = "div.page-break.no-gaps img"
}
