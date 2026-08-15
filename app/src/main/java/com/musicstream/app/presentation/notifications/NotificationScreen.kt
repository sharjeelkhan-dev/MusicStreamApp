package com.musicstream.app.presentation.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.musicstream.app.R
import com.musicstream.app.domain.model.Notification
import com.musicstream.app.domain.model.NotificationType
import com.musicstream.app.ui.theme.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack

@Composable
fun NotificationScreen(
    viewModel: NotificationViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    NotificationScreenContent(
        state = state,
        onBackClick = onBackClick,
        onRefresh = { viewModel.refresh() },
        onClearAll = { viewModel.clearAll() },
        onClearNotification = { viewModel.clearNotification(it) }
    )
}

@Composable
fun NotificationItem(
    notification: Notification,
    onClear: () -> Unit
) {
    val icon = when (notification.type) {
        NotificationType.NEW_RELEASE -> Icons.Default.Star
        NotificationType.PLAYLIST_UPDATE -> Icons.Default.Update
        NotificationType.PROMOTION -> Icons.Default.Campaign
        NotificationType.GENERAL -> Icons.Default.Notifications
    }

    val iconColor = when (notification.type) {
        NotificationType.NEW_RELEASE -> AccentOrange
        NotificationType.PLAYLIST_UPDATE -> AccentCyan
        NotificationType.PROMOTION -> MaterialTheme.colorScheme.primary
        NotificationType.GENERAL -> AccentAmber
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = notification.time,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = notification.message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun NotificationScreenPreview() {
    MusicStreamTheme {
        NotificationScreenContent(
            state = NotificationUiState(
                notifications = listOf(
                    Notification(
                        id = "1",
                        title = "New Release",
                        message = "Diljit Dosanjh just dropped a new single!",
                        time = "2 mins ago",
                        type = NotificationType.NEW_RELEASE
                    ),
                    Notification(
                        id = "2",
                        title = "Playlist Updated",
                        message = "Your 'Focus' playlist has 5 new tracks.",
                        time = "1 hour ago",
                        type = NotificationType.PLAYLIST_UPDATE
                    ),
                    Notification(
                        id = "3",
                        title = "Premium Offer",
                        message = "Get 3 months of Premium for the price of 1!",
                        time = "5 hours ago",
                        type = NotificationType.PROMOTION
                    )
                ),
                isLoading = false
            ),
            onBackClick = {},
            onRefresh = {},
            onClearAll = {},
            onClearNotification = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NotificationScreenEmptyPreview() {
    MusicStreamTheme {
        NotificationScreenContent(
            state = NotificationUiState(
                notifications = emptyList(),
                isLoading = false
            ),
            onBackClick = {},
            onRefresh = {},
            onClearAll = {},
            onClearNotification = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreenContent(
    state: NotificationUiState,
    onBackClick: () -> Unit,
    onRefresh: () -> Unit,
    onClearAll: () -> Unit,
    onClearNotification: (String) -> Unit
) {
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Card(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    onClick = onBackClick
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Text(
                    text = "Notifications",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (state.notifications.isNotEmpty()) {
                    IconButton(
                        onClick = onClearAll,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.recycle_bin_icon),
                            contentDescription = "Clear All",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(44.dp))
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.notifications.isEmpty() && !state.isLoading) {
                EmptyNotificationsView()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.notifications, key = { it.id }) { notification ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = {
                                if (it == SwipeToDismissBoxValue.EndToStart) {
                                    onClearNotification(notification.id)
                                    true
                                } else false
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                                    MaterialTheme.colorScheme.errorContainer
                                } else Color.Transparent

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 24.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(color),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(end = 24.dp)
                                    )
                                }
                            },
                            enableDismissFromStartToEnd = false,
                            content = {
                                NotificationItem(
                                    notification = notification,
                                    onClear = { onClearNotification(notification.id) }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyNotificationsView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.notification_alarm_buzzer_icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(60.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Quiet for now",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "We'll notify you when your favorite artists drop new tracks or playlists are updated.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

