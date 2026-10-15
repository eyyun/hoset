package com.delidehset.mobile.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Colour tokens read straight out of the website's own stylesheets so the native
 * UI keeps the exact look of delidehset.com:
 *
 *  - themes/midnight.css  -> --bs-body-bg: #0d1117, --bs-body-secondary-bg: #161b22,
 *                            --bs-border-color: #262b32, --bs-body-color: #c9d1d9
 *  - <html style="--primary:#805ad5">  -> the favicon/purple accent
 *  - main.css             -> --downvote-color: #38b2ac
 *  - themes/midnight.css  -> --bs-orange: #fd7e14 (the upvote arrow colour)
 */
object DeliColors {
    val Background = Color(0xFF0D1117)
    val Surface = Color(0xFF161B22)
    val SurfaceVariant = Color(0xFF21262D)
    val Border = Color(0xFF262B32)
    val OnBackground = Color(0xFFC9D1D9)
    val OnSurfaceVariant = Color(0xFF8B949E)
    val Primary = Color(0xFF805AD5)
    val PrimaryContainer = Color(0xFF3B2A63)
    val Blue = Color(0xFF58A6FF)
    val Upvote = Color(0xFFFD7E14)
    val Downvote = Color(0xFF38B2AC)
    val Success = Color(0xFF3FB950)
    val Warning = Color(0xFFD29922)
    val Danger = Color(0xFFF85149)
}
