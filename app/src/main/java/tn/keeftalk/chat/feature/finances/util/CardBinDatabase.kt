package tn.keeftalk.chat.feature.finances.util

import android.content.Context
import io.github.jan.supabase.postgrest.postgrest
import java.util.concurrent.ConcurrentHashMap

enum class CardCategory {
    DEBIT, CREDIT, PREPAID, UNKNOWN
}

data class BinRecord(
    val binPrefix: String,
    val bankName: String,
    val countryCode: String,
    val countryName: String,
    val cardCategory: CardCategory,
    val cardLevel: String,
    val network: CardNetwork
)

object CardBinDatabase {

    private val onlineCache = ConcurrentHashMap<String, BinRecord>()

    private val binRecords = listOf(
        // Tunisian Banks (BIAT, Attijari, STB, BH, AMEN, UIB, ATB, BNA, Poste Tunisienne / CCP)
        BinRecord("535896", "Banque Internationale Arabe de Tunisie (BIAT)", "TN", "Tunisia", CardCategory.CREDIT, "Platinum", CardNetwork.MASTERCARD),
        BinRecord("545896", "Banque Internationale Arabe de Tunisie (BIAT)", "TN", "Tunisia", CardCategory.DEBIT, "World Elite", CardNetwork.MASTERCARD),
        BinRecord("402123", "Banque Internationale Arabe de Tunisie (BIAT)", "TN", "Tunisia", CardCategory.DEBIT, "Classic", CardNetwork.VISA),
        BinRecord("412123", "Banque Internationale Arabe de Tunisie (BIAT)", "TN", "Tunisia", CardCategory.CREDIT, "Gold", CardNetwork.VISA),
        BinRecord("520088", "Banque Internationale Arabe de Tunisie (BIAT)", "TN", "Tunisia", CardCategory.DEBIT, "Standard", CardNetwork.MASTERCARD),
        BinRecord("453312", "Attijari Bank Tunisia", "TN", "Tunisia", CardCategory.DEBIT, "Gold", CardNetwork.VISA),
        BinRecord("518921", "Attijari Bank Tunisia", "TN", "Tunisia", CardCategory.CREDIT, "World", CardNetwork.MASTERCARD),
        BinRecord("540112", "Attijari Bank Tunisia", "TN", "Tunisia", CardCategory.PREPAID, "C'Net", CardNetwork.MASTERCARD),
        BinRecord("401234", "Société Tunisienne de Banque (STB)", "TN", "Tunisia", CardCategory.DEBIT, "Classic", CardNetwork.VISA),
        BinRecord("530123", "Société Tunisienne de Banque (STB)", "TN", "Tunisia", CardCategory.CREDIT, "Gold", CardNetwork.MASTERCARD),
        BinRecord("525200", "Société Tunisienne de Banque (STB)", "TN", "Tunisia", CardCategory.DEBIT, "Standard", CardNetwork.MASTERCARD),
        BinRecord("409876", "BH Bank", "TN", "Tunisia", CardCategory.DEBIT, "Classic", CardNetwork.VISA),
        BinRecord("539876", "BH Bank", "TN", "Tunisia", CardCategory.CREDIT, "Gold", CardNetwork.MASTERCARD),
        BinRecord("404567", "AMEN Bank", "TN", "Tunisia", CardCategory.DEBIT, "Classic", CardNetwork.VISA),
        BinRecord("534567", "AMEN Bank", "TN", "Tunisia", CardCategory.CREDIT, "Gold", CardNetwork.MASTERCARD),
        BinRecord("450012", "AMEN Bank", "TN", "Tunisia", CardCategory.DEBIT, "Platinum", CardNetwork.VISA),
        BinRecord("403210", "Union Internationale de Banques (UIB)", "TN", "Tunisia", CardCategory.DEBIT, "Classic", CardNetwork.VISA),
        BinRecord("523210", "Union Internationale de Banques (UIB)", "TN", "Tunisia", CardCategory.CREDIT, "Gold", CardNetwork.MASTERCARD),
        BinRecord("407890", "Arab Tunisian Bank (ATB)", "TN", "Tunisia", CardCategory.DEBIT, "Classic", CardNetwork.VISA),
        BinRecord("517890", "Arab Tunisian Bank (ATB)", "TN", "Tunisia", CardCategory.CREDIT, "Gold", CardNetwork.MASTERCARD),
        BinRecord("406543", "Banque Nationale Agricole (BNA)", "TN", "Tunisia", CardCategory.DEBIT, "Classic", CardNetwork.VISA),
        BinRecord("526543", "Banque Nationale Agricole (BNA)", "TN", "Tunisia", CardCategory.CREDIT, "Gold", CardNetwork.MASTERCARD),
        BinRecord("508930", "La Poste Tunisienne (CCP / Edinar)", "TN", "Tunisia", CardCategory.PREPAID, "e-Dinar Smart", CardNetwork.MAESTRO),
        BinRecord("508931", "La Poste Tunisienne (CCP / Edinar)", "TN", "Tunisia", CardCategory.PREPAID, "e-Dinar Jeune", CardNetwork.MAESTRO),
        BinRecord("508932", "La Poste Tunisienne (CCP / Edinar)", "TN", "Tunisia", CardCategory.PREPAID, "e-Dinar Corporate", CardNetwork.MAESTRO),
        BinRecord("603344", "La Poste Tunisienne (CCP / Edinar)", "TN", "Tunisia", CardCategory.PREPAID, "Carte Jalouss", CardNetwork.MAESTRO),

        // US Banks (JPMorgan Chase, Bank of America, Citigroup, Wells Fargo, Capital One, Amex)
        BinRecord("400022", "JPMorgan Chase Bank", "US", "United States", CardCategory.DEBIT, "Classic", CardNetwork.VISA),
        BinRecord("414720", "JPMorgan Chase Bank", "US", "United States", CardCategory.CREDIT, "Freedom Unlimited", CardNetwork.VISA),
        BinRecord("424631", "JPMorgan Chase Bank", "US", "United States", CardCategory.CREDIT, "Sapphire Preferred", CardNetwork.VISA),
        BinRecord("438857", "JPMorgan Chase Bank", "US", "United States", CardCategory.CREDIT, "Sapphire Reserve", CardNetwork.VISA),
        BinRecord("440066", "JPMorgan Chase Bank", "US", "United States", CardCategory.DEBIT, "Business", CardNetwork.VISA),
        BinRecord("540111", "JPMorgan Chase Bank", "US", "United States", CardCategory.CREDIT, "Mastercard World", CardNetwork.MASTERCARD),
        BinRecord("371440", "American Express", "US", "United States", CardCategory.CREDIT, "Platinum Card", CardNetwork.AMEX),
        BinRecord("378282", "American Express", "US", "United States", CardCategory.CREDIT, "Gold Card", CardNetwork.AMEX),
        BinRecord("371200", "American Express", "US", "United States", CardCategory.CREDIT, "Centurion", CardNetwork.AMEX),
        BinRecord("400115", "Bank of America", "US", "United States", CardCategory.DEBIT, "Classic", CardNetwork.VISA),
        BinRecord("402400", "Bank of America", "US", "United States", CardCategory.CREDIT, "Customized Cash", CardNetwork.VISA),
        BinRecord("412174", "Bank of America", "US", "United States", CardCategory.CREDIT, "Travel Rewards", CardNetwork.VISA),
        BinRecord("521020", "Bank of America", "US", "United States", CardCategory.CREDIT, "Mastercard World", CardNetwork.MASTERCARD),
        BinRecord("412800", "Citigroup / Citibank", "US", "United States", CardCategory.CREDIT, "Double Cash", CardNetwork.VISA),
        BinRecord("430000", "Citigroup / Citibank", "US", "United States", CardCategory.DEBIT, "Classic", CardNetwork.VISA),
        BinRecord("542000", "Citigroup / Citibank", "US", "United States", CardCategory.CREDIT, "AAdvantage", CardNetwork.MASTERCARD),
        BinRecord("400000", "Wells Fargo Bank", "US", "United States", CardCategory.DEBIT, "Classic", CardNetwork.VISA),
        BinRecord("470000", "Wells Fargo Bank", "US", "United States", CardCategory.CREDIT, "Autograph", CardNetwork.VISA),
        BinRecord("410000", "Capital One", "US", "United States", CardCategory.CREDIT, "Venture Rewards", CardNetwork.VISA),
        BinRecord("517800", "Capital One", "US", "United States", CardCategory.CREDIT, "Quicksilver", CardNetwork.MASTERCARD),
        BinRecord("601100", "Discover Financial Services", "US", "United States", CardCategory.CREDIT, "Discover it Cash Back", CardNetwork.DISCOVER),
        BinRecord("650000", "Discover Financial Services", "US", "United States", CardCategory.CREDIT, "Discover Chrome", CardNetwork.DISCOVER),

        // European & UK Banks (BNP Paribas, SocGen, Crédit Agricole, Revolut, Wise, N26, Barclays, HSBC, UBS, Deutsche Bank)
        BinRecord("403500", "BNP Paribas", "FR", "France", CardCategory.DEBIT, "Visa Premier", CardNetwork.VISA),
        BinRecord("455600", "BNP Paribas", "FR", "France", CardCategory.DEBIT, "Visa Classic", CardNetwork.VISA),
        BinRecord("510000", "BNP Paribas", "FR", "France", CardCategory.CREDIT, "Mastercard Gold", CardNetwork.MASTERCARD),
        BinRecord("415000", "Société Générale", "FR", "France", CardCategory.DEBIT, "Visa Premier", CardNetwork.VISA),
        BinRecord("497000", "Société Générale", "FR", "France", CardCategory.DEBIT, "Visa Infinite", CardNetwork.VISA),
        BinRecord("540000", "Société Générale", "FR", "France", CardCategory.CREDIT, "Mastercard Gold", CardNetwork.MASTERCARD),
        BinRecord("453000", "Crédit Agricole", "FR", "France", CardCategory.DEBIT, "Visa Classic", CardNetwork.VISA),
        BinRecord("459654", "Revolut Bank", "GB", "United Kingdom", CardCategory.DEBIT, "Metal", CardNetwork.VISA),
        BinRecord("535234", "Revolut Bank", "GB", "United Kingdom", CardCategory.DEBIT, "Premium", CardNetwork.MASTERCARD),
        BinRecord("516808", "Revolut Bank", "GB", "United Kingdom", CardCategory.DEBIT, "Standard", CardNetwork.MASTERCARD),
        BinRecord("531993", "Wise (TransferWise)", "GB", "United Kingdom", CardCategory.DEBIT, "Multi-Currency", CardNetwork.MASTERCARD),
        BinRecord("559105", "Wise (TransferWise)", "GB", "United Kingdom", CardCategory.DEBIT, "Business", CardNetwork.MASTERCARD),
        BinRecord("516918", "N26 Bank", "DE", "Germany", CardCategory.DEBIT, "You / Metal", CardNetwork.MASTERCARD),
        BinRecord("516919", "N26 Bank", "DE", "Germany", CardCategory.DEBIT, "Standard", CardNetwork.MASTERCARD),
        BinRecord("465800", "Barclays Bank", "GB", "United Kingdom", CardCategory.DEBIT, "Barclaycard Platinum", CardNetwork.VISA),
        BinRecord("492900", "Barclays Bank", "GB", "United Kingdom", CardCategory.DEBIT, "Classic", CardNetwork.VISA),
        BinRecord("400002", "HSBC Bank", "GB", "United Kingdom", CardCategory.CREDIT, "Premier World Elite", CardNetwork.VISA),
        BinRecord("541234", "HSBC Bank", "GB", "United Kingdom", CardCategory.DEBIT, "Advance", CardNetwork.MASTERCARD),
        BinRecord("410200", "UBS Switzerland", "CH", "Switzerland", CardCategory.CREDIT, "Visa Gold", CardNetwork.VISA),
        BinRecord("524100", "UBS Switzerland", "CH", "Switzerland", CardCategory.CREDIT, "Mastercard Platinum", CardNetwork.MASTERCARD),
        BinRecord("402000", "Deutsche Bank", "DE", "Germany", CardCategory.DEBIT, "Visa Gold", CardNetwork.VISA),
        BinRecord("512000", "Deutsche Bank", "DE", "Germany", CardCategory.CREDIT, "Mastercard Travel", CardNetwork.MASTERCARD),

        // Asia & Middle East (JCB, UnionPay, Emirates NBD, Al Rajhi)
        BinRecord("352800", "JCB Co. Ltd.", "JP", "Japan", CardCategory.CREDIT, "JCB Gold", CardNetwork.JCB),
        BinRecord("356600", "JCB Co. Ltd.", "JP", "Japan", CardCategory.CREDIT, "JCB Platinum", CardNetwork.JCB),
        BinRecord("621234", "China UnionPay", "CN", "China", CardCategory.DEBIT, "UnionPay Gold", CardNetwork.UNIONPAY),
        BinRecord("622126", "China UnionPay", "CN", "China", CardCategory.CREDIT, "UnionPay Platinum", CardNetwork.UNIONPAY),
        BinRecord("400010", "Emirates NBD", "AE", "United Arab Emirates", CardCategory.DEBIT, "Visa Infinite", CardNetwork.VISA),
        BinRecord("520010", "Emirates NBD", "AE", "United Arab Emirates", CardCategory.CREDIT, "Mastercard World", CardNetwork.MASTERCARD),
        BinRecord("412345", "Al Rajhi Bank", "SA", "Saudi Arabia", CardCategory.DEBIT, "Visa Signature", CardNetwork.VISA),
        BinRecord("512345", "Al Rajhi Bank", "SA", "Saudi Arabia", CardCategory.CREDIT, "Mastercard World", CardNetwork.MASTERCARD)
    )

    fun lookup(binPrefix: String): BinRecord? {
        if (binPrefix.length < 6) return null
        val prefix6 = binPrefix.take(6)
        return onlineCache[prefix6] ?: binRecords.firstOrNull { it.binPrefix == prefix6 }
    }

    /**
     * Queries Supabase BIN_DB table when online and caches the result locally.
     * Falls back to local offline lookup if network is unavailable or query fails.
     */
    suspend fun lookupOnline(context: Context, binPrefix: String): BinRecord? {
        if (binPrefix.length < 6) return null
        val prefix6 = binPrefix.take(6)

        // 1. Return cached online or offline record if present
        lookup(prefix6)?.let { return it }

        // 2. Query online Supabase table "BIN_DB"
        return try {
            val supabase = tn.keeftalk.chat.di.AppModule.provideSupabaseClientAsync(context)
            val response = supabase.postgrest["BIN_DB"]
                .select {
                    filter {
                        eq("BIN", prefix6)
                    }
                }
                .decodeList<SupabaseBinDto>()

            val match = response.firstOrNull() ?: return null

            val cardCat = when (match.type?.uppercase()) {
                "DEBIT" -> CardCategory.DEBIT
                "CREDIT" -> CardCategory.CREDIT
                "PREPAID" -> CardCategory.PREPAID
                else -> CardCategory.UNKNOWN
            }

            val net = when (match.brand?.uppercase()) {
                "VISA" -> CardNetwork.VISA
                "MASTERCARD" -> CardNetwork.MASTERCARD
                "AMERICAN EXPRESS", "AMEX" -> CardNetwork.AMEX
                "DISCOVER" -> CardNetwork.DISCOVER
                "JCB" -> CardNetwork.JCB
                "DINERS CLUB", "DINERS" -> CardNetwork.DINERS_CLUB
                "UNIONPAY" -> CardNetwork.UNIONPAY
                "MAESTRO" -> CardNetwork.MAESTRO
                "MIR" -> CardNetwork.MIR
                "TROY" -> CardNetwork.TROY
                "RUPAY" -> CardNetwork.RUPAY
                else -> OfflineCardDetector.detectNetwork(prefix6)
            }

            val record = BinRecord(
                binPrefix = prefix6,
                bankName = match.issuer?.ifBlank { null } ?: "Unknown Issuer",
                countryCode = match.isoCode2?.ifBlank { null } ?: "XX",
                countryName = match.countryName?.ifBlank { null } ?: "Unknown Country",
                cardCategory = cardCat,
                cardLevel = match.category?.ifBlank { null } ?: "Standard",
                network = net
            )

            onlineCache[prefix6] = record
            record
        } catch (e: Exception) {
            android.util.Log.d("CardBinDatabase", "Supabase BIN_DB query offline/failed: ${e.message}")
            lookup(prefix6)
        }
    }
}
