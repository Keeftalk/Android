package tn.keeftalk.chat.data.billing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CloudPricing(
    @SerialName("version_name") val versionName: String = "v1.0-default",
    val currency: String = "USD",
    @SerialName("storage_rate_per_gb_month") val storageRatePerGbMonth: Double = 0.015000,
    @SerialName("bandwidth_rate_per_gb") val bandwidthRatePerGb: Double = 0.020000,
    @SerialName("read_op_rate_per_10k") val readOpRatePer10k: Double = 0.003600,
    @SerialName("write_op_rate_per_10k") val writeOpRatePer10k: Double = 0.045000,
    @SerialName("backup_rate_per_gb_month") val backupRatePerGbMonth: Double = 0.020000,
    @SerialName("maintenance_allocation_pct") val maintenanceAllocationPct: Double = 0.1200,
    @SerialName("founder_margin_pct") val founderMarginPct: Double = 0.0500,
    @SerialName("effective_from") val effectiveFrom: String = ""
)

@Serializable
data class ResourceUsage(
    @SerialName("storage_bytes") val storageBytes: Long = 0,
    @SerialName("storage_gb") val storageGb: Double = 0.0,
    @SerialName("bandwidth_bytes") val bandwidthBytes: Long = 0,
    @SerialName("bandwidth_gb") val bandwidthGb: Double = 0.0,
    @SerialName("read_operations") val readOperations: Long = 0,
    @SerialName("write_operations") val writeOperations: Long = 0
)

@Serializable
data class CostEstimate(
    @SerialName("base_cost") val baseCost: Double = 0.0,
    @SerialName("maintenance_fee") val maintenanceFee: Double = 0.0,
    @SerialName("founder_margin") val founderMargin: Double = 0.0,
    @SerialName("total_estimated") val totalEstimated: Double = 0.0,
    val currency: String = "USD"
)

@Serializable
data class CloudBillingState(
    @SerialName("user_id") val userId: String = "",
    @SerialName("prepaid_balance") val prepaidBalance: Double = 0.0,
    @SerialName("low_balance_threshold") val lowBalanceThreshold: Double = 2.0,
    @SerialName("payment_mode") val paymentMode: String = "PREPAID",
    @SerialName("credit_limit") val creditLimit: Double = 50.0,
    @SerialName("spending_limit") val spendingLimit: Double = 100.0,
    @SerialName("spending_alert_threshold") val spendingAlertThreshold: Double = 80.0,
    @SerialName("current_usage") val currentUsage: ResourceUsage = ResourceUsage(),
    @SerialName("estimated_charges") val estimatedCharges: CostEstimate = CostEstimate()
)

@Serializable
data class InvoiceSummary(
    val id: String = "",
    @SerialName("invoice_number") val invoiceNumber: String = "",
    @SerialName("billing_period_start") val billingPeriodStart: String = "",
    @SerialName("billing_period_end") val billingPeriodEnd: String = "",
    val subtotal: Double = 0.0,
    @SerialName("maintenance_amount") val maintenanceAmount: Double = 0.0,
    @SerialName("margin_amount") val marginAmount: Double = 0.0,
    val total: Double = 0.0,
    val currency: String = "USD",
    val status: String = "UNPAID"
)

@Serializable
data class InvoiceHistoryResponse(
    val invoices: List<InvoiceSummary> = emptyList()
)
