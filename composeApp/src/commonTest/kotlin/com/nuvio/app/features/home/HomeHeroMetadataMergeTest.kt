package com.nuvio.app.features.home

import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.tmdb.TmdbSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeHeroMetadataMergeTest {

    @Test
    fun `merge tmdb hero metadata keeps catalog identity and upgrades artwork and text`() {
        val item = MetaPreview(
            id = "tt0111161",
            type = "movie",
            name = "Addon title",
            poster = "https://addon/poster.jpg",
            banner = "https://addon/banner.jpg",
            logo = null,
            description = "Addon description",
            genres = listOf("Addon"),
        )
        val meta = MetaDetails(
            id = "tmdb:278",
            type = "movie",
            name = "Localized title",
            poster = "https://image.tmdb.org/t/p/w500/poster.jpg",
            background = "https://image.tmdb.org/t/p/w1280/backdrop.jpg",
            logo = "https://image.tmdb.org/t/p/w300/logo.png",
            description = "TMDB overview",
            releaseInfo = "1994",
            imdbRating = "9.3",
            genres = listOf("Drama", "Crime"),
        )

        val result = item.mergeTmdbHeroMetadata(
            meta = meta,
            settings = TmdbSettings(
                enabled = true,
                apiKey = "key",
                useArtwork = true,
                useBasicInfo = true,
                useDetails = true,
            ),
        )

        assertEquals("tt0111161", result.id)
        assertEquals("movie", result.type)
        assertEquals("Localized title", result.name)
        assertEquals("https://image.tmdb.org/t/p/w500/poster.jpg", result.poster)
        assertEquals("https://image.tmdb.org/t/p/w1280/backdrop.jpg", result.banner)
        assertEquals("https://image.tmdb.org/t/p/w300/logo.png", result.logo)
        assertEquals("TMDB overview", result.description)
        assertEquals("1994", result.releaseInfo)
        assertEquals("9.3", result.imdbRating)
        assertEquals(listOf("Drama", "Crime"), result.genres)
    }

    @Test
    fun `merge tmdb hero metadata can update text when artwork is disabled`() {
        val item = MetaPreview(
            id = "tt0111161",
            type = "movie",
            name = "Addon title",
            poster = "https://addon/poster.jpg",
            banner = "https://addon/banner.jpg",
            description = "Addon description",
            releaseInfo = "1990",
        )
        val meta = MetaDetails(
            id = "tmdb:278",
            type = "movie",
            name = "TMDB title",
            poster = "https://image.tmdb.org/t/p/w500/poster.jpg",
            background = "https://image.tmdb.org/t/p/w1280/backdrop.jpg",
            description = "TMDB description",
            releaseInfo = "1994",
            imdbRating = "9.3",
            genres = listOf("Drama"),
        )

        val result = item.mergeTmdbHeroMetadata(
            meta = meta,
            settings = TmdbSettings(
                enabled = true,
                apiKey = "key",
                useArtwork = false,
                useBasicInfo = true,
                useDetails = true,
            ),
        )

        assertEquals("tt0111161", result.id)
        assertEquals("movie", result.type)
        assertEquals("https://addon/poster.jpg", result.poster)
        assertEquals("https://addon/banner.jpg", result.banner)
        assertEquals("TMDB title", result.name)
        assertEquals("TMDB description", result.description)
        assertEquals("1994", result.releaseInfo)
        assertEquals("9.3", result.imdbRating)
        assertEquals(listOf("Drama"), result.genres)
    }
}
