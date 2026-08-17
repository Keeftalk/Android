package com.keeftalk.chat.data.billing

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
                // Attempt to reconnect later if needed
            }
        })
    }

    private fun queryProductDetails() {
        Log.d("BillingManager", "Querying product details...")
        _isQuerying.value = true
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PLUS_MONTHLY)
                .setProductType(ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PLUS_YEARLY)
                .setProductType(ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PRO_MONTHLY)
                .setProductType(ProductType.SUBS)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(FAMILY_MONTHLY)
                .setProductType(ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, result ->
            _isQuerying.value = false
            if (billingResult.responseCode == BillingResponseCode.OK) {
                Log.d("BillingManager", "Products queried successfully: ${result.productDetailsList.size} found")
                result.productDetailsList.forEach { 
                    Log.d("BillingManager", "Product: ${it.productId}, Title: ${it.title}")
                }
                _productDetails.value = result.productDetailsList
            } else {
                Log.e("BillingManager", "Failed to query products: ${billingResult.responseCode} - ${billingResult.debugMessage}")
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails) {
        Log.d("BillingManager", "Attempting to launch purchase flow for: ${productDetails.productId}")
        
        if (!_isServiceConnected.value) {
            Log.e("BillingManager", "Cannot launch purchase flow: Billing service not connected")
            startConnection() // Attempt to reconnect
            scope.launch {
                _purchaseEvents.emit(PurchaseResult.Error("Billing service is connecting. Please try again in a moment."))
            }
            return
        }

        val subscriptionOfferDetails = productDetails.subscriptionOfferDetails
        if (subscriptionOfferDetails.isNullOrEmpty()) {
            Log.e("BillingManager", "No subscription offer details found for ${productDetails.productId}")
            scope.launch {
                _purchaseEvents.emit(PurchaseResult.Error("This plan is currently unavailable (no active offers found)."))
            }
            return
        }

        // Try to find the base plan offer (usually has no offerId) or take the first eligible offer
        val offerDetails = subscriptionOfferDetails.find { it.offerId == null } 
            ?: subscriptionOfferDetails.firstOrNull()
            
        val offerToken = offerDetails?.offerToken
        if (offerToken.isNullOrBlank()) {
            Log.e("BillingManager", "Offer token is null or blank for ${productDetails.productId}")
            scope.launch {
                _purchaseEvents.emit(PurchaseResult.Error("Could not initiate purchase: Invalid offer token."))
            }
            return
        }
        
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val billingResult = billingClient.launchBillingFlow(activity, billingFlowParams)
        val responseCode = billingResult.responseCode
        val debugMessage = billingResult.debugMessage
        
        Log.d("BillingManager", "Launch billing flow result: $responseCode $debugMessage")
        
        if (responseCode != BillingResponseCode.OK) {
            val errorMessage = when (responseCode) {
                BillingResponseCode.USER_CANCELED -> null // Handled in onPurchasesUpdated
                BillingResponseCode.ITEM_ALREADY_OWNED -> "You already own this subscription."
                BillingResponseCode.DEVELOPER_ERROR -> "Internal billing error. Please check your account configuration."
                else -> debugMessage.ifBlank { "Billing error (code $responseCode)" }
            }
            
            errorMessage?.let {
                scope.launch {
                    _purchaseEvents.emit(PurchaseResult.Error(it))
                }
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
        const val PLUS_MONTHLY = "keeftalk_plus_monthly"
        const val PLUS_YEARLY = "keeftalk_plus_yearly"
        const val PRO_MONTHLY = "keeftalk_pro_monthly"
        const val FAMILY_MONTHLY = "keeftalk_family_monthly"
    }
}

sealed class PurchaseResult {
    data class Success(val purchase: Purchase) : PurchaseResult()
    data class Error(val message: String) : PurchaseResult()
    object Cancelled : PurchaseResult()
}
