package eu.kanade.tachiyomi.multisrc.bilibili

class BilibiliIntl(lang: String) {

    val statusLabel: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> "进度"
        else -> "Status"
    }

    val sortLabel: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> "排序"
        INDONESIAN -> "Urutkan dengan"
        else -> "Sort by"
    }

    val genreLabel: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> "题材"
        else -> "Genre"
    }

    val areaLabel: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> "地区"
        else -> "Area"
    }

    val priceLabel: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> "收费"
        INDONESIAN -> "Harga"
        else -> "Price"
    }

    val episodePrefix: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> ""
        else -> "Ep. "
    }

    val hasPaidChaptersWarning: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE ->
            "${Bilibili.EMOJI_WARNING} 此漫画的付费章节已从章节列表中过滤。如果您已购买章节，请在 WebView " +
                "登录并刷新章节列表以阅读已购章节。"
        else ->
            "${Bilibili.EMOJI_WARNING} WARNING: This series has paid chapters that were filtered " +
                "out from the chapter list. If you have already bought and have any in your " +
                "account, sign in through WebView and refresh the chapter list to read them."
    }

    val imageQualityPrefTitle: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> "章节图片质量"
        INDONESIAN -> "Kualitas gambar"
        else -> "Chapter image quality"
    }

    val imageQualityPrefEntries: Array<String> = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> arrayOf("原图", "高", "低")
        else -> arrayOf("Raw", "HD", "SD")
    }

    val imageFormatPrefTitle: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> "章节图片格式"
        INDONESIAN -> "Format gambar"
        else -> "Chapter image format"
    }

    val sortInterest: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> "为你推荐"
        INDONESIAN -> "Kamu Mungkin Suka"
        else -> "Interest"
    }

    val sortPopular: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> "人气推荐"
        INDONESIAN -> "Populer"
        else -> "Popular"
    }

    val sortUpdated: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> "更新时间"
        INDONESIAN -> "Terbaru"
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
        else -> "All"
    }

    val statusOngoing: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> "连载中"
        INDONESIAN -> "Berlangsung"
        else -> "Ongoing"
    }

    val statusComplete: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> "已完结"
        INDONESIAN -> "Tamat"
        else -> "Completed"
    }

    val priceAll: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> "全部"
        INDONESIAN -> "Semua"
        else -> "All"
    }

    val priceFree: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> "免费"
        INDONESIAN -> "Bebas"
        else -> "Free"
    }

    val pricePaid: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> "付费"
        INDONESIAN -> "Dibayar"
        else -> "Paid"
    }

    val priceWaitForFree: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> "等就免费"
        else -> "Wait for free"
    }

    val failedToRefreshToken: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> "无法刷新令牌。请打开 WebView 修正错误。"
        else -> "Failed to refresh the token. Open the WebView to fix this error."
    }

    val failedToGetCredential: String = when (lang) {
        CHINESE, SIMPLIFIED_CHINESE -> "无法获取阅读章节所需的凭证。"
        else -> "Failed to get the credential to read the chapter."
    }

    companion object {
        const val CHINESE = "zh"
        const val ENGLISH = "en"
        const val INDONESIAN = "id"
        const val SIMPLIFIED_CHINESE = "zh-Hans"
    }
}
