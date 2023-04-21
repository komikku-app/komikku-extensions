package eu.kanade.tachiyomi.extension.zh.dmzj

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.ListPreference
import androidx.preference.MultiSelectListPreference
import androidx.preference.SwitchPreferenceCompat

// Legacy preferences:
// "apiRatelimitPreference" -> 1..10 default "5"
// "imgCDNRatelimitPreference" -> 1..10 default "5"

fun getPreferencesInternal(context: Context, preferences: SharedPreferences) = arrayOf(

    ListPreference(context).apply {
        key = IMAGE_QUALITY_PREF
        title = "图片质量"
        summary = "%s\n如果选择“只用原图”可能会有部分图片无法加载。"
        entries = arrayOf("优先原图", "只用原图", "只用低清")
        entryValues = arrayOf(AUTO_RES, ORIGINAL_RES, LOW_RES)
        setDefaultValue(AUTO_RES)
    },

    SwitchPreferenceCompat(context).apply {
        key = CHAPTER_COMMENTS_PREF
        title = "章末吐槽页"
        summary = "修改后，已加载的章节需要清除章节缓存才能生效。"
        setDefaultValue(false)
    },

    SwitchPreferenceCompat(context).apply {
        key = MULTI_GENRE_FILTER_PREF
        title = "分类筛选时允许勾选多个题材"
        summary = "可以更精细地筛选出同时符合多个题材的作品。"
        setDefaultValue(false)
    },

    MultiSelectListPreference(context).setupIdList(
        LICENSED_LIST_PREF,
        "特殊漫画 ID 列表 (1)",
        preferences.licensedList.toTypedArray(),
    ),

    MultiSelectListPreference(context).setupIdList(
        HIDDEN_LIST_PREF,
        "特殊漫画 ID 列表 (2)",
        preferences.hiddenList.toTypedArray(),
    ),
)

val SharedPreferences.imageQuality get() = getString(IMAGE_QUALITY_PREF, AUTO_RES)!!

val SharedPreferences.showChapterComments get() = getBoolean(CHAPTER_COMMENTS_PREF, false)

val SharedPreferences.isMultiGenreFilter get() = getBoolean(MULTI_GENRE_FILTER_PREF, false)

val SharedPreferences.licensedList: Set<String> get() = getStringSet(LICENSED_LIST_PREF, emptySet())!!
val SharedPreferences.hiddenList: Set<String> get() = getStringSet(HIDDEN_LIST_PREF, emptySet())!!

fun SharedPreferences.addLicensed(id: String) = addToSet(LICENSED_LIST_PREF, id, licensedList)
fun SharedPreferences.addHidden(id: String) = addToSet(HIDDEN_LIST_PREF, id, hiddenList)

private fun MultiSelectListPreference.setupIdList(
    key: String,
    title: String,
    values: Array<String>,
): MultiSelectListPreference {
    this.key = key
    this.title = title
    summary = "如果漫画网页版可以正常访问，但是应用内章节目录加载异常，可以点开列表删除记录。" +
        "删除方法是【取消勾选】要删除的 ID 再点击确定，勾选的项目会保留。" +
        "如果点开为空，就表示没有记录。刷新漫画页并展开简介即可查看 ID。"
    entries = values
    entryValues = values
    setDefaultValue(emptySet<Nothing>())
    return this
}

@Synchronized
private fun SharedPreferences.addToSet(key: String, id: String, oldSet: Set<String>) {
    if (id in oldSet) return
    val newSet = HashSet<String>((oldSet.size + 1) * 2)
    newSet.addAll(oldSet)
    newSet.add(id)
    edit().putStringSet(key, newSet).apply()
}

fun SharedPreferences.migrate(): SharedPreferences {
    val currentVersion = 1
    val versionPref = "version"
    val oldVersion = getInt(versionPref, 0)
    if (oldVersion >= currentVersion) return this
    val editor = edit()
    if (oldVersion < 1) {
        editor.remove(LICENSED_LIST_PREF).remove(HIDDEN_LIST_PREF)
    }
    editor.putInt(versionPref, currentVersion).apply()
    return this
}

private const val IMAGE_QUALITY_PREF = "imageSourcePreference"
const val AUTO_RES = "PREFER_ORIG_RES"
const val ORIGINAL_RES = "ORIG_RES_ONLY"
const val LOW_RES = "LOW_RES_ONLY"

private const val CHAPTER_COMMENTS_PREF = "chapterComments"
private const val MULTI_GENRE_FILTER_PREF = "multiGenreFilter"

private const val LICENSED_LIST_PREF = "licensedList"
private const val HIDDEN_LIST_PREF = "hiddenList"
