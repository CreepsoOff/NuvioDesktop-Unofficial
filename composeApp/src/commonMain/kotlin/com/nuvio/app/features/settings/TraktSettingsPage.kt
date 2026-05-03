package com.nuvio.app.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nuvio.app.features.trakt.TraktAuthRepository
import com.nuvio.app.features.trakt.TraktBrandAsset
import com.nuvio.app.features.trakt.TraktAuthUiState
import com.nuvio.app.features.trakt.TraktConnectionMode
import com.nuvio.app.features.trakt.traktBrandPainter

internal fun LazyListScope.traktSettingsContent(
    isTablet: Boolean,
    uiState: TraktAuthUiState,
    commentsEnabled: Boolean,
    onCommentsEnabledChange: (Boolean) -> Unit,
) {
    item {
        SettingsGroup(isTablet = isTablet) {
            TraktBrandIntro(isTablet = isTablet)
        }
    }

    item {
        SettingsSection(
            title = "AUTHENTICATION",
            isTablet = isTablet,
        ) {
            SettingsGroup(isTablet = isTablet) {
                TraktConnectionCard(
                    isTablet = isTablet,
                    uiState = uiState,
                )
            }
        }
    }

    if (uiState.mode == TraktConnectionMode.CONNECTED) {
        item {
            SettingsSection(
                title = "FEATURES",
                isTablet = isTablet,
            ) {
                SettingsGroup(isTablet = isTablet) {
                    SettingsSwitchRow(
                        title = "Comments",
                        description = "Show Trakt comments on movie and show details",
                        checked = commentsEnabled,
                        isTablet = isTablet,
                        onCheckedChange = onCommentsEnabledChange,
                    )
                }
            }
        }
    }
}

@Composable
private fun TraktBrandIntro(
    isTablet: Boolean,
) {
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val verticalPadding = if (isTablet) 18.dp else 16.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.foundation.Image(
                painter = traktBrandPainter(TraktBrandAsset.Glyph),
                contentDescription = "Trakt",
                modifier = Modifier.size(if (isTablet) 84.dp else 72.dp),
                contentScale = ContentScale.Fit,
            )
            Text(
                text = "Track what you watch, save to watchlist or custom lists, and keep your library synced with Trakt.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TraktConnectionCard(
    isTablet: Boolean,
    uiState: TraktAuthUiState,
) {
    val uriHandler = LocalUriHandler.current
    val horizontalPadding = if (isTablet) 20.dp else 16.dp
    val verticalPadding = if (isTablet) 18.dp else 16.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (uiState.mode) {
            TraktConnectionMode.CONNECTED -> {
                Text(
                    text = "Connected as ${uiState.username ?: "Trakt user"}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "Your Save actions can now target Trakt watchlist and personal lists.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = TraktAuthRepository::onDisconnectRequested,
                    enabled = !uiState.isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onSurface,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp),
                        )
                    } else {
                        Text("Disconnect")
                    }
                }
            }

            TraktConnectionMode.AWAITING_APPROVAL -> {
                Text(
                    text = "Finish Trakt sign in in your browser",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "After approval, you will be redirected back automatically.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = {
                        val authUrl = TraktAuthRepository.pendingAuthorizationUrl()
                            ?: TraktAuthRepository.onConnectRequested()
                        if (authUrl == null) return@Button
                        runCatching { uriHandler.openUri(authUrl) }
                            .onFailure {
                                TraktAuthRepository.onAuthLaunchFailed(
                                    it.message ?: "Failed to open browser",
                                )
                            }
                    },
                    enabled = !uiState.isLoading,
                ) {
                    Text("Open Trakt Login")
                }
                Button(
                    onClick = TraktAuthRepository::onCancelAuthorization,
                    enabled = !uiState.isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Text("Cancel")
                }
            }

            TraktConnectionMode.DISCONNECTED -> {
                Text(
                    text = "Sign in with Trakt to enable list-based saving and Trakt library mode.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = {
                        val authUrl = TraktAuthRepository.onConnectRequested() ?: return@Button
                        runCatching { uriHandler.openUri(authUrl) }
                            .onFailure {
                                TraktAuthRepository.onAuthLaunchFailed(
                                    it.message ?: "Failed to open browser",
                                )
                            }
                    },
                    enabled = uiState.credentialsConfigured && !uiState.isLoading,
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp),
                        )
                    } else {
                        Text("Connect Trakt")
                    }
                }
                if (!uiState.credentialsConfigured) {
                    Text(
                        text = "Missing Trakt credentials in local.properties (TRAKT_CLIENT_ID / TRAKT_CLIENT_SECRET).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }

        uiState.statusMessage?.takeIf { it.isNotBlank() }?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        uiState.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}
