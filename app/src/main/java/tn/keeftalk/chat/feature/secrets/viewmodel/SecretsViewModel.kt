package tn.keeftalk.chat.feature.secrets.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tn.keeftalk.chat.feature.secrets.crypto.TotpGenerator
import tn.keeftalk.chat.feature.secrets.model.BackupCode
import tn.keeftalk.chat.feature.secrets.model.PasswordHistoryItem
import tn.keeftalk.chat.feature.secrets.model.SecretCredential
import tn.keeftalk.chat.feature.secrets.model.SecretSettings
import tn.keeftalk.chat.feature.secrets.model.SecretVaultItem
import tn.keeftalk.chat.feature.secrets.model.SecurityScoreSummary
import tn.keeftalk.chat.feature.secrets.model.TotpAccount
import tn.keeftalk.chat.feature.secrets.repository.SecretsRepository
import tn.keeftalk.chat.feature.secrets.util.CsvImportResult

enum class SecretSheetType {
    NONE,
    ADD_PICKER,
    ADD_CREDENTIAL,
    UPDATE_PASSWORD,
    GENERATE_PASSWORD,
    ADD_TOTP_MANUAL,
    SCAN_QR_CAMERA,
    SCAN_QR_IMAGE,
    SHOW_BACKUP_CODES,
    IMPORT_CSV,
    IMPORT_PREVIEW,
    EXPORT_CSV,
    CREDENTIAL_DETAIL
}

data class SecretsUiState(
    val selectedTab: Int = 0, // 0=Passwords, 1=Codes, 2=Vault, 3=Security
    val isSectionRailExpanded: Boolean = false,
    val selectedCategory: String = "All",
    val searchQuery: String = "",
    val credentials: List<SecretCredential> = emptyList(),
    val filteredCredentials: List<SecretCredential> = emptyList(),
    val totpAccounts: List<TotpAccount> = emptyList(),
    val backupCodes: List<BackupCode> = emptyList(),
    val vaultItems: List<SecretVaultItem> = emptyList(),
    val settings: SecretSettings = SecretSettings(userId = ""),
    val securitySummary: SecurityScoreSummary = SecurityScoreSummary(0, emptyList(), emptyList(), emptyList(), 0),
    val isVaultUnlocked: Boolean = false,
    val isHoldToRevealActive: Boolean = false,
    val totpRemainingSeconds: Int = 30,
    val totpProgress: Float = 1.0f,
    val toastMessage: String? = null,
    val activeSheet: SecretSheetType = SecretSheetType.NONE,
    val selectedCredential: SecretCredential? = null,
    val csvImportResult: CsvImportResult? = null
)

class SecretsViewModel(
    private val userId: String,
    private val repository: SecretsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SecretsUiState(settings = SecretSettings(userId = userId)))
    val uiState: StateFlow<SecretsUiState> = _uiState.asStateFlow()

    private var tickingJob: Job? = null
    private var clipboardClearJob: Job? = null

    init {
        observeData()
        startTotpTicker()
    }

    private fun observeData() {
        viewModelScope.launch {
            repository.getCredentials(userId).collectLatest { list ->
                _uiState.update { state ->
                    val filtered = filterCredentials(list, state.selectedCategory, state.searchQuery)
                    state.copy(
                        credentials = list,
                        filteredCredentials = filtered,
                        selectedCredential = list.find { it.id == state.selectedCredential?.id } ?: state.selectedCredential
                    )
                }
            }
        }

        viewModelScope.launch {
            repository.getTotpAccounts(userId).collectLatest { list ->
                _uiState.update { it.copy(totpAccounts = list) }
            }
        }

        viewModelScope.launch {
            repository.getBackupCodes(userId).collectLatest { list ->
                _uiState.update { it.copy(backupCodes = list) }
            }
        }

        viewModelScope.launch {
            repository.getVaultItems(userId).collectLatest { list ->
                _uiState.update { it.copy(vaultItems = list) }
            }
        }

        viewModelScope.launch {
            repository.getSettings(userId).collectLatest { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }

        viewModelScope.launch {
            repository.getSecuritySummary(userId).collectLatest { summary ->
                _uiState.update { it.copy(securitySummary = summary) }
            }
        }
    }

    private fun filterCredentials(list: List<SecretCredential>, category: String, query: String): List<SecretCredential> {
        return list.filter { item ->
            val matchCategory = category == "All" || item.category.equals(category, ignoreCase = true)
            val matchQuery = query.isBlank() ||
                    item.serviceName.contains(query, ignoreCase = true) ||
                    item.username.contains(query, ignoreCase = true) ||
                    item.website.contains(query, ignoreCase = true)
            matchCategory && matchQuery
        }
    }

    private fun startTotpTicker() {
        tickingJob?.cancel()
        tickingJob = viewModelScope.launch {
            while (true) {
                val timeSec = System.currentTimeMillis() / 1000L
                val rem = TotpGenerator.getRemainingSeconds(timeSec, 30)
                val progress = rem.toFloat() / 30.0f
                _uiState.update { it.copy(totpRemainingSeconds = rem, totpProgress = progress) }
                delay(1000L)
            }
        }
    }

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTab = index, isSectionRailExpanded = false) }
    }

    fun toggleSectionRail() {
        _uiState.update { it.copy(isSectionRailExpanded = !it.isSectionRailExpanded) }
    }

    fun setCategory(category: String) {
        _uiState.update { state ->
            val filtered = filterCredentials(state.credentials, category, state.searchQuery)
            state.copy(selectedCategory = category, filteredCredentials = filtered)
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { state ->
            val filtered = filterCredentials(state.credentials, state.selectedCategory, query)
            state.copy(searchQuery = query, filteredCredentials = filtered)
        }
    }

    fun openSheet(sheet: SecretSheetType) {
        _uiState.update { it.copy(activeSheet = sheet) }
    }

    fun closeSheet() {
        _uiState.update { it.copy(activeSheet = SecretSheetType.NONE) }
    }

    fun openCredentialDetail(credential: SecretCredential) {
        _uiState.update { it.copy(selectedCredential = credential, activeSheet = SecretSheetType.CREDENTIAL_DETAIL) }
    }

    fun saveCredential(credential: SecretCredential) {
        viewModelScope.launch {
            val result = repository.saveCredential(credential.copy(userId = userId))
            if (result.isSuccess) {
                showToast("Credential saved")
                closeSheet()
            } else {
                showToast("Error saving credential")
            }
        }
    }

    fun updatePassword(credentialId: String, newPw: String) {
        viewModelScope.launch {
            val result = repository.updatePassword(credentialId, userId, newPw)
            if (result.isSuccess) {
                showToast("Password updated · old one logged to history")
                closeSheet()
            } else {
                showToast("Error updating password")
            }
        }
    }

    fun restoreOldPassword(credentialId: String, historyItem: PasswordHistoryItem) {
        viewModelScope.launch {
            val result = repository.restoreOldPassword(credentialId, userId, historyItem)
            if (result.isSuccess) {
                showToast("Old password restored · current one logged")
            } else {
                showToast("Error restoring password")
            }
        }
    }

    fun deleteCredential(credential: SecretCredential) {
        viewModelScope.launch {
            repository.deleteCredential(credential)
            showToast("Credential deleted")
            closeSheet()
        }
    }

    fun saveTotpAccount(account: TotpAccount) {
        viewModelScope.launch {
            val result = repository.saveTotpAccount(account.copy(userId = userId))
            if (result.isSuccess) {
                showToast("Account added to Codes")
                closeSheet()
                selectTab(1)
            } else {
                showToast("Invalid secret or account format")
            }
        }
    }

    fun deleteTotpAccount(account: TotpAccount) {
        viewModelScope.launch {
            repository.deleteTotpAccount(account)
            showToast("Account deleted")
        }
    }

    fun generateBackupCodes() {
        viewModelScope.launch {
            val result = repository.generateBackupCodes(userId)
            if (result.isSuccess) {
                showToast("8 Backup codes generated and saved")
                openSheet(SecretSheetType.SHOW_BACKUP_CODES)
            } else {
                showToast("Error generating backup codes")
            }
        }
    }

    fun setVaultUnlocked(unlocked: Boolean) {
        _uiState.update { it.copy(isVaultUnlocked = unlocked) }
        if (!unlocked) {
            _uiState.update { it.copy(isHoldToRevealActive = false) }
        }
    }

    fun setHoldToReveal(active: Boolean) {
        _uiState.update { it.copy(isHoldToRevealActive = active) }
    }

    fun saveVaultItem(item: SecretVaultItem) {
        viewModelScope.launch {
            repository.saveVaultItem(item.copy(userId = userId))
            showToast("Vault item saved")
            closeSheet()
        }
    }

    fun deleteVaultItem(item: SecretVaultItem) {
        viewModelScope.launch {
            repository.deleteVaultItem(item)
            showToast("Vault item removed")
        }
    }

    fun updateSettings(settings: SecretSettings) {
        viewModelScope.launch {
            repository.updateSettings(settings)
            showToast("Settings updated")
        }
    }

    fun parseCsvForImport(csvText: String) {
        viewModelScope.launch {
            val result = repository.importFromCsv(csvText, userId)
            if (result.isSuccess) {
                val data = result.getOrThrow()
                _uiState.update { it.copy(csvImportResult = data, activeSheet = SecretSheetType.IMPORT_PREVIEW) }
            } else {
                showToast("Failed to parse CSV file")
            }
        }
    }

    fun confirmCsvImport() {
        val count = _uiState.value.csvImportResult?.importedItems?.size ?: 0
        showToast("$count logins imported · remember to delete the plaintext CSV")
        _uiState.update { it.copy(csvImportResult = null) }
        closeSheet()
        selectTab(0)
    }

    fun exportToCsv(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.exportToCsv(userId)
            if (result.isSuccess) {
                showToast("CSV generated · handle securely")
                onResult(result.getOrNull())
            } else {
                showToast("Export failed")
                onResult(null)
            }
        }
    }

    fun copyToClipboard(context: Context, label: String, text: String, isSensitive: Boolean = true) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            if (isSensitive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                clip.description.extras = PersistableBundle().apply {
                    putBoolean("android.content.extra.IS_SENSITIVE", true)
                }
            }
            clipboard.setPrimaryClip(clip)

            val timeoutSecs = _uiState.value.settings.clearClipboardSeconds
            showToast("$label copied · clears in $timeoutSecs s")

            clipboardClearJob?.cancel()
            if (isSensitive && timeoutSecs > 0) {
                clipboardClearJob = viewModelScope.launch {
                    delay(timeoutSecs * 1000L)
                    val emptyClip = ClipData.newPlainText("", "")
                    clipboard.setPrimaryClip(emptyClip)
                }
            }
        } catch (e: Exception) {
            showToast("Could not copy to clipboard")
        }
    }

    fun showToast(msg: String) {
        _uiState.update { it.copy(toastMessage = msg) }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        tickingJob?.cancel()
        clipboardClearJob?.cancel()
    }
}
