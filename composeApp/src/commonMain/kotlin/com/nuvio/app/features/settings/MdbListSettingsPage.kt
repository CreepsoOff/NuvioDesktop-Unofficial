package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nuvio.app.features.mdblist.MdbListMetadataService
import com.nuvio.app.features.mdblist.MdbListSettings
import com.nuvio.app.features.mdblist.MdbListSettingsRepository

internal fun LazyListScope.mdbListSettingsContent(
    isTablet: Boolean,
    settings: MdbListSettings,
) {
    val providerControlsEnabled = settings.enabled && settings.hasApiKey

    item {
        SettingsSection(
            title = "MDBLIST",
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                SettingsSwitchRow(
                    title = "Enable MDBList ratings",
                    description = "Show external ratings from MDBList on metadata pages when an IMDb ID is available.",
                    checked = settings.enabled,
                    enabled = settings.hasApiKey,
                    isTablet = isTablet,
                    onCheckedChange = MdbListSettingsRepository::setEnabled,
                )
                if (!settings.hasApiKey) {
                    SettingsGroupDivider(isTablet = isTablet)
                    MdbListInfoRow(
                        isTablet = isTablet,
                        text = "Add your MDBList API key below before turning ratings on.",
                    )
                }
            }
        }
    }

    item {
        SettingsSection(
            title = "API KEY",
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                MdbListApiKeyRow(
                    isTablet = isTablet,
                    value = settings.apiKey,
                    onApiKeyCommitted = MdbListSettingsRepository::setApiKey,
                )
            }
        }
    }

    item {
        SettingsSection(
            title = "RATING PROVIDERS",
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                ProviderRows(
                    isTablet = isTablet,
                    settings = settings,
                    controlsEnabled = providerControlsEnabled,
                )
            }
        }
    }
}

@Composable
private fun ProviderRows(
    isTablet: Boolean,
    settings: MdbListSettings,
    controlsEnabled: Boolean,
) {
    val providers = listOf(
        MdbListMetadataService.PROVIDER_IMDB to "IMDb",
        MdbListMetadataService.PROVIDER_TMDB to "TMDB",
        MdbListMetadataService.PROVIDER_TOMATOES to "Rotten Tomatoes",
        MdbListMetadataService.PROVIDER_METACRITIC to "Metacritic",
        MdbListMetadataService.PROVIDER_TRAKT to "Trakt",
        MdbListMetadataService.PROVIDER_LETTERBOXD to "Letterboxd",
        MdbListMetadataService.PROVIDER_AUDIENCE to "Audience Score",
    )

    providers.forEachIndexed { index, (providerId, providerLabel) ->
        SettingsSwitchRow(
            title = providerLabel,
            checked = settings.isProviderEnabled(providerId),
            enabled = controlsEnabled,
            isTablet = isTablet,
            onCheckedChange = { checked ->
                MdbListSettingsRepository.setProviderEnabled(providerId, checked)
            },
        )
        if (index < providers.lastIndex) {
            SettingsGroupDivider(isTablet = isTablet)
        }
    }
}

@Composable
private fun MdbListApiKeyRow(
    isTablet: Boolean,
    value: String,
    onApiKeyCommitted: (String) -> Unit,
) {
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val verticalPadding = if (isTablet) 16.dp else 14.dp
    var draft by rememberSaveable(value) { mutableStateOf(value) }
    val normalizedDraft = draft.trim()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "MDBList API key",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "Get a key from https://mdblist.com/preferences and paste it here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OutlinedTextField(
            value = draft,
            onValueChange = {
                draft = it
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("API key") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                disabledContainerColor = MaterialTheme.colorScheme.surface,
            ),
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    draft = normalizedDraft
                    onApiKeyCommitted(normalizedDraft)
                },
                enabled = normalizedDraft != value,
            ) {
                Text("Save Key")
            }
        }
    }
}

@Composable
private fun MdbListInfoRow(
    isTablet: Boolean,
    text: String,
) {
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val verticalPadding = if (isTablet) 14.dp else 12.dp

    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
