package com.keeftalk.chat.ui.debug

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.keeftalk.chat.util.PerformanceProfiler
import java.io.File

class PerformanceDashboardFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                PerformanceDashboardScreen(onClose = { parentFragmentManager.popBackStack() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceDashboardScreen(onClose: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Overview", "Compose", "Reports")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Performance Dashboard") },
                actions = {
                    IconButton(onClick = onClose) {
                        Text("Close")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> OverviewTab()
                1 -> ComposeTab()
                2 -> ReportsTab()
            }
        }
    }
}

@Composable
fun OverviewTab() {
    val sessions = remember { PerformanceProfiler.getAllSessions() }
    
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text("Active Sessions", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
        }
        items(sessions) { session ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(session.name, fontWeight = FontWeight.Bold)
                    Text("Trace ID: ${session.traceId}", fontSize = 12.sp)
                    val duration = ((session.endTimeNanos ?: android.os.SystemClock.elapsedRealtimeNanos()) - session.startTimeNanos) / 1_000_000.0
                    Text("Duration: %.2f ms".format(duration), color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
fun ComposeTab() {
    val stats = remember { PerformanceProfiler.getCompositionStats() }
    
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(stats.sortedByDescending { it.totalTimeNanos.get() }) { stat ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(stat.name, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Comp: ${stat.compositions.get()}")
                        Text("Recomp: ${stat.recompositions.get()}", color = if (stat.recompositions.get() > 10) Color.Red else Color.Unspecified)
                        Text("Skipped: ${stat.skipped.get()}", color = Color.Green)
                    }
                    Text("Total Time: %.2f ms".format(stat.totalTimeNanos.get() / 1_000_000.0))
                }
            }
        }
    }
}

@Composable
fun ReportsTab() {
    val files = remember { PerformanceProfiler.getReportFiles() }
    
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        items(files) { file ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(file.name, fontWeight = FontWeight.Medium)
                    Text("Size: ${file.length() / 1024} KB", fontSize = 12.sp)
                    Text("Date: ${java.util.Date(file.lastModified())}", fontSize = 12.sp)
                }
            }
        }
    }
}
