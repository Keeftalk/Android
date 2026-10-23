package tn.keeftalk.chat.ui.docs.viewer

import android.content.Context
import java.io.File

object MermaidDownloadManager {
    fun isDeclined(): Boolean = DocAssetDownloadManager.isDeclined(DocAssetDownloadManager.ASSET_MERMAID)

    fun setDeclined(declined: Boolean) {
        DocAssetDownloadManager.setDeclined(DocAssetDownloadManager.ASSET_MERMAID, declined)
    }

    fun getMermaidFile(context: Context): File {
        return DocAssetDownloadManager.getAssetFile(context, DocAssetDownloadManager.ASSET_MERMAID)
    }

    fun isMermaidInstalled(context: Context): Boolean {
        return DocAssetDownloadManager.isAssetInstalled(context, DocAssetDownloadManager.ASSET_MERMAID)
    }

    suspend fun downloadMermaid(
        context: Context,
        onProgress: (Float) -> Unit = {}
    ): Result<File> {
        return DocAssetDownloadManager.downloadAsset(context, DocAssetDownloadManager.ASSET_MERMAID, onProgress)
    }
}
