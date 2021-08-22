package eu.kanade.tachiyomi.extension.en.yaoihentai

import eu.kanade.tachiyomi.annotations.Nsfw
import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

@Nsfw
class YaoiHentai : Madara("Yaoi Hentai", "https://yaoihentai.me", "en", dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.US))
