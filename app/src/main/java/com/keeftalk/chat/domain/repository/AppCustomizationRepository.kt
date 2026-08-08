package com.keeftalk.chat.domain.repository

import com.keeftalk.chat.domain.model.AppCustomization
import kotlinx.coroutines.flow.Flow

interface AppCustomizationRepository {
    val customization: Flow<AppCustomization>
    fun startCustomizationObservation()
    suspend fun updateCustomization(customization: AppCustomization): Result<Unit>
    suspend fun resetToDefault(): Result<Unit>

    fun shutdown()
}
