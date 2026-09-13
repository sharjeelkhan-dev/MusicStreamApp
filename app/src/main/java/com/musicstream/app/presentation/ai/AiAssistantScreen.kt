package com.musicstream.app.presentation.ai

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musicstream.app.R
import com.musicstream.app.data.MockData
import com.musicstream.app.domain.model.Song
import com.musicstream.app.presentation.components.SongListItem
import com.musicstream.app.ui.theme.AccentGreen
import com.musicstream.app.ui.theme.AccentPurple
import com.musicstream.app.ui.theme.MusicStreamTheme

@Composable
fun AiAssistantScreen(
    onBackClick: () -> Unit,
    onPlaySong: (Song) -> Unit,
    viewModel: AiAssistantViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var textInput by remember { mutableStateOf("") }

    AiAssistantScreenContent(
        uiState = uiState,
        textInput = textInput,
        onTextInputChange = { textInput = it },
        onSendClick = {
            viewModel.sendMessage(textInput)
            textInput = ""
        },
        onBackClick = onBackClick,
        onPlaySong = onPlaySong,
        onPromptClick = { prompt ->
            viewModel.sendMessage(prompt)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreenContent(
    uiState: AiAssistantUiState,
    textInput: String,
    onTextInputChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onBackClick: () -> Unit,
    onPlaySong: (Song) -> Unit,
    onPromptClick: (String) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size, uiState.isLoading) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AiAssistantHeader(onBackClick)
        },
        bottomBar = {
            AiAssistantInput(
                textInput = textInput,
                onValueChange = onTextInputChange,
                onSendClick = onSendClick,
                isLoading = uiState.isLoading
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (uiState.messages.isEmpty() && !uiState.isLoading) {
                AiEmptyState(onPromptClick = onPromptClick)
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(uiState.messages) { message ->
                        Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                            AiChatBubble(message)
                        }
                    }

                    if (uiState.isLoading) {
                        item {
                            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                                AiLoadingIndicator()
                            }
                        }
                    }

                    if (uiState.recommendedSongs.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Recommended for you ✨",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                        items(uiState.recommendedSongs) { song ->
                            SongListItem(
                                song = song,
                                onSongClick = { onPlaySong(song) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AiAssistantHeader(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier.size(44.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            onClick = onBackClick
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = "AI Assistant",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AccentGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Online",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun AiAssistantInput(
    textInput: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isLoading: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = textInput,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "Ask me anything...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                textStyle = MaterialTheme.typography.bodyMedium,
                maxLines = 4
            )

            FloatingActionButton(
                onClick = onSendClick,
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                containerColor = if (textInput.isNotBlank() && !isLoading) AccentPurple else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onBackground,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun AiChatBubble(message: ChatMessage) {
    val isUser = message.isUser

    if (isUser) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.End
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 20.dp,
                            topEnd = 20.dp,
                            bottomStart = 20.dp,
                            bottomEnd = 4.dp
                        )
                    )
                    .background(Brush.linearGradient(listOf(AccentPurple, Color(0xFF7E3FF2))))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = message.text,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineHeight = 22.sp,
                        fontSize = 15.sp
                    )
                )
            }
        }
    } else {
        // Screenshot exact-match typography rendering
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            val lines = message.text.split("\n")
            lines.forEach { line ->
                val trimmed = line.trim()
                if (trimmed.isNotBlank()) {
                    val isHeading = isHeadlineText(trimmed)
                    Text(
                        text = parseFormattedMarkdown(
                            text = trimmed,
                            isHeading = isHeading,
                            headingColor = AccentPurple,
                            textColor = MaterialTheme.colorScheme.onBackground
                        ),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = if (isHeading) 16.sp else 15.sp,
                            lineHeight = if (isHeading) 24.sp else 23.sp,
                            letterSpacing = 0.15.sp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = if (isHeading) 8.dp else 6.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

/**
 * Detects whether a paragraph line is a title or numbered section like in screenshot
 */
private fun isHeadlineText(line: String): Boolean {
    val clean = line.replace("#", "").trim()
    return (clean.firstOrNull()?.isDigit() == true && clean.contains(".")) ||
            line.startsWith("#") ||
            (line.endsWith(")") && line.contains("("))
}

/**
 * Parses bold Markdown (**text**) and colors headings according to design theme
 */
private fun parseFormattedMarkdown(
    text: String,
    isHeading: Boolean,
    headingColor: Color,
    textColor: Color
): AnnotatedString {
    val cleanText = text.replace(Regex("^#+\\s*"), "")
    val parts = cleanText.split("**")

    return buildAnnotatedString {
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                // Bold inline elements
                withStyle(
                    style = SpanStyle(
                        fontWeight = FontWeight.Bold,
                        color = if (isHeading) headingColor else textColor
                    )
                ) {
                    append(part)
                }
            } else {
                // Regular line text
                withStyle(
                    style = SpanStyle(
                        fontWeight = if (isHeading) FontWeight.Bold else FontWeight.Normal,
                        color = if (isHeading) headingColor else textColor.copy(alpha = 0.88f)
                    )
                ) {
                    append(part)
                }
            }
        }
    }
}

@Composable
fun AiEmptyState(onPromptClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ai_sparkles_icon),
            contentDescription = null,
            tint = AccentPurple.copy(alpha = 0.2f),
            modifier = Modifier.size(100.dp)
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "How can I help you today?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Ask me to find songs, create playlists, or explain lyrics.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(Modifier.height(40.dp))

        Text(
            text = "TRY ASKING",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )

        Spacer(Modifier.height(16.dp))

        val suggestions = listOf(
            "Recommend some 90s hits",
            "Late night relaxing music",
            "Songs for a workout session",
            "Sad indie songs for rainy days"
        )

        suggestions.forEach { prompt ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { onPromptClick(prompt) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        null,
                        tint = AccentPurple,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun AiLoadingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier.size(32.dp),
            shape = CircleShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = AccentPurple
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            "Gemini is thinking...",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AiAssistantScreenEmptyPreview() {
    MusicStreamTheme {
        AiAssistantScreenContent(
            uiState = AiAssistantUiState(),
            textInput = "",
            onTextInputChange = {},
            onSendClick = {},
            onBackClick = {},
            onPlaySong = {},
            onPromptClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AiAssistantScreenChatPreview() {
    MusicStreamTheme {
        AiAssistantScreenContent(
            uiState = AiAssistantUiState(
                messages = listOf(
                    ChatMessage("Hey! Can you recommend some 90s hits?", true),
                    ChatMessage("Here is a list of **90s Hits**:\n1. Top Classic Songs\n• **Song A** - Artist\n• **Song B** - Artist", false)
                ),
                recommendedSongs = MockData.trendingSongs.take(3)
            ),
            textInput = "Thanks!",
            onTextInputChange = {},
            onSendClick = {},
            onBackClick = {},
            onPlaySong = {},
            onPromptClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AiAssistantScreenLoadingPreview() {
    MusicStreamTheme {
        AiAssistantScreenContent(
            uiState = AiAssistantUiState(
                messages = listOf(
                    ChatMessage("Recommend some relaxing music", true)
                ),
                isLoading = true
            ),
            textInput = "",
            onTextInputChange = {},
            onSendClick = {},
            onBackClick = {},
            onPlaySong = {},
            onPromptClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AiChatBubbleUserPreview() {
    MusicStreamTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            AiChatBubble(ChatMessage("Hello AI!", true))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AiChatBubbleAiPreview() {
    MusicStreamTheme {
        Box(modifier = Modifier.padding(16.dp)) {
            AiChatBubble(ChatMessage("1. Recommended Song\n• **Artist Name** - Song title", false))
        }
    }
}