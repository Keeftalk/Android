package tn.keeftalk.chat.feature.secrets.crypto

import java.net.URLDecoder
import java.nio.ByteBuffer
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.pow

data class ParsedOtpUri(
    val serviceName: String,
    val username: String,
    val secretKey: String,
    val issuer: String = "",
    val algorithm: String = "SHA1",
    val digits: Int = 6,
    val period: Int = 30
)

object TotpGenerator {

    private const val BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    /**
     * Decodes a Base32 string into a ByteArray according to RFC 4648.
     */
    fun decodeBase32(secret: String): ByteArray {
        val cleanSecret = secret.uppercase(Locale.US)
            .replace("\\s+".toRegex(), "")
            .replace("-", "")
            .trimEnd('=')

        if (cleanSecret.isEmpty()) {
            throw IllegalArgumentException("Empty Base32 secret key")
        }

        var buffer = 0
        var bitsLeft = 0
        val result = mutableListOf<Byte>()

        for (ch in cleanSecret) {
            val value = BASE32_ALPHABET.indexOf(ch)
            if (value < 0) continue
            buffer = (buffer shl 5) or value
            bitsLeft += 5
            if (bitsLeft >= 8) {
                bitsLeft -= 8
                result.add((buffer shr bitsLeft).toByte())
                buffer = buffer and ((1 shl bitsLeft) - 1)
            }
        }
        return result.toByteArray()
    }

    /**
     * Generates a TOTP code from raw key bytes according to RFC 6238.
     */
    fun generateCodeFromBytes(
        keyBytes: ByteArray,
        timeSeconds: Long = System.currentTimeMillis() / 1000L,
        period: Int = 30,
        digits: Int = 6,
        algorithm: String = "SHA1"
    ): String {
        val timeStep = timeSeconds / period
        val buffer = ByteBuffer.allocate(8).putLong(timeStep).array()
        val macAlg = when (algorithm.uppercase(Locale.US)) {
            "SHA256", "HMACSHA256" -> "HmacSHA256"
            "SHA512", "HMACSHA512" -> "HmacSHA512"
            else -> "HmacSHA1"
        }

        val mac = Mac.getInstance(macAlg)
        mac.init(SecretKeySpec(keyBytes, macAlg))
        val hash = mac.doFinal(buffer)

        val offset = hash[hash.size - 1].toInt() and 0x0F
        val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                (hash[offset + 3].toInt() and 0xFF)

        val modulus = 10.0.pow(digits.toDouble()).toInt()
        val codeInt = binary % modulus

        return String.format(Locale.US, "%0${digits}d", codeInt)
    }

    /**
     * Generates a TOTP code from a Base32 secret according to RFC 6238.
     */
    fun generateCode(
        secret: String,
        timeSeconds: Long = System.currentTimeMillis() / 1000L,
        period: Int = 30,
        digits: Int = 6,
        algorithm: String = "SHA1"
    ): String {
        val keyBytes = decodeBase32(secret)
        return generateCodeFromBytes(keyBytes, timeSeconds, period, digits, algorithm)
    }

    /**
     * Generates Steam Guard 5-character alphanumeric code.
     */
    fun generateSteamCode(secret: String, timeSeconds: Long = System.currentTimeMillis() / 1000L): String {
        val keyBytes = try { decodeBase32(secret) } catch (e: Exception) { secret.toByteArray(Charsets.UTF_8) }
        val timeStep = timeSeconds / 30L
        val buffer = ByteBuffer.allocate(8).putLong(timeStep).array()

        val mac = Mac.getInstance("HmacSHA1")
        mac.init(SecretKeySpec(keyBytes, "HmacSHA1"))
        val hash = mac.doFinal(buffer)

        val offset = hash[hash.size - 1].toInt() and 0x0F
        var fullCode = ((hash[offset].toInt() and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                (hash[offset + 3].toInt() and 0xFF)

        val steamAlphabet = "23456789BCDFGHJKMNPQRTVWXY"
        val code = StringBuilder()
        repeat(5) {
            code.append(steamAlphabet[fullCode % steamAlphabet.length])
            fullCode /= steamAlphabet.length
        }
        return code.toString()
    }

    /**
     * Generates HOTP counter-based code according to RFC 4226.
     */
    fun generateHotpCode(
        secret: String,
        counter: Long,
        digits: Int = 6,
        algorithm: String = "SHA1"
    ): String {
        val keyBytes = decodeBase32(secret)
        val buffer = ByteBuffer.allocate(8).putLong(counter).array()
        val macAlg = when (algorithm.uppercase(Locale.US)) {
            "SHA256", "HMACSHA256" -> "HmacSHA256"
            "SHA512", "HMACSHA512" -> "HmacSHA512"
            else -> "HmacSHA1"
        }

        val mac = Mac.getInstance(macAlg)
        mac.init(SecretKeySpec(keyBytes, macAlg))
        val hash = mac.doFinal(buffer)

        val offset = hash[hash.size - 1].toInt() and 0x0F
        val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                (hash[offset + 3].toInt() and 0xFF)

        val modulus = 10.0.pow(digits.toDouble()).toInt()
        val codeInt = binary % modulus

        return String.format(Locale.US, "%0${digits}d", codeInt)
    }

    /**
     * Generates Mobile OTP (mOTP) code using MD5 hash.
     */
    fun generateMotpCode(
        secret: String,
        pin: String,
        timeSeconds: Long = System.currentTimeMillis() / 1000L
    ): String {
        val epochMin = timeSeconds / 60L
        val data = "$epochMin$secret$pin"
        val md5 = java.security.MessageDigest.getInstance("MD5").digest(data.toByteArray(Charsets.UTF_8))
        val hex = md5.take(3).joinToString("") { "%02x".format(it) }
        return hex.take(6)
    }

    /**
     * Master OTP Code Generator supporting TOTP, HOTP, STEAM, YANDEX, MOTP.
     */
    fun generateOtpCode(
        type: String = "TOTP",
        secret: String,
        timeSeconds: Long = System.currentTimeMillis() / 1000L,
        counter: Long = 0L,
        period: Int = 30,
        digits: Int = 6,
        algorithm: String = "SHA1",
        pin: String = ""
    ): String {
        return when (type.uppercase(Locale.US)) {
            "STEAM" -> generateSteamCode(secret, timeSeconds)
            "HOTP" -> generateHotpCode(secret, counter, digits, algorithm)
            "MOTP" -> generateMotpCode(secret, pin, timeSeconds)
            else -> generateCode(secret, timeSeconds, period, digits, algorithm) // TOTP, YANDEX, etc.
        }
    }

    /**
     * Calculates remaining seconds in the current TOTP period.
     */
    fun getRemainingSeconds(
        timeSeconds: Long = System.currentTimeMillis() / 1000L,
        period: Int = 30
    ): Int {
        val rem = (period - (timeSeconds % period)).toInt()
        return if (rem <= 0) period else rem
    }

    /**
     * Parses an otpauth:// URI string without relying on android.net.Uri.
     */
    fun parseOtpUri(uriString: String): ParsedOtpUri? {
        return try {
            if (!uriString.startsWith("otpauth://totp/", ignoreCase = true)) {
                return null
            }
            val withoutScheme = uriString.substring("otpauth://totp/".length)
            val parts = withoutScheme.split("?", limit = 2)
            val rawLabel = URLDecoder.decode(parts[0], "UTF-8")
            val queryMap = mutableMapOf<String, String>()

            if (parts.size > 1) {
                parts[1].split("&").forEach { param ->
                    val kv = param.split("=", limit = 2)
                    if (kv.size == 2) {
                        queryMap[kv[0].lowercase(Locale.US)] = URLDecoder.decode(kv[1], "UTF-8")
                    }
                }
            }

            val secret = queryMap["secret"] ?: return null
            var issuer = queryMap["issuer"] ?: ""
            var label = rawLabel

            if (label.contains(":")) {
                val labelParts = label.split(":", limit = 2)
                if (issuer.isBlank()) issuer = labelParts[0].trim()
                label = labelParts[1].trim()
            }

            val serviceName = if (issuer.isNotBlank()) issuer else label.ifBlank { "Account" }
            val username = if (issuer.isNotBlank() && label != issuer) label else ""

            val algorithm = queryMap["algorithm"]?.uppercase(Locale.US) ?: "SHA1"
            val digits = queryMap["digits"]?.toIntOrNull() ?: 6
            val period = queryMap["period"]?.toIntOrNull() ?: 30

            ParsedOtpUri(
                serviceName = serviceName,
                username = username,
                secretKey = secret,
                issuer = issuer,
                algorithm = algorithm,
                digits = digits,
                period = period
            )
        } catch (e: Exception) {
            null
        }
    }
}
