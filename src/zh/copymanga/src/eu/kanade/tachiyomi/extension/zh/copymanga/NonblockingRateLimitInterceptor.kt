package eu.kanade.tachiyomi.extension.zh.copymanga

import android.os.SystemClock
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

// See https://github.com/tachiyomiorg/tachiyomi/pull/7389
internal class NonblockingRateLimitInterceptor(
    private val permits: Int,
    period: Long = 1,
    unit: TimeUnit = TimeUnit.SECONDS,
) : Interceptor {

    private val requestQueue = ArrayList<Long>(permits)
    private val rateLimitMillis = unit.toMillis(period)

    override fun intercept(chain: Interceptor.Chain): Response {
        // Ignore canceled calls, otherwise they would jam the queue
        if (chain.call().isCanceled()) {
            throw IOException()
        }

        synchronized(requestQueue) {
            val now = SystemClock.elapsedRealtime()
            val waitTime = if (requestQueue.size < permits) {
                0
            } else {
                val oldestReq = requestQueue[0]
                val newestReq = requestQueue[permits - 1]

                if (newestReq - oldestReq > rateLimitMillis) {
                    0
                } else {
                    oldestReq + rateLimitMillis - now // Remaining time
                }
            }

            // Final check
            if (chain.call().isCanceled()) {
                throw IOException()
            }

            if (requestQueue.size == permits) {
                requestQueue.removeAt(0)
            }
            if (waitTime > 0) {
                requestQueue.add(now + waitTime)
                Thread.sleep(waitTime) // Sleep inside synchronized to pause queued requests
            } else {
                requestQueue.add(now)
            }
        }

        return chain.proceed(chain.request())
    }
}
