package tn.keeftalk.chat.ui.books.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LibraryTourDialog(
    onDismiss: () -> Unit
) {
    var step by remember { mutableStateOf(0) }
    val totalSteps = 5

    val titles = listOf(
        "Welcome to My Books",
        "Reading a Book",
        "Pages That Really Count",
        "How XP Works",
        "Tour of Other Tabs"
    )

    val descriptions = listOf(
        "Explore your reading vault, views, filters, and book menu. Keep your library organized, secure, and ready for your next session.",
        "Tap the green Read button on any book to begin. Add bookmarks, highlight key passages, and capture memorable quotes effortlessly.",
        "Pages require first-time confirmation only. Quick tapping earns nothing—genuine attention and engagement are what count.",
        "XP comes from verified pages, and real attention is worth more than skimming.\n\nConsistency and streak are the big multiplier.\n\nCramming has diminishing returns.\n\nA missed day softens the multiplier but never removes XP, and deleting a book changes nothing.\n\nSome pages in this session weren't counted.",
        "Explore Quotes, Goals, Stats, and Achievements using the side navigation rail or tabs to track your complete reading journey."
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Step ${step + 1} of $totalSteps",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = titles[step],
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = descriptions[step],
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )

                // Dots indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(totalSteps) { idx ->
                        val isCurrent = idx == step
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 3.dp)
                                .size(if (isCurrent) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isCurrent) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Skip")
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (step > 0) {
                        TextButton(onClick = { step-- }) {
                            Text("Back")
                        }
                    }
                    Button(
                        onClick = {
                            if (step < totalSteps - 1) {
                                step++
                            } else {
                                onDismiss()
                            }
                        }
                    ) {
                        Text(if (step < totalSteps - 1) "Next" else "Finish")
                    }
                }
            }
        }
    )
}

@Composable
fun QuotesInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quotes & Notes", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "Collect and revisit your favorite highlights, notes, and quotes from all your books in one place. Export your insights anytime with ease.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Got it")
            }
        }
    )
}

@Composable
fun GoalsInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Goals & Decisions", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Your reading goals cover your decisions and habits:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "• PDF & EPUB only format support\n• No manual logging (verified reading only)\n• 30-minute minimum daily goal\n• 'Read now' quick session access\n• 🏆 / ⚠️ / ❌ status card tracking your streak and progress",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Got it")
            }
        }
    )
}

@Composable
fun AchievementsInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Achievements", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Milestones that celebrate real progress, from your first 10 pages to finished books.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        "• Once unlocked, an achievement stays unlocked for good.\n• XP grows from verified pages, boosted by how consistently you read.\n• A short session every day beats one marathon a week.",
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp),
                        lineHeight = 18.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Got it")
            }
        }
    )
}
