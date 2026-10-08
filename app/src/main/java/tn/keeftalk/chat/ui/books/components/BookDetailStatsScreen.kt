package tn.keeftalk.chat.ui.books.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tn.keeftalk.chat.data.local.entities.ReadingSessionEntity
import tn.keeftalk.chat.domain.model.Book
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailStatsScreen(
    book: Book,
    sessions: List<ReadingSessionEntity> = emptyList(),
    totalXp: Long = 0L,
    onBack: () -> Unit,
    onContinueReading: (Book) -> Unit,
    modifier: Modifier = Modifier
) {
    val pagesReadCount = (book.totalPages * book.percentProgress / 100)
    val remainingPages = maxOf(0, book.totalPages - pagesReadCount)
    val hoursToFinish = if (book.speedPagesPerHour > 0) {
        maxOf(0.1f, remainingPages / book.speedPagesPerHour.toFloat())
    } else {
        maxOf(0.1f, remainingPages / 40f)
    }
    val etaStr = if (hoursToFinish >= 10) "~${hoursToFinish.toInt()}h left" else "~${String.format(Locale.getDefault(), "%.1f", hoursToFinish)}h left"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book Insight", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Book Header Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.width(54.dp)) {
                    BookCover(book = book)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(book.title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(book.author, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${book.totalPages} pages · ${book.format} · $etaStr", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }

            // Progress Hero Card
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF6C63FF), Color(0xFF7D5CFF), Color(0xFF00CCCC))
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Text("READING PROGRESS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.8f))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("${book.percentProgress}%", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        Text("Page ${book.lastReadPage} of ${book.totalPages} · $remainingPages pages remaining", fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { book.percentProgress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.3f)
                        )
                    }
                }
            }

            // Real KPI Tiles (Hours, Speed, XP, ET2Finish)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KpiTile("${book.hoursRead.toInt()}h", "Time read", modifier = Modifier.weight(1f))
                KpiTile("${book.speedPagesPerHour}", "Pages/h", modifier = Modifier.weight(1f))
                KpiTile("$totalXp", "XP Earned", modifier = Modifier.weight(1f))
                KpiTile(etaStr, "ET2Finish", modifier = Modifier.weight(1f))
            }

            // Charts & Graphs Section
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BarChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reading Pace & Sessions Graph", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val sessionBarHeights = if (sessions.isNotEmpty()) {
                            sessions.takeLast(7).map { (it.pagesConfirmed * 10).coerceIn(15, 80) }
                        } else {
                            listOf(20, 35, 50, 40, 65, 50, 75)
                        }
                        sessionBarHeights.forEach { height ->
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(height.dp)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(Color(0xFF6C63FF), Color(0xFF00CCCC))
                                        )
                                    )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Last ${sessions.size.coerceAtLeast(1)} reading sessions activity", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // All Sessions for this Book
            Text("All Reading Sessions", fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
            if (sessions.isEmpty()) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(16.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No sessions recorded yet for this book.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    sessions.forEach { session ->
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF6C63FF), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${session.pagesConfirmed} pages read", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("Duration: ${session.activeSec / 60} mins", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Surface(
                                    color = Color(0xFF00CCCC).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Bolt, contentDescription = null, tint = Color(0xFF00CCCC), modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("+${session.totalXpGained} XP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00CCCC))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Continue Reading Action Button
            Button(
                onClick = { onContinueReading(book) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Continue Reading", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
