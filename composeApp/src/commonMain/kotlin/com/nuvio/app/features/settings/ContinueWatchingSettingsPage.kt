package com.nuvio.app.features.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nuvio.app.features.home.components.ContinueWatchingStylePreview
import com.nuvio.app.features.watchprogress.ContinueWatchingPreferencesRepository
import com.nuvio.app.features.watchprogress.ContinueWatchingSectionStyle

internal fun LazyListScope.continueWatchingSettingsContent(
    isTablet: Boolean,
    isVisible: Boolean,
    style: ContinueWatchingSectionStyle,
    upNextFromFurthestEpisode: Boolean,
    showResumePromptOnLaunch: Boolean,
) {
    item {
        SettingsSection(
            title = "VISIBILITY",
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = "Show Continue Watching",
                    description = "Display the Continue Watching shelf on the Home screen.",
                    checked = isVisible,
                    isTablet = isTablet,
                    onCheckedChange = ContinueWatchingPreferencesRepository::setVisible,
                )
            }
        }
    }
    item {
        SettingsSection(
            title = "CARD STYLE",
            isTablet = isTablet,
        ) {
            ContinueWatchingStyleSelector(
                isTablet = isTablet,
                selectedStyle = style,
                onStyleSelected = ContinueWatchingPreferencesRepository::setStyle,
            )
        }
    }
    item {
        SettingsSection(
            title = "UP NEXT BEHAVIOR",
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = "Up Next from furthest episode",
                    description = "When enabled, Up Next always continues from the furthest watched episode. When disabled, it follows from the most recently watched episode. useful if you rewatch earlier episodes.",
                    checked = upNextFromFurthestEpisode,
                    isTablet = isTablet,
                    onCheckedChange = ContinueWatchingPreferencesRepository::setUpNextFromFurthestEpisode,
                )
            }
        }
    }
    item {
        SettingsSection(
            title = "ON LAUNCH",
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = "Resume prompt on launch",
                    description = "Show a popup to continue where you left off when opening the app after leaving from the player.",
                    checked = showResumePromptOnLaunch,
                    isTablet = isTablet,
                    onCheckedChange = ContinueWatchingPreferencesRepository::setShowResumePromptOnLaunch,
                )
            }
        }
    }
}

@Composable
private fun ContinueWatchingStyleSelector(
    isTablet: Boolean,
    selectedStyle: ContinueWatchingSectionStyle,
    onStyleSelected: (ContinueWatchingSectionStyle) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ContinueWatchingSectionStyle.entries.forEach { style ->
            Box(modifier = Modifier.weight(1f)) {
                ContinueWatchingStyleOption(
                    style = style,
                    selected = selectedStyle == style,
                    isTablet = isTablet,
                    onClick = { onStyleSelected(style) },
                )
            }
        }
    }
}

@Composable
private fun ContinueWatchingStyleOption(
    style: ContinueWatchingSectionStyle,
    selected: Boolean,
    isTablet: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.alpha(if (selected) 1f else 0f),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp),
                contentAlignment = Alignment.Center,
            ) {
                ContinueWatchingStylePreview(
                    style = style,
                    isSelected = selected,
                )
            }
            Text(
                text = style.name.lowercase().replaceFirstChar(Char::uppercase),
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (style == ContinueWatchingSectionStyle.Wide) {
                    "Info-dense horizontal card"
                } else {
                    "Artwork-first poster card"
                },
                style = if (isTablet) MaterialTheme.typography.bodySmall else MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
