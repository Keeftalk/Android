package tn.keeftalk.chat.feature.finances.util

data class CountryIbanSpec(
    val countryCode: String,
    val countryName: String,
    val expectedIbanLength: Int
)

data class BankRecord(
    val bankCode: String,
    val bankName: String,
    val countryCode: String,
    val swiftBic: String? = null
)

object BankMetadataDatabase {

    val countrySpecs = mapOf(
        "TN" to CountryIbanSpec("TN", "Tunisia", 24),
        "FR" to CountryIbanSpec("FR", "France", 27),
        "DE" to CountryIbanSpec("DE", "Germany", 22),
        "GB" to CountryIbanSpec("GB", "United Kingdom", 22),
        "CH" to CountryIbanSpec("CH", "Switzerland", 21),
        "AE" to CountryIbanSpec("AE", "United Arab Emirates", 23),
        "SA" to CountryIbanSpec("SA", "Saudi Arabia", 24),
        "QA" to CountryIbanSpec("QA", "Qatar", 29),
        "IT" to CountryIbanSpec("IT", "Italy", 27),
        "ES" to CountryIbanSpec("ES", "Spain", 24),
        "BE" to CountryIbanSpec("BE", "Belgium", 16),
        "NL" to CountryIbanSpec("NL", "Netherlands", 18),
        "AT" to CountryIbanSpec("AT", "Austria", 20),
        "PT" to CountryIbanSpec("PT", "Portugal", 25),
        "LU" to CountryIbanSpec("LU", "Luxembourg", 20),
        "MA" to CountryIbanSpec("MA", "Morocco", 28),
        "DZ" to CountryIbanSpec("DZ", "Algeria", 24),
        "EG" to CountryIbanSpec("EG", "Egypt", 29),
        "US" to CountryIbanSpec("US", "United States", 0) // US doesn't use IBAN, uses routing/account
    )

    val tunisianBankCodes = mapOf(
        "01" to BankRecord("01", "Arab Tunisian Bank (ATB)", "TN", "ATBKINTT"),
        "02" to BankRecord("02", "Banque Franco-Tunisienne (BFT)", "TN", "BFNTTNTT"),
        "03" to BankRecord("03", "Société Tunisienne de Banque (STB)", "TN", "STBKTNTT"),
        "04" to BankRecord("04", "BH Bank (Banque de l'Habitat)", "TN", "BHBKTNTT"),
        "05" to BankRecord("05", "Banque Nationale Agricole (BNA)", "TN", "BNAGTNTT"),
        "07" to BankRecord("07", "AMEN Bank", "TN", "AMNTTNTT"),
        "08" to BankRecord("08", "Banque Internationale Arabe de Tunisie (BIAT)", "TN", "BIATTNTT"),
        "10" to BankRecord("10", "Société Tunisienne de Banque (STB)", "TN", "STBKTNTT"),
        "11" to BankRecord("11", "Union Bancaire pour le Commerce et l'Industrie (UBCI)", "TN", "UBCITNTT"),
        "12" to BankRecord("12", "Union Internationale de Banques (UIB)", "TN", "UIBKTNTT"),
        "14" to BankRecord("14", "Banque de Tunisie et des Émirats (BTE)", "TN", "BTETTNTT"),
        "17" to BankRecord("17", "Attijari Bank Tunisia", "TN", "BSTUTNTT"),
        "20" to BankRecord("20", "Banque de Tunisie (BT)", "TN", "BTBKTNTT"),
        "21" to BankRecord("21", "Banque de Tunisie et des Émirats (BTE)", "TN", "BTETTNTT"),
        "25" to BankRecord("25", "Banque de Tunisie et de Libye (BTL)", "TN", "BTLITNTT"),
        "26" to BankRecord("26", "North Africa International Bank (NAIB)", "TN", "NAIBTNTT"),
        "32" to BankRecord("32", "Al Baraka Bank Tunisia", "TN", "BARKTNTT"),
        "47" to BankRecord("47", "WIFAK International Bank", "TN", "WIFAKTNTT"),
        "81" to BankRecord("81", "La Poste Tunisienne (CCP)", "TN", "LPTTTNTT")
    )

    val frenchBankCodes = mapOf(
        "30002" to BankRecord("30002", "BNP Paribas", "FR", "BNPAFRPP"),
        "30003" to BankRecord("30003", "Société Générale", "FR", "SOGEFRPP"),
        "30004" to BankRecord("30004", "BNP Paribas", "FR", "BNPAFRPP"),
        "30006" to BankRecord("30006", "Crédit Agricole", "FR", "AGRIFRPP"),
        "10278" to BankRecord("10278", "Crédit Mutuel", "FR", "CMCIFRPP"),
        "17515" to BankRecord("17515", "Groupe BPCE / Caisse d'Épargne", "FR", "CEPAFRPP"),
        "20041" to BankRecord("20041", "La Banque Postale", "FR", "LBPOFRPP")
    )

    val germanBankCodes = mapOf(
        "10010010" to BankRecord("10010010", "Postbank Berlin", "DE", "PBNKDEFF"),
        "10070000" to BankRecord("10070000", "Deutsche Bank", "DE", "DEUTDEDB"),
        "10050000" to BankRecord("10050000", "Landesbank Berlin", "DE", "BEBE3DED"),
        "10011001" to BankRecord("10011001", "N26 Bank", "DE", "N26BDEFF")
    )

    val ukSortCodes = mapOf(
        "200000" to BankRecord("200000", "Barclays Bank UK", "GB", "BARCGB22"),
        "400000" to BankRecord("400000", "HSBC UK", "GB", "MIDLGB22"),
        "300000" to BankRecord("300000", "Lloyds Bank", "GB", "LOYDGB2L"),
        "600000" to BankRecord("600000", "NatWest", "GB", "NWBKGB22"),
        "040004" to BankRecord("040004", "Monzo Bank", "GB", "MONZGB22"),
        "236972" to BankRecord("236972", "Revolut Ltd", "GB", "REVOGB21")
    )

    fun getCountrySpec(countryCode: String): CountryIbanSpec? = countrySpecs[countryCode.uppercase()]

    fun lookupBank(countryCode: String, bankCode: String): BankRecord? {
        val upperCountry = countryCode.uppercase()
        return when (upperCountry) {
            "TN" -> tunisianBankCodes[bankCode.padStart(2, '0')]
            "FR" -> frenchBankCodes[bankCode]
            "DE" -> germanBankCodes[bankCode]
            "GB" -> ukSortCodes[bankCode]
            else -> null
        }
    }
}
