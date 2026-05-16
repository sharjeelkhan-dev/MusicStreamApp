package com.musicstream.app.presentation.components
import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.musicstream.app.ui.theme.MusicStreamTheme

@Composable
fun SectionHeader(
    title: String,
    emoji: String = "",
    iconRes: Int? = null,
    onSeeAllClick: (() -> Unit)? = null,
    @SuppressLint("ModifierParameter") modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.offset(y = (-23).dp)
            .fillMaxWidth()
            .padding(
                start = 24.dp,
                end = 12.dp,
                top = 5.dp,
                bottom = 5.dp,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (iconRes != null) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = when (title) {
                        "Trending" -> Color(0xFFFF5722)
                        "Recently Played" -> MaterialTheme.colorScheme.onBackground
                        else -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.padding(end = 10.dp).size(22.dp)
                )
            } else if (emoji.isNotEmpty()) {
                Text(
                    text = emoji,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
        }

        if (onSeeAllClick != null) {
            Card(
                modifier = Modifier.offset(x = (-15).dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                onClick = onSeeAllClick
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp,
                        vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "See all",
                        color = MaterialTheme
                            .colorScheme
                            .onBackground
                            .copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        fontWeight =
                            FontWeight
                            .SemiBold
                    )
                    Icon(
                        imageVector = Icons
                            .AutoMirrored
                            .Filled
                            .ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier =
                            Modifier
                                .size(14.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SectionHeaderPreview() {
    MusicStreamTheme {
        Box(modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)) {
            SectionHeader(
                title = "Trending",
                emoji = "🔥",
                onSeeAllClick = {}
            )
        }
    }
}
