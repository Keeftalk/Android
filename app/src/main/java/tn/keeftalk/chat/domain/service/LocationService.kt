package tn.keeftalk.chat.domain.service

import android.location.Location

interface LocationService {
    suspend fun getCurrentLocation(): Location?
}
