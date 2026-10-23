package tn.keeftalk.chat.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class CanonicalLocation(
    val latitude: Double,
    val longitude: Double,
    val name: String? = null,
    val address: String? = null,
    val placeId: String? = null,
    val provider: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable
