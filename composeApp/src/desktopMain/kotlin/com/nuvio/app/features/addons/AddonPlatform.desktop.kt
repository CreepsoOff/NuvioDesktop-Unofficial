package com.nuvio.app.features.addons

import com.nuvio.app.desktop.DesktopPreferences
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal actual object AddonStorage {
    private const val preferencesName = "nuvio_addons"
    private const val addonUrlsKey = "installed_manifest_urls"

    actual fun loadInstalledAddonUrls(profileId: Int): List<String> =
        DesktopPreferences.getString(preferencesName, "${addonUrlsKey}_$profileId")
            .orEmpty()
            .lineSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toList()

    actual fun saveInstalledAddonUrls(profileId: Int, urls: List<String>) {
        DesktopPreferences.putString(
            preferencesName,
            "${addonUrlsKey}_$profileId",
            urls.joinToString(separator = "\n"),
        )
    }
}

private val addonHttpClient: HttpClient = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(60))
    .followRedirects(HttpClient.Redirect.NORMAL)
    .build()

private const val maxRawResponseBodyChars = 1024 * 1024
private const val truncationSuffix = "\n...[truncated]"

private fun requestAllowsBody(method: String): Boolean =
    when (method.uppercase()) {
        "POST", "PUT", "PATCH", "DELETE" -> true
        else -> false
    }

private fun Map<String, String>.withoutAcceptEncoding(): Map<String, String> =
    entries
        .filterNot { (key, _) -> key.equals("Accept-Encoding", ignoreCase = true) }
        .associate { (key, value) -> key to value }

private suspend fun executeRequest(
    method: String,
    url: String,
    headers: Map<String, String>,
    body: String,
) = withContext(Dispatchers.IO) {
    val builder = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .timeout(Duration.ofSeconds(60))

    headers.withoutAcceptEncoding().forEach { (key, value) ->
        builder.header(key, value)
    }

    val request = if (requestAllowsBody(method)) {
        builder.method(method.uppercase(), HttpRequest.BodyPublishers.ofString(body))
    } else {
        builder.method(method.uppercase(), HttpRequest.BodyPublishers.noBody())
    }.build()

    addonHttpClient.send(request, HttpResponse.BodyHandlers.ofString())
}

private suspend fun executeTextRequest(
    method: String,
    url: String,
    headers: Map<String, String> = emptyMap(),
    body: String = "",
): String {
    val response = executeRequest(method, url, headers, body)
    val payload = response.body()
    if (response.statusCode() !in 200..299) {
        error("Request failed with HTTP ${response.statusCode()}")
    }
    if (payload.isBlank()) {
        throw IllegalStateException("Empty response body")
    }
    return payload
}

actual suspend fun httpGetText(url: String): String =
    executeTextRequest(
        method = "GET",
        url = url,
        headers = mapOf("Accept" to "application/json"),
    )

actual suspend fun httpPostJson(url: String, body: String): String =
    executeTextRequest(
        method = "POST",
        url = url,
        headers = mapOf(
            "Accept" to "application/json",
            "Content-Type" to "application/json",
        ),
        body = body,
    )

actual suspend fun httpGetTextWithHeaders(
    url: String,
    headers: Map<String, String>,
): String =
    executeTextRequest(
        method = "GET",
        url = url,
        headers = mapOf("Accept" to "application/json") + headers,
    )

actual suspend fun httpPostJsonWithHeaders(
    url: String,
    body: String,
    headers: Map<String, String>,
): String =
    executeTextRequest(
        method = "POST",
        url = url,
        headers = mapOf(
            "Accept" to "application/json",
            "Content-Type" to "application/json",
        ) + headers,
        body = body,
    )

actual suspend fun httpRequestRaw(
    method: String,
    url: String,
    headers: Map<String, String>,
    body: String,
): RawHttpResponse {
    val response = executeRequest(method, url, headers, body)
    val payload = response.body()
    val limitedPayload = if (payload.length > maxRawResponseBodyChars) {
        payload.take(maxRawResponseBodyChars) + truncationSuffix
    } else {
        payload
    }
    return RawHttpResponse(
        status = response.statusCode(),
        statusText = response.version().toString(),
        url = response.uri().toString(),
        body = limitedPayload,
        headers = response.headers().map().entries.associate { (key, values) ->
            key.lowercase() to values.joinToString(",")
        },
    )
}