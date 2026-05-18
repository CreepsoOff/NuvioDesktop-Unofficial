package com.nuvio.app.desktop

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Base64
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteExisting
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.outputStream
import kotlin.io.path.readText
import kotlin.io.path.writeText

internal object DesktopPreferences {
    private const val setSeparator = "\u001F"
    private val keyEncoder = Base64.getUrlEncoder().withoutPadding()

    private val rootDir: Path by lazy {
        Paths.get(
            System.getProperty("user.home"),
            "Library",
            "Application Support",
            "Nuvio",
            "preferences",
        ).apply {
            createDirectories()
        }
    }

    private fun namespaceDir(namespace: String): Path =
        rootDir.resolve(encodePathPart(namespace)).apply {
            createDirectories()
        }

    private fun keyFile(namespace: String, key: String): Path =
        namespaceDir(namespace).resolve(encodePathPart(key))

    private fun encodePathPart(value: String): String =
        keyEncoder.encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    fun contains(namespace: String, key: String): Boolean =
        keyFile(namespace, key).exists()

    fun getString(namespace: String, key: String): String? =
        keyFile(namespace, key)
            .takeIf(Path::exists)
            ?.readText(StandardCharsets.UTF_8)

    @Synchronized
    fun putString(namespace: String, key: String, value: String) {
        keyFile(namespace, key).writeText(value, StandardCharsets.UTF_8)
    }

    fun putNullableString(namespace: String, key: String, value: String?) {
        if (value == null) {
            remove(namespace, key)
        } else {
            putString(namespace, key, value)
        }
    }

    fun getBoolean(namespace: String, key: String): Boolean? =
        getString(namespace, key)?.toBooleanStrictOrNull()

    @Synchronized
    fun putBoolean(namespace: String, key: String, value: Boolean) {
        putString(namespace, key, value.toString())
    }

    fun getInt(namespace: String, key: String): Int? =
        getString(namespace, key)?.toIntOrNull()

    @Synchronized
    fun putInt(namespace: String, key: String, value: Int) {
        putString(namespace, key, value.toString())
    }

    fun getFloat(namespace: String, key: String): Float? =
        getString(namespace, key)?.toFloatOrNull()

    @Synchronized
    fun putFloat(namespace: String, key: String, value: Float) {
        putString(namespace, key, value.toString())
    }

    fun getStringSet(namespace: String, key: String): Set<String>? {
        val raw = getString(namespace, key) ?: return null
        if (raw.isEmpty()) return emptySet()
        return raw.split(setSeparator)
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toSet()
    }

    fun putStringSet(namespace: String, key: String, values: Set<String>) {
        putString(namespace, key, values.toSortedSet().joinToString(setSeparator))
    }

    @Synchronized
    fun remove(namespace: String, key: String) {
        runCatching {
            keyFile(namespace, key).deleteExisting()
        }
    }

    @Synchronized
    fun clearNode(namespace: String) {
        runCatching {
            deleteRecursively(namespaceDir(namespace))
        }
    }

    private fun deleteRecursively(path: Path) {
        if (!path.exists()) return
        if (path.isDirectory()) {
            path.listDirectoryEntries().forEach(::deleteRecursively)
        }
        path.deleteExisting()
    }
}