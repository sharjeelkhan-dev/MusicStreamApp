package com.attendance.app.presentation.components

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.attendance.app.presentation.theme.LocalIsDarkMode
import kotlinx.coroutines.delay

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun VerticalScrollbar(
    lazyListState: LazyListState,
    modifier: Modifier = Modifier,
    thickness: Dp = 5.dp,
    color: Color? = null
) {
    val isDark = LocalIsDarkMode.current
    val scrollbarColor = color ?: if (isDark) {
        Color.White.copy(alpha = 0.8f)
    } else {
        Color.Black.copy(alpha = 0.45f)
    }

    // State for scrollbar visibility
    var isVisible by remember { mutableStateOf(false) }

    // Efficiently calculate scroll metrics using derivedStateOf
    val scrollbarMetrics by remember {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            val totalItems = layoutInfo.totalItemsCount

            if (visibleItems.isEmpty() || totalItems == 0) return@derivedStateOf null

            val viewportHeight = layoutInfo.viewportSize.height.toFloat()
            if (viewportHeight <= 0f) return@derivedStateOf null

            // Calculate average height of visible items for accurate estimation
            val firstItem = visibleItems.first()
            val lastItem = visibleItems.last()
            val visibleHeight = lastItem.offset + lastItem.size - firstItem.offset
            val averageItemHeight = visibleHeight.toFloat() / visibleItems.size

            // Estimate total scrollable content height including paddings
            val estimatedTotalHeight = (averageItemHeight * totalItems) +
                    layoutInfo.beforeContentPadding + layoutInfo.afterContentPadding

            // If content fits in viewport, hide scrollbar handle
            if (estimatedTotalHeight <= viewportHeight) return@derivedStateOf null

            // Calculate current scroll position in pixels relative to viewport start
            val scrolledPixels = (firstItem.index * averageItemHeight) + 
                    (layoutInfo.viewportStartOffset - firstItem.offset)

            // Map content scroll progress to handle position and size
            val heightFraction = (viewportHeight / estimatedTotalHeight).coerceIn(0.1f, 0.9f)
            val scrollPositionFraction = (scrolledPixels / (estimatedTotalHeight - viewportHeight)).coerceIn(0f, 1f)

            scrollPositionFraction to heightFraction
        }
    }

    // Handle auto-hide logic based on scrolling activity
    val isScrolling = lazyListState.isScrollInProgress
    val firstVisibleItemIndex by remember { derivedStateOf { lazyListState.firstVisibleItemIndex } }
    val firstVisibleItemScrollOffset by remember { derivedStateOf { lazyListState.firstVisibleItemScrollOffset } }

    LaunchedEffect(isScrolling, firstVisibleItemIndex, firstVisibleItemScrollOffset) {
        isVisible = true
        if (!isScrolling) {
            delay(2000)
            isVisible = false
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible && scrollbarMetrics != null) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "scrollbarAlpha"
    )

    val metrics = scrollbarMetrics ?: return

    BoxWithConstraints(
        modifier = modifier
            .fillMaxHeight()
            .width(thickness + 12.dp) // Hit area for potential interaction
            .padding(vertical = 4.dp)
            .alpha(alpha),
        contentAlignment = Alignment.TopCenter
    ) {
        val trackHeight = maxHeight
        val handleHeight = (trackHeight * metrics.second).coerceAtLeast(32.dp)
        val scrollPosition = metrics.first

        Box(
            modifier = Modifier
                .width(thickness)
                .height(handleHeight)
                .graphicsLayer {
                    // Smooth translation using graphicsLayer to avoid layout passes during scroll
                    val maxTravel = (trackHeight - handleHeight).toPx()
                    translationY = scrollPosition * maxTravel
                }
                .clip(CircleShape)
                .background(scrollbarColor)
        )
    }
}
