package tn.keeftalk.chat.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import tn.keeftalk.chat.domain.model.CanonicalLocation

@Entity(tableName = "saved_places")
data class SavedPlaceEntity(
    @PrimaryKey val id: String,
    val latitude: Double,
    val longitude: Double,
    val name: String?,
    val address: String?,
    @ColumnInfo(name = "place_id") val placeId: String?,
    val provider: String?,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis()
)

fun SavedPlaceEntity.toDomain() = CanonicalLocation(
    latitude = latitude,
    longitude = longitude,
    name = name,
    address = address,
    placeId = placeId,
    provider = provider,
    timestamp = createdAt
)

fun CanonicalLocation.toEntity(id: String = java.util.UUID.randomUUID().toString()) = SavedPlaceEntity(
    id = placeId ?: id,
    latitude = latitude,
    longitude = longitude,
    name = name,
    address = address,
    placeId = placeId,
    provider = provider,
    createdAt = timestamp
)
