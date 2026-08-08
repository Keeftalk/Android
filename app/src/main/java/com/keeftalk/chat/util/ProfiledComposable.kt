package com.keeftalk.chat.util

import android.os.SystemClock
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout

@Composable
fun Profiled(
    name: String,
    content: @Composable () -> Unit
) {
    val startTime = SystemClock.elapsedRealtimeNanos()
    val isRecomposition = remember { mutableStateOf(false) }
    
    SideEffect {
        val duration = SystemClock.elapsedRealtimeNanos() - startTime
        PerformanceProfiler.trackComposition(name, duration, isRecomposition.value)
        isRecomposition.value = true
    }
    
    content()
}

fun Modifier.profileLayout(name: String): Modifier = this.then(
    Modifier.layout { measurable, constraints ->
        val measureStart = SystemClock.elapsedRealtimeNanos()
        val placeable = measurable.measure(constraints)
        val measureEnd = SystemClock.elapsedRealtimeNanos()
        
        layout(placeable.width, placeable.height) {
            val layoutStart = SystemClock.elapsedRealtimeNanos()
            placeable.placeRelative(0, 0)
            val layoutEnd = SystemClock.elapsedRealtimeNanos()
            
            PerformanceProfiler.logEvent(
                name = "$name (Layout)",
                info = "Measure: ${(measureEnd - measureStart) / 1000}us | Layout: ${(layoutEnd - layoutStart) / 1000}us",
                category = PerformanceProfiler.Category.LAYOUT
            )
        }
    }
)
