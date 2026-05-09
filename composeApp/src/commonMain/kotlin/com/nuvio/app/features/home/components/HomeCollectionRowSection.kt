package com.nuvio.app.features.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nuvio.app.core.ui.NuvioShelfSection
import com.nuvio.app.core.ui.PosterLandscapeAspectRatio
import com.nuvio.app.core.ui.landscapePosterWidth
import com.nuvio.app.core.ui.posterCardClickable
import com.nuvio.app.core.ui.rememberPosterCardStyleUiState
import com.nuvio.app.features.collection.Collection
import com.nuvio.app.features.collection.CollectionFolder
import com.nuvio.app.features.home.HomeCatalogSettingsRepository
import com.nuvio.app.features.home.PosterShape
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun HomeCollectionRowSection(
    collection: Collection,
    modifier: Modifier = Modifier,
    sectionPadding: Dp? = null,
    onFolderClick: ((collectionId: String, folderId: String) -> Unit)? = null,
) {
    if (collection.folders.isEmpty()) return

    if (sectionPadding != null) {
        HomeCollectionRowSectionContent(
            collection = collection,
            modifier = modifier.fillMaxWidth(),
            sectionPadding = sectionPadding,
            onFolderClick = onFolderClick,
        )
    } else {
        BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
            HomeCollectionRowSectionContent(
                collection = collection,
                modifier = Modifier.fillMaxWidth(),
                sectionPadding = homeSectionHorizontalPaddingForWidth(maxWidth.value),
                onFolderClick = onFolderClick,
            )
        }
    }
}

@Composable
private fun HomeCollectionRowSectionContent(
    collection: Collection,
    modifier: Modifier,
    sectionPadding: Dp,
    onFolderClick: ((collectionId: String, folderId: String) -> Unit)?,
) {
    val homeSettings by HomeCatalogSettingsRepository.uiState.collectAsStateWithLifecycle()
    NuvioShelfSection(
        title = collection.title,
        entries = collection.folders,
        modifier = modifier,
        headerHorizontalPadding = sectionPadding,
        rowContentPadding = PaddingValues(horizontal = sectionPadding),
        key = { folder -> "collection_${collection.id}_folder_${folder.id}" },
    ) { folder ->
        val folderClick = onFolderClick?.let { callback ->
            remember(collection.id, folder.id, callback) { { callback(collection.id, folder.id) } }
        }
        CollectionFolderCard(
            folder = folder,
            alwaysAnimateGif = homeSettings.alwaysAnimateCollectionGifs,
            onClick = folderClick,
        )
    }
}

@Composable
private fun CollectionFolderCard(
    folder: CollectionFolder,
    alwaysAnimateGif: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val posterCardStyle = rememberPosterCardStyleUiState()
    val isLandscapeMode = posterCardStyle.catalogLandscapeModeEnabled
    val shape = if (isLandscapeMode) PosterShape.Landscape else folder.posterShape
    val cardWidth: Dp
    val aspectRatio: Float

    when (shape) {
        PosterShape.Poster -> {
            cardWidth = posterCardStyle.widthDp.dp
            aspectRatio = 0.675f
        }
        PosterShape.Landscape -> {
            cardWidth = landscapePosterWidth(posterCardStyle.widthDp)
            aspectRatio = PosterLandscapeAspectRatio
        }
        PosterShape.Square -> {
            cardWidth = posterCardStyle.widthDp.dp
            aspectRatio = 1f
        }
    }

    Column(
        modifier = modifier.width(cardWidth),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val shapeCorner = RoundedCornerShape(posterCardStyle.cornerRadiusDp.dp)
        val interactionSource = remember(folder.id) { MutableInteractionSource() }
        val isHovered by interactionSource.collectIsHoveredAsState()
        val gifUrl = firstNonBlank(folder.focusGifUrl)?.takeIf { folder.focusGifEnabled }
        val coverUrl = firstNonBlank(folder.coverImageUrl)
        val shouldAnimateGif = shouldAnimateCollectionGif(
            hasGif = gifUrl != null,
            alwaysAnimateGif = alwaysAnimateGif,
            isHovered = isHovered,
        )
        val imageUrl = when {
            shouldAnimateGif -> gifUrl
            !coverUrl.isNullOrBlank() -> coverUrl
            else -> gifUrl
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .hoverable(interactionSource),
            shape = shapeCorner,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp,
            ),
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    !imageUrl.isNullOrBlank() -> {
                        CollectionCardRemoteImage(
                            imageUrl = imageUrl,
                            contentDescription = folder.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            animateIfPossible = shouldAnimateGif && imageUrl == gifUrl,
                        )
                    }
                    !folder.coverEmoji.isNullOrBlank() -> {
                        Text(
                            text = folder.coverEmoji,
                            fontSize = 36.sp,
                        )
                    }
                    else -> {
                        Text(
                            text = folder.title.take(2).uppercase(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }

                if (onClick != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .posterCardClickable(onClick = onClick, onLongClick = null),
                    )
                }
            }
        }

        if (!folder.hideTitle) {
            Text(
                text = folder.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun firstNonBlank(
    first: String?,
    second: String? = null,
    third: String? = null,
    fourth: String? = null,
): String? {
    first?.takeIf { it.isNotBlank() }?.trim()?.let { return it }
    second?.takeIf { it.isNotBlank() }?.trim()?.let { return it }
    third?.takeIf { it.isNotBlank() }?.trim()?.let { return it }
    fourth?.takeIf { it.isNotBlank() }?.trim()?.let { return it }
    return null
}

private fun shouldAnimateCollectionGif(
    hasGif: Boolean,
    alwaysAnimateGif: Boolean,
    isHovered: Boolean,
): Boolean = hasGif && (alwaysAnimateGif || isHovered)
