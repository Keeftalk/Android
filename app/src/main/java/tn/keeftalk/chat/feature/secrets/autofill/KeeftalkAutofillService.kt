package tn.keeftalk.chat.feature.secrets.autofill

import android.app.assist.AssistStructure
import android.os.CancellationSignal
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.text.InputType
import android.util.Log
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import tn.keeftalk.chat.data.local.KeeftalkDatabase

class KeeftalkAutofillService : android.service.autofill.AutofillService() {

    companion object {
        private const val TAG = "KeeftalkAutofillService"
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        val structure = request.fillContexts.lastOrNull()?.structure ?: return
        val packageName = structure.activityComponent?.packageName ?: ""

        serviceScope.launch {
            try {
                val db = Room.databaseBuilder(
                    applicationContext,
                    KeeftalkDatabase::class.java,
                    "keeftalk_db"
                ).build()

                val secretsDao = db.secretsDao()
                val credentialEntities = secretsDao.getCredentialsOnce("default_user")

                val responseBuilder = FillResponse.Builder()
                var addedDatasets = 0

                val usernameFields = mutableListOf<AutofillId>()
                val passwordFields = mutableListOf<AutofillId>()
                traverseStructure(structure, usernameFields, passwordFields)

                // If any authentication or form fields are found, offer credentials
                if (usernameFields.isNotEmpty() || passwordFields.isNotEmpty() || credentialEntities.isNotEmpty()) {
                    for (entity in credentialEntities) {
                        val presentation = RemoteViews(packageName.ifBlank { "tn.keeftalk.chat" }, android.R.layout.simple_list_item_1).apply {
                            setTextViewText(android.R.id.text1, "Keeftalk: ${entity.serviceName} (${entity.username})")
                        }

                        val datasetBuilder = Dataset.Builder(presentation)
                        val decryptedPassword = try {
                            entity.encryptedPassword
                        } catch (_: Exception) {
                            ""
                        }

                        if (usernameFields.isNotEmpty() && entity.username.isNotBlank()) {
                            for (id in usernameFields) {
                                datasetBuilder.setValue(id, AutofillValue.forText(entity.username))
                            }
                        }
                        if (passwordFields.isNotEmpty() && decryptedPassword.isNotBlank()) {
                            for (id in passwordFields) {
                                datasetBuilder.setValue(id, AutofillValue.forText(decryptedPassword))
                            }
                        }

                        try {
                            val dataset = datasetBuilder.build()
                            responseBuilder.addDataset(dataset)
                            addedDatasets++
                        } catch (err: Exception) {
                            Log.e(TAG, "Failed to build autofill dataset", err)
                        }
                    }
                }

                if (addedDatasets > 0) {
                    callback.onSuccess(responseBuilder.build())
                } else {
                    callback.onSuccess(null)
                }
                db.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error handling fill request", e)
                callback.onFailure(e.localizedMessage)
            }
        }
    }

    private fun traverseStructure(
        structure: AssistStructure,
        usernameFields: MutableList<AutofillId>,
        passwordFields: MutableList<AutofillId>
    ) {
        val nodes = structure.windowNodeCount
        for (i in 0 until nodes) {
            val windowNode = structure.getWindowNodeAt(i)
            val rootNode = windowNode.rootViewNode
            traverseNode(rootNode, usernameFields, passwordFields)
        }
    }

    private fun traverseNode(
        node: AssistStructure.ViewNode,
        usernameFields: MutableList<AutofillId>,
        passwordFields: MutableList<AutofillId>
    ) {
        val autofillId = node.autofillId
        val hints = node.autofillHints?.map { it.lowercase() } ?: emptyList()
        val idEntry = node.idEntry?.lowercase() ?: ""
        val className = node.className?.lowercase() ?: ""

        val isPassword = (node.inputType and InputType.TYPE_TEXT_VARIATION_PASSWORD) != 0 ||
                (node.inputType and InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) != 0 ||
                (node.inputType and InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD) != 0 ||
                hints.any { it.contains("password") || it.contains("pass") || it.contains("secret") || it.contains("pwd") } ||
                idEntry.contains("password") ||
                idEntry.contains("pass") ||
                idEntry.contains("pwd") ||
                idEntry.contains("secret")

        val isUsername = hints.any { it.contains("username") || it.contains("email") || it.contains("phone") || it.contains("name") || it.contains("login") || it.contains("user") || it.contains("account") } ||
                idEntry.contains("username") ||
                idEntry.contains("email") ||
                idEntry.contains("phone") ||
                idEntry.contains("login") ||
                idEntry.contains("user") ||
                idEntry.contains("account") ||
                idEntry.contains("id")

        val isOtp = hints.any { it.contains("otp") || it.contains("code") || it.contains("pin") || it.contains("token") || it.contains("verification") || it.contains("2fa") } ||
                idEntry.contains("otp") ||
                idEntry.contains("code") ||
                idEntry.contains("pin") ||
                idEntry.contains("token") ||
                idEntry.contains("2fa")

        if (autofillId != null) {
            if (isPassword || isOtp) {
                passwordFields.add(autofillId)
            } else if (isUsername) {
                usernameFields.add(autofillId)
            } else if (node.isFocusable && (className.contains("edittext") || className.contains("textfield") || (node.inputType and InputType.TYPE_CLASS_TEXT != 0))) {
                usernameFields.add(autofillId)
            }
        }

        for (i in 0 until node.childCount) {
            traverseNode(node.getChildAt(i), usernameFields, passwordFields)
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        callback.onSuccess()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
