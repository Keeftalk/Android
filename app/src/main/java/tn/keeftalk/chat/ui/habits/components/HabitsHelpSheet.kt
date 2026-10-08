package tn.keeftalk.chat.ui.habits.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsHelpSheet(
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Drag Handle
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                    .align(Alignment.CenterHorizontally)
            )

            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "How My Habits Works",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Practical guide & answers to common questions",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 540.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. How to Add or Edit a Habit
                HelpSectionCard(
                    title = "➕ How to Add or Edit a Habit",
                    icon = Icons.Default.AddCircle,
                    color = Color(0xFF7D5CFF)
                ) {
                    HelpBullet(
                        icon = Icons.Default.Add,
                        title = "Create New Habit",
                        desc = "Tap the blue '+' button at the bottom right. Enter a habit name, pick an icon, schedule days, set an amount (optional), and choose a time slot or exact custom time."
                    )
                    HelpBullet(
                        icon = Icons.Default.Edit,
                        title = "Edit or Delete",
                        desc = "Use the Edit sheet from top options to edit existing habits or delete habits you no longer want to track."
                    )
                }

                // 2. What is a Habit Type & How to Choose One
                HelpSectionCard(
                    title = "🏷️ What is a Habit Type & How to Choose One?",
                    icon = Icons.Default.Category,
                    color = Color(0xFF00CCCC)
                ) {
                    HelpBullet(
                        icon = Icons.Default.Psychology,
                        title = "Sets Your Automaticity Target",
                        desc = "Habit Type sets how many total check-ins are needed before the habit becomes automatic second-nature:"
                    )
                    HelpTypeBadgeRow("💧 Starter / Simple", "~18–30 reps", "Water, Floss, Skincare (<2 min actions)")
                    HelpTypeBadgeRow("📖 Core Routine", "~45 reps", "Reading, Journaling, Meditation")
                    HelpTypeBadgeRow("🏋️ Active Lifestyle", "~106 reps", "Gym, Yoga, Walking (physical & planned)")
                    HelpTypeBadgeRow("🚀 Transformative", "~254 reps", "Marathon training, Major lifestyle change")
                    HelpBullet(
                        icon = Icons.Default.AutoAwesome,
                        title = "Smart Auto-Detection",
                        desc = "Keeftalk automatically suggests the right category based on your habit's name as you type!"
                    )
                }

                // 3. Why Can't I Mark a Habit Done Right Now?
                HelpSectionCard(
                    title = "⏰ Why Can't I Mark a Habit Done Right Now?",
                    icon = Icons.Default.Lock,
                    color = Color(0xFFF59E0B)
                ) {
                    HelpBullet(
                        icon = Icons.Default.Schedule,
                        title = "Active Time Window (\"Open now\")",
                        desc = "Habits become open to mark done when the clock enters their scheduled slot time (with 1-hour grace tolerance). If it's too early, it displays \"Opens at HH:MM\"."
                    )
                    HelpBullet(
                        icon = Icons.Default.CalendarToday,
                        title = "Read-Only Past Days",
                        desc = "Selecting past days in the Day Strip enters Read-Only Preview Mode to preserve progress history. Only today's open habits can be checked in!"
                    )
                }

                // 4. What is a Habit Slot / Home Slot?
                HelpSectionCard(
                    title = "🏠 What is a Habit Slot / Home Slot?",
                    icon = Icons.Default.GridView,
                    color = Color(0xFF3B82F6)
                ) {
                    HelpBullet(
                        icon = Icons.Default.WbSunny,
                        title = "24-Hour Day Windows",
                        desc = "Home Slots organize your 24-hour timeline into 6 windows: Early Morning (04:00–08:00), Morning (08:00–11:30), Midday (11:30–14:00), Afternoon (14:00–17:30), Evening (17:30–21:00), and Late Night (21:00–24:00)."
                    )
                    HelpBullet(
                        icon = Icons.Default.FilterList,
                        title = "Slot Filter Chips",
                        desc = "Tap the category chips below the Hero Card to filter your habit list by slot!"
                    )
                }

                // 5. Grace Days & Grace Challenges
                HelpSectionCard(
                    title = "🛡️ What are Grace Days & Grace Challenges?",
                    icon = Icons.Default.Security,
                    color = Color(0xFF2ECC71)
                ) {
                    HelpBullet(
                        icon = Icons.Default.Shield,
                        title = "2 Grace Days Per Month",
                        desc = "Use a Grace Day on a missed day to forgive it completely with 0% streak penalty and no target increase."
                    )
                    HelpBullet(
                        icon = Icons.Default.LocalFireDepartment,
                        title = "Grace Challenge (Recovery Streak)",
                        desc = "Missed a day without grace? Complete a recovery streak (7–28 consecutive days) to automatically earn back your grace!"
                    )
                }

                // 6. Mastered & Retired Habits
                HelpSectionCard(
                    title = "🏅 What Happens when a Habit is Mastered?",
                    icon = Icons.Default.EmojiEvents,
                    color = Color(0xFFEAB308)
                ) {
                    HelpBullet(
                        icon = Icons.Default.MilitaryTech,
                        title = "200% Mastery Retirement",
                        desc = "Reaching 200% of your automaticity target permanently moves the habit to Mastered Habits as a built-in behavior!"
                    )
                }

                // 7. Storage Rewards & My Books Sync
                HelpSectionCard(
                    title = "🎁 Storage Rewards & My Books Integration",
                    icon = Icons.Default.CloudQueue,
                    color = Color(0xFF8B5CF6)
                ) {
                    HelpBullet(
                        icon = Icons.Default.CardGiftcard,
                        title = "Unlock Up to 10 GB Storage",
                        desc = "Keeftalk rewards consistency! Start with 5 GB free storage and earn +0.5 to +2.0 GB bonus storage by reaching streak milestones (7d, 30d, 100d, 1 year)."
                    )
                    HelpBullet(
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        title = "My Books Sync",
                        desc = "Reading habits automatically check in when you read in My Books!"
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Got it", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun HelpSectionCard(
    title: String,
    icon: ImageVector,
    color: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(color.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            content()
        }
    }
}

@Composable
private fun HelpBullet(
    icon: ImageVector,
    title: String,
    desc: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.labelSmall.copy(lineHeight = 14.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HelpTypeBadgeRow(
    typeLabel: String,
    reps: String,
    examples: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(typeLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Text(examples, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(
                shape = RoundedCornerShape(99.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = reps,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
