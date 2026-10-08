package tn.keeftalk.chat.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object LucideIcons {
    val Send: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideSend",
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
            moveTo(14.536f, 21.686f)
            curveToRelative(0.0f, 0.0f, -0.466f, 0.229f, -0.937f, -0.024f)
            lineTo(7.1f, 18.482f)
            arcToRelative(2f, 2f, 0f, false, true, -1.112f, -1.11f)
            lineTo(2.808f, 9.442f)
            arcToRelative(0.5f, 0.5f, 0f, false, true, 0.024f, -0.937f)
            lineToRelative(19f, -6.5f)
            arcToRelative(0.496f, 0.496f, 0f, false, true, 0.635f, 0.635f)
            lineToRelative(-6.5f, 19f)
            close()
            moveTo(21.854f, 2.147f)
            lineTo(10.914f, 13.086f)
        }.build()
    }

    val EyeClosed: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideEyeClosed",
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
            moveTo(15f, 18f)
            lineToRelative(-0.722f, -3.25f)
            moveTo(2f, 8f)
            arcToRelative(10.645f, 10.645f, 0f, false, false, 20f, 0f)
            moveToRelative(-2f, 7f)
            lineToRelative(-1.726f, -2.05f)
            moveToRelative(-12.274f, 2.05f)
            lineToRelative(1.726f, -2.05f)
            moveToRelative(3.274f, 5.05f)
            lineToRelative(0.722f, -3.25f)
        }.build()
    }

    val EyeOff: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideEyeOff",
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
            moveTo(10.733f, 5.076f)
            arcToRelative(10.744f, 10.744f, 0f, false, true, 11.205f, 6.575f)
            arcToRelative(1f, 1f, 0f, false, true, 0f, 0.696f)
            arcToRelative(10.747f, 10.747f, 0f, false, true, -1.444f, 2.49f)
            moveTo(14.084f, 14.158f)
            arcToRelative(3f, 3f, 0f, false, true, -4.242f, -4.242f)
            moveTo(17.479f, 17.499f)
            arcToRelative(10.75f, 10.75f, 0f, false, true, -15.417f, -5.151f)
            arcToRelative(1f, 1f, 0f, false, true, 0f, -0.696f)
            arcToRelative(10.75f, 10.75f, 0f, false, true, 4.446f, -5.143f)
            moveToRelative(-4.508f, -4.508f)
            lineToRelative(20f, 20f)
        }.build()
    }

    val BellRing: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideBellRing",
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
            moveTo(10.268f, 21f)
            arcToRelative(2f, 2f, 0f, false, false, 3.464f, 0f)
            moveTo(22f, 8f)
            curveToRelative(0f, -2.3f, -0.8f, -4.3f, -2f, -6f)
            moveTo(3.262f, 15.326f)
            arcToRelative(1f, 1f, 0f, false, false, 0.738f, 1.674f)
            lineToRelative(16f, 0f)
            arcToRelative(1f, 1f, 0f, false, false, 0.74f, -1.673f)
            curveToRelative(-1.33f, -1.371f, -2.74f, -2.828f, -2.74f, -7.327f)
            arcToRelative(6f, 6f, 0f, false, false, -12f, 0f)
            curveToRelative(0f, 4.499f, -1.411f, 5.956f, -2.738f, 7.326f)
            moveTo(4f, 2f)
            curveToRelative(-1.2f, 1.7f, -2f, 3.7f, -2f, 6f)
        }.build()
    }

    val Eye: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideEye",
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
            moveTo(2.062f, 12.348f)
            arcToRelative(1f, 1f, 0f, false, true, 0f, -0.696f)
            arcToRelative(10.75f, 10.75f, 0f, false, true, 19.876f, 0f)
            arcToRelative(1f, 1f, 0f, false, true, 0f, 0.696f)
            arcToRelative(10.75f, 10.75f, 0f, false, true, -19.876f, 0f)
            close()
        }.path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 12f)
            moveToRelative(-3f, 0f)
            arcToRelative(3f, 3f, 0f, true, true, 6f, 0f)
            arcToRelative(3f, 3f, 0f, true, true, -6f, 0f)
        }.build()
    }

    val MessageCircle: ImageVector by lazy {
        ImageVector.Builder(
            name = "MessageCircle",
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
            moveTo(21f, 11.5f)
            arcToRelative(8.38f, 8.38f, 0f, false, true, -0.9f, 3.8f)
            arcToRelative(8.5f, 8.5f, 0f, false, true, -7.6f, 4.7f)
            arcToRelative(8.38f, 8.38f, 0f, false, true, -3.8f, -0.9f)
            lineTo(3f, 21f)
            lineToRelative(1.9f, -5.7f)
            arcToRelative(8.38f, 8.38f, 0f, false, true, -0.9f, -3.8f)
            arcToRelative(8.5f, 8.5f, 0f, false, true, 4.7f, -7.6f)
            arcToRelative(8.38f, 8.38f, 0f, false, true, 3.8f, -0.9f)
            horizontalLineToRelative(0.5f)
            arcToRelative(8.48f, 8.48f, 0f, false, true, 8f, 8f)
            verticalLineToRelative(0.5f)
            close()
        }.build()
    }

    val MessageCircleAgenda: ImageVector by lazy {
        ImageVector.Builder(
            name = "MessageCircleAgenda",
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
            moveTo(21f, 11.5f)
            arcToRelative(8.38f, 8.38f, 0f, false, true, -0.9f, 3.8f)
            arcToRelative(8.5f, 8.5f, 0f, false, true, -7.6f, 4.7f)
            arcToRelative(8.38f, 8.38f, 0f, false, true, -3.8f, -0.9f)
            lineTo(3f, 21f)
            lineToRelative(1.9f, -5.7f)
            arcToRelative(8.38f, 8.38f, 0f, false, true, -0.9f, -3.8f)
            arcToRelative(8.5f, 8.5f, 0f, false, true, 4.7f, -7.6f)
            arcToRelative(8.38f, 8.38f, 0f, false, true, 3.8f, -0.9f)
            horizontalLineToRelative(0.5f)
            arcToRelative(8.48f, 8.48f, 0f, false, true, 8f, 8f)
            verticalLineToRelative(0.5f)
            close()
        }.path(
            fill = SolidColor(Color.Black),
            strokeLineWidth = 0f
        ) {
            // Agenda dots (6x5 grid) - Perfectly centered and sized
            val startX = 7.25f
            val startY = 6.8f
            val spacing = 2.1f
            for (row in 0 until 5) {
                for (col in 0 until 6) {
                    val x = startX + col * spacing
                    val y = startY + row * spacing
                    moveTo(x, y)
                    arcToRelative(0.5f, 0.5f, 0f, true, true, 0f, 1f)
                    arcToRelative(0.5f, 0.5f, 0f, false, true, 0f, -1f)
                    close()
                }
            }
        }.build()
    }

    val MessageSquare: ImageVector by lazy {
        ImageVector.Builder(
            name = "MessageSquare",
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
            moveTo(21f, 15f)
            arcToRelative(2f, 2f, 0f, false, true, -2f, 2f)
            horizontalLineTo(7f)
            lineToRelative(-4f, 4f)
            verticalLineTo(5f)
            arcToRelative(2f, 2f, 0f, false, true, 2f, -2f)
            horizontalLineToRelative(14f)
            arcToRelative(2f, 2f, 0f, false, true, 2f, 2f)
            close()
        }.build()
    }

    val Video: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideVideo",
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
            moveTo(23f, 7f)
            lineTo(16f, 12f)
            lineTo(23f, 17f)
            verticalLineTo(7f)
            close()
            moveTo(3f, 5f)
            horizontalLineTo(14f)
            arcToRelative(2f, 2f, 0f, false, true, 2f, 2f)
            verticalLineTo(17f)
            arcToRelative(2f, 2f, 0f, false, true, -2f, 2f)
            horizontalLineTo(3f)
            arcToRelative(2f, 2f, 0f, false, true, -2f, -2f)
            verticalLineTo(7f)
            arcToRelative(2f, 2f, 0f, false, true, 2f, -2f)
            close()
        }.build()
    }

    val Phone: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucidePhone",
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
            moveTo(22f, 16.92f)
            verticalLineToRelative(3f)
            arcToRelative(2f, 2f, 0f, false, true, -2.18f, 2f)
            arcToRelative(19.79f, 19.79f, 0f, false, true, -8.63f, -3.07f)
            arcToRelative(19.5f, 19.5f, 0f, false, true, -6f, -6f)
            arcToRelative(19.79f, 19.79f, 0f, false, true, -3.07f, -8.67f)
            arcTo(2f, 2f, 0f, false, true, 4.11f, 2f)
            horizontalLineToRelative(3f)
            arcToRelative(2f, 2f, 0f, false, true, 2f, 1.72f)
            arcToRelative(12.84f, 12.84f, 0f, false, false, 0.7f, 2.81f)
            arcToRelative(2f, 2f, 0f, false, true, -0.45f, 2.11f)
            lineTo(8.09f, 9.91f)
            arcToRelative(16f, 16f, 0f, false, false, 6f, 6f)
            lineToRelative(1.27f, -1.27f)
            arcToRelative(2f, 2f, 0f, false, true, 2.11f, -0.45f)
            arcToRelative(12.84f, 12.84f, 0f, false, false, 2.81f, 0.7f)
            arcTo(2f, 2f, 0f, false, true, 22f, 16.92f)
            close()
        }.build()
    }

    val Rss: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideRss",
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
            moveTo(4f, 11f)
            arcToRelative(9f, 9f, 0f, false, true, 9f, 9f)
            moveTo(4f, 4f)
            arcToRelative(16f, 16f, 0f, false, true, 16f, 16f)
            moveToRelative(-16f, 0f)
            arcToRelative(1f, 1f, 0f, true, true, 2f, 0f)
            arcToRelative(1f, 1f, 0f, false, true, -2f, 0f)
            close()
        }.build()
    }

    val BadgeDollarSign: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideBadgeDollarSign",
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
            moveTo(3.85f, 8.62f)
            arcToRelative(4f, 4f, 0f, false, true, 4.78f, -4.77f)
            arcToRelative(4f, 4f, 0f, false, true, 6.74f, 0f)
            arcToRelative(4f, 4f, 0f, false, true, 4.78f, 4.78f)
            arcToRelative(4f, 4f, 0f, false, true, 0f, 6.74f)
            arcToRelative(4f, 4f, 0f, false, true, -4.77f, 4.78f)
            arcToRelative(4f, 4f, 0f, false, true, -6.75f, 0f)
            arcToRelative(4f, 4f, 0f, false, true, -4.78f, -4.75f)
            arcToRelative(4f, 4f, 0f, false, true, 0f, -6.76f)
            close()
        }.path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(16f, 8f)
            horizontalLineToRelative(-6f)
            arcToRelative(2f, 2f, 0f, true, false, 0f, 4f)
            horizontalLineToRelative(4f)
            arcToRelative(2f, 2f, 0f, true, true, 0f, 4f)
            horizontalLineTo(8f)
        }.path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 18f)
            verticalLineTo(6f)
        }.build()
    }

    val Key: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideKey",
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
            moveTo(15.5f, 7.5f)
            lineToRelative(2.3f, 2.3f)
            arcToRelative(1f, 1f, 0f, false, false, 1.4f, 0f)
            lineToRelative(2.1f, -2.1f)
            arcToRelative(1f, 1f, 0f, false, false, 0f, -1.4f)
            lineToRelative(-1.3f, -1.3f)
            arcToRelative(1f, 1f, 0f, false, false, -1.4f, 0f)
            lineToRelative(-2.1f, 2.1f)
            arcToRelative(1f, 1f, 0f, false, false, 0f, 1.4f)
        }.path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(15.5f, 7.5f)
            lineToRelative(-3f, 3f)
        }.path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(7.5f, 15.5f)
            moveToRelative(-5.5f, 0f)
            arcToRelative(5.5f, 5.5f, 0f, true, true, 11f, 0f)
            arcToRelative(5.5f, 5.5f, 0f, true, true, -11f, 0f)
        }.build()
    }

    val FolderLock: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideFolderLock",
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
            moveTo(14f, 15f)
            horizontalLineTo(20f)
            arcToRelative(1f, 1f, 0f, false, true, 1f, 1f)
            verticalLineTo(19f)
            arcToRelative(1f, 1f, 0f, false, true, -1f, 1f)
            horizontalLineTo(14f)
            arcToRelative(1f, 1f, 0f, false, true, -1f, -1f)
            verticalLineTo(16f)
            arcToRelative(1f, 1f, 0f, false, true, 1f, -1f)
            close()
        }.path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(10f, 20f)
            horizontalLineTo(4f)
            arcToRelative(2f, 2f, 0f, false, true, -2f, -2f)
            verticalLineTo(5f)
            arcToRelative(2f, 2f, 0f, false, true, 2f, -2f)
            horizontalLineTo(7.9f)
            arcToRelative(2f, 2f, 0f, false, true, 1.69f, 0.9f)
            lineTo(10.4f, 5.1f)
            arcToRelative(2f, 2f, 0f, false, true, 1.67f, 0.9f)
            horizontalLineTo(20f)
            arcToRelative(2f, 2f, 0f, false, true, 2f, 2f)
            verticalLineTo(10.5f)
        }.path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(15f, 15f)
            verticalLineTo(13.5f)
            arcToRelative(2.5f, 2.5f, 0f, false, true, 5f, 0f)
            verticalLineTo(15f)
        }.build()
    }

    val ChartNoAxesCombined: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideChartNoAxesCombined",
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
            moveTo(12f, 16f)
            verticalLineTo(21f)
        }.path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(16f, 14f)
            verticalLineTo(21f)
        }.path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(20f, 10f)
            verticalLineTo(21f)
        }.path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(22f, 3f)
            lineToRelative(-8.646f, 8.646f)
            arcToRelative(0.5f, 0.5f, 0f, false, true, -0.708f, 0f)
            lineToRelative(-3.292f, -3.292f)
            arcToRelative(0.5f, 0.5f, 0f, false, false, -0.707f, 0f)
            lineTo(2f, 15f)
        }.path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(4f, 18f)
            verticalLineTo(21f)
        }.path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(8f, 14f)
            verticalLineTo(21f)
        }.build()
    }

    val ListTodo: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideListTodo",
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
            moveTo(13f, 6f)
            lineTo(21f, 6f)
            moveTo(11f, 12f)
            lineTo(21f, 12f)
            moveTo(13f, 18f)
            lineTo(21f, 18f)
            moveTo(3f, 6f)
            lineTo(4f, 6f)
            moveTo(3f, 12f)
            lineTo(4f, 12f)
            moveTo(3f, 18f)
            lineTo(4f, 18f)
            moveTo(3f, 9f)
            lineTo(5f, 11f)
            lineTo(9f, 7f)
        }.build()
    }

    val Trophy: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideTrophy",
            defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(6f, 9f)
            horizontalLineTo(4.5f)
            curveTo(3.12f, 9f, 2f, 7.88f, 2f, 6.5f)
            verticalLineTo(5f)
            curveTo(2f, 3.89f, 2.89f, 3f, 4f, 3f)
            horizontalLineTo(6f)
            moveTo(18f, 9f)
            horizontalLineTo(19.5f)
            curveTo(20.88f, 9f, 22f, 7.88f, 22f, 6.5f)
            verticalLineTo(5f)
            curveTo(22f, 3.89f, 21.11f, 3f, 20f, 3f)
            horizontalLineTo(18f)
            moveTo(6f, 3f)
            horizontalLineTo(18f)
            verticalLineTo(11.5f)
            curveTo(18f, 14.54f, 15.54f, 17f, 12.5f, 17f)
            horizontalLineTo(11.5f)
            curveTo(8.46f, 17f, 6f, 14.54f, 6f, 11.5f)
            verticalLineTo(3f)
            moveTo(12f, 17f)
            verticalLineTo(21f)
            moveTo(8f, 21f)
            horizontalLineTo(16f)
        }.build()
    }

    val Flame: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideFlame",
            defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(8.5f, 14.5f)
            curveTo(8.5f, 12.5f, 10.5f, 10f, 12f, 7.5f)
            curveTo(15f, 11f, 17.5f, 13f, 17.5f, 16.5f)
            curveTo(17.5f, 19.5f, 15f, 22f, 12f, 22f)
            curveTo(9f, 22f, 6.5f, 19.5f, 6.5f, 16.5f)
            curveTo(6.5f, 15.2f, 7.3f, 14f, 8.5f, 12.5f)
            lineTo(8.5f, 14.5f)
            close()
        }.build()
    }

    val Clock: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideClock",
            defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 2f)
            curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
            curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
            curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
            curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
            close()
            moveTo(12f, 6f)
            verticalLineTo(12f)
            lineTo(16f, 14f)
        }.build()
    }

    val Calendar: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideCalendar",
            defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(3f, 4f)
            horizontalLineTo(21f)
            verticalLineTo(20f)
            curveTo(21f, 21.1f, 20.1f, 22f, 19f, 22f)
            horizontalLineTo(5f)
            curveTo(3.9f, 22f, 3f, 21.1f, 3f, 20f)
            verticalLineTo(4f)
            close()
            moveTo(16f, 2f)
            verticalLineTo(6f)
            moveTo(8f, 2f)
            verticalLineTo(6f)
            moveTo(3f, 10f)
            horizontalLineTo(21f)
        }.build()
    }

    val Search: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideSearch",
            defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(11f, 19f)
            curveTo(15.4183f, 19f, 19f, 15.4183f, 19f, 11f)
            curveTo(19f, 6.58172f, 15.4183f, 3f, 11f, 3f)
            curveTo(6.58172f, 3f, 3f, 6.58172f, 3f, 11f)
            curveTo(3f, 15.4183f, 6.58172f, 19f, 11f, 19f)
            close()
            moveTo(21f, 21f)
            lineTo(16.65f, 16.65f)
        }.build()
    }

    val Sparkles: ImageVector by lazy {
        ImageVector.Builder(
            name = "LucideSparkles",
            defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f
        ).path(
            stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round
        ) {
            moveTo(12f, 2f)
            lineTo(15.09f, 8.26f)
            lineTo(22f, 9.27f)
            lineTo(17f, 14.14f)
            lineTo(18.18f, 21.02f)
            lineTo(12f, 17.77f)
            lineTo(5.82f, 21.02f)
            lineTo(7f, 14.14f)
            lineTo(2f, 9.27f)
            lineTo(8.91f, 8.26f)
            lineTo(12f, 2f)
            close()
        }.build()
    }
}
