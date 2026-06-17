package com.nuvio.app.features.plugins

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.refTo
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import com.nuvio.app.features.plugins.cryptointerop.CC_MD5
import com.nuvio.app.features.plugins.cryptointerop.CC_MD5_DIGEST_LENGTH
import com.nuvio.app.features.plugins.cryptointerop.CC_SHA1
import com.nuvio.app.features.plugins.cryptointerop.CC_SHA1_DIGEST_LENGTH
import com.nuvio.app.features.plugins.cryptointerop.CC_SHA256
import com.nuvio.app.features.plugins.cryptointerop.CC_SHA256_DIGEST_LENGTH
import com.nuvio.app.features.plugins.cryptointerop.CC_SHA512
import com.nuvio.app.features.plugins.cryptointerop.CC_SHA512_DIGEST_LENGTH
import com.nuvio.app.features.plugins.cryptointerop.CCHmac
import com.nuvio.app.features.plugins.cryptointerop.kCCHmacAlgMD5
import com.nuvio.app.features.plugins.cryptointerop.kCCHmacAlgSHA1
import com.nuvio.app.features.plugins.cryptointerop.kCCHmacAlgSHA256
import com.nuvio.app.features.plugins.cryptointerop.kCCHmacAlgSHA512

private fun UByteArray.toHex(): String = joinToString(separator = "") { byte ->
    byte.toString(16).padStart(2, '0')
}

@OptIn(ExperimentalForeignApi::class)
internal fun pluginDigestHex(algorithm: String, data: String): String {
    val normalized = algorithm.uppercase()
    val input = data.encodeToByteArray()
    val output = UByteArray(
        when (normalized) {
            "MD5" -> CC_MD5_DIGEST_LENGTH.toInt()
            "SHA1" -> CC_SHA1_DIGEST_LENGTH.toInt()
            "SHA256" -> CC_SHA256_DIGEST_LENGTH.toInt()
            "SHA512" -> CC_SHA512_DIGEST_LENGTH.toInt()
            else -> error("Unsupported digest algorithm: $algorithm")
        },
    )

    when (normalized) {
        "MD5" -> CC_MD5(input.refTo(0), input.size.toUInt(), output.refTo(0))
        "SHA1" -> CC_SHA1(input.refTo(0), input.size.toUInt(), output.refTo(0))
        "SHA256" -> CC_SHA256(input.refTo(0), input.size.toUInt(), output.refTo(0))
        "SHA512" -> CC_SHA512(input.refTo(0), input.size.toUInt(), output.refTo(0))
    }

    return output.toHex()
}

@OptIn(ExperimentalForeignApi::class)
internal fun pluginHmacHex(algorithm: String, key: String, data: String): String {
    val normalized = algorithm.uppercase()
    val keyBytes = key.encodeToByteArray()
    val input = data.encodeToByteArray()

    val (alg, outputSize) = when (normalized) {
        "MD5" -> kCCHmacAlgMD5 to CC_MD5_DIGEST_LENGTH.toInt()
        "SHA1" -> kCCHmacAlgSHA1 to CC_SHA1_DIGEST_LENGTH.toInt()
        "SHA256" -> kCCHmacAlgSHA256 to CC_SHA256_DIGEST_LENGTH.toInt()
        "SHA512" -> kCCHmacAlgSHA512 to CC_SHA512_DIGEST_LENGTH.toInt()
        else -> error("Unsupported HMAC algorithm: $algorithm")
    }

    val output = UByteArray(outputSize)
    CCHmac(
        alg,
        keyBytes.refTo(0),
        keyBytes.size.toULong(),
        input.refTo(0),
        input.size.toULong(),
        output.refTo(0),
    )

    return output.toHex()
}

@OptIn(ExperimentalEncodingApi::class)
internal fun pluginBase64Encode(data: String): String =
    Base64.encode(data.encodeToByteArray())

@OptIn(ExperimentalEncodingApi::class)
internal fun pluginBase64Decode(data: String): String {
    val normalized = data.trim().replace("\n", "").replace("\r", "").replace(" ", "")
    val decoded = Base64.decode(normalized)
    return decoded.decodeToString()
}

internal fun pluginUtf8ToHex(value: String): String =
    value.encodeToByteArray().joinToString(separator = "") { byte ->
        byte.toUByte().toString(16).padStart(2, '0')
    }

internal fun pluginHexToUtf8(hex: String): String {
    val normalized = hex.trim().lowercase()
        .replace(" ", "")
        .removePrefix("0x")
    if (normalized.isEmpty()) return ""

    val evenHex = if (normalized.length % 2 == 0) normalized else "0$normalized"
    val out = ByteArray(evenHex.length / 2)
    for (index in out.indices) {
        val part = evenHex.substring(index * 2, index * 2 + 2)
        out[index] = part.toInt(16).toByte()
    }
    return out.decodeToString()
}
