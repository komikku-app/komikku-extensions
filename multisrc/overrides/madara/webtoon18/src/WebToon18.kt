package eu.kanade.tachiyomi.extension.en.webtoon18

import eu.kanade.tachiyomi.annotations.Nsfw
import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

@Nsfw
class WebToon18 : Madara("WebToon18", "http://webtoon18.net", "en", dateFormat = SimpleDateFormat("d MMMM, yyyy", Locale.US))
