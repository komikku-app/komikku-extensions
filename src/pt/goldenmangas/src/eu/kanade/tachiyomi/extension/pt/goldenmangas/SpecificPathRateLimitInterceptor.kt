package eu.kanade.tachiyomi.extension.pt.goldenmangas

import android.os.SystemClock
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import java.util.ArrayDeque
import java.util.concurrent.Semaphore
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
class SpecificPathRateLimitInterceptor(
    private val path: String,
    private val permits: Int,
    period: Duration,
) : Interceptor {

    private val requestQueue = ArrayDeque<Long>(permits)
    private val rateLimitMillis = period.inWholeMilliseconds
    private val fairLock = Semaphore(1, true)

    override fun intercept(chain: Interceptor.Chain): Response {
        val call = chain.call()
        if (call.isCanceled()) throw IOException("Canceled")

        val request = chain.request()

        if (!request.url.encodedPath.startsWith(path)) {
            return chain.proceed(request)
        }

        try {
            fairLock.acquire()
        } catch (e: InterruptedException) {
            throw IOException(e)
        }

        val requestQueue = this.requestQueue
        val timestamp: Long

        try {
            synchronized(requestQueue) {
                while (requestQueue.size >= permits) { // queue is full, remove expired entries
                    val periodStart = SystemClock.elapsedRealtime() - rateLimitMillis
                    var hasRemovedExpired = false
                    while (requestQueue.isEmpty().not() && requestQueue.first <= periodStart) {
                        requestQueue.removeFirst()
                        hasRemovedExpired = true
                    }
                    if (call.isCanceled()) {
                        throw IOException("Canceled")
                    } else if (hasRemovedExpired) {
                        break
                    } else {
                        try { // wait for the first entry to expire, or notified by cached response
                            (requestQueue as Object).wait(requestQueue.first - periodStart)
                        } catch (_: InterruptedException) {
                            continue
                        }
                    }
                }

                // add request to queue
                timestamp = SystemClock.elapsedRealtime()
                requestQueue.addLast(timestamp)
            }
        } finally {
            fairLock.release()
        }

        val response = chain.proceed(request)
        if (response.networkResponse == null) { // response is cached, remove it from queue
            synchronized(requestQueue) {
                if (requestQueue.isEmpty() || timestamp < requestQueue.first) return@synchronized
                requestQueue.removeFirstOccurrence(timestamp)
                (requestQueue as Object).notifyAll()
            }
        }

        return response
    }
}

fun OkHttpClient.Builder.rateLimitPath(
    path: String,
    permits: Int,
    period: Duration = 1.seconds,
) = addInterceptor(SpecificPathRateLimitInterceptor(path, permits, period))
