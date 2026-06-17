package com.nuvio.app.features.home.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.size.Precision
import coil3.size.Size
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Codec
import org.jetbrains.skia.Data
import org.jetbrains.skia.Image as SkiaImage
import kotlin.math.max

internal const val DefaultGifDelayMs = 100
private const val DecodeSizeBucketPx = 32
private const val FallbackDecodeDimensionPx = 360
private const val MaxDecodedGifEntries = 8
private const val MaxDecodedDimensionPx = 1920
private const val MaxGifSourceBytes = 16L * 1024 * 1024
private const val MaxDecodedGifBytes = 64L * 1024 * 1024
private const val MaxDecodedGifBytesTotal = 192L * 1024 * 1024
private const val MaxLogicalGifPixels = 4096L * 4096L
private const val MaxGifDecodeUpscale = 3.0

private data class DesktopGifCacheKey(
    val url: String,
    val widthPx: Int,
    val heightPx: Int,
)

internal data class GifDecodeTarget(
    val widthPx: Int,
    val heightPx: Int,
)

internal data class DecodedDesktopGif(
    val frames: List<ImageBitmap>,
    val delaysMs: IntArray,
    val approxBytes: Long,
)

private object DesktopDecodedGifCache {
    private var totalBytes: Long = 0
    private val map = LinkedHashMap<DesktopGifCacheKey, DecodedDesktopGif>(16, 0.75f, true)

    @Synchronized
    fun get(key: DesktopGifCacheKey): DecodedDesktopGif? = map[key]

    @Synchronized
    fun put(key: DesktopGifCacheKey, gif: DecodedDesktopGif) {
        if (gif.approxBytes > MaxDecodedGifBytes) return
        map.remove(key)?.let { totalBytes -= it.approxBytes }
        map[key] = gif
        totalBytes += gif.approxBytes
        while ((map.size > MaxDecodedGifEntries || totalBytes > MaxDecodedGifBytesTotal) && map.isNotEmpty()) {
            val eldest = map.entries.first()
            map.remove(eldest.key)
            totalBytes -= eldest.value.approxBytes
        }
    }
}

private object DesktopGifInFlight {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val requests = mutableMapOf<DesktopGifCacheKey, Deferred<DecodedDesktopGif?>>()

    suspend fun getOrDecode(
        key: DesktopGifCacheKey,
        decode: suspend () -> DecodedDesktopGif?,
    ): DecodedDesktopGif? {
        DesktopDecodedGifCache.get(key)?.let { return it }

        val request = synchronized(requests) {
            requests[key] ?: scope.async {
                DesktopDecodedGifCache.get(key) ?: decode()?.also { decoded ->
                    DesktopDecodedGifCache.put(key, decoded)
                }
            }.also { deferred ->
                requests[key] = deferred
                deferred.invokeOnCompletion {
                    synchronized(requests) {
                        if (requests[key] === deferred) {
                            requests.remove(key)
                        }
                    }
                }
            }
        }

        return request.await()
    }
}

private val desktopGifHttpClient by lazy { HttpClient(CIO) }

private sealed interface DesktopGifState {
    data object Loading : DesktopGifState
    data class Ready(val gif: DecodedDesktopGif) : DesktopGifState
    data object UseStaticCoil : DesktopGifState
}

@Composable
internal actual fun CollectionCardRemoteImage(
    imageUrl: String,
    animatedImageUrl: String?,
    contentDescription: String,
    modifier: Modifier,
    contentScale: ContentScale,
    animateIfPossible: Boolean,
    animateNow: Boolean,
) {
    val gifUrl = animatedImageUrl?.takeIf { animateIfPossible && it.isNotBlank() }
    val shouldAnimate = gifUrl != null && animateNow

    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val platformContext = LocalPlatformContext.current
        val targetWidthPx = maxWidth.value
            .takeIf { it.isFinite() && it > 0f }
            ?.let { with(density) { maxWidth.roundToPx() } }
            ?: FallbackDecodeDimensionPx
        val targetHeightPx = maxHeight.value
            .takeIf { it.isFinite() && it > 0f }
            ?.let { with(density) { maxHeight.roundToPx() } }
            ?: FallbackDecodeDimensionPx

        val staticDecodeWidthPx = targetWidthPx.coerceAtLeast(1)
        val staticDecodeHeightPx = targetHeightPx.coerceAtLeast(1)

        val decodeTarget = remember(targetWidthPx, targetHeightPx) {
            GifDecodeTarget(
                widthPx = targetWidthPx.roundUpToDecodeBucket().coerceIn(1, MaxDecodedDimensionPx),
                heightPx = targetHeightPx.roundUpToDecodeBucket().coerceIn(1, MaxDecodedDimensionPx),
            )
        }
        val staticRequest = remember(
            platformContext,
            imageUrl,
            staticDecodeWidthPx,
            staticDecodeHeightPx,
        ) {
            ImageRequest.Builder(platformContext)
                .data(imageUrl)
                .size(Size(staticDecodeWidthPx, staticDecodeHeightPx))
                .precision(Precision.EXACT)
                .memoryCacheKey(
                    "home-collection-static:$staticDecodeWidthPx:$staticDecodeHeightPx:${imageUrl.hashCode()}",
                )
                .diskCacheKey(imageUrl)
                .build()
        }
        val cacheKey = remember(gifUrl, decodeTarget) {
            gifUrl?.let { url ->
                DesktopGifCacheKey(
                    url = url,
                    widthPx = decodeTarget.widthPx,
                    heightPx = decodeTarget.heightPx,
                )
            }
        }
        val cachedGif = remember(cacheKey) {
            cacheKey?.let(DesktopDecodedGifCache::get)
        }
        var state by remember(cacheKey) {
            mutableStateOf<DesktopGifState>(
                cachedGif?.let(DesktopGifState::Ready) ?: DesktopGifState.Loading,
            )
        }

        LaunchedEffect(cacheKey, gifUrl) {
            if (cacheKey == null || gifUrl == null) {
                state = DesktopGifState.UseStaticCoil
                return@LaunchedEffect
            }
            cachedGif?.let {
                state = DesktopGifState.Ready(it)
                return@LaunchedEffect
            }

            state = DesktopGifState.Loading
            val decoded = DesktopGifInFlight.getOrDecode(cacheKey) {
                downloadAndDecodeGif(gifUrl, decodeTarget)
            }

            state = if (decoded != null) {
                DesktopGifState.Ready(decoded)
            } else {
                DesktopGifState.UseStaticCoil
            }
        }

        AsyncImage(
            model = staticRequest,
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale,
            filterQuality = FilterQuality.High,
        )
        if (shouldAnimate && state is DesktopGifState.Ready) {
            val readyState = state as DesktopGifState.Ready
            var gifLoaded by remember { mutableStateOf(false) }
            val gifAlpha by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (gifLoaded) 1f else 0f,
                animationSpec = androidx.compose.animation.core.tween(200),
                label = "gifFadeIn",
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = gifAlpha },
            ) {
                AnimatedComposeGif(
                    gif = readyState.gif,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale,
                    isAnimating = true,
                )
            }
            LaunchedEffect(readyState) {
                gifLoaded = true
            }
        }
    }
}

@Composable
private fun AnimatedComposeGif(
    gif: DecodedDesktopGif,
    contentDescription: String,
    modifier: Modifier,
    contentScale: ContentScale,
    isAnimating: Boolean = true,
) {
    if (gif.frames.isEmpty()) return
    var frameIndex by remember(gif) { mutableIntStateOf(0) }

    if (isAnimating && gif.frames.size > 1) {
        LaunchedEffect(gif) {
            var lastFrameNanos = 0L
            var accumulatedNanos = 0L
            while (true) {
                val now = androidx.compose.runtime.withFrameNanos { it }
                if (lastFrameNanos != 0L) {
                    accumulatedNanos += now - lastFrameNanos
                }
                lastFrameNanos = now

                val frameDelayNanos = gif.delaysMs
                    .getOrElse(frameIndex) { DefaultGifDelayMs }
                    .coerceAtLeast(10)
                    .toLong() * 1_000_000L

                if (accumulatedNanos >= frameDelayNanos) {
                    var advance = (accumulatedNanos / frameDelayNanos)
                        .toInt()
                        .coerceAtMost(gif.frames.size)
                    if (advance < 1) advance = 1
                    frameIndex = (frameIndex + advance) % gif.frames.size
                    accumulatedNanos -= advance * frameDelayNanos
                }
            }
        }
    }

    Image(
        bitmap = gif.frames[frameIndex],
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        filterQuality = FilterQuality.High,
    )
}

private suspend fun downloadAndDecodeGif(
    imageUrl: String,
    target: GifDecodeTarget,
): DecodedDesktopGif? = withContext(Dispatchers.IO) {
    runCatching {
        val response = desktopGifHttpClient.get(imageUrl)
        if (!response.status.isSuccess()) return@runCatching null
        val contentLength = response.headers[HttpHeaders.ContentLength]?.toLongOrNull()
        if (contentLength != null && contentLength > MaxGifSourceBytes) return@runCatching null
        val bytes = response.body<ByteArray>()
        if (bytes.size.toLong() > MaxGifSourceBytes) return@runCatching null
        decodeGifForCompose(bytes, target)
    }.getOrNull()
}

private fun decodeGifForCompose(
    bytes: ByteArray,
    target: GifDecodeTarget,
): DecodedDesktopGif? = decodeGifForComposeWithSkia(bytes, target)

internal fun decodeGifForComposeWithSkia(
    bytes: ByteArray,
    target: GifDecodeTarget,
): DecodedDesktopGif? {
    if (!bytes.isGifHeader()) return null

    val data = Data.makeFromBytes(bytes)
    try {
        val codec = Codec.makeFromData(data)
        try {
            val frameCount = codec.frameCount
            if (frameCount <= 0) return null

            val baseW = codec.size.x.coerceAtLeast(1)
            val baseH = codec.size.y.coerceAtLeast(1)
            if (baseW.toLong() * baseH.toLong() > MaxLogicalGifPixels) return null

            val coverScale = max(
                target.widthPx.toDouble() / baseW.toDouble(),
                target.heightPx.toDouble() / baseH.toDouble(),
            )
            val scale = coverScale.coerceAtMost(MaxGifDecodeUpscale)
            val canvasW = max(1, (baseW * scale).toInt())
            val canvasH = max(1, (baseH * scale).toInt())
            val approxBytes = frameCount.toLong() * canvasW.toLong() * canvasH.toLong() * 4L
            if (approxBytes > MaxDecodedGifBytes) return null

            val bitmap = Bitmap()
            try {
                bitmap.allocPixels(codec.imageInfo)
                val outFrames = ArrayList<ImageBitmap>(frameCount)
                val outDelays = IntArray(frameCount)
                var priorFrame = -1

                for (frame in 0 until frameCount) {
                    codec.readPixels(bitmap, frame, priorFrame)
                    val image = SkiaImage.makeFromBitmap(bitmap)
                    try {
                        outFrames += image.toComposeImageBitmap()
                    } finally {
                        image.close()
                    }
                    val durationMs = codec.getFrameInfo(frame).duration
                    outDelays[frame] = durationMs.takeIf { it > 0 } ?: DefaultGifDelayMs
                    priorFrame = frame
                }

                if (outFrames.isEmpty()) return null
                return DecodedDesktopGif(
                    frames = outFrames,
                    delaysMs = outDelays,
                    approxBytes = approxBytes,
                )
            } finally {
                bitmap.close()
            }
        } finally {
            codec.close()
        }
    } catch (_: Throwable) {
        return null
    } finally {
        data.close()
    }
}

private fun Int.roundUpToDecodeBucket(): Int {
    if (this <= 0) return FallbackDecodeDimensionPx
    return (((this + DecodeSizeBucketPx - 1) / DecodeSizeBucketPx) * DecodeSizeBucketPx)
        .coerceAtLeast(DecodeSizeBucketPx)
}

internal fun ByteArray.isGifHeader(): Boolean =
    size >= 6 &&
        this[0] == 'G'.code.toByte() &&
        this[1] == 'I'.code.toByte() &&
        this[2] == 'F'.code.toByte() &&
        this[3] == '8'.code.toByte() &&
        (this[4] == '7'.code.toByte() || this[4] == '9'.code.toByte()) &&
        this[5] == 'a'.code.toByte()
