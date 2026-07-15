package com.musicstream.app.data.remote.extractor
import android.util.Log
import com.musicstream.app.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Response
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class YouTubeExtractor @Inject constructor(
    @Named("baseClient")
    private val okHttpClient: OkHttpClient,
    private val pipedInterceptor: com.musicstream.app.data.remote.interceptor.PipedInstanceInterceptor
) {
    private var isInitialized = false

    // High-Performance Browser Fingerprint Identity Pool
    private val userAgents = listOf(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"
    )

    private fun init() {
        if (!isInitialized) {
            NewPipe.init(object : Downloader() {
                override fun execute(request: org.schabi.newpipe.extractor.downloader.Request): Response {
                    val method = request.httpMethod()
                    val url = request.url()
                    val body = if (method == "POST") {
                        request.dataToSend()?.toRequestBody(null)
                    } else null

                    // Consistent Identity mirroring the player's network stack
                    val selectedUA = userAgents[0]

                    val okRequest = Request.Builder()
                        .url(url)
                        .method(method, body)
                        .apply {
                            // Copy original headers first
                            request.headers().forEach { (key, values) ->
                                values.forEach { value -> addHeader(key, value) }
                            }
                            // Then override with our stable identity
                            header("User-Agent", selectedUA)
                            header("Accept", "*/*")
                            header("Accept-Language", "en-US,en;q=0.9")
                            header("X-Goog-Api-Format-Version", "2")
                        }
                        .build()

                    val okResponse = try { 
                        okHttpClient.newCall(okRequest).execute()
                    } catch (e: Exception) {
                        Log.e("YouTubeExtractor", "Network failure in NewPipe Downloader: ${e.message}")
                        pipedInterceptor.rotateInstance()
                        throw e
                    }

                    val responseCode = okResponse.code

                    if (responseCode == 429 || responseCode == 403 || responseCode >= 500) {
                        Log.e("YouTubeExtractor", "YouTube Blocked Request (Code: $responseCode). Rotating Server Identity...")
                        pipedInterceptor.rotateInstance()
                    }

                    return Response(
                        responseCode,
                        okResponse.message,
                        okResponse.headers.toMultimap(),
                        okResponse.body?.string(),
                        okResponse.request.url.toString()
                    )
                }
            })

            isInitialized = true
        }
    }

    suspend fun search(query: String): List<Song> = withContext(Dispatchers.IO) {
        init()
        try {
            val service = ServiceList.YouTube
            val extractor = service.getSearchExtractor(query)
            extractor.fetchPage()
            
            extractor.initialPage.items
                .filterIsInstance<org.schabi.newpipe.extractor.stream.StreamInfoItem>()
                .map { item ->
                    val title = item.name ?: "Unknown"
                    val videoId = item.url.substringAfter("v=")
                    val thumbnail = item.thumbnails.maxByOrNull { it.width }?.url
                        ?: "https://img.youtube.com/vi/$videoId/hqdefault.jpg"

                    Song(
                        id = videoId,
                        title = title,
                        artist = item.uploaderName ?: "YouTube",
                        coverUrl = thumbnail.replace("http://", "https://"),
                        streamUrl = "youtube://$videoId",
                        duration = item.duration * 1000L,
                        gradientIndex = (title.hashCode() and Integer.MAX_VALUE) % 5
                    )
                }
        } catch (e: Exception) {
            Log.e("YouTubeExtractor", "Search failed: ${e.message}")
            pipedInterceptor.rotateInstance()
            emptyList()
        }
    }

    suspend fun getAudioUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        init()
        Log.d("YouTubeExtractor", "Resolving audio URL for: $videoId")

        // Attempt resolution with multiple retries and instance rotation
        for (attempt in 1..3) {
            try {
                val service = ServiceList.YouTube
                val streamInfo = StreamInfo.getInfo(service, "https://www.youtube.com/watch?v=$videoId")

                // Priority: Opus 160kbps -> M4A 128kbps -> Max Quality
                val streams = streamInfo.audioStreams
                val bestStream = streams.find { it.itag == 251 }
                    ?: streams.find { it.itag == 140 }
                    ?: streams.maxByOrNull { it.bitrate }

                val url = bestStream?.content
                if (url != null && url.startsWith("http")) {
                    Log.d("YouTubeExtractor", "Resolved URL on attempt $attempt: ${url.take(50)}...")
                    return@withContext url
                }

                Log.w("YouTubeExtractor", "Attempt $attempt failed: No valid stream found")
                pipedInterceptor.rotateInstance()
            } catch (e: Exception) {
                Log.e("YouTubeExtractor", "Attempt $attempt failed for $videoId: ${e.message}")
                pipedInterceptor.rotateInstance()
            }
            
            if (attempt < 3) delay(500L * attempt) // Progressive delay
        }
        
        Log.e("YouTubeExtractor", "All resolution attempts failed for $videoId")
        null
    }
}
