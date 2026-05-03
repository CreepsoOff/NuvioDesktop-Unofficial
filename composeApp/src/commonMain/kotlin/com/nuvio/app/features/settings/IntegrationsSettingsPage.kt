package com.nuvio.app.features.settings

import androidx.compose.foundation.lazy.LazyListScope

internal fun LazyListScope.integrationsContent(
    isTablet: Boolean,
    onTmdbClick: () -> Unit,
    onMdbListClick: () -> Unit,
) {
    item {
        SettingsSection(
            title = "INTEGRATIONS",
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsNavigationRow(
                    title = "TMDB Enrichment",
                    description = "Enhance detail pages with TMDB artwork, credits, episode metadata, and more.",
                    iconPainter = integrationLogoPainter(IntegrationLogo.Tmdb),
                    isTablet = isTablet,
                    onClick = onTmdbClick,
                )
                SettingsGroupDivider(isTablet = isTablet)
                SettingsNavigationRow(
                    title = "MDBList Ratings",
                    description = "Add IMDb, Rotten Tomatoes, Metacritic, and other external ratings to details pages.",
                    iconPainter = integrationLogoPainter(IntegrationLogo.MdbList),
                    isTablet = isTablet,
                    onClick = onMdbListClick,
                )
            }
        }
    }
}
