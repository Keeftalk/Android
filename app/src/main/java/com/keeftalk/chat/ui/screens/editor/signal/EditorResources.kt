package com.keeftalk.chat.ui.screens.editor.signal

import android.content.res.Resources
import android.util.TypedValue

/**
 * Ported from Signal Android (AGPL-3.0)
 */
object EditorResources {
    val density: Float by lazy { Resources.getSystem().displayMetrics.density }

    fun dpToPx(dp: Float): Float {
        return dp * density
    }

    // Dimen constants (from Signal's crop_area_renderer.xml)
    val CROP_AREA_RENDERER_EDGE_SIZE: Float by lazy { dpToPx(32f) }
    val CROP_AREA_RENDERER_EDGE_THICKNESS: Float by lazy { dpToPx(2f) }
    val OVAL_GUIDE_STROKE_WIDTH: Float by lazy { dpToPx(1f) }
    
    // Color constants
    const val CROP_AREA_RENDERER_EDGE_COLOR = 0xFFFFFFFF.toInt()
    const val CROP_AREA_RENDERER_OUTER_COLOR = 0x7F000000.toInt()
    const val CROP_CIRCLE_GUIDE_COLOR = 0x66FFFFFF.toInt()
}
