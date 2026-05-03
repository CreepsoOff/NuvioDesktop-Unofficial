package com.nuvio.app.features.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Tune

internal fun LazyListScope.contentDiscoveryContent(
    isTablet: Boolean,
    showPluginsEntry: Boolean,
    onAddonsClick: () -> Unit,
    onPluginsClick: () -> Unit,
    onHomescreenClick: () -> Unit,
    onMetaScreenClick: () -> Unit,
    onCollectionsClick: () -> Unit = {},
) {
    item {
        SettingsSection(
            title = "SOURCES",
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsNavigationRow(
                    title = "Addons",
                    description = "Install, remove, refresh, and sort your content sources.",
                    icon = Icons.Rounded.Extension,
                    isTablet = isTablet,
                    onClick = onAddonsClick,
                )
                if (showPluginsEntry) {
                    SettingsNavigationRow(
                        title = "Plugins",
                        description = "Install JavaScript scraper repositories and test providers internally.",
                        icon = Icons.Rounded.Hub,
                        isTablet = isTablet,
                        onClick = onPluginsClick,
                    )
                }
            }
        }
    }
    item {
        SettingsSection(
            title = "HOME",
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsNavigationRow(
                    title = "Homescreen",
                    description = "Control which catalogs appear on Home and in what order.",
                    icon = Icons.Rounded.Home,
                    isTablet = isTablet,
                    onClick = onHomescreenClick,
                )
                SettingsNavigationRow(
                    title = "Meta Screen",
                    description = "Disable detail sections and reorder everything below Hero.",
                    icon = Icons.Rounded.Tune,
                    isTablet = isTablet,
                    onClick = onMetaScreenClick,
                )
                SettingsNavigationRow(
                    title = "Collections",
                    description = "Create custom catalog groupings with folders shown on Home.",
                    icon = Icons.Rounded.CollectionsBookmark,
                    isTablet = isTablet,
                    onClick = onCollectionsClick,
                )
            }
        }
    }
}
