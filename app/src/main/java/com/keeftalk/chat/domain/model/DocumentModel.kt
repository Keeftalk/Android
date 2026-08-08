package com.keeftalk.chat.domain.model

import android.net.Uri

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class DocumentType : Parcelable {
    PDF, WORD, EXCEL, PPT, ZIP, ARCHIVE, CODE, EBOOK, SECURITY, LOCATION, CAD, OTHER
}

@Parcelize
data class DocumentModel(
    val id: Long,
    val uri: Uri,
    val name: String,
    val size: String,
    val mimeType: String,
    val date: String,
    val type: DocumentType
) : Parcelable

data class DocumentFolder(
    val id: String,
    val name: String,
    val path: String,
    val documentCount: Int
)
