package tn.keeftalk.chat.feature.finances.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

object FinancesIcons {
    // Categories
    val Housing: ImageVector = Icons.Default.Home
    val Food: ImageVector = Icons.Default.Restaurant
    val Transport: ImageVector = Icons.Default.DirectionsBus
    val Fun: ImageVector = Icons.Default.Movie
    val Health: ImageVector = Icons.Default.LocalHospital
    val Shopping: ImageVector = Icons.Default.ShoppingBag
    val Bills: ImageVector = Icons.Default.Receipt
    val Income: ImageVector = Icons.Default.Work

    // Account & Asset kinds
    val Card: ImageVector = Icons.Default.CreditCard
    val Bank: ImageVector = Icons.Default.AccountBalance
    val Wallet: ImageVector = Icons.Default.AccountBalanceWallet
    val Cash: ImageVector = Icons.Default.AttachMoney
    val Fund: ImageVector = Icons.Default.Equalizer
    val Stock: ImageVector = Icons.Default.ShowChart
    val Crypto: ImageVector = Icons.Default.CurrencyBitcoin
    val Property: ImageVector = Icons.Default.LocationCity

    // Tools & Forms
    val Tools: ImageVector = Icons.Default.Build
    val Settings: ImageVector = Icons.Default.Settings
    val Calendar: ImageVector = Icons.Default.CalendarToday
    val Forecast: ImageVector = Icons.Default.QueryStats
    val Simulator: ImageVector = Icons.Default.Science
    val HealthFire: ImageVector = Icons.Default.Psychology
    val DebtPayoff: ImageVector = Icons.Default.Payments
    val Subscriptions: ImageVector = Icons.Default.HourglassEmpty
    val SharedExpenses: ImageVector = Icons.Default.Group
    val Challenges: ImageVector = Icons.Default.EmojiEvents
    val NetWorth: ImageVector = Icons.Default.BarChart
    val Roadmap: ImageVector = Icons.Default.Explore
    val Assistant: ImageVector = Icons.Default.SmartToy
    val Documents: ImageVector = Icons.Default.Folder
    val Trends: ImageVector = Icons.Default.ShowChart

    // Actions & General
    val Add: ImageVector = Icons.Default.Add
    val Back: ImageVector = Icons.Default.ArrowBack
    val Close: ImageVector = Icons.Default.Close
    val Copy: ImageVector = Icons.Default.ContentCopy
    val Scan: ImageVector = Icons.Default.CameraAlt
    val EditNote: ImageVector = Icons.Default.EditNote
    val Attach: ImageVector = Icons.Default.AttachFile
    val Lock: ImageVector = Icons.Default.Lock
    val LockOpen: ImageVector = Icons.Default.LockOpen
    val Search: ImageVector = Icons.Default.Search
    val Up: ImageVector = Icons.Default.ArrowDropUp
    val Down: ImageVector = Icons.Default.ArrowDropDown
    val Goal: ImageVector = Icons.Default.Flag
    val Repeat: ImageVector = Icons.Default.Repeat
    val Shield: ImageVector = Icons.Default.Shield
    val Fire: ImageVector = Icons.Default.Whatshot
    val Runway: ImageVector = Icons.Default.FlightTakeoff
    val Warning: ImageVector = Icons.Default.Warning
    val Check: ImageVector = Icons.Default.CheckCircle
    val Cut: ImageVector = Icons.Default.ContentCut
    val Sunny: ImageVector = Icons.Default.WbSunny
    val Spa: ImageVector = Icons.Default.Spa

    fun getCategoryIcon(catKey: String): ImageVector = when (catKey) {
        "rent" -> Housing
        "food" -> Food
        "transport" -> Transport
        "fun" -> Fun
        "health" -> Health
        "shop" -> Shopping
        "bills" -> Bills
        "income" -> Income
        else -> Shopping
    }

    fun getAccountKindIcon(kind: String): ImageVector = when (kind) {
        "card" -> Card
        "bank" -> Bank
        "wallet" -> Wallet
        else -> Cash
    }

    fun getAssetKindIcon(kind: String): ImageVector = when (kind) {
        "fund" -> Fund
        "stock" -> Stock
        "crypto" -> Crypto
        "cash" -> Cash
        "property" -> Property
        else -> Fund
    }

    fun getDocKindIcon(kind: String): ImageVector = when (kind) {
        "invoice" -> Bills
        "payslip" -> Cash
        "tax" -> Bills
        "contract" -> Documents
        "warranty" -> Shield
        else -> Attach
    }
}
