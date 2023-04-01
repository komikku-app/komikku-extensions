package eu.kanade.tachiyomi.extension.zh.zerobyw

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.preference.EditTextPreference
import eu.kanade.tachiyomi.network.GET
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException

private const val DEFAULT_BASE_URL = "http://www.zerobyw4090.com"

private const val BASE_URL_PREF = "ZEROBYW_BASEURL"
private const val DEFAULT_BASE_URL_PREF = "defaultBaseUrl"
private const val JSON_URL = "https://cdn.jsdelivr.net/gh/zerozzz123456/1/url.json"

var SharedPreferences.baseUrl: String
    get() = getString(BASE_URL_PREF, DEFAULT_BASE_URL)!!
    set(value) = edit().putString(BASE_URL_PREF, value).apply()

fun SharedPreferences.clearOldBaseUrl(): SharedPreferences {
    if (getString(DEFAULT_BASE_URL_PREF, "")!! == DEFAULT_BASE_URL) return this
    edit()
        .remove(BASE_URL_PREF)
        .putString(DEFAULT_BASE_URL_PREF, DEFAULT_BASE_URL)
        .apply()
    return this
}

fun getBaseUrlPreference(context: Context) = EditTextPreference(context).apply {
    key = BASE_URL_PREF
    title = "网址"
    summary = "正常情况下会自动更新。" +
        "如果出现错误，请在 GitHub 上报告，并且可以在 $JSON_URL 找到最新网址手动填写。" +
        "填写时按照 $DEFAULT_BASE_URL 格式。"
    setDefaultValue(DEFAULT_BASE_URL)

    setOnPreferenceChangeListener { _, newValue ->
        try {
            checkBaseUrl(newValue as String)
            true
        } catch (_: Throwable) {
            Toast.makeText(context, "网址格式错误", Toast.LENGTH_LONG).show()
            false
        }
    }
}

fun ciGetUrl(client: OkHttpClient): String {
    println("[Zerobyw] CI detected, getting latest URL...")
    return try {
        val response = client.newCall(GET(JSON_URL)).execute()
        parseJson(response).also { println("[Zerobyw] Latest URL is $it") }
    } catch (e: Throwable) {
        println("[Zerobyw] Failed to fetch latest URL")
        e.printStackTrace()
        DEFAULT_BASE_URL
    }
}

private fun parseJson(response: Response): String {
    val string = response.body.string()
    val json: HashMap<String, String> = Json.decodeFromString(string)
    val newUrl = json["url"]!!.trim()
    checkBaseUrl(newUrl)
    return newUrl
}

private fun checkBaseUrl(url: String) {
    require(url == url.trim() && !url.endsWith('/'))
    val pathSegments = url.toHttpUrl().pathSegments
    require(pathSegments.size == 1 && pathSegments[0].isEmpty())
}

class UpdateUrlInterceptor(
    private val preferences: SharedPreferences,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url
        val baseUrl = preferences.baseUrl
        if (!url.toString().startsWith(baseUrl)) return chain.proceed(request)

        val failedResult = try {
            val response = chain.proceed(request)
            if (response.code < 500) return response
            Result.success(response)
        } catch (e: IOException) {
            if (chain.call().isCanceled()) throw e
            Result.failure(e)
        }

        val newUrl = try {
            val response = chain.proceed(GET(JSON_URL))
            val newUrl = parseJson(response)
            require(newUrl != baseUrl)
            newUrl
        } catch (e: Throwable) {
            return failedResult.getOrThrow()
        }

        preferences.baseUrl = newUrl
        val (scheme, host) = newUrl.split("://")
        val retryUrl = url.newBuilder().scheme(scheme).host(host).build()
        val retryRequest = request.newBuilder().url(retryUrl).build()
        return chain.proceed(retryRequest)
    }
}
