package com.nuvio.app.core.ui

internal actual fun resolvedPosterWidthDp(preset: PosterCardWidthPreset): Int =
    legacyMobilePosterWidthDp(preset)
