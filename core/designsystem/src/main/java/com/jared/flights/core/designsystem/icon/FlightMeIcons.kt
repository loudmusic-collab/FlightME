package com.jared.flights.core.designsystem.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * FlightME's own thin-line icons, drawn on a 24×24 grid with a 1.5 stroke.
 * The Icon() composable tints them, so the black here is only a placeholder.
 */
object FlightMeIcons {

    /** Top-down aeroplane, nose up. */
    val Flight: ImageVector by lazy {
        lineIcon("Flight") {
            moveTo(12f, 2.5f)
            curveTo(12.8f, 2.5f, 13.3f, 3.3f, 13.3f, 4.5f)
            lineTo(13.3f, 9.3f)
            lineTo(21f, 13.5f)
            lineTo(21f, 15.3f)
            lineTo(13.3f, 12.8f)
            lineTo(13.3f, 17.8f)
            lineTo(15.8f, 19.7f)
            lineTo(15.8f, 21.2f)
            lineTo(12f, 20.2f)
            lineTo(8.2f, 21.2f)
            lineTo(8.2f, 19.7f)
            lineTo(10.7f, 17.8f)
            lineTo(10.7f, 12.8f)
            lineTo(3f, 15.3f)
            lineTo(3f, 13.5f)
            lineTo(10.7f, 9.3f)
            lineTo(10.7f, 4.5f)
            curveTo(10.7f, 3.3f, 11.2f, 2.5f, 12f, 2.5f)
            close()
        }
    }

    /** Globe: outline, one meridian and the equator. Used for Passport. */
    val Globe: ImageVector by lazy {
        lineIcon("Globe") {
            circle(12f, 12f, 9f)
            // Meridian (a narrow ellipse)
            moveTo(12f, 3f)
            curveTo(9.5f, 5.5f, 8.2f, 8.6f, 8.2f, 12f)
            curveTo(8.2f, 15.4f, 9.5f, 18.5f, 12f, 21f)
            curveTo(14.5f, 18.5f, 15.8f, 15.4f, 15.8f, 12f)
            curveTo(15.8f, 8.6f, 14.5f, 5.5f, 12f, 3f)
            close()
            // Equator
            moveTo(3f, 12f)
            lineTo(21f, 12f)
        }
    }

    /** Three sliders. Used for Settings. */
    val Sliders: ImageVector by lazy {
        lineIcon("Sliders") {
            moveTo(4f, 7f); lineTo(13f, 7f)
            moveTo(17f, 7f); lineTo(20f, 7f)
            circle(15f, 7f, 2f)

            moveTo(4f, 12f); lineTo(7f, 12f)
            moveTo(11f, 12f); lineTo(20f, 12f)
            circle(9f, 12f, 2f)

            moveTo(4f, 17f); lineTo(11f, 17f)
            moveTo(15f, 17f); lineTo(20f, 17f)
            circle(13f, 17f, 2f)
        }
    }
}

private fun lineIcon(name: String, pathBuilder: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).path(
        fill = null,
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.5f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = pathBuilder,
    ).build()

private fun PathBuilder.circle(cx: Float, cy: Float, r: Float) {
    moveTo(cx - r, cy)
    arcToRelative(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = 2 * r, dy1 = 0f)
    arcToRelative(r, r, 0f, isMoreThanHalf = true, isPositiveArc = true, dx1 = -2 * r, dy1 = 0f)
    close()
}
