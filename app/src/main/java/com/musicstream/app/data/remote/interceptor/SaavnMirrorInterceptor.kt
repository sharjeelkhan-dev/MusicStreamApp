package com.musicstream.app.data.remote.interceptor

import android.util.Log
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SaavnMirrorInterceptor @Inject constructor() : Interceptor {

    // Working & Active JioSaavn API Mirrors List
    private val mirrors = listOf(
        "https://saavn.me/api",
        "https://jiosaavn-api-liard.vercel.app/api",
        "https://saavn.dev/api",
        "https://jiosaavn-api.vercel.app"
    )

    private val currentMirrorIndex = AtomicInteger(0)
    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"

    fun rotateMirror() {
        val nextIndex = (currentMirrorIndex.get() + 1) % mirrors.size
        currentMirrorIndex.set(nextIndex)
        Log.d("SaavnRotation", "Rotated to active mirror: ${mirrors[currentMirrorIndex.get()]}")
    }

    fun getCurrentMirror(): String = mirrors[currentMirrorIndex.get()]

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val originalHost = originalRequest.url.host

        // Pass-through for Audio streams / Images CDNs
        if (originalHost.contains("saavncdn") ||
            originalHost.contains("google") ||
            originalHost.contains("googlevideo") ||
            originalHost.contains("unsplash")) {
            return chain.proceed(originalRequest)
        }

        var lastException: Exception? = null
        val maxAttempts = mirrors.size

        // Normalize extraction: trim trailing and leading 'api' or slashes
        var rawPath = originalRequest.url.encodedPath.trimStart('/')
        if (rawPath.startsWith("api/")) {
            rawPath = rawPath.removePrefix("api/")
        } else if (rawPath == "api") {
            rawPath = ""
        }

        val queryParams = originalRequest.url.encodedQuery?.let { "?$it" } ?: ""

        for (attempt in 0 until maxAttempts) {
            val mirrorBase = getCurrentMirror().trimEnd('/')

            // Build reconstructed URL correctly matching mirror structure
            val fullUrlString = if (rawPath.isNotBlank()) {
                "$mirrorBase/$rawPath$queryParams"
            } else {
                "$mirrorBase$queryParams"
            }

            val parsedUrl = fullUrlString.toHttpUrlOrNull()

            if (parsedUrl == null) {
                rotateMirror()
                continue
            }

            try {
                val newRequest = originalRequest.newBuilder()
                    .url(parsedUrl)
                    .header("User-Agent", userAgent)
                    .header("Accept", "application/json, text/plain, */*")
                    .header("Origin", "https://${parsedUrl.host}")
                    .header("Referer", "https://${parsedUrl.host}/")
                    .build()

                Log.d("SaavnInterceptor", "Executing Request [Attempt $attempt]: $fullUrlString")
                val response = chain.proceed(newRequest)

                if (response.isSuccessful) {
                    Log.d("SaavnInterceptor", "Success Response from: ${parsedUrl.host}")
                    return response
                }

                Log.w("SaavnInterceptor", "HTTP Failure ${response.code} from ${parsedUrl.host}. Switching Mirror...")
                response.close()
                rotateMirror()

            } catch (e: Exception) {
                Log.e("SaavnInterceptor", "Connection Exception on ${parsedUrl.host}: ${e.message}")
                lastException = e
                rotateMirror()
            }
        }

        // Final fallback: proceed original request as last resort instead of breaking execution
        return try {
            chain.proceed(originalRequest)
        } catch (e: Exception) {
            throw lastException ?: IOException("All API mirrors failed to reach Saavn servers.")
        }
    }
}