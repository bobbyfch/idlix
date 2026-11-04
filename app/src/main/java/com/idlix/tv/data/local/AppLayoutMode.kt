package com.idlix.tv.data.local

/** TV uses D-pad; Mobile is always bottom-nav; Tablet adapts to rotation. */
enum class AppLayoutMode { AUTO, TV, MOBILE, TABLET }

fun AppLayoutMode.useTvLayout(deviceIsTelevision: Boolean): Boolean = when (this) {
    AppLayoutMode.AUTO -> deviceIsTelevision
    AppLayoutMode.TV -> true
    AppLayoutMode.MOBILE, AppLayoutMode.TABLET -> false
}

fun AppLayoutMode.useTabletLayout(
    deviceIsTelevision: Boolean,
    smallestScreenWidthDp: Int
): Boolean = when (this) {
    AppLayoutMode.AUTO -> !deviceIsTelevision && smallestScreenWidthDp >= 600
    AppLayoutMode.TABLET -> true
    AppLayoutMode.TV, AppLayoutMode.MOBILE -> false
}
