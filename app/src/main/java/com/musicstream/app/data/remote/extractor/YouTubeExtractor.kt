package com.musicstream.app.data.remote.extractor

import com.musicstream.app.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.services.youtube.YoutubeService
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamType
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Response
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YouTubeExtractor @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private var isInitialized = false

    private fun init() {
        if (!isInitialized) {
            NewPipe.init(object : Downloader() {
                override fun execute(request: org.schabi.newpipe.extractor.downloader.Request): Response {
                    val method = request.httpMethod()
                    val body = if (method == "POST") {
                        val requestBody = request.dataToSend() ?: byteArrayOf()
                        requestBody.toRequestBody(null)
                    } else null

                    val okRequest = Request.Builder()
                        .url(request.url())
                        .method(method, body)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36")
                        .apply {
                            request.headers().forEach { (key, values) ->
                                values.forEach { value -> addHeader(key, value) }
                            }
                        }
                        .build()

                    val okResponse = okHttpClient.newCall(okRequest).execute()
                    return Response(
                        okResponse.code,
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
                    val secureThumbnail = thumbnail.replace("http://", "https://")
                    Song(
                        id = videoId,
                        title = title,
                        artist = item.uploaderName ?: "YouTube",
                        coverUrl = secureThumbnail,
                        streamUrl = "youtube://$videoId",
                        duration = item.duration * 1000L,
                        gradientIndex = (title.hashCode() and Integer.MAX_VALUE) % 5
                    )
                }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getAudioUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        init()
        try {
            val service = ServiceList.YouTube
            val streamInfo = StreamInfo.getInfo(service, "https://www.youtube.com/watch?v=$videoId")
            
            // Priority: itag 251 (Opus 160kbps) -> itag 140 (M4A 128kbps) -> Max Bitrate
            val streams = streamInfo.audioStreams
            
            val bestStream = streams.find { it.itag == 251 } // 160kbps Opus
                ?: streams.find { it.itag == 140 } // 128kbps M4A
                ?: streams.maxByOrNull { it.bitrate }

            android.util.Log.d("YouTubeExtractor", "Selected itag: ${bestStream?.itag} with bitrate: ${bestStream?.bitrate}")
            
            bestStream?.content
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
