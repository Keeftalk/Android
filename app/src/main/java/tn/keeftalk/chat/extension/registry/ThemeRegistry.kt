package tn.keeftalk.chat.extension.registry

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import tn.keeftalk.chat.extension.model.ThemePack

class ThemeRegistry {
    private val installedThemesMap = mutableMapOf<String, ThemePack>()
    private val _activeCustomTheme = MutableStateFlow<ThemePack?>(null)
    val activeCustomTheme: StateFlow<ThemePack?> = _activeCustomTheme.asStateFlow()

    fun registerTheme(themePack: ThemePack) {
        installedThemesMap[themePack.id] = themePack
    }

    fun unregisterTheme(themeId: String) {
        installedThemesMap.remove(themeId)
        if (_activeCustomTheme.value?.id == themeId) {
            _activeCustomTheme.value = null
        }
    }

    fun setActiveTheme(themeId: String?): Boolean {
        if (themeId == null) {
            _activeCustomTheme.value = null
            return true
        }
        val pack = installedThemesMap[themeId] ?: return false
        _activeCustomTheme.value = pack
        return true
    }

    fun getInstalledThemes(): List<ThemePack> {
        return installedThemesMap.values.toList()
    }
}
