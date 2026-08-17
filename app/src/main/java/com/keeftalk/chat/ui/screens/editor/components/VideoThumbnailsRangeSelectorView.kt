package com.keeftalk.chat.ui.screens.editor.components

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import java.util.Locale

@Composable
fun VideoThumbnailsRangeSelectorView(
    uri: Uri,
    totalDuration: Long,
    selectedRange: LongRange,
    currentPosition: Long = 0, // Add position
    onRangeChange: (LongRange) -> Unit,
    modifier: Modifier = Modifier
) {
    var width by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val minGapPx = remember(density) { with(density) { 40.dp.toPx() } }
    
    val startX = if (totalDuration > 0) (selectedRange.first.toFloat() / totalDuration) * width else 0f
    val endX = if (totalDuration > 0) (selectedRange.last.toFloat() / totalDuration) * width else width
    val cursorX = if (totalDuration > 0) (currentPosition.toFloat() / totalDuration) * width else 0f

    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(selectedRange.first), color = Color.White, fontSize = 10.sp)
            Text(formatTime(selectedRange.last), color = Color.White, fontSize = 10.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(Color.Black, RoundedCornerShape(8.dp))
                .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .onGloballyPositioned { width = it.size.width.toFloat() }
        ) {
            // Background Thumbnails
            VideoThumbnailsView(uri = uri, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)))

            // Overlay for unselected regions
            Canvas(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))) {
                drawRect(
                    color = Color.Black.copy(alpha = 0.6f),
                    size = Size(startX, size.height)
                )
                drawRect(
                    color = Color.Black.copy(alpha = 0.6f),
                    topLeft = Offset(endX, 0f),
                    size = Size(size.width - endX, size.height)
                )
                
                // Yellow border for active region
                drawRoundRect(
                    color = Color(0xFFFFD600), // Signal Yellow
                    topLeft = Offset(startX, 0f),
                    size = Size(endX - startX, size.height),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )
                
                // Playhead cursor
                if (cursorX in startX..endX) {
                    drawRect(
                        color = Color.White,
                        topLeft = Offset(cursorX - 1.dp.toPx(), 0f),
                        size = Size(2.dp.toPx(), size.height)
                    )
                }
            }

            Handle(
                x = startX,
                isLeft = true,
                onDrag = { delta ->
                    val newX = (startX + delta).coerceIn(0f, endX - minGapPx)
                    val newStart = (newX / width.coerceAtLeast(1f) * totalDuration).toLong()
                    onRangeChange(newStart..selectedRange.last)
                }
            )
            Handle(
                x = endX,
                isLeft = false,
                onDrag = { delta ->
                    val newX = (endX + delta).coerceIn(startX + minGapPx, width)
                    val newEnd = (newX / width.coerceAtLeast(1f) * totalDuration).toLong()
                    onRangeChange(selectedRange.first..newEnd)
                }
            )
        }
    }
}

@Composable
private fun Handle(
    x: Float,
    isLeft: Boolean,
    onDrag: (Float) -> Unit
) {
    val density = LocalDensity.current
    val widthPx = with(density) { 16.dp.toPx() }
    val offsetX = if (isLeft) x else x - widthPx
    
    Box(
        modifier = Modifier
            .offset(x = with(density) { offsetX.toDp() })
            .width(16.dp)
            .fillMaxHeight()
            .background(Color(0xFFFFD600), RoundedCornerShape(if (isLeft) 8.dp else 0.dp, if (isLeft) 0.dp else 8.dp, if (isLeft) 0.dp else 8.dp, if (isLeft) 8.dp else 0.dp))
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Vertical bars
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(2) {
                Box(
                    modifier = Modifier
                        .size(2.dp, 16.dp)
                        .background(Color.Black.copy(alpha = 0.3f))
                )
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val seconds = ms / 1000
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", mins, secs)
}
