package com.nuvio.app.features.tmdb

import com.nuvio.app.features.details.MetaCompany
import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.MetaPerson
import com.nuvio.app.features.details.MetaVideo
import kotlin.test.Test
import kotlin.test.assertEquals

class TmdbMetadataServiceTest {
    @Test
    fun `applyEnrichment replaces enabled metadata groups`() {
        val base = MetaDetails(
            id = "tt1234567",
            type = "series",
            name = "Original",
            description = "Addon description",
            videos = listOf(
                MetaVideo(
                    id = "ep1",
                    title = "Episode 1",
                    season = 1,
                    episode = 1,
                ),
            ),
        )
        val enrichment = TmdbEnrichment(
            localizedTitle = "Localized",
            description = "TMDB description",
            genres = listOf("Drama", "Mystery"),
            backdrop = "https://example.com/backdrop.jpg",
            logo = "https://example.com/logo.png",
            poster = "https://example.com/poster.jpg",
            people = listOf(MetaPerson(name = "Person", role = "Creator")),
            director = listOf("Director Name"),
            writer = emptyList(),
            releaseInfo = "2024-01-01",
            rating = 8.4,
            runtimeMinutes = 52,
            ageRating = "TV-MA",
            status = "Returning Series",
            countries = listOf("US"),
            language = "en",
            productionCompanies = listOf(MetaCompany(name = "A24")),
            networks = listOf(MetaCompany(name = "HBO")),
        )
        val episodes = mapOf(
            (1 to 1) to TmdbEpisodeEnrichment(
                title = "Pilot",
                overview = "Episode overview",
                thumbnail = "https://example.com/thumb.jpg",
                airDate = "2024-01-01",
                runtimeMinutes = 58,
            ),
        )

        val result = TmdbMetadataService.applyEnrichment(
            meta = base,
            enrichment = enrichment,
            episodeMap = episodes,
            settings = TmdbSettings(enabled = true),
        )

        assertEquals("Localized", result.name)
        assertEquals("TMDB description", result.description)
        assertEquals(listOf("Drama", "Mystery"), result.genres)
        assertEquals("8.4", result.imdbRating)
        assertEquals("TV-MA", result.ageRating)
        assertEquals("52m", result.runtime)
        assertEquals(listOf("Director Name"), result.director)
        assertEquals(listOf("A24"), result.productionCompanies.map { it.name })
        assertEquals(listOf("HBO"), result.networks.map { it.name })
        assertEquals("Pilot", result.videos.first().title)
        assertEquals(58, result.videos.first().runtime)
    }

    @Test
    fun `applyEnrichment preserves disabled groups`() {
        val base = MetaDetails(
            id = "tt7654321",
            type = "movie",
            name = "Original",
            description = "Original description",
            videos = listOf(
                MetaVideo(
                    id = "movie",
                    title = "Original title",
                ),
            ),
        )
        val enrichment = TmdbEnrichment(
            localizedTitle = "Localized",
            description = "TMDB description",
            genres = listOf("Sci-Fi"),
            backdrop = "backdrop",
            logo = "logo",
            poster = "poster",
            people = listOf(MetaPerson(name = "Cast Member")),
            director = listOf("Director"),
            writer = listOf("Writer"),
            releaseInfo = "2025-05-05",
            rating = 7.2,
            runtimeMinutes = 124,
            ageRating = "PG-13",
            status = "Released",
            countries = listOf("US"),
            language = "en",
            productionCompanies = listOf(MetaCompany(name = "Studio")),
            networks = emptyList(),
        )

        val result = TmdbMetadataService.applyEnrichment(
            meta = base,
            enrichment = enrichment,
            episodeMap = emptyMap(),
            settings = TmdbSettings(
                enabled = true,
                useArtwork = false,
                useBasicInfo = false,
                useDetails = false,
                useCredits = false,
                useProductions = false,
                useNetworks = false,
                useEpisodes = false,
            ),
        )

        assertEquals(base.name, result.name)
        assertEquals(base.description, result.description)
        assertEquals(base.genres, result.genres)
        assertEquals(base.director, result.director)
        assertEquals(base.cast, result.cast)
        assertEquals(base.productionCompanies, result.productionCompanies)
    }
}
