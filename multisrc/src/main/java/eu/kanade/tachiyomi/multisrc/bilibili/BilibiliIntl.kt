package eu.kanade.tachiyomi.multisrc.bilibili

import java.text.DateFormatSymbols
import java.text.NumberFormat
import java.util.Locale

class BilibiliIntl(private val lang: String) {

    private val locale by lazy { Locale.forLanguageTag(lang) }

    private val dateFormatSymbols by lazy { DateFormatSymbols(locale) }

    private val numberFormat by lazy { NumberFormat.getInstance(locale) }

    val statusLabel: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> "进度"
        SPANISH -> "Estado"
        else -> "Status"
    }

    val sortLabel: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> "排序"
        INDONESIAN -> "Urutkan dengan"
        SPANISH -> "Ordenar por"
        else -> "Sort by"
    }

    val genreLabel: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> "题材"
        SPANISH -> "Género"
        else -> "Genre"
    }

    val areaLabel: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> "地区"
        else -> "Area"
    }

    val priceLabel: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> "收费"
        INDONESIAN -> "Harga"
        SPANISH -> "Precio"
        else -> "Price"
    }

    val episodePrefix: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> ""
        SPANISH -> "Cap. "
        else -> "Ep. "
    }

    fun hasPaidChaptersWarning(chapterCount: Int): String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE ->
            "${Bilibili.EMOJI_WARNING} 此漫画有 ${chapterCount.localized} 个付费章节，已在目录中隐藏。" +
                "如果你已购买，请在 WebView 登录并刷新目录，即可阅读已购章节。"
        SPANISH ->
            "${Bilibili.EMOJI_WARNING} ADVERTENCIA: Esta serie tiene ${chapterCount.localized} " +
                "capítulos pagos que fueron filtrados de la lista de capítulos. Si ya has " +
                "desbloqueado y tiene alguno en su cuenta, inicie sesión en WebView y " +
                "actualice la lista de capítulos para leerlos."
        else ->
            "${Bilibili.EMOJI_WARNING} WARNING: This series has ${chapterCount.localized} paid " +
                "chapters that were filtered out from the chapter list. If you have already " +
                "unlocked and have any in your account, sign in through WebView and refresh " +
                "the chapter list to read them."
    }

    val imageQualityPrefTitle: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> "章节图片质量"
        INDONESIAN -> "Kualitas gambar"
        SPANISH -> "Calidad de imagen del capítulo"
        else -> "Chapter image quality"
    }

    val imageQualityPrefEntries: Array<String> = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> arrayOf("原图+", "原图 (1600w)", "高 (1000w)", "低 (800w)")
        else -> arrayOf("Raw+", "Raw (1600w)", "HD (1000w)", "SD (800w)")
    }

    val imageFormatPrefTitle: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> "章节图片格式"
        INDONESIAN -> "Format gambar"
        SPANISH -> "Formato de la imagen del capítulo"
        else -> "Chapter image format"
    }

    val sortInterest: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> "为你推荐"
        INDONESIAN -> "Kamu Mungkin Suka"
        SPANISH -> "Sugerencia"
        else -> "Interest"
    }

    val sortPopular: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> "人气推荐"
        INDONESIAN -> "Populer"
        SPANISH -> "Popularidad"
        FRENCH -> "Préférences"
        else -> "Popular"
    }

    val sortUpdated: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> "更新时间"
        INDONESIAN -> "Terbaru"
        SPANISH -> "Actualización"
        FRENCH -> "Récent"
        else -> "Updated"
    }

    val sortAdded: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> "上架时间"
        else -> "Added"
    }

    val sortFollowers: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> "追漫人数"
        else -> "Followers count"
    }

    val statusAll: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> "全部"
        INDONESIAN -> "Semua"
        SPANISH -> "Todos"
        FRENCH -> "Tout"
        else -> "All"
    }

    val statusOngoing: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> "连载中"
        INDONESIAN -> "Berlangsung"
        SPANISH -> "En curso"
        FRENCH -> "En cours"
        else -> "Ongoing"
    }

    val statusComplete: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> "已完结"
        INDONESIAN -> "Tamat"
        SPANISH -> "Finalizado"
        FRENCH -> "Complet"
        else -> "Completed"
    }

    val priceAll: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> "全部"
        INDONESIAN -> "Semua"
        SPANISH -> "Todos"
        else -> "All"
    }

    val priceFree: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> "免费"
        INDONESIAN -> "Bebas"
        SPANISH -> "Gratis"
        else -> "Free"
    }

    val pricePaid: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> "付费"
        INDONESIAN -> "Dibayar"
        SPANISH -> "Pago"
        else -> "Paid"
    }

    val priceWaitForFree: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> "等就免费"
        else -> "Wait for free"
    }

    val failedToRefreshToken: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> "无法刷新令牌。请打开 WebView 修正错误。"
        SPANISH -> "Error al actualizar el token. Abra el WebView para solucionar este error."
        else -> "Failed to refresh the token. Open the WebView to fix this error."
    }

    val failedToGetCredential: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> "无法获取阅读章节所需的凭证。"
        SPANISH -> "Erro al obtener la credencial para leer el capítulo."
        else -> "Failed to get the credential to read the chapter."
    }

    val informationTitle: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> "信息"
        SPANISH -> "Información"
        else -> "Information"
    }

    val totalChapterCount: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> "章节总数"
        SPANISH -> "Número total de capítulos"
        else -> "Total chapter count"
    }

    val updatedEvery: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> "每周更新时间"
        SPANISH -> "Actualizado en"
        else -> "Updated every"
    }

    fun getWeekdays(dayIndexes: List<Int>): String {
        val weekdays = dateFormatSymbols.weekdays
            .filter(String::isNotBlank)
            .map { dayName -> dayName.replaceFirstChar { it.uppercase(locale) } }

        return dayIndexes.joinToString { weekdays[it] }
    }

    fun localize(value: Int) = value.localized

    private val Int.localized: String
        get() = numberFormat.format(this)

    companion object {
        const val CHINESE = "zh"
        const val ENGLISH = "en"
        const val INDONESIAN = "id"
        const val SIMPLIFIED_CHINESE = "zh-Hans"
        const val SPANISH = "es"
        const val FRENCH = "fr"
    }
}
