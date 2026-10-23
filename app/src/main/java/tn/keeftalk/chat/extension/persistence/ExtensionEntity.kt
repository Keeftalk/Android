package tn.keeftalk.chat.extension.persistence

import androidx.room.Entity
import androidx.room.PrimaryKey
import tn.keeftalk.chat.extension.model.ExtensionType
import tn.keeftalk.chat.extension.model.InstalledExtension

@Entity(tableName = "installed_extensions")
data class ExtensionEntity(
    @PrimaryKey val id: String,
    val type: String,
    val name: String,
    val version: String,
    val description: String,
    val publisherName: String,
    val isPublisherTrusted: Boolean,
    val installedPath: String,
    val installedTimeMillis: Long,
    val isEnabled: Boolean,
    val sizeBytes: Long,
    val entry: String,
    val manifestJson: String
) {
    fun toDomain(): InstalledExtension {
        return InstalledExtension(
            id = id,
            type = try { ExtensionType.valueOf(type) } catch (_: Exception) { ExtensionType.THEME },
            name = name,
            version = version,
            description = description,
            publisherName = publisherName,
            isPublisherTrusted = isPublisherTrusted,
            installedPath = installedPath,
            installedTimeMillis = installedTimeMillis,
            isEnabled = isEnabled,
            sizeBytes = sizeBytes,
            entry = entry,
            manifestJson = manifestJson
        )
    }

    companion object {
        fun fromDomain(domain: InstalledExtension): ExtensionEntity {
            return ExtensionEntity(
                id = domain.id,
                type = domain.type.name,
                name = domain.name,
                version = domain.version,
                description = domain.description,
                publisherName = domain.publisherName,
                isPublisherTrusted = domain.isPublisherTrusted,
                installedPath = domain.installedPath,
                installedTimeMillis = domain.installedTimeMillis,
                isEnabled = domain.isEnabled,
                sizeBytes = domain.sizeBytes,
                entry = domain.entry,
                manifestJson = domain.manifestJson
            )
        }
    }
}
