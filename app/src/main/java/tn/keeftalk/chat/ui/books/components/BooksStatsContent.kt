package tn.keeftalk.chat.ui.books.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.keeftalk.chat.domain.model.Book

@Composable
fun BooksStatsContent(
    books: List<Book>,
    onBookStatsClick: (Book) -> Unit,
    currentTab: String = "sta",
    onSelectTab: (String) -> Unit = {},
    isRailExpanded: Boolean = false,
    modifier: Modifier = Modifier
) {
    val totalHoursRead = books.sumOf { it.hoursRead.toDouble() }.toFloat()
    val totalPagesRead = books.sumOf { (it.totalPages * it.percentProgress / 100).toDouble() }.toInt()
    val finishedCount = books.count { it.percentProgress >= 100 }
    val activeCount = books.count { !it.isArchived && !it.isDeleted }

    val hasReadingData = books.isNotEmpty() && books.any { it.percentProgress > 0 || it.hoursRead > 0 }

    val pdfCount = books.count { it.format.equals("PDF", true) }
    val epubCount = books.count { it.format.equals("EPUB", true) || it.format.equals("EPUB3", true) }
    val totalFmt = maxOf(1, pdfCount + epubCount)
    val pdfPercent = (pdfCount * 100 / totalFmt)
    val epubPercent = 100 - pdfPercent

    val avgSpeed = if (books.isNotEmpty()) books.map { it.speedPagesPerHour }.average().toInt() else 35

    val activityData = remember(books) {
        val now = System.currentTimeMillis()
        val dayMillis = 24L * 60L * 60L * 1000L
        List(126) { idx ->
            val daysAgo = 125 - idx
            val targetDayStart = now - (daysAgo + 1) * dayMillis
            val targetDayEnd = now - daysAgo * dayMillis
            val matchingBooks = books.filter { book ->
                book.updatedAt in targetDayStart..targetDayEnd || book.createdAt in targetDayStart..targetDayEnd
            }
            if (matchingBooks.isNotEmpty()) {
                val progressSum = matchingBooks.sumOf { it.percentProgress }.toFloat()
                (0.3f + (progressSum / 100f) * 0.7f).coerceIn(0.3f, 1f)
            } else {
                0f
            }
        }
    }

    val dailyMinutes = remember(books) {
        val calendar = java.util.Calendar.getInstance()
        val currentDayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
        val todayMonIndex = if (currentDayOfWeek == java.util.Calendar.SUNDAY) 6 else currentDayOfWeek - 2
        val minutesPerDay = IntArray(7) { 0 }
        books.forEach { book ->
            val bookMins = (book.hoursRead * 60).toInt()
            if (bookMins > 0) {
                calendar.timeInMillis = book.updatedAt
                val updateDayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
                val monIdx = if (updateDayOfWeek == java.util.Calendar.SUNDAY) 6 else updateDayOfWeek - 2
                if (monIdx in 0..6) {
                    minutesPerDay[monIdx] += bookMins.coerceAtMost(180)
                } else {
                    minutesPerDay[todayMonIndex.coerceIn(0, 6)] += bookMins.coerceAtMost(180) / maxOf(1, books.size)
                }
            }
        }
        minutesPerDay.toList()
    }

    val monthlyPages = remember(books) {
        val calendar = java.util.Calendar.getInstance()
        List(9) { mIdx ->
            val targetMonthsAgo = 8 - mIdx
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(java.util.Calendar.MONTH, -targetMonthsAgo)
            val targetMonth = calendar.get(java.util.Calendar.MONTH)
            val targetYear = calendar.get(java.util.Calendar.YEAR)
            val pagesInMonth = books.filter { book ->
                calendar.timeInMillis = book.updatedAt
                calendar.get(java.util.Calendar.MONTH) == targetMonth && calendar.get(java.util.Calendar.YEAR) == targetYear
            }.sumOf { (it.totalPages * it.percentProgress / 100) }
            maxOf(0, pagesInMonth)
        }
    }

    val speedHistory = remember(books) {
        val speeds = books.map { it.speedPagesPerHour }.filter { it > 0 }
        if (speeds.isNotEmpty()) {
            val avg = speeds.average().toInt()
            speeds.takeLast(7).let { list ->
                if (list.size < 7) List(7 - list.size) { avg } + list else list
            }
        } else {
            listOf(30, 30, 30, 30, 30, 30, 30)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        // Hero Row: This Year Hero Card + Section Rail
        HeroRowWithRail(
            currentTab = currentTab,
            onSelectTab = onSelectTab,
            isRailExpanded = isRailExpanded,
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 170.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 170.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF6C63FF), Color(0xFF7D5CFF), Color(0xFF00CCCC))
                            )
                        )
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "THIS YEAR",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${totalHoursRead.toInt()}h",
                                    fontSize = 34.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "≈ ${if (totalHoursRead > 0) (totalHoursRead / 24).toInt() else 0} days of pure reading · $totalPagesRead pages",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                            DonutProgressRing(
                                percent = if (books.isNotEmpty()) (finishedCount * 100 / books.size).coerceIn(0, 100) else 0,
                                ringColor = Color.White,
                                trackColor = Color.White.copy(alpha = 0.3f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 9.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.TrackChanges,
                                    contentDescription = "Target",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$finishedCount of ${books.size} books completed",
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        if (books.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "Stats",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No Stats Yet", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Add a book to your library to track reading time, pages, and insights.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // KPI Tiles
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            KpiTile("$totalPagesRead", "Pages", modifier = Modifier.weight(1f))
                            KpiTile("$finishedCount", "Finished", modifier = Modifier.weight(1f))
                            KpiTile("$activeCount", "Active", modifier = Modifier.weight(1f))
                        }
                    }
                }

                if (!hasReadingData) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("No Reading Activity Yet", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Start reading your books to unlock real-time charts, graphs, and reading speed analytics.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    // Finish Line Estimates
                    val unfinished = books.filter { !it.isArchived && !it.isDeleted && it.percentProgress < 100 }
                    if (unfinished.isNotEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Flag,
                                            contentDescription = "Finish Line",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "FINISH LINE · ESTIMATES",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    unfinished.take(3).forEach { book ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .clickable { onBookStatsClick(book) },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(modifier = Modifier.width(30.dp)) {
                                                BookCover(book = book, isMini = true)
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(book.title, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                                LinearProgressIndicator(
                                                    progress = { book.percentProgress / 100f },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(5.dp)
                                                        .clip(RoundedCornerShape(3.dp)),
                                                    color = Color(0xFF6C63FF)
                                                )
                                                Text("${book.totalPages * (100 - book.percentProgress) / 100} pages remaining", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text("In progress", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00CCCC))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 1. Activity Heatmap (18 weeks)
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("ACTIVITY · 18 WEEKS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(10.dp))
                                ActivityHeatmap(activityData = activityData, modifier = Modifier.height(84.dp))
                            }
                        }
                    }

                    // 2. This week · minutes per day BarChart
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("THIS WEEK · MINUTES PER DAY", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(10.dp))
                                BarChart(
                                    values = dailyMinutes,
                                    unit = "m",
                                    labels = listOf("M", "T", "W", "T", "F", "S", "S")
                                )
                            }
                        }
                    }

                    // 3. Pages per month BarChart
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("PAGES PER MONTH", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(10.dp))
                                BarChart(
                                    values = monthlyPages,
                                    labels = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S")
                                )
                            }
                        }
                    }

                    // 4. Reading speed · pages per hour AreaChart
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("READING SPEED · PAGES PER HOUR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(10.dp))
                                AreaChart(
                                    actualValues = speedHistory,
                                    unit = "p/h"
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF00CCCC).copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 9.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                            contentDescription = "Speed",
                                            tint = Color(0xFF00CCCC),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Average speed: $avgSpeed pages/hour", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }

                    // 5. Format Share Donut
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DonutProgressRing(
                                    percent = pdfPercent,
                                    ringColor = Color(0xFF6C63FF),
                                    trackColor = Color(0xFF00CCCC),
                                    size = 64.dp
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("PDF $pdfPercent% · EPUB $epubPercent%", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Total books: ${books.size}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                // All Books List
                item {
                    Text("ALL BOOKS · TAP FOR DETAILS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                items(books.filter { !it.isDeleted }, key = { it.id }) { book ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onBookStatsClick(book) }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.width(30.dp)) {
                                BookCover(book = book, isMini = true)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(book.title, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                LinearProgressIndicator(
                                    progress = { book.percentProgress / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = Color(0xFF6C63FF)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("${book.hoursRead.toInt()}h", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KpiTile(
    value: String,
    caption: String,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(text = caption, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
