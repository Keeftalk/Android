package com.keeftalk.chat.ui.screens.editor.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
fun VideoThumbnailsRangeSelectorView(
    totalDuration: Long,
    selectedRange: LongRange,
    onRangeChange: (LongRange) -> Unit,
    modifier: Modifier = Modifier
) {
    var width by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val minGapPx = remember(density) { with(density) { 40.dp.toPx() } }
    
    val startX = (selectedRange.first.toFloat() / totalDuration.coerceAtLeast(1)) * width
    val endX = (selectedRange.last.toFloat() / totalDuration.coerceAtLeast(1)) * width

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .onGloballyPositioned { width = it.size.width.toFloat() }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                size = Size(startX, size.height)
            )
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                topLeft = Offset(endX, 0f),
                size = Size(size.width - endX, size.height)
            )
            drawRect(
                color = Color.Yellow,
                topLeft = Offset(startX, 0f),
                size = Size(endX - startX, size.height),
                style = Stroke(width = 2.dp.toPx())
            )
        }

        Handle(
            x = startX,
            onDrag = { delta ->
                val newX = (startX + delta).coerceIn(0f, endX - minGapPx)
                val newStart = (newX / width.coerceAtLeast(1f) * totalDuration).toLong()
                onRangeChange(newStart..selectedRange.last)
            }
        )
        Handle(
            x = endX,
            onDrag = { delta ->
                val newX = (endX + delta).coerceIn(startX + minGapPx, width)
                val newEnd = (newX / width.coerceAtLeast(1f) * totalDuration).toLong()
                onRangeChange(selectedRange.first..newEnd)
            }
        )
    }
}

@Composable
private fun Handle(
    x: Float,
    onDrag: (Float) -> Unit
) {
    val density = LocalDensity.current
    val offsetDp = remember(x, density) { with(density) { (x - 10.dp.toPx()).toDp() } }
    
    Box(
        modifier = Modifier
            .offset(x = offsetDp)
            .size(20.dp, 60.dp)
            .background(Color.Yellow, RoundedCornerShape(4.dp))
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(2.dp, 12.dp)
                        .background(Color.Black.copy(alpha = 0.3f))
                )
            }
        }
    }
}
