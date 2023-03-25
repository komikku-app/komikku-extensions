package eu.kanade.tachiyomi.extension.zh.jinmantiantang

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.EditTextPreference
import androidx.preference.ListPreference

internal fun getPreferenceList(context: Context) = arrayOf(
    ListPreference(context).apply {
        key = MAINSITE_RATELIMIT_PREF
        title = "在限制时间内（下个设置项）允许的请求数量。"
        entries = Array(10) { "${it + 1}" }
        entryValues = Array(10) { "${it + 1}" }
        summary = "此值影响更新书架时发起连接请求的数量。调低此值可能减小IP被屏蔽的几率，但加载速度也会变慢。需要重启软件以生效。\n当前值：%s"

        setDefaultValue(MAINSITE_RATELIMIT_PREF_DEFAULT)
    },

    ListPreference(context).apply {
        key = MAINSITE_RATELIMIT_PERIOD
        title = "限制持续时间。单位秒"
        entries = Array(60) { "${it + 1}" }
        entryValues = Array(60) { "${it + 1}" }
        summary = "此值影响更新书架时请求的间隔时间。调大此值可能减小IP被屏蔽的几率，但更新时间也会变慢。需要重启软件以生效。\n当前值：%s"

        setDefaultValue(MAINSITE_RATELIMIT_PERIOD_DEFAULT)
    },

    ListPreference(context).apply {
        val count = SITE_ENTRIES_ARRAY.size
        key = USE_MIRROR_URL_PREF
        title = "使用镜像网址"
        entries = Array(count) { "${SITE_ENTRIES_ARRAY_DESCRIPTION[it]} (${SITE_ENTRIES_ARRAY[it]})" }
        entryValues = Array(count) { "$it" }.apply { this[count - 1] = "-1" }
        summary = "%s\n重启后生效。"

        setDefaultValue("0")
    },

    EditTextPreference(context).apply {
        key = OVERRIDE_BASE_URL_PREF
        title = "自定义网址"
        summary = "需要在上一个设置选择“自定义”，重启后生效。" +
            "不需要输入 https:// 前缀。最新网址可在 jmcomic1.bet 找到。"
    },

    EditTextPreference(context).apply {
        key = BLOCK_PREF
        title = "屏蔽词列表"
        setDefaultValue(
            "// 例如 \"YAOI cos 扶他 毛絨絨 獵奇 韩漫 韓漫\", " +
                "关键词之间用空格分离, 大小写不敏感, \"//\"后的字符会被忽略",
        )
        dialogTitle = "关键词列表"
    },
)

val SharedPreferences.baseUrl: String
    get() {
        val list = SITE_ENTRIES_ARRAY
        val index = getString(USE_MIRROR_URL_PREF, "0")!!.toInt()
            .coerceAtMost(list.size - 1)
        return if (index == -1) {
            getString(OVERRIDE_BASE_URL_PREF, list[0])!!
        } else {
            list[index]
        }
    }

internal const val BLOCK_PREF = "BLOCK_GENRES_LIST"

internal const val MAINSITE_RATELIMIT_PREF = "mainSiteRateLimitPreference"
internal const val MAINSITE_RATELIMIT_PREF_DEFAULT = 1.toString()

internal const val MAINSITE_RATELIMIT_PERIOD = "mainSiteRateLimitPeriodPreference"
internal const val MAINSITE_RATELIMIT_PERIOD_DEFAULT = 3.toString()

private const val USE_MIRROR_URL_PREF = "useMirrorWebsitePreference"
private const val OVERRIDE_BASE_URL_PREF = "overrideBaseUrl"

private val SITE_ENTRIES_ARRAY_DESCRIPTION = arrayOf(
    "主站",
    "海外分流",
    "东南亚线路1",
    "东南亚线路2",
    "中国大陆线路1",
    "中国大陆线路2",
    "中国大陆线路3",
    "自定义", // -1
)

// List is based on https://jmcomic1.bet/
// Please also update AndroidManifest
private val SITE_ENTRIES_ARRAY = arrayOf(
    "18comic.vip",
    "18comic.org",
    "jmcomic.me",
    "jmcomic1.me",
    "jmcomic1.group",
    "jmcomic2.group",
    "jm-comic.cc",
    "自定义", // -1
)
