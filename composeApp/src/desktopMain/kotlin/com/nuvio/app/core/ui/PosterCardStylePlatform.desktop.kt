package com.nuvio.app.core.ui

internal actual fun resolvedPosterWidthDp(preset: PosterCardWidthPreset): Int =
    when (preset) {
        PosterCardWidthPreset.Compact -> 132
        PosterCardWidthPreset.Dense -> 148
        PosterCardWidthPreset.Standard -> 164
        PosterCardWidthPreset.Balanced -> 180
        PosterCardWidthPreset.Comfort -> 196
        PosterCardWidthPreset.Large -> 212
    }
