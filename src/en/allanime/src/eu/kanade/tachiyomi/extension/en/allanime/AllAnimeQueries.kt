package eu.kanade.tachiyomi.extension.en.allanime

fun buildQuery(queryAction: () -> String): String {
    return queryAction()
        .trimIndent()
        .replace("%", "$")
}

val POPULAR_QUERY: String = buildQuery {
    """
        query(
                %type: VaildPopularTypeEnumType!
                %size: Int!
                %page: Int
                %dateRange: Int
                %allowAdult: Boolean
                %allowUnknown: Boolean
            ) {
            queryPopular(
                type: %type
                size: %size
                dateRange: %dateRange
                page: %page
                allowAdult: %allowAdult
                allowUnknown: %allowUnknown
            ) {
                recommendations {
                    anyCard {
                        _id
                        name
                        thumbnail
                        englishName
                        nativeName
                    }
                }
            }
        }
    """
}

val SEARCH_QUERY: String = buildQuery {
    """
        query(
                %search: SearchInput
                %limit: Int
                %page: Int
                %translationType: VaildTranslationTypeMangaEnumType
                %countryOrigin: VaildCountryOriginEnumType
            ) {
            mangas(
                search: %search
                limit: %limit
                page: %page
                translationType: %translationType
                countryOrigin: %countryOrigin
            ) {
                edges {
                    _id
                    name
                    thumbnail
                    englishName
                    nativeName
                }
            }
        }
    """
}

val DETAILS_QUERY: String = buildQuery {
    """
        query (%_id: String!) {
            manga(
                _id: %_id
            ) {
                _id
                name
                thumbnail
                description
                authors
                genres
                tags
                status
                altNames
                englishName
                nativeName
            }
        }
    """
}

val CHAPTERS_QUERY: String = buildQuery {
    """
        query (%_id: String!) {
            manga(
                _id: %_id
            ) {
                availableChaptersDetail
            }
        }
    """
}

val PAGE_QUERY: String = buildQuery {
    """
        query(
                %mangaId: String!,
                %translationType: VaildTranslationTypeMangaEnumType!,
                %chapterString: String!
            ) {
            chapterPages(
                mangaId: %mangaId
                translationType: %translationType
                chapterString: %chapterString
            ) {
                edges {
                    pictureUrls
                    pictureUrlHead
                }
            }
        }
    """
}
