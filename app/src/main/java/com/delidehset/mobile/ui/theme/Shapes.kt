package com.delidehset.mobile.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * The corners the app's own surfaces use, which follow the palette in force.
 *
 * A kit is not only its colours: the same screens drawn with 24 dp corners and with 2 dp
 * corners feel like two different apps. These three tokens are what the screens ask for —
 * cards and sheets, the controls on them, and the small chips inside — and each palette
 * carries its own numbers, so picking a kit changes the shapes everywhere at once.
 *
 * Like [DeliColors] these read a snapshot state, so a screen that draws `DeliShapes.card`
 * is repainted when the kit changes.
 */
object DeliShapes {

    /** Cards, sheets, list sections. */
    val card: Shape get() = RoundedCornerShape(DeliColors.palette.cardRadius)

    /** Buttons, fields, tiles, avatars. */
    val control: Shape get() = RoundedCornerShape(DeliColors.palette.controlRadius)

    /** Small chips and badges inside a card. */
    val small: Shape get() = RoundedCornerShape(DeliColors.palette.smallRadius)

    /** Fully round, for pills and badges that are meant to read as pills in every kit. */
    val pill: Shape get() = RoundedCornerShape(50)
}
