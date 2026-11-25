package tn.keeftalk.chat.data.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BillingManager(private val context: Context) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .enablePrepaidPlans()
                .build()
        )
        .enableAutoServiceReconnection()
        .build()

    private val _productDetails = MutableStateFlow<List<ProductDetails>>(emptyList())
    val productDetails = _productDetails.asStateFlow()

    private val _purchaseEvents = MutableSharedFlow<PurchaseResult>()
    val purchaseEvents = _purchaseEvents.asSharedFlow()

    private val _isServiceConnected = MutableStateFlow(false)
    val isServiceConnected = _isServiceConnected.asStateFlow()

    private val _isQuerying = MutableStateFlow(false)
    val isQuerying = _isQuerying.asStateFlow()

    init {
        startConnection()
    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    Log.d("BillingManager", "Billing setup finished")
                    _isServiceConnected.value = true
                    queryProductDetails()
                } else {
                    Log.e("BillingManager", "Billing setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w("BillingManager", "Billing service disconnected")
                _isServiceConnected.value = false
            }
        })
    }

    private fun queryProductDetails() {
        Log.d("BillingManager", "Querying pay-as-you-go balance top-up products...")
        _isQuerying.value = true
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(BALANCE_TOPUP_5)
                .setProductType(ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(BALANCE_TOPUP_10)
                .setProductType(ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(BALANCE_TOPUP_25)
                .setProductType(ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, result ->
            _isQuerying.value = false
            if (billingResult.responseCode == BillingResponseCode.OK) {
                Log.d("BillingManager", "Products queried successfully: ${result.productDetailsList.size} found")
                _productDetails.value = result.productDetailsList
            } else {
                Log.e("BillingManager", "Failed to query products: ${billingResult.responseCode} - ${billingResult.debugMessage}")
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails) {
        Log.d("BillingManager", "Attempting to launch top-up purchase flow for: ${productDetails.productId}")
        
        if (!_isServiceConnected.value) {
            Log.e("BillingManager", "Cannot launch purchase flow: Billing service not connected")
            startConnection()
            scope.launch {
                _purchaseEvents.emit(PurchaseResult.Error("Billing service is connecting. Please try again in a moment."))
            }
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)
        val responseCode = billingResult.responseCode
        val debugMessage = billingResult.debugMessage
        
        Log.d("BillingManager", "Launch billing flow result: $responseCode $debugMessage")
        
        if (responseCode != BillingResponseCode.OK && responseCode != BillingResponseCode.USER_CANCELED) {
            val errorMessage = debugMessage.ifBlank { "Billing error (code $responseCode)" }
            scope.launch {
                _purchaseEvents.emit(PurchaseResult.Error(errorMessage))
            }
        }
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        if (billingResult.responseCode == BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                scope.launch {
                    _purchaseEvents.emit(PurchaseResult.Success(purchase))
                }
            }
        } else if (billingResult.responseCode == BillingResponseCode.USER_CANCELED) {
            scope.launch {
                _purchaseEvents.emit(PurchaseResult.Cancelled)
            }
        } else {
            scope.launch {
                _purchaseEvents.emit(PurchaseResult.Error(billingResult.debugMessage))
            }
        }
    }

    fun endConnection() {
        billingClient.endConnection()
    }

    companion object {
        const val BALANCE_TOPUP_5 = "keeftalk_balance_5"
        const val BALANCE_TOPUP_10 = "keeftalk_balance_10"
        const val BALANCE_TOPUP_25 = "keeftalk_balance_25"

        // Legacy compatibility alias constants
        const val PLUS_MONTHLY = BALANCE_TOPUP_5
        const val PLUS_YEARLY = BALANCE_TOPUP_10
        const val PRO_MONTHLY = BALANCE_TOPUP_25
        const val FAMILY_MONTHLY = BALANCE_TOPUP_25
    }
}

sealed class PurchaseResult {
    data class Success(val purchase: Purchase) : PurchaseResult()
    data class Error(val message: String) : PurchaseResult()
    object Cancelled : PurchaseResult()
}
