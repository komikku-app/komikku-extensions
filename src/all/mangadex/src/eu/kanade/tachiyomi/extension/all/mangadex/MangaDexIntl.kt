package eu.kanade.tachiyomi.extension.all.mangadex

import eu.kanade.tachiyomi.extension.all.mangadex.dto.ContentRatingDto
import eu.kanade.tachiyomi.extension.all.mangadex.dto.PublicationDemographicDto
import java.text.Collator
import java.util.Locale

class MangaDexIntl(lang: String) {

    val availableLang: String = if (lang in AVAILABLE_LANGS) lang else ENGLISH

    private val locale: Locale = Locale.forLanguageTag(availableLang)

    val collator: Collator = Collator.getInstance(locale)

    val invalidUuids: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "O texto contém UUIDs inválidos"
        else -> "The text contains invalid UUIDs"
    }

    val invalidGroupId: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "ID do grupo inválido"
        SPANISH_LATAM, SPANISH -> "ID de grupo inválida"
        RUSSIAN -> "Недействительный ID группы"
        else -> "Not a valid group ID"
    }

    val invalidAuthorId: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "ID do autor inválido"
        SPANISH_LATAM, SPANISH -> "ID de autor inválida"
        RUSSIAN -> "Недействительный ID автора"
        else -> "Not a valid author ID"
    }

    val noSeriesInList: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Sem séries na lista"
        SPANISH_LATAM, SPANISH -> "No hay series en la lista"
        RUSSIAN -> "Лист пуст"
        else -> "No series in the list"
    }

    val migrateWarning: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE ->
            "Migre esta entrada do $MANGADEX_NAME para o $MANGADEX_NAME para atualizar"
        SPANISH_LATAM, SPANISH ->
            "Migre la entrada $MANGADEX_NAME a $MANGADEX_NAME para actualizarla"
        else -> "Migrate this entry from $MANGADEX_NAME to $MANGADEX_NAME to update it"
    }

    val coverQuality: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Qualidade da capa"
        SPANISH_LATAM, SPANISH -> "Calidad de la portada"
        RUSSIAN -> "Качество обложки"
        else -> "Cover quality"
    }

    val coverQualityOriginal: String = when (availableLang) {
        RUSSIAN -> "Оригинальное"
        else -> "Original"
    }

    val coverQualityMedium: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Média"
        SPANISH_LATAM, SPANISH -> "Medio"
        RUSSIAN -> "Среднее"
        else -> "Medium"
    }

    val coverQualityLow: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Baixa"
        SPANISH_LATAM, SPANISH -> "Bajo"
        RUSSIAN -> "Низкое"
        else -> "Low"
    }

    val dataSaver: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Economia de dados"
        SPANISH_LATAM, SPANISH -> "Ahorro de datos"
        RUSSIAN -> "Экономия трафика"
        else -> "Data saver"
    }

    val dataSaverSummary: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Utiliza imagens menores e mais compactadas"
        SPANISH_LATAM, SPANISH -> "Utiliza imágenes más pequeñas y más comprimidas"
        RUSSIAN -> "Использует меньшие по размеру, сжатые изображения"
        else -> "Enables smaller, more compressed images"
    }

    val standardHttpsPort: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Utilizar somente a porta 443 do HTTPS"
        SPANISH_LATAM, SPANISH -> "Utilizar el puerto 443 de HTTPS"
        RUSSIAN -> "Использовать только HTTPS порт 443"
        else -> "Use HTTPS port 443 only"
    }

    val standardHttpsPortSummary: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE ->
            "Ative para fazer requisições em somente servidores de imagem que usem a porta 443. " +
                "Isso permite com que usuários com regras mais restritas de firewall possam acessar " +
                "as imagens do $MANGADEX_NAME."
        SPANISH_LATAM, SPANISH ->
            "Habilite esta opción solicitar las imágenes a los servidores que usan el puerto 443. " +
                "Esto permite a los usuarios con restricciones estrictas de firewall acceder " +
                "a las imagenes en $MANGADEX_NAME"
        RUSSIAN ->
            "Запрашивает изображения только с серверов которые используют порт 443. " +
                "Это позволяет пользователям со строгими правилами брандмауэра загружать " +
                "изображения с $MANGADEX_NAME."
        else ->
            "Enable to only request image servers that use port 443. This allows users with " +
                "stricter firewall restrictions to access $MANGADEX_NAME images"
    }

    val contentRating: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Classificação de conteúdo"
        SPANISH_LATAM, SPANISH -> "Clasificación de contenido"
        RUSSIAN -> "Рейтинг контента"
        else -> "Content rating"
    }

    val standardContentRating: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Classificação de conteúdo padrão"
        SPANISH_LATAM, SPANISH -> "Clasificación de contenido por defecto"
        RUSSIAN -> "Рейтинг контента по умолчанию"
        else -> "Default content rating"
    }

    val standardContentRatingSummary: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE ->
            "Mostra os conteúdos com as classificações selecionadas por padrão"
        SPANISH_LATAM, SPANISH ->
            "Muestra el contenido con la clasificación de contenido seleccionada por defecto"
        RUSSIAN ->
            "Показывать контент с выбранным рейтингом по умолчанию"
        else -> "Show content with the selected ratings by default"
    }

    val contentRatingSafe: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Seguro"
        SPANISH_LATAM, SPANISH -> "Seguro"
        RUSSIAN -> "Безопасный"
        else -> "Safe"
    }

    val contentRatingSuggestive: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Sugestivo"
        SPANISH_LATAM, SPANISH -> "Sugestivo"
        RUSSIAN -> "Намекающий"
        else -> "Suggestive"
    }

    val contentRatingErotica: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Erótico"
        SPANISH_LATAM, SPANISH -> "Erótico"
        RUSSIAN -> "Эротический"
        else -> "Erotica"
    }

    val contentRatingPornographic: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Pornográfico"
        SPANISH_LATAM, SPANISH -> "Pornográfico"
        RUSSIAN -> "Порнографический"
        else -> "Pornographic"
    }

    private val contentRatingMap: Map<ContentRatingDto, String> = mapOf(
        ContentRatingDto.SAFE to contentRatingSafe,
        ContentRatingDto.SUGGESTIVE to contentRatingSuggestive,
        ContentRatingDto.EROTICA to contentRatingErotica,
        ContentRatingDto.PORNOGRAPHIC to contentRatingPornographic,
    )

    fun contentRatingGenre(contentRating: ContentRatingDto): String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Classificação: ${contentRatingMap[contentRating]}"
        SPANISH_LATAM, SPANISH -> "Clasificación: ${contentRatingMap[contentRating]}"
        RUSSIAN -> "Рейтинг контента: ${contentRatingMap[contentRating]}"
        else -> "${this.contentRating}: ${contentRatingMap[contentRating]}"
    }

    val originalLanguage: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Idioma original"
        SPANISH_LATAM, SPANISH -> "Lenguaje original"
        RUSSIAN -> "Язык оригинала"
        else -> "Original language"
    }

    val originalLanguageFilterJapanese: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "${languageDisplayName(JAPANESE)} (Mangá)"
        RUSSIAN -> "${languageDisplayName(JAPANESE)} (Манга)"
        else -> "${languageDisplayName(JAPANESE)} (Manga)"
    }

    val originalLanguageFilterChinese: String = when (availableLang) {
        RUSSIAN -> "${languageDisplayName(CHINESE)} (Манхуа)"
        else -> "${languageDisplayName(CHINESE)} (Manhua)"
    }

    val originalLanguageFilterKorean: String = when (availableLang) {
        RUSSIAN -> "${languageDisplayName(KOREAN)} (Манхва)"
        else -> "${languageDisplayName(KOREAN)} (Manhwa)"
    }

    val filterOriginalLanguages: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Filtrar os idiomas originais"
        SPANISH_LATAM, SPANISH -> "Filtrar por lenguajes"
        RUSSIAN -> "Фильтр по языку оригинала"
        else -> "Filter original languages"
    }

    val filterOriginalLanguagesSummary: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE ->
            "Mostra somente conteúdos que foram publicados originalmente nos idiomas " +
                "selecionados nas seções de recentes e navegar"
        SPANISH_LATAM, SPANISH ->
            "Muestra solo el contenido publicado en los idiomas " +
                "seleccionados en recientes y en la búsqueda"
        RUSSIAN ->
            "Показывать тайтлы которые изначально были выпущены только в выбранных" +
                " языках в последних обновлениях и при поиске"
        else ->
            "Only show content that was originally published in the selected languages in " +
                "both latest and browse"
    }

    val blockGroupByUuid: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Bloquear grupos por UUID"
        SPANISH_LATAM, SPANISH -> "Bloquear grupos por UUID"
        RUSSIAN -> "Заблокировать группы по UUID"
        else -> "Block groups by UUID"
    }

    val blockGroupByUuidSummary: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE ->
            "Capítulos de grupos bloqueados não irão aparecer no feed de Recentes ou Mangás. " +
                "Digite uma lista de UUIDs dos grupos separados por vírgulas"
        SPANISH_LATAM, SPANISH ->
            "Los capítulos de los grupos bloqueados no aparecerán en Recientes o en el Feed" +
                " de mangas. Introduce una coma para separar la lista de UUIDs"
        RUSSIAN ->
            "Главы от заблокированных групп не будут отображаться в последних обновлениях" +
                " и в списке глав тайтла. Введите через запятую список UUID групп."
        else ->
            "Chapters from blocked groups will not show up in Latest or Manga feed. " +
                "Enter as a Comma-separated list of group UUIDs"
    }

    val blockUploaderByUuid: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Bloquear uploaders por UUID"
        SPANISH_LATAM, SPANISH -> "Bloquear uploader por UUID"
        RUSSIAN -> "Заблокировать загрузчика по UUID"
        else -> "Block uploader by UUID"
    }

    val blockUploaderByUuidSummary: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE ->
            "Capítulos de usuários bloqueados não irão aparecer no feed de Recentes ou Mangás. " +
                "Digite uma lista de UUIDs dos usuários separados por vírgulas"
        SPANISH_LATAM, SPANISH ->
            "Los capítulos de los uploaders bloqueados no aparecerán en Recientes o en el Feed" +
                " de mangas. Introduce una coma para separar la lista de UUIDs"
        RUSSIAN ->
            "Главы от заблокированных загрузчиков не будут отображаться в последних обновлениях" +
                " и в списке глав тайтла. Введите через запятую список UUID загрузчиков."
        else ->
            "Chapters from blocked uploaders will not show up in Latest or Manga feed. " +
                "Enter as a Comma-separated list of uploader UUIDs"
    }

    val tryUsingFirstVolumeCover: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Tentar usar a capa do primeiro volume como capa"
        else -> "Attempt to use the first volume cover as cover"
    }

    val tryUsingFirstVolumeCoverSummary: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE ->
            "Pode ser necessário atualizar os itens já adicionados na biblioteca. " +
                "Alternativamente, limpe o banco de dados para as novas capas aparecerem."
        else ->
            "May need to manually refresh entries already in library. " +
                "Otherwise, clear database to have new covers to show up."
    }

    val publicationDemographic: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Demografia da publicação"
        SPANISH_LATAM, SPANISH -> "Demografía"
        RUSSIAN -> "Целевая аудитория"
        else -> "Publication demographic"
    }

    val publicationDemographicNone: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Nenhuma"
        SPANISH_LATAM, SPANISH -> "Ninguna"
        RUSSIAN -> "Нет"
        else -> "None"
    }

    val publicationDemographicShounen: String = when (availableLang) {
        RUSSIAN -> "Сёнэн"
        else -> "Shounen"
    }

    val publicationDemographicShoujo: String = when (availableLang) {
        RUSSIAN -> "Сёдзё"
        else -> "Shoujo"
    }

    val publicationDemographicSeinen: String = when (availableLang) {
        RUSSIAN -> "Сэйнэн"
        else -> "Seinen"
    }

    val publicationDemographicJosei: String = when (availableLang) {
        RUSSIAN -> "Дзёсэй"
        else -> "Josei"
    }

    fun publicationDemographic(demographic: PublicationDemographicDto): String = when (demographic) {
        PublicationDemographicDto.NONE -> publicationDemographicNone
        PublicationDemographicDto.SHOUNEN -> publicationDemographicShounen
        PublicationDemographicDto.SHOUJO -> publicationDemographicShoujo
        PublicationDemographicDto.SEINEN -> publicationDemographicSeinen
        PublicationDemographicDto.JOSEI -> publicationDemographicJosei
    }

    val status: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Estado"
        SPANISH_LATAM, SPANISH -> "Estado"
        RUSSIAN -> "Статус"
        else -> "Status"
    }

    val statusOngoing: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Em andamento"
        SPANISH_LATAM, SPANISH -> "Publicandose"
        RUSSIAN -> "Онгоинг"
        else -> "Ongoing"
    }

    val statusCompleted: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Completo"
        SPANISH_LATAM, SPANISH -> "Completado"
        RUSSIAN -> "Завершён"
        else -> "Completed"
    }

    val statusHiatus: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Hiato"
        SPANISH_LATAM, SPANISH -> "Pausado"
        RUSSIAN -> "Приостановлен"
        else -> "Hiatus"
    }

    val statusCancelled: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Cancelado"
        SPANISH_LATAM, SPANISH -> "Cancelado"
        RUSSIAN -> "Отменён"
        else -> "Cancelled"
    }

    val content: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Conteúdo"
        SPANISH_LATAM, SPANISH -> "Contenido"
        RUSSIAN -> "Неприемлемый контент"
        else -> "Content"
    }

    val contentGore: String = when (availableLang) {
        RUSSIAN -> "Жестокость"
        else -> "Gore"
    }

    val contentSexualViolence: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Violência sexual"
        SPANISH_LATAM, SPANISH -> "Violencia Sexual"
        RUSSIAN -> "Сексуальное насилие"
        else -> "Sexual Violence"
    }

    val format: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Formato"
        SPANISH_LATAM, SPANISH -> "Formato"
        RUSSIAN -> "Формат"
        else -> "Format"
    }

    val formatAdaptation: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Adaptação"
        SPANISH_LATAM, SPANISH -> "Adaptación"
        RUSSIAN -> "Адаптация"
        else -> "Adaptation"
    }

    val formatAnthology: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Antologia"
        SPANISH_LATAM, SPANISH -> "Antología"
        RUSSIAN -> "Антология"
        else -> "Anthology"
    }

    val formatAwardWinning: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Premiado"
        SPANISH_LATAM, SPANISH -> "Ganador de premio"
        RUSSIAN -> "Отмеченный наградами"
        else -> "Award Winning"
    }

    val formatDoujinshi: String = when (availableLang) {
        RUSSIAN -> "Додзинси"
        else -> "Doujinshi"
    }

    val formatFanColored: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Colorizado por fãs"
        SPANISH_LATAM, SPANISH -> "Coloreado por fans"
        RUSSIAN -> "Раскрашенная фанатами"
        else -> "Fan Colored"
    }

    val formatFourKoma: String = when (availableLang) {
        RUSSIAN -> "Ёнкома"
        else -> "4-Koma"
    }

    val formatFullColor: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Colorido"
        SPANISH_LATAM, SPANISH -> "Todo a color"
        RUSSIAN -> "В цвете"
        else -> "Full Color"
    }

    val formatLongStrip: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Vertical"
        SPANISH_LATAM, SPANISH -> "Tira larga"
        RUSSIAN -> "Веб"
        else -> "Long Strip"
    }

    val formatOfficialColored: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Colorizado oficialmente"
        SPANISH_LATAM, SPANISH -> "Coloreo oficial"
        RUSSIAN -> "Официально раскрашенная"
        else -> "Official Colored"
    }

    val formatOneshot: String = when (availableLang) {
        RUSSIAN -> "Сингл"
        else -> "Oneshot"
    }

    val formatUserCreated: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Criado por usuários"
        SPANISH_LATAM, SPANISH -> "Creado por usuario"
        RUSSIAN -> "Созданная пользователями"
        else -> "User Created"
    }

    val formatWebComic: String = when (availableLang) {
        RUSSIAN -> "Веб-комикс"
        else -> "Web Comic"
    }

    val genre: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Gênero"
        SPANISH_LATAM, SPANISH -> "Genero"
        RUSSIAN -> "Жанр"
        else -> "Genre"
    }

    val genreAction: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Ação"
        SPANISH_LATAM, SPANISH -> "Acción"
        RUSSIAN -> "Боевик"
        else -> "Action"
    }

    val genreAdventure: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Aventura"
        SPANISH_LATAM, SPANISH -> "Aventura"
        RUSSIAN -> "Приключения"
        else -> "Adventure"
    }

    val genreBoysLove: String = when (availableLang) {
        RUSSIAN -> "BL"
        else -> "Boy's Love"
    }

    val genreComedy: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Comédia"
        SPANISH_LATAM, SPANISH -> "Comedia"
        RUSSIAN -> "Комедия"
        else -> "Comedy"
    }

    val genreCrime: String = when (availableLang) {
        SPANISH_LATAM, SPANISH -> "Crimen"
        RUSSIAN -> "Криминал"
        else -> "Crime"
    }

    val genreDrama: String = when (availableLang) {
        RUSSIAN -> "Драма"
        else -> "Drama"
    }

    val genreFantasy: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Fantasia"
        SPANISH_LATAM, SPANISH -> "Fantasia"
        RUSSIAN -> "Фэнтези"
        else -> "Fantasy"
    }

    val genreGirlsLove: String = when (availableLang) {
        RUSSIAN -> "GL"
        else -> "Girl's Love"
    }

    val genreHistorical: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Histórico"
        SPANISH_LATAM, SPANISH -> "Histórico"
        RUSSIAN -> "История"
        else -> "Historical"
    }

    val genreHorror: String = when (availableLang) {
        RUSSIAN -> "Ужасы"
        else -> "Horror"
    }

    val genreIsekai: String = when (availableLang) {
        RUSSIAN -> "Исекай"
        else -> "Isekai"
    }

    val genreMagicalGirls: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Garotas mágicas"
        SPANISH_LATAM, SPANISH -> "Chicas mágicas"
        RUSSIAN -> "Махо-сёдзё"
        else -> "Magical Girls"
    }

    val genreMecha: String = when (availableLang) {
        RUSSIAN -> "Меха"
        else -> "Mecha"
    }

    val genreMedical: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Médico"
        SPANISH_LATAM, SPANISH -> "Medico"
        RUSSIAN -> "Медицина"
        else -> "Medical"
    }

    val genreMystery: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Mistério"
        SPANISH_LATAM, SPANISH -> "Misterio"
        RUSSIAN -> "Мистика"
        else -> "Mystery"
    }

    val genrePhilosophical: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Filosófico"
        SPANISH_LATAM, SPANISH -> "Filosófico"
        RUSSIAN -> "Философия"
        else -> "Philosophical"
    }

    val genreRomance: String = when (availableLang) {
        RUSSIAN -> "Романтика"
        else -> "Romance"
    }

    val genreSciFi: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Ficção científica"
        SPANISH_LATAM, SPANISH -> "Ciencia ficción"
        RUSSIAN -> "Научная фантастика"
        else -> "Sci-Fi"
    }

    val genreSliceOfLife: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Cotidiano"
        SPANISH_LATAM, SPANISH -> "Recuentos de la vida"
        RUSSIAN -> "Повседневность"
        else -> "Slice of Life"
    }

    val genreSports: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Esportes"
        SPANISH_LATAM, SPANISH -> "Deportes"
        RUSSIAN -> "Спорт"
        else -> "Sports"
    }

    val genreSuperhero: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Super-heroi"
        SPANISH_LATAM, SPANISH -> "Superhéroes"
        RUSSIAN -> "Супергерои"
        else -> "Superhero"
    }

    val genreThriller: String = when (availableLang) {
        RUSSIAN -> "Триллер"
        else -> "Thriller"
    }

    val genreTragedy: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Tragédia"
        SPANISH_LATAM, SPANISH -> "Tragedia"
        RUSSIAN -> "Трагедия"
        else -> "Tragedy"
    }

    val genreWuxia: String = when (availableLang) {
        RUSSIAN -> "Культивация"
        else -> "Wuxia"
    }

    val theme: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Tema"
        SPANISH_LATAM, SPANISH -> "Tema"
        RUSSIAN -> "Теги"
        else -> "Theme"
    }

    val themeAliens: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Alienígenas"
        SPANISH_LATAM, SPANISH -> "Alienígenas"
        RUSSIAN -> "Инопланетяне"
        else -> "Aliens"
    }

    val themeAnimals: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Animais"
        SPANISH_LATAM, SPANISH -> "Animales"
        RUSSIAN -> "Животные"
        else -> "Animals"
    }

    val themeCooking: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Culinária"
        SPANISH_LATAM, SPANISH -> "Cocina"
        RUSSIAN -> "Кулинария"
        else -> "Cooking"
    }

    val themeCrossdressing: String = when (availableLang) {
        SPANISH_LATAM, SPANISH -> "Travestismo"
        RUSSIAN -> "Кроссдрессинг"
        else -> "Crossdressing"
    }

    val themeDelinquents: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Delinquentes"
        SPANISH_LATAM, SPANISH -> "Delincuentes"
        RUSSIAN -> "Хулиганы"
        else -> "Delinquents"
    }

    val themeDemons: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Demônios"
        SPANISH_LATAM, SPANISH -> "Demonios"
        RUSSIAN -> "Демоны"
        else -> "Demons"
    }

    val themeGenderSwap: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Troca de gêneros"
        SPANISH_LATAM, SPANISH -> "Cambio de sexo"
        RUSSIAN -> "Смена гендера"
        else -> "Genderswap"
    }

    val themeGhosts: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Fantasmas"
        SPANISH_LATAM, SPANISH -> "Fantasmas"
        RUSSIAN -> "Призраки"
        else -> "Ghosts"
    }

    val themeGyaru: String = when (availableLang) {
        RUSSIAN -> "Гяру"
        else -> "Gyaru"
    }

    val themeHarem: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Harém"
        RUSSIAN -> "Гарем"
        else -> "Harem"
    }

    val themeIncest: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Incesto"
        SPANISH_LATAM, SPANISH -> "Incesto"
        RUSSIAN -> "Инцест"
        else -> "Incest"
    }

    val themeLoli: String = when (availableLang) {
        RUSSIAN -> "Лоли"
        else -> "Loli"
    }

    val themeMafia: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Máfia"
        RUSSIAN -> "Мафия"
        else -> "Mafia"
    }

    val themeMagic: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Magia"
        SPANISH_LATAM, SPANISH -> "Magia"
        RUSSIAN -> "Магия"
        else -> "Magic"
    }

    val themeMartialArts: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Artes marciais"
        SPANISH_LATAM, SPANISH -> "Artes marciales"
        RUSSIAN -> "Боевые исскуства"
        else -> "Martial Arts"
    }

    val themeMilitary: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Militar"
        SPANISH_LATAM, SPANISH -> "Militar"
        RUSSIAN -> "Военные"
        else -> "Military"
    }

    val themeMonsterGirls: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Garotas monstro"
        SPANISH_LATAM, SPANISH -> "Chicas monstruo"
        RUSSIAN -> "Монстродевушки"
        else -> "Monster Girls"
    }

    val themeMonsters: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Monstros"
        SPANISH_LATAM, SPANISH -> "Monstruos"
        RUSSIAN -> "Монстры"
        else -> "Monsters"
    }

    val themeMusic: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Musical"
        SPANISH_LATAM, SPANISH -> "Musica"
        RUSSIAN -> "Музыка"
        else -> "Music"
    }

    val themeNinja: String = when (availableLang) {
        RUSSIAN -> "Ниндзя"
        else -> "Ninja"
    }

    val themeOfficeWorkers: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Funcionários de escritório"
        SPANISH_LATAM, SPANISH -> "Oficinistas"
        RUSSIAN -> "Офисные работники"
        else -> "Office Workers"
    }

    val themePolice: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Policial"
        SPANISH_LATAM, SPANISH -> "Policial"
        RUSSIAN -> "Полиция"
        else -> "Police"
    }

    val themePostApocalyptic: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Pós-apocalíptico"
        SPANISH_LATAM, SPANISH -> "Post-Apocalíptico"
        RUSSIAN -> "Постапокалиптика"
        else -> "Post-Apocalyptic"
    }

    val themePsychological: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Psicológico"
        SPANISH_LATAM, SPANISH -> "Psicológico"
        RUSSIAN -> "Психология"
        else -> "Psychological"
    }

    val themeReincarnation: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Reencarnação"
        SPANISH_LATAM, SPANISH -> "Reencarnación"
        RUSSIAN -> "Реинкарнация"
        else -> "Reincarnation"
    }

    val themeReverseHarem: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Harém reverso"
        SPANISH_LATAM, SPANISH -> "Harem Inverso"
        RUSSIAN -> "Обратный гарем"
        else -> "Reverse Harem"
    }

    val themeSamurai: String = when (availableLang) {
        RUSSIAN -> "Самураи"
        else -> "Samurai"
    }

    val themeSchoolLife: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Vida escolar"
        SPANISH_LATAM, SPANISH -> "Vida escolar"
        RUSSIAN -> "Школа"
        else -> "School Life"
    }

    val themeShota: String = when (availableLang) {
        RUSSIAN -> "Шота"
        else -> "Shota"
    }

    val themeSupernatural: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Sobrenatural"
        SPANISH_LATAM, SPANISH -> "Sobrenatural"
        RUSSIAN -> "Сверхъестественное"
        else -> "Supernatural"
    }

    val themeSurvival: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Sobrevivência"
        SPANISH_LATAM, SPANISH -> "Supervivencia"
        RUSSIAN -> "Выживание"
        else -> "Survival"
    }

    val themeTimeTravel: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Viagem no tempo"
        SPANISH_LATAM, SPANISH -> "Viaje en el tiempo"
        RUSSIAN -> "Путешествие во времени"
        else -> "Time Travel"
    }

    val themeTraditionalGames: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Jogos tradicionais"
        SPANISH_LATAM, SPANISH -> "Juegos tradicionales"
        RUSSIAN -> "Традиционные игры"
        else -> "Traditional Games"
    }

    val themeVampires: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Vampiros"
        SPANISH_LATAM, SPANISH -> "Vampiros"
        RUSSIAN -> "Вампиры"
        else -> "Vampires"
    }

    val themeVideoGames: String = when (availableLang) {
        SPANISH_LATAM, SPANISH -> "Videojuegos"
        RUSSIAN -> "Видеоигры"
        else -> "Video Games"
    }

    val themeVillainess: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Vilania"
        SPANISH_LATAM, SPANISH -> "Villana"
        RUSSIAN -> "Злодейка"
        else -> "Villainess"
    }

    val themeVirtualReality: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Realidade virtual"
        SPANISH_LATAM, SPANISH -> "Realidad Virtual"
        RUSSIAN -> "Виртуальная реальность"
        else -> "Virtual Reality"
    }

    val themeZombies: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Zumbis"
        RUSSIAN -> "Зомби"
        else -> "Zombies"
    }

    val tags: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Modo das tags"
        SPANISH_LATAM, SPANISH -> "Modo de etiquetas"
        RUSSIAN -> "Режим поиска"
        else -> "Tags mode"
    }

    val includedTagsMode: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Modo de inclusão de tags"
        SPANISH_LATAM, SPANISH -> "Modo de etiquetas incluidas"
        RUSSIAN -> "Включая"
        else -> "Included tags mode"
    }

    val excludedTagsMode: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Modo de exclusão de tags"
        SPANISH_LATAM, SPANISH -> "Modo de etiquetas excluidas"
        RUSSIAN -> "Исключая"
        else -> "Excluded tags mode"
    }

    val modeAnd: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "E"
        SPANISH_LATAM, SPANISH -> "Y"
        RUSSIAN -> "И"
        else -> "And"
    }

    val modeOr: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Ou"
        SPANISH_LATAM, SPANISH -> "O"
        RUSSIAN -> "Или"
        else -> "Or"
    }

    val sort: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Ordenar"
        SPANISH_LATAM, SPANISH -> "Ordenar"
        RUSSIAN -> "Сортировать по"
        else -> "Sort"
    }

    val sortAlphabetic: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Alfabeticamente"
        SPANISH_LATAM, SPANISH -> "Alfabeticamente"
        RUSSIAN -> "Алфавиту"
        else -> "Alphabetic"
    }

    val sortChapterUploadedAt: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Upload do capítulo"
        SPANISH_LATAM, SPANISH -> "Capítulo subido en"
        RUSSIAN -> "Загруженной главе"
        else -> "Chapter uploaded at"
    }

    val sortNumberOfFollows: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Número de seguidores"
        SPANISH_LATAM, SPANISH -> "Número de seguidores"
        RUSSIAN -> "Количеству фолловеров"
        else -> "Number of follows"
    }

    val sortContentCreatedAt: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Criação do conteúdo"
        SPANISH_LATAM, SPANISH -> "Contenido creado en"
        RUSSIAN -> "По дате создания"
        else -> "Content created at"
    }

    val sortContentInfoUpdatedAt: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Atualização das informações"
        SPANISH_LATAM, SPANISH -> "Información del contenido actualizada en"
        RUSSIAN -> "По дате обновления"
        else -> "Content info updated at"
    }

    val sortRelevance: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Relevância"
        SPANISH_LATAM, SPANISH -> "Relevancia"
        RUSSIAN -> "Лучшему соответствию"
        else -> "Relevance"
    }

    val sortYear: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Ano de lançamento"
        SPANISH_LATAM, SPANISH -> "Año"
        RUSSIAN -> "Год"
        else -> "Year"
    }

    val sortRating: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Nota"
        SPANISH_LATAM, SPANISH -> "Calificación"
        RUSSIAN -> "Популярности"
        else -> "Rating"
    }

    val hasAvailableChapters: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Há capítulos disponíveis"
        SPANISH_LATAM, SPANISH -> "Tiene capítulos disponibles"
        RUSSIAN -> "Есть главы"
        else -> "Has available chapters"
    }

    fun languageDisplayName(localeCode: String): String =
        Locale.forLanguageTag(localeCode)
            .getDisplayName(locale)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

    fun unableToProcessChapterRequest(code: Int): String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE ->
            "Não foi possível processar a requisição do capítulo. Código HTTP: $code"
        SPANISH_LATAM, SPANISH ->
            "No se ha podido procesar la solicitud del capítulo. Código HTTP: $code"
        RUSSIAN ->
            "Не удалось обработать ссылку на главу. Ошибка: $code"
        else -> "Unable to process Chapter request. HTTP code: $code"
    }

    fun uploadedBy(users: List<String>): String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Enviado por ${users.joinToString(" & ")}"
        SPANISH_LATAM, SPANISH -> "Subido por: ${users.joinToString(" & ") }"
        RUSSIAN -> "Загрузил ${users.joinToString(" & ")}"
        else -> "Uploaded by ${users.joinToString(" & ")}"
    }

    val noGroup: String = when (availableLang) {
        BRAZILIAN_PORTUGUESE, PORTUGUESE -> "Sem grupo"
        SPANISH_LATAM, SPANISH -> "Sin grupo"
        RUSSIAN -> "Нет группы"
        else -> "No Group"
    }

    companion object {
        const val BRAZILIAN_PORTUGUESE = "pt-BR"
        const val CHINESE = "zh"
        const val ENGLISH = "en"
        const val JAPANESE = "ja"
        const val KOREAN = "ko"
        const val PORTUGUESE = "pt"
        const val SPANISH_LATAM = "es-419"
        const val SPANISH = "es"
        const val RUSSIAN = "ru"

        val AVAILABLE_LANGS = arrayOf(
            ENGLISH,
            BRAZILIAN_PORTUGUESE,
            PORTUGUESE,
            SPANISH_LATAM,
            SPANISH,
            RUSSIAN,
        )

        const val MANGADEX_NAME = "MangaDex"
    }
}
