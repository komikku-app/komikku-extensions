package eu.kanade.tachiyomi.extension.ko.newtoki

import android.util.Log
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.util.asJsoup
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Source changes domain names every few days (e.g. newtoki31.net to newtoki32.net)
 * The domain name was newtoki32 on 2019-11-14, this attempts to match the rate at which the domain changes
 *
 * Since 2020-09-20, They changed manga side to Manatoki.
 * It was merged after shutdown of ManaMoa.
 * This is by the head of Manamoa, as they decided to move to Newtoki.
 *
 * Updated on 2023-02-10, see `domain_log.md`.
 * To avoid going too fast and to utilize redirections,
 * the number is decremented by 1 initially,
 * and increments every 8 days which is a bit slower than the average.
 */
val fallbackDomainNumber get() = (217 - 1) + ((System.currentTimeMillis() - 1675818000_000) / 691200_000).toInt()

var domainNumber = ""
    get() {
        val currentValue = field
        if (currentValue.isNotEmpty()) return currentValue

        val prefValue = newTokiPreferences.domainNumber
        if (prefValue.isNotEmpty()) {
            field = prefValue
            return prefValue
        }

        val fallback = fallbackDomainNumber.toString()
        domainNumber = fallback
        return fallback
    }
    set(value) {
        for (preference in arrayOf(manaTokiPreferences, newTokiPreferences)) {
            preference.domainNumber = value
        }
        field = value
    }

object DomainInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val response = try {
            chain.proceed(request)
        } catch (e: IOException) {
            if (chain.call().isCanceled()) throw e
            Log.e("NewToki", "failed to fetch ${request.url}", e)

            val newDomainNumber = try {
                val document = chain.proceed(GET("https://t.me/s/newtoki5")).asJsoup()
                val description = document.select("a[href^=https://newtoki]").last()!!.attr("href")
                numberRegex.find(description)!!.value
            } catch (_: Throwable) {
                fallbackDomainNumber
                    .also { if (it <= domainNumber.toInt()) throw e }
                    .toString()
            }
            domainNumber = newDomainNumber

            val url = request.url
            val newHost = numberRegex.replaceFirst(url.host, newDomainNumber)
            val newUrl = url.newBuilder().host(newHost).build()
            try {
                chain.proceed(request.newBuilder().url(newUrl).build())
            } catch (e: IOException) {
                Log.e("NewToki", "failed to fetch $newUrl", e)
                throw IOException(editDomainNumber(), e)
            }
        }

        if (response.priorResponse == null) return response

        val newUrl = response.request.url
        if ("captcha" in newUrl.toString()) throw IOException(solveCaptcha())

        val newHost = newUrl.host
        if (newHost.startsWith(MANATOKI_PREFIX) || newHost.startsWith(NEWTOKI_PREFIX)) {
            numberRegex.find(newHost)?.run { domainNumber = value }
        }
        return response
    }

    private val numberRegex by lazy { Regex("""\d+""") }
}
