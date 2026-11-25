package tn.keeftalk.chat.feature.updates

import android.os.Build

object UpdateUtils {

    data class VersionInfo(
        val major: Int,
        val minor: Int,
        val patch: Int,
        val prerelease: String?,
        val raw: String
    )

    fun parseVersion(versionStr: String): VersionInfo {
        val cleaned = versionStr.trimStart('v', 'V')
        val parts = cleaned.split("-", limit = 2)
        val numbers = parts[0].split(".").map { it.toIntOrNull() ?: 0 }
        val major = numbers.getOrNull(0) ?: 0
        val minor = numbers.getOrNull(1) ?: 0
        val patch = numbers.getOrNull(2) ?: 0
        val prerelease = parts.getOrNull(1)
        return VersionInfo(major, minor, patch, prerelease, versionStr)
    }

    fun isNewerVersion(installed: String, remote: String): Boolean {
        val vInst = parseVersion(installed)
        val vRem = parseVersion(remote)

        if (vRem.major != vInst.major) return vRem.major > vInst.major
        if (vRem.minor != vInst.minor) return vRem.minor > vInst.minor
        if (vRem.patch != vInst.patch) return vRem.patch > vInst.patch

        val instPre = vInst.prerelease
        val remPre = vRem.prerelease
        if (instPre == null && remPre != null) return false
        if (instPre != null && remPre == null) return true
        if (instPre != null && remPre != null) {
            return remPre > instPre
        }
        return false
    }

    fun selectBestAsset(assets: List<GitHubAsset>): GitHubAsset? {
        if (assets.isEmpty()) return null
        val supportedAbis = Build.SUPPORTED_ABIS ?: arrayOf(Build.CPU_ABI)

        val apkAssets = assets.filter { it.name.endsWith(".apk", ignoreCase = true) }
        if (apkAssets.isEmpty()) return null

        for (abi in supportedAbis) {
            val matching = apkAssets.find { it.name.contains(abi, ignoreCase = true) }
            if (matching != null) return matching
        }

        val universal = apkAssets.find { it.name.contains("universal", ignoreCase = true) || it.name.contains("all", ignoreCase = true) }
        if (universal != null) return universal

        return apkAssets.firstOrNull()
    }
}
