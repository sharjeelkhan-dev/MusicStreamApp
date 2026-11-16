package com.musicstream.app.presentation.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.musicstream.app.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Composable
fun LyricsShareCardDialog(
    song: Song,
    lyricQuote: String,
    primaryColor: Color,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isSharing by remember { mutableStateOf(false) }

    val gradientColors = remember(primaryColor) {
        listOf(
            primaryColor,
            primaryColor.copy(alpha = 0.75f),
            Color(0xFF141414)
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 620.dp)
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Share Lyric Card",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Lyric Story Card Preview
                Card(
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(gradientColors))
                            .padding(20.dp)
                    ) {
                        Column {
                            Icon(
                                imageVector = Icons.Default.FormatQuote,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = lyricQuote.ifBlank { "🎵 \"${song.title}\" by ${song.artist}" },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                lineHeight = 28.sp,
                                maxLines = 5,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = song.coverUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = song.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${song.artist} • MusicStream",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White.copy(alpha = 0.8f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (!isSharing) {
                            isSharing = true
                            coroutineScope.launch {
                                generateAndShareLyricCard(context, song, lyricQuote, primaryColor)
                                isSharing = false
                                onDismiss()
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    shape = RoundedCornerShape(16.dp),
                    enabled = !isSharing
                ) {
                    if (isSharing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Share to Instagram / WhatsApp",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}

private suspend fun generateAndShareLyricCard(
    context: Context,
    song: Song,
    lyricQuote: String,
    primaryColor: Color
) {
    withContext(Dispatchers.IO) {
        try {
            val width = 1080
            val height = 1350
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val primaryInt = primaryColor.toArgb()
            val gradient = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(primaryInt, manipulateColor(primaryInt, 0.75f), android.graphics.Color.parseColor("#141414")),
                floatArrayOf(0f, 0.65f, 1f),
                Shader.TileMode.CLAMP
            )

            val bgPaint = Paint().apply {
                shader = gradient
                isAntiAlias = true
            }
            val cardRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
            canvas.drawRoundRect(cardRect, 48f, 48f, bgPaint)

            // Quote Symbol
            val quotePaint = TextPaint().apply {
                color = android.graphics.Color.WHITE
                alpha = 180
                textSize = 120f
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("“", 80f, 160f, quotePaint)

            // Lyric Text Layout
            val lyricText = if (lyricQuote.isNotBlank()) lyricQuote else "🎵 \"${song.title}\" by ${song.artist}"
            val textPaint = TextPaint().apply {
                color = android.graphics.Color.WHITE
                textSize = 62f
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val textWidth = width - 160
            val staticLayout = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                StaticLayout.Builder.obtain(lyricText, 0, lyricText.length, textPaint, textWidth)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setLineSpacing(0f, 1.25f)
                    .build()
            } else {
                @Suppress("DEPRECATION")
                StaticLayout(lyricText, textPaint, textWidth, Layout.Alignment.ALIGN_NORMAL, 1.25f, 0f, false)
            }

            canvas.save()
            canvas.translate(80f, 220f)
            staticLayout.draw(canvas)
            canvas.restore()

            // Fetch Cover Image Bitmap via Coil
            val coverBitmap = loadCoverBitmap(context, song.coverUrl)

            // Draw Cover Image + Details at bottom
            val bottomY = height - 220f
            if (coverBitmap != null) {
                val coverRect = RectF(80f, bottomY, 80f + 140f, bottomY + 140f)
                val coverPaint = Paint().apply { isAntiAlias = true }
                canvas.drawBitmap(coverBitmap, null, coverRect, coverPaint)
            }

            // Song Title & Artist
            val titlePaint = TextPaint().apply {
                color = android.graphics.Color.WHITE
                textSize = 44f
                isAntiAlias = true
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val textX = if (coverBitmap != null) 250f else 80f
            canvas.drawText(truncateText(song.title, titlePaint, (width - textX - 60f)), textX, bottomY + 55f, titlePaint)

            val artistPaint = TextPaint().apply {
                color = android.graphics.Color.argb(220, 255, 255, 255)
                textSize = 34f
                isAntiAlias = true
            }
            val subText = "${song.artist} • MusicStream"
            canvas.drawText(truncateText(subText, artistPaint, (width - textX - 60f)), textX, bottomY + 115f, artistPaint)

            // Save Bitmap to Cache
            val cachePath = File(context.cacheDir, "shared_images")
            cachePath.mkdirs()
            val imageFile = File(cachePath, "shared_lyric_card.png")
            val stream = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val contentUri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )

            withContext(Dispatchers.Main) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    putExtra(Intent.EXTRA_TEXT, "\"${lyricQuote.ifBlank { song.title }}\" — ${song.artist}\nShared via MusicStream 🎵")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Lyric Card"))
            }
        } catch (e: Exception) {
            Log.e("LyricsShareCard", "Error generating lyric card image", e)
        }
    }
}

private suspend fun loadCoverBitmap(context: Context, url: String): Bitmap? {
    if (url.isBlank()) return null
    return try {
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false)
            .build()
        val result = loader.execute(request)
        if (result is SuccessResult) {
            (result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
        } else null
    } catch (e: Exception) {
        null
    }
}

private fun manipulateColor(color: Int, factor: Float): Int {
    val a = android.graphics.Color.alpha(color)
    val r = (android.graphics.Color.red(color) * factor).toInt().coerceIn(0, 255)
    val g = (android.graphics.Color.green(color) * factor).toInt().coerceIn(0, 255)
    val b = (android.graphics.Color.blue(color) * factor).toInt().coerceIn(0, 255)
    return android.graphics.Color.argb(a, r, g, b)
}

private fun truncateText(text: String, paint: TextPaint, maxWidth: Float): String {
    if (paint.measureText(text) <= maxWidth) return text
    var truncated = text
    while (truncated.length > 3 && paint.measureText("$truncated...") > maxWidth) {
        truncated = truncated.dropLast(1)
    }
    return "$truncated..."
}
