package com.keeftalk.chat.ui.screens.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object EditIcons {
    val Edit2: ImageVector
        get() {
            if (_edit2 != null) return _edit2!!
            _edit2 = ImageVector.Builder(
                name = "Edit2",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 24f,
                viewportHeight = 24f
            ).path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(17f, 3f)
                curveToRelative(0.751f, 0f, 1.471f, 0.298f, 2.002f, 0.828f)
                curveToRelative(0.531f, 0.53f, 0.828f, 1.25f, 0.828f, 2.002f)
                curveToRelative(0f, 0.751f, -0.298f, 1.471f, -0.828f, 2.002f)
                lineTo(7.5f, 20.5f)
                lineTo(2f, 22f)
                lineToRelative(1.5f, -5.5f)
                lineTo(17f, 3f)
                close()
            }.build()
            return _edit2!!
        }

    private var _edit2: ImageVector? = null
}
