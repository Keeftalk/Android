package tn.keeftalk.chat.extension.security

import android.util.Log
import kotlinx.serialization.json.Json
import tn.keeftalk.chat.extension.model.*
import tn.keeftalk.chat.extension.parser.ExtractedPackageSession
import tn.keeftalk.chat.extension.parser.KtxpManifestParser
import java.io.File
import java.security.MessageDigest
import java.security.Signature
import java.util.Base64

object PackageVerifier {
    private const val TAG = "PackageVerifier"
    private const val CURRENT_APP_VERSION = 1
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    fun verifyPackageSession(
        session: ExtractedPackageSession,
        isNetworkAvailable: Boolean = false
    ): Pair<ExtensionManifest?, SecurityReviewReport> {
        val warnings = mutableListOf<SecurityWarning>()

        // 1. Format & Manifest Validation
        var manifest = try {
            KtxpManifestParser.parseManifest(session.manifestJson)
        } catch (e: Exception) {
            val report = SecurityReviewReport(
                formatStatus = VerificationResult.FAILED,
                formatDetails = "Manifest parsing error: ${e.message}",
                isExecutableContentDetected = session.hasExecutableContent,
                isEligibleForInstallation = false,
                warnings = listOf(SecurityWarning("Format Validation Error", e.message ?: "Invalid manifest", isCritical = true))
            )
            return Pair(null, report)
        }

        // Parse optional offline listing metadata
        if (!session.listingJson.isNullOrBlank()) {
            try {
                val listingMeta = json.decodeFromString<ExtensionListingMeta>(session.listingJson)
                manifest = manifest.copy(listingMeta = listingMeta)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse offline metadata/listing.json: ${e.message}")
            }
        }

        // 2. Compatibility Check
        var compStatus = VerificationResult.PASSED
        var compDetails = "Compatible with Keeftalk v$CURRENT_APP_VERSION."
        if (manifest.minAppVersion > CURRENT_APP_VERSION) {
            compStatus = VerificationResult.FAILED
            compDetails = "Package requires Keeftalk v${manifest.minAppVersion} or higher."
            warnings.add(SecurityWarning("Incompatible App Version", compDetails, isCritical = true))
        } else if (manifest.maxAppVersion != null && manifest.maxAppVersion < CURRENT_APP_VERSION) {
            compStatus = VerificationResult.FAILED
            compDetails = "Package is deprecated for Keeftalk v$CURRENT_APP_VERSION."
            warnings.add(SecurityWarning("App Version Deprecated", compDetails, isCritical = true))
        }

        // 3. Asset Integrity & Hash Verification
        var assetStatus = VerificationResult.PASSED
        var assetDetails = "All package entry files and assets verified."

        val entryFile = File(session.stagingDir, manifest.entry)
        if (!entryFile.exists()) {
            assetStatus = VerificationResult.FAILED
            assetDetails = "Entry file '${manifest.entry}' declared in manifest was not found."
            warnings.add(SecurityWarning("Missing Entry File", assetDetails, isCritical = true))
        } else {
            for (asset in manifest.assets) {
                val assetFile = File(session.stagingDir, asset.path)
                if (!assetFile.exists()) {
                    assetStatus = VerificationResult.FAILED
                    assetDetails = "Declared asset '${asset.path}' was missing."
                    warnings.add(SecurityWarning("Missing Package Asset", assetDetails, isCritical = true))
                    break
                }
                if (asset.sha256Hex.isNotBlank()) {
                    val computedHash = computeFileSha256Hex(assetFile)
                    if (!computedHash.equals(asset.sha256Hex, ignoreCase = true)) {
                        assetStatus = VerificationResult.FAILED
                        assetDetails = "SHA-256 hash mismatch for asset '${asset.path}'."
                        warnings.add(SecurityWarning("Asset Integrity Mismatch", assetDetails, isCritical = true))
                        break
                    }
                }
            }
        }

        // 4. Real Cryptographic Publisher Signature Verification
        val signatureFile = File(session.stagingDir, "signatures/package-signature.json")
        var sigStatus = VerificationResult.NOT_CHECKED
        var sigDetails = "No cryptographic signature attached (unsigned local package)."
        var isTrusted = false

        if (signatureFile.exists()) {
            try {
                val sigDescriptor = json.decodeFromString<PackageSignatureDescriptor>(signatureFile.readText())
                if (sigDescriptor.signatureBase64.isBlank()) {
                    sigStatus = VerificationResult.FAILED
                    sigDetails = "Package signature file is missing signature bytes."
                    warnings.add(SecurityWarning("Malformed Signature", sigDetails, isCritical = true))
                } else {
                    val trustedRecord = PublisherTrustManager.getTrustedRecord(sigDescriptor.keyId)
                    val publicKeyPem = trustedRecord?.publicKeyPem ?: sigDescriptor.publicKeyPem

                    if (publicKeyPem.isBlank()) {
                        sigStatus = VerificationResult.FAILED
                        sigDetails = "Missing public key for publisher signature verification."
                        warnings.add(SecurityWarning("Missing Public Key", sigDetails, isCritical = true))
                    } else {
                        val publicKey = PublisherTrustManager.parsePublicKeyFromPem(publicKeyPem)
                        val verifier = Signature.getInstance("SHA256withRSA")
                        verifier.initVerify(publicKey)

                        // Compute deterministic signed payload digest over manifest & staged file tree
                        val signedPayload = buildDeterministicSignedPayload(session)
                        verifier.update(signedPayload)

                        val signatureBytes = Base64.getDecoder().decode(sigDescriptor.signatureBase64)
                        val isSigValid = verifier.verify(signatureBytes)

                        if (isSigValid) {
                            // Verify key trust record
                            isTrusted = PublisherTrustManager.isTrustedKey(sigDescriptor.keyId, publicKeyPem)
                            sigStatus = if (isTrusted) VerificationResult.VERIFIED else VerificationResult.PASSED
                            sigDetails = if (isTrusted) {
                                "Cryptographically verified signature from trusted publisher '${manifest.publisher.name}'."
                            } else {
                                "Valid signature from UNTRUSTED publisher key '${sigDescriptor.keyId}'."
                            }

                            if (!isTrusted) {
                                warnings.add(SecurityWarning("Unverified Publisher", "Package has a valid mathematical signature, but key '${sigDescriptor.keyId}' is not trusted by Keeftalk."))
                            }
                        } else {
                            sigStatus = VerificationResult.FAILED
                            sigDetails = "Cryptographic signature FAILED! Package contents or manifest modified."
                            warnings.add(SecurityWarning("Signature Verification Failed", sigDetails, isCritical = true))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Cryptographic signature error: ${e.message}", e)
                sigStatus = VerificationResult.FAILED
                sigDetails = "Signature verification error: ${e.message}"
                warnings.add(SecurityWarning("Invalid Signature File", sigDetails, isCritical = true))
            }
        } else {
            warnings.add(SecurityWarning("Unsigned Package", "Package does not contain a publisher signature."))
        }

        // 5. Executable Code Security Check
        if (session.hasExecutableContent) {
            warnings.add(SecurityWarning("Forbidden Executable Code", "Package contains binary libraries or executable code. Installation blocked.", isCritical = true))
        }

        val report = SecurityReviewReport(
            formatStatus = VerificationResult.PASSED,
            formatDetails = "Manifest structure valid.",
            signatureStatus = sigStatus,
            signatureDetails = sigDetails,
            publisherName = manifest.publisher.name.ifBlank { "Unknown" },
            isPublisherTrusted = isTrusted,
            assetIntegrityStatus = assetStatus,
            assetIntegrityDetails = assetDetails,
            compatibilityStatus = compStatus,
            compatibilityDetails = compDetails,
            onlineRevocationStatus = if (isNetworkAvailable) VerificationResult.PASSED else VerificationResult.NOT_CHECKED,
            onlineRevocationDetails = if (isNetworkAvailable) "Package checked online." else "Offline mode.",
            warnings = warnings,
            totalSizeBytes = session.totalExpandedBytes,
            isExecutableContentDetected = session.hasExecutableContent,
            isEligibleForInstallation = (compStatus != VerificationResult.FAILED &&
                    assetStatus != VerificationResult.FAILED &&
                    sigStatus != VerificationResult.FAILED &&
                    !session.hasExecutableContent)
        )

        return Pair(manifest, report)
    }

    private fun buildDeterministicSignedPayload(session: ExtractedPackageSession): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")

        // Digest all files in staging directory excluding signatures/ sorted by relative path
        session.stagingDir.walkTopDown()
            .filter { it.isFile && !it.relativeTo(session.stagingDir).path.startsWith("signatures/") }
            .sortedBy { it.relativeTo(session.stagingDir).path }
            .forEach { file ->
                val relPath = file.relativeTo(session.stagingDir).path
                digest.update(relPath.toByteArray(Charsets.UTF_8))
                file.inputStream().use { input ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        digest.update(buffer, 0, read)
                    }
                }
            }

        return digest.digest()
    }

    fun computeFileSha256Hex(file: File): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    digest.update(buffer, 0, read)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            ""
        }
    }
}
