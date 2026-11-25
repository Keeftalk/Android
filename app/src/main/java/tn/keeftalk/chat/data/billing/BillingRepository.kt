package tn.keeftalk.chat.data.billing

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import tn.keeftalk.chat.BuildConfig

class BillingRepository(
    private val context: Context,
    private val okHttpClient: OkHttpClient = OkHttpClient()
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val mediaTypeJson = "application/json; charset=utf-8".toMediaType()

    suspend fun fetchActivePricing(): Result<CloudPricing> = withContext(Dispatchers.IO) {
        try {
            val url = "${BuildConfig.SUPABASE_URL.trimEnd('/')}/functions/v1/billing-pricing"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful && body.isNotBlank()) {
                    val pricing = json.decodeFromString<CloudPricing>(body)
                    Result.success(pricing)
                } else {
                    Result.failure(Exception("Failed to fetch pricing: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Log.e("BillingRepository", "Error fetching active pricing", e)
            Result.failure(e)
        }
    }

    suspend fun fetchCloudBillingState(authToken: String): Result<CloudBillingState> = withContext(Dispatchers.IO) {
        try {
            val url = "${BuildConfig.SUPABASE_URL.trimEnd('/')}/functions/v1/billing-usage"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $authToken")
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful && body.isNotBlank()) {
                    val state = json.decodeFromString<CloudBillingState>(body)
                    Result.success(state)
                } else {
                    Result.failure(Exception("Failed to fetch billing state: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Log.e("BillingRepository", "Error fetching cloud billing state", e)
            Result.failure(e)
        }
    }

    suspend fun fetchInvoiceHistory(authToken: String): Result<List<InvoiceSummary>> = withContext(Dispatchers.IO) {
        try {
            val url = "${BuildConfig.SUPABASE_URL.trimEnd('/')}/functions/v1/billing-invoices"
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $authToken")
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val body = response.body?.string() ?: ""
                if (response.isSuccessful && body.isNotBlank()) {
                    val history = json.decodeFromString<InvoiceHistoryResponse>(body)
                    Result.success(history.invoices)
                } else {
                    Result.failure(Exception("Failed to fetch invoices: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Log.e("BillingRepository", "Error fetching invoice history", e)
            Result.failure(e)
        }
    }

    suspend fun topUpPrepaidBalance(authToken: String, amountUsd: Double, purchaseToken: String): Result<Double> = withContext(Dispatchers.IO) {
        try {
            val url = "${BuildConfig.SUPABASE_URL.trimEnd('/')}/functions/v1/verify-purchase"
            val payload = """{"purchaseToken":"$purchaseToken","topupAmount":$amountUsd}"""
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
                .addHeader("Authorization", "Bearer $authToken")
                .post(payload.toRequestBody(mediaTypeJson))
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    Result.success(amountUsd)
                } else {
                    Result.failure(Exception("Top-up failed: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Log.e("BillingRepository", "Error topping up balance", e)
            Result.failure(e)
        }
    }
}
