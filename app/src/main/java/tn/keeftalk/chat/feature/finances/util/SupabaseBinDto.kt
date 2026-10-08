package tn.keeftalk.chat.feature.finances.util

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseBinDto(
    @SerialName("BIN") val bin: String? = null,
    @SerialName("Brand") val brand: String? = null,
    @SerialName("Type") val type: String? = null,
    @SerialName("Category") val category: String? = null,
    @SerialName("Issuer") val issuer: String? = null,
    @SerialName("IssuerPhone") val issuerPhone: String? = null,
    @SerialName("IssuerUrl") val issuerUrl: String? = null,
    @SerialName("isoCode2") val isoCode2: String? = null,
    @SerialName("isoCode3") val isoCode3: String? = null,
    @SerialName("CountryName") val countryName: String? = null
)
