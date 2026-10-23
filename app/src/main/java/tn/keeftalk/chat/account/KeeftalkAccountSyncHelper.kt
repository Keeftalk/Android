package tn.keeftalk.chat.account

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.util.Log

object KeeftalkAccountSyncHelper {
    private const val TAG = "KeeftalkAccountSync"
    const val ACCOUNT_TYPE = "tn.keeftalk.account"

    fun ensureAccountExists(context: Context, accountName: String?) {
        try {
            val accountManager = AccountManager.get(context)
            val accounts = accountManager.getAccountsByType(ACCOUNT_TYPE)
            val name = if (!accountName.isNullOrBlank()) accountName else "Keeftalk User"

            if (accounts.isEmpty()) {
                val account = Account(name, ACCOUNT_TYPE)
                val added = accountManager.addAccountExplicitly(account, null, null)
                if (added) {
                    accountManager.setAuthToken(account, "Full Access", "keeftalk_active_session_token")
                    Log.d(TAG, "Successfully added Keeftalk account to system AccountManager: $name")
                } else {
                    Log.w(TAG, "Failed to add Keeftalk account explicitly")
                }
            } else {
                // Update name if changed
                val existing = accounts[0]
                if (existing.name != name) {
                    // AccountManager doesn't allow renaming directly, but ensures it's present
                    Log.d(TAG, "Keeftalk account already exists in AccountManager: ${existing.name}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error ensuring AccountManager account exists", e)
        }
    }

    fun removeAccount(context: Context) {
        try {
            val accountManager = AccountManager.get(context)
            val accounts = accountManager.getAccountsByType(ACCOUNT_TYPE)
            for (account in accounts) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                    accountManager.removeAccountExplicitly(account)
                }
                Log.d(TAG, "Removed Keeftalk account from system AccountManager")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing Keeftalk account", e)
        }
    }
}
