package eu.kanade.tachiyomi.extension.all.mangadex

import java.text.Collator
import java.util.Locale

class MangaDexIntl(val lang: String) {

    val availableLang: String = if (lang in AVAILABLE_LANGS) lang else ENGLISH

    val locale: Locale = Locale.forLanguageTag(availableLang)

    val collator: Collator = Collator.getInstance(locale)

    val invalidGroupId: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "ID do grupo inválido"
        else -> "Not a valid group ID"
    }

    val invalidAuthorId: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "ID do autor inválido"
        else -> "Not a valid author ID"
    }

    val noSeriesInList: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Sem séries na lista"
        else -> "No series in the list"
    }

    val migrateWarning: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE ->
            "Migre esta entrada do $MANGADEX_NAME para o $MANGADEX_NAME para atualizar"
        else -> "Migrate this entry from $MANGADEX_NAME to $MANGADEX_NAME to update it"
    }

    val coverQuality: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Qualidade da capa"
        else -> "Cover quality"
    }

    val coverQualityOriginal: String = "Original"

    val coverQualityMedium: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Média"
        else -> "Medium"
    }

    val coverQualityLow: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Baixa"
        else -> "Low"
    }

    val dataSaver: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Economia de dados"
        else -> "Data saver"
    }

    val dataSaverSummary: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Utiliza imagens menores e mais compactadas"
        else -> "Enables smaller, more compressed images"
    }

    val standardHttpsPort: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Utilizar somente a porta 443 do HTTPS"
        else -> "Use HTTPS port 443 only"
    }

    val standardHttpsPortSummary: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE ->
            "Ative para fazer requisições em somente servidores de imagem que usem a porta 443. " +
                "Isso permite com que usuários com regras mais restritas de firewall possam acessar " +
                "as imagens do MangaDex."
        else ->
            "Enable to only request image servers that use port 443. This allows users with " +
                "stricter firewall restrictions to access MangaDex images"
    }

    val contentRating: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Classificação de conteúdo"
        else -> "Content rating"
    }

    val standardContentRating: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Classificação de conteúdo padrão"
        else -> "Default content rating"
    }

    val standardContentRatingSummary: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE ->
            "Mostra os conteúdos com as classificações selecionadas por padrão"
        else -> "Show content with the selected ratings by default"
    }

    val contentRatingSafe: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Seguro"
        else -> "Safe"
    }

    val contentRatingSuggestive: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Sugestivo"
        else -> "Suggestive"
    }

    val contentRatingErotica: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Erótico"
        else -> "Erotica"
    }

    val contentRatingPornographic: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Pornográfico"
        else -> "Pornographic"
    }

    private val contentRatingMap: Map<String, String> = mapOf(
        "safe" to contentRatingSafe,
        "suggestive" to contentRatingSuggestive,
        "erotica" to contentRatingErotica,
        "pornographic" to contentRatingPornographic
    )

    fun contentRatingGenre(contentRatingKey: String): String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Classificação: ${contentRatingMap[contentRatingKey]}"
        else -> "$contentRating: ${contentRatingMap[contentRatingKey]}"
    }

    val originalLanguage: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Idioma original"
        else -> "Original language"
    }

    val originalLanguageFilterJapanese: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "${languageDisplayName(JAPANESE)} (Mangá)"
        else -> "${languageDisplayName(JAPANESE)} (Manga)"
    }

    val originalLanguageFilterChinese: String = "${languageDisplayName(CHINESE)} (Manhua)"

    val originalLanguageFilterKorean: String = "${languageDisplayName(KOREAN)} (Manhwa)"

    val filterOriginalLanguages: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Filtrar os idiomas originais"
        else -> "Filter original languages"
    }

    val filterOriginalLanguagesSummary: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE ->
            "Mostra somente conteúdos que foram publicados originalmente nos idiomas " +
                "selecionados nas seções de recentes e navegar"
        else ->
            "Only show content that was originally published in the selected languages in " +
                "both latest and browse"
    }

    val blockGroupByUuid: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Bloquear grupos por UUID"
        else -> "Block groups by UUID"
    }

    val blockGroupByUuidSummary: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE ->
            "Capítulos de grupos bloqueados não irão aparecer no feed de Recentes ou Mangás. " +
                "Digite uma lista de UUIDs dos grupos separados por vírgulas"
        else ->
            "Chapters from blocked groups will not show up in Latest or Manga feed. " +
                "Enter as a Comma-separated list of group UUIDs"
    }

    val blockUploaderByUuid: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Bloquear uploaders por UUID"
        else -> "Block uploader by UUID"
    }

    val blockUploaderByUuidSummary: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE ->
            "Capítulos de usuários bloqueados não irão aparecer no feed de Recentes ou Mangás. " +
                "Digite uma lista de UUIDs dos usuários separados por vírgulas"
        else ->
            "Chapters from blocked uploaders will not show up in Latest or Manga feed. " +
                "Enter as a Comma-separated list of uploader UUIDs"
    }

    val publicationDemographic: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Demografia da publicação"
        else -> "Publication demographic"
    }

    val publicationDemographicNone: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Nenhuma"
        else -> "None"
    }

    val publicationDemographicShounen: String = "Shounen"

    val publicationDemographicShoujo: String = "Shoujo"

    val publicationDemographicSeinen: String = "Seinen"

    val publicationDemographicJosei: String = "Josei"

    val status: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Estado"
        else -> "Status"
    }

    val statusOngoing: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Em andamento"
        else -> "Ongoing"
    }

    val statusCompleted: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Completo"
        else -> "Completed"
    }

    val statusHiatus: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Hiato"
        else -> "Hiatus"
    }

    val statusCancelled: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Cancelado"
        else -> "Cancelled"
    }

    val content: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Conteúdo"
        else -> "Content"
    }

    val contentGore: String = "Gore"

    val contentSexualViolence: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Violência sexual"
        else -> "Sexual Violence"
    }

    val format: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Formato"
        else -> "Format"
    }

    val formatAdaptation: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Adaptação"
        else -> "Adaptation"
    }

    val formatAnthology: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Antologia"
        else -> "Anthology"
    }

    val formatAwardWinning: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Premiado"
        else -> "Award Winning"
    }

    val formatDoujinshi: String = "Doujinshi"

    val formatFanColored: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Colorizado por fãs"
        else -> "Fan Colored"
    }

    val formatFourKoma: String = "4-Koma"

    val formatFullColor: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Colorido"
        else -> "Full Color"
    }

    val formatLongStrip: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Vertical"
        else -> "Long Strip"
    }

    val formatOfficialColored: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Colorizado oficialmente"
        else -> "Official Colored"
    }

    val formatOneshot: String = "Oneshot"

    val formatUserCreated: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Criado por usuários"
        else -> "User Created"
    }

    val formatWebComic: String = "Web Comic"

    val genre: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Gênero"
        else -> "Genre"
    }

    val genreAction: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Ação"
        else -> "Action"
    }

    val genreAdventure: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Aventura"
        else -> "Adventure"
    }

    val genreBoysLove: String = "Boy's Love"

    val genreComedy: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Comédia"
        else -> "Comedy"
    }

    val genreCrime: String = "Crime"

    val genreDrama: String = "Drama"

    val genreFantasy: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Fantasia"
        else -> "Fantasy"
    }

    val genreGirlsLove: String = "Girl's Love"

    val genreHistorical: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Histórico"
        else -> "Historical"
    }

    val genreHorror: String = "Horror"

    val genreIsekai: String = "Isekai"

    val genreMagicalGirls: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Garotas mágicas"
        else -> "Magical Girls"
    }

    val genreMecha: String = "Mecha"

    val genreMedical: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Médico"
        else -> "Medical"
    }

    val genreMystery: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Mistério"
        else -> "Mystery"
    }

    val genrePhilosophical: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Filosófico"
        else -> "Philosophical"
    }

    val genreRomance: String = "Romance"

    val genreSciFi: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Ficção científica"
        else -> "Sci-Fi"
    }

    val genreSliceOfLife: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Cotidiano"
        else -> "Slice of Life"
    }

    val genreSports: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Esportes"
        else -> "Sports"
    }

    val genreSuperhero: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Super-heroi"
        else -> "Superhero"
    }

    val genreThriller: String = "Thriller"

    val genreTragedy: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Tragédia"
        else -> "Tragedy"
    }

    val genreWuxia: String = "Wuxia"

    val theme: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Tema"
        else -> "Theme"
    }

    val themeAliens: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Alienígenas"
        else -> "Aliens"
    }

    val themeAnimals: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Animais"
        else -> "Animals"
    }

    val themeCooking: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Culinária"
        else -> "Cooking"
    }

    val themeCrossdressing: String = "Crossdressing"

    val themeDelinquents: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Delinquentes"
        else -> "Delinquents"
    }

    val themeDemons: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Demônios"
        else -> "Demons"
    }

    val themeGenderSwap: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Troca de gêneros"
        else -> "Genderswap"
    }

    val themeGhosts: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Fantasmas"
        else -> "Ghosts"
    }

    val themeGyaru: String = "Gyaru"

    val themeHarem: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Harém"
        else -> "Harem"
    }

    val themeIncest: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Incesto"
        else -> "Incest"
    }

    val themeLoli: String = "Loli"

    val themeMafia: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Máfia"
        else -> "Mafia"
    }

    val themeMagic: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Magia"
        else -> "Magic"
    }

    val themeMartialArts: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Artes marciais"
        else -> "Martial Arts"
    }

    val themeMilitary: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Militar"
        else -> "Military"
    }

    val themeMonsterGirls: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Garotas monstro"
        else -> "Monster Girls"
    }

    val themeMonsters: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Monstros"
        else -> "Monsters"
    }

    val themeMusic: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Musical"
        else -> "Music"
    }

    val themeNinja: String = "Ninja"

    val themeOfficeWorkers: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Funcionários de escritório"
        else -> "Office Workers"
    }

    val themePolice: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Policial"
        else -> "Police"
    }

    val themePostApocalyptic: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Pós-apocalíptico"
        else -> "Post-Apocalypytic"
    }

    val themePsychological: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Psicológico"
        else -> "Psychological"
    }

    val themeReincarnation: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Reencarnação"
        else -> "Reincarnation"
    }

    val themeReverseHarem: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Harém reverso"
        else -> "Reverse Harem"
    }

    val themeSamurai: String = "Samurai"

    val themeSchoolLife: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Vida escolar"
        else -> "School Life"
    }

    val themeShota: String = "Shota"

    val themeSupernatural: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Sobrenatural"
        else -> "Supernatural"
    }

    val themeSurvival: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Sobrevivência"
        else -> "Survival"
    }

    val themeTimeTravel: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Viagem no tempo"
        else -> "Time Travel"
    }

    val themeTraditionalGames: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Jogos tradicionais"
        else -> "Traditional Games"
    }

    val themeVampires: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Vampiros"
        else -> "Vampires"
    }

    val themeVideoGames: String = "Video Games"

    val themeVillainess: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Vilania"
        else -> "Villainess"
    }

    val themeVirtualReality: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Realidade virtual"
        else -> "Virtual Reality"
    }

    val themeZombies: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Zumbis"
        else -> "Zombies"
    }

    val tags: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Modo das tags"
        else -> "Tags mode"
    }

    val includedTagsMode: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Modo de inclusão de tags"
        else -> "Included tags mode"
    }

    val excludedTagsMode: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Modo de exclusão de tags"
        else -> "Excluded tags mode"
    }

    val modeAnd: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "E"
        else -> "And"
    }

    val modeOr: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Ou"
        else -> "Or"
    }

    val sort: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Ordenar"
        else -> "Sort"
    }

    val sortAlphabetic: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Alfabeticamente"
        else -> "Alphabetic"
    }

    val sortChapterUploadedAt: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Upload do capítulo"
        else -> "Chapter uploaded at"
    }

    val sortNumberOfFollows: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Número de seguidores"
        else -> "Number of follows"
    }

    val sortContentCreatedAt: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Criação do conteúdo"
        else -> "Content created at"
    }

    val sortContentInfoUpdatedAt: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Atualização das informações"
        else -> "Content info updated at"
    }

    val sortRelevance: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Relevância"
        else -> "Relevance"
    }

    val sortYear: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Ano de lançamento"
        else -> "Year"
    }

    val sortRating: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Nota"
        else -> "Rating"
    }

    val hasAvailableChapters: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Há capítulos disponíveis"
        else -> "Has available chapters"
    }

    fun languageDisplayName(localeCode: String): String =
        Locale.forLanguageTag(localeCode)
            .getDisplayName(locale)
            .capitalize(locale)

    fun unableToProcessChapterRequest(code: Int): String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE ->
            "Não foi possível processar a requisição do capítulo. Código HTTP: $code"
        else -> "Unable to process Chapter request. HTTP code: $code"
    }

    fun uploadedBy(users: List<String>): String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Enviado por ${users.joinToString(" & ")}"
        else -> "Uploaded by ${users.joinToString(" & ")}"
    }

    val noGroup: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Sem grupo"
        else -> "No Group"
    }

    companion object {
        const val BRAZILIAN_PORTUGUESE = "pt-BR"
        const val CHINESE = "zh"
        const val ENGLISH = "en"
        const val JAPANESE = "ja"
        const val KOREAN = "ko"
        const val PORTUGUESE = "pt"

        val AVAILABLE_LANGS = arrayOf(BRAZILIAN_PORTUGUESE, ENGLISH, PORTUGUESE)

        const val MANGADEX_NAME = "MangaDex"
    }
}
