package tn.keeftalk.chat.ui.books.reader

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import tn.keeftalk.chat.ui.books.components.parseHexColor
import kotlin.math.roundToInt

/**
 * Wraps the reader content with (1) a "page read" colour tint and (2) a draggable check button.
 *
 * @param sizePx button diameter. NOTE: interpreted as **dp** so the reader matches the live
 *   preview in BooksSettingsScreen (which uses `.dp`). It used to be converted from px, which
 *   made the button ~3x smaller than the preview and below the minimum touch target.
 * @param parentWidth / parentHeight optional overrides in px. Leave null: the overlay measures
 *   itself, which is always correct (the old 1000x2000 defaults clamped to a fake area).
 */
@Composable
fun ReadMarkerOverlay(
    isPageRead: Boolean = false,
    greenOpacityPercent: Int = 50,
    readMarkerColorHex: String = "#2ECC71",
    showMovableButton: Boolean = false,
    sizePx: Int = 38,
    dockX: Float? = null,
    dockY: Float? = null,
    onToggleRead: () -> Unit = {},
    onToggleReadWithDetails: (tapX: Float, tapY: Float, isObscured: Boolean) -> Unit = { _, _, _ -> },
    onDockPositionChanged: (Float, Float) -> Unit = { _, _ -> },
    parentWidth: Float? = null,
    parentHeight: Float? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    val density = LocalDensity.current
    val buttonDp = sizePx.coerceIn(24, 96).dp
    val buttonPx = with(density) { buttonDp.toPx() }
    val edgeMarginPx = with(density) { 16.dp.toPx() }

    val markerColor = parseHexColor(readMarkerColorHex, Color(0xFF2ECC71))
    val targetAlpha = if (isPageRead) greenOpacityPercent.coerceIn(10, 90) / 100f else 0f
    val tintAlpha by animateFloatAsState(targetValue = targetAlpha, label = "readTint")

    val currentOnToggle by rememberUpdatedState(onToggleRead)
    val currentOnDock by rememberUpdatedState(onDockPositionChanged)

    var isCoolingDown by remember { mutableStateOf(false) }

    LaunchedEffect(isCoolingDown) {
        if (isCoolingDown) {
            delay(300L) // 300ms debounce
            isCoolingDown = false
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val areaW = parentWidth ?: if (constraints.hasBoundedWidth) constraints.maxWidth.toFloat() else 0f
        val areaH = parentHeight ?: if (constraints.hasBoundedHeight) constraints.maxHeight.toFloat() else 0f

        val maxX = (areaW - buttonPx).coerceAtLeast(0f)
        val maxY = (areaH - buttonPx).coerceAtLeast(0f)
        val defaultX = (areaW - buttonPx - edgeMarginPx).coerceAtLeast(0f)
        // right edge, vertically centred: keeps clear of the bottom-corner page arrows
        val defaultY = ((areaH - buttonPx) / 2f).coerceIn(0f, maxY)

        var offsetX by remember { mutableFloatStateOf(dockX ?: defaultX) }
        var offsetY by remember { mutableFloatStateOf(dockY ?: defaultY) }

        // Follow the saved spot of the page being shown; pages without one keep the current
        // position (previously the button snapped back to the corner on every page turn).
        LaunchedEffect(dockX, dockY) {
            if (dockX != null && dockY != null) {
                offsetX = dockX
                offsetY = dockY
            }
        }

        // Base content layer (pager, selection, zoom...)
        content()

        // Page-read tint: does not intercept touches (no pointer modifiers)
        if (tintAlpha > 0.001f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(markerColor.copy(alpha = tintAlpha))
            )
        }

        if (showMovableButton) {
            val buttonColors = if (isCoolingDown) {
                listOf(Color(0xFF8E8E93), Color(0xFF636366))
            } else if (isPageRead) {
                listOf(markerColor, markerColor.copy(alpha = 0.85f))
            } else {
                listOf(Color(0xFF6C63FF), Color(0xFF7D5CFF))
            }

            val borderColor = if (isCoolingDown) Color(0xFFAEAEB2) else Color.White
            val iconTint = if (isCoolingDown) Color(0xFFE5E5EA) else Color.White

            val currentOnToggleDetails by rememberUpdatedState(onToggleReadWithDetails)
            var lastTapX by remember { mutableFloatStateOf(0f) }
            var lastTapY by remember { mutableFloatStateOf(0f) }
            var lastIsObscured by remember { mutableStateOf(false) }

            Box(
                modifier = Modifier
                    // clamp at draw time so rotation / size changes can never leave it off-screen
                    .offset {
                        IntOffset(
                            offsetX.coerceIn(0f, maxX).roundToInt(),
                            offsetY.coerceIn(0f, maxY).roundToInt()
                        )
                    }
                    .size(buttonDp)
                    .shadow(elevation = 6.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .border(2.dp, borderColor, CircleShape)
                    .background(brush = Brush.linearGradient(colors = buttonColors))
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                                val motionEvent = event.motionEvent
                                if (motionEvent != null) {
                                    lastTapX = motionEvent.x
                                    lastTapY = motionEvent.y
                                    lastIsObscured = tn.keeftalk.chat.domain.xp.TapGuard.isObscured(motionEvent)
                                }
                            }
                        }
                    }
                    .clickable(enabled = !isCoolingDown) {
                        isCoolingDown = true
                        currentOnToggle()
                        if (!isPageRead) {
                            currentOnToggleDetails(lastTapX, lastTapY, lastIsObscured)
                        }
                    }
                    // keyed on bounds so the clamp values are never stale after a resize
                    .pointerInput(maxX, maxY) {
                        detectDragGestures(
                            onDragEnd = { currentOnDock(offsetX, offsetY) },
                            onDragCancel = { currentOnDock(offsetX, offsetY) }
                        ) { change, dragAmount ->
                            change.consume()
                            offsetX = (offsetX + dragAmount.x).coerceIn(0f, maxX)
                            offsetY = (offsetY + dragAmount.y).coerceIn(0f, maxY)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Read Marker",
                    tint = iconTint,
                    modifier = Modifier.size(buttonDp * 0.5f)
                )
            }
        }
    }
}