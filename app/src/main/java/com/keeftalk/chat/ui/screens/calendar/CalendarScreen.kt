package com.keeftalk.chat.ui.screens.calendar

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.keeftalk.chat.domain.model.User
import com.keeftalk.chat.domain.model.calendar.*
import com.keeftalk.chat.ui.components.*
import com.keeftalk.chat.util.AvatarUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import java.text.SimpleDateFormat
import java.util.*

import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel,
    onBack: () -> Unit,
    onNavigateToEditor: (CalendarItemType) -> Unit,
    onSettingsClick: () -> Unit = {},
    fabActionFlow: SharedFlow<FabActionType>? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddSheet by remember { mutableStateOf(false) }
    var isSearching by remember { mutableStateOf(false) }
    var editingItemType by remember { mutableStateOf<CalendarItemType?>(null) }
    var showInviteModal by remember { mutableStateOf(false) }
    var manualRevealFilters by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableLongStateOf(0L) }
    val pullToRefreshState = rememberPullToRefreshState()

    LaunchedEffect(manualRevealFilters, lastInteractionTime) {
        if (manualRevealFilters && uiState.filterType == null) {
            delay(5.seconds)
            manualRevealFilters = false
        }
    }

    val showFiltersActual = remember(uiState.filterType, manualRevealFilters) {
        uiState.filterType != null || manualRevealFilters
    }

    LaunchedEffect(Unit) {
        fabActionFlow?.collect { action ->
            val type = when (action) {
                FabActionType.EVENT -> CalendarItemType.EVENT
                FabActionType.TASK -> CalendarItemType.TASK
                FabActionType.REMINDER -> CalendarItemType.REMINDER
                FabActionType.BIRTHDAY -> CalendarItemType.BIRTHDAY
                FabActionType.MEETING -> CalendarItemType.MEETING
                FabActionType.GOAL -> CalendarItemType.GOAL
                else -> null
            }
            type?.let { editingItemType = it }
        }
    }

    Scaffold(
        containerColor = CalendarDesign.Background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoading,
            onRefresh = {
                if (!showFiltersActual) {
                    manualRevealFilters = true
                    lastInteractionTime = System.currentTimeMillis()
                } else {
                    viewModel.syncCalendar()
                }
            },
            state = pullToRefreshState,
            modifier = Modifier.padding(innerPadding).fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = if (LocalConfiguration.current.screenWidthDp >= 700) 24.dp else 16.dp,
                        end = if (LocalConfiguration.current.screenWidthDp >= 700) 24.dp else 16.dp,
                        bottom = if (LocalConfiguration.current.screenWidthDp >= 700) 24.dp else 16.dp,
                        top = 0.dp
                    )
            ) {
                if (showFiltersActual) {
                    FeatureTabs(
                        selectedType = uiState.filterType,
                        isSharedSelected = uiState.filterShared,
                        items = uiState.items,
                        onSelect = { viewModel.setFilterType(it); viewModel.setFilterShared(false) },
                        onSelectShared = { viewModel.setFilterShared(true); viewModel.setFilterType(null) }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

            val configuration = LocalConfiguration.current
            val isTablet = configuration.screenWidthDp >= 1100

            if (isTablet) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        CalendarWidget(
                            selectedDate = uiState.selectedDate,
                            items = uiState.items,
                            onDateSelected = { viewModel.setSelectedDate(it) }
                        )
                    }

                    Box(modifier = Modifier.width(420.dp)) {
                        Sidebar(
                            uiState = uiState,
                            viewModel = viewModel,
                            onInviteClick = { showInviteModal = true }
                        )
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    CalendarWidget(
                        selectedDate = uiState.selectedDate,
                        items = uiState.items,
                        onDateSelected = { viewModel.setSelectedDate(it) }
                    )
                    Sidebar(
                        uiState = uiState,
                        viewModel = viewModel,
                        onInviteClick = { showInviteModal = true }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(80.dp)) // FAB space
            }
        }
    }

    if (showAddSheet) {
        AddCalendarItemSheet(
            onDismiss = { showAddSheet = false },
            onAdd = { type ->
                showAddSheet = false
                editingItemType = type
            }
        )
    }

    if (editingItemType != null) {
        CalendarItemEditorModal(
            type = editingItemType!!,
            initialDate = uiState.selectedDate,
            onDismiss = { editingItemType = null },
            onSave = { title, startTime, extra ->
                uiState.currentUserProfile?.let { profile ->
                    val newItem = CalendarItem(
                        id = UUID.randomUUID().toString(),
                        type = editingItemType!!,
                        title = title,
                        description = extra["description"] ?: "",
                        color = "#A78BFA",
                        icon = null,
                        location = extra["location"],
                        startTime = startTime,
                        endTime = startTime + 3600000, // Default 1 hour
                        isAllDay = false,
                        timezone = TimeZone.getDefault().id,
                        recurrenceRule = null,
                        priority = try { CalendarPriority.valueOf((extra["priority"] ?: "MEDIUM").uppercase()) } catch(e: Exception) { CalendarPriority.MEDIUM },
                        status = CalendarStatus.PENDING,
                        isPrivate = false,
                        ownerId = profile.id,
                        categoryId = null,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),
                        meetingType = if (editingItemType == CalendarItemType.MEETING) MeetingType.ONLINE else null,
                        meetingLink = null,
                        progress = 0,
                        pomodoroCount = 0,
                        deadline = null,
                        parentItemId = null
                    )
                    viewModel.saveItem(newItem)
                }
                editingItemType = null
            }
        )
    }

    if (uiState.showShareModal) {
        CalendarShareModal(viewModel)
    }

    if (showInviteModal) {
        CalendarInviteModal(
            viewModel = viewModel,
            onDismiss = { showInviteModal = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCalendarItemSheet(
    onDismiss: () -> Unit,
    onAdd: (CalendarItemType) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CalendarDesign.SurfaceGradientEnd
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
        ) {
            Text(
                "Create New", 
                style = MaterialTheme.typography.titleLarge, 
                fontWeight = FontWeight.Bold, 
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            CalendarItemType.entries.forEach { type ->
                ListItem(
                    headlineContent = { 
                        Text(
                            type.name.lowercase().replaceFirstChar { it.uppercase() }, 
                            color = Color.White
                        ) 
                    },
                    leadingContent = { 
                        Icon(
                            imageVector = when(type) {
                                CalendarItemType.EVENT -> Icons.Default.Event
                                CalendarItemType.TASK -> Icons.Default.TaskAlt
                                CalendarItemType.REMINDER -> Icons.Default.Notifications
                                CalendarItemType.BIRTHDAY -> Icons.Default.Cake
                                CalendarItemType.MEETING -> Icons.Default.VideoCall
                                CalendarItemType.GOAL -> Icons.Default.Flag
                            },
                            contentDescription = null,
                            tint = CalendarDesign.Purple
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onAdd(type) }
                )
            }
        }
    }
}

@Composable
private fun FeatureTabs(
    selectedType: CalendarItemType?,
    isSharedSelected: Boolean,
    items: List<CalendarItem>,
    onSelect: (CalendarItemType?) -> Unit,
    onSelectShared: () -> Unit
) {
    val tabs = listOf(
        null to "All",
        CalendarItemType.EVENT to "Events",
        CalendarItemType.TASK to "Tasks",
        CalendarItemType.REMINDER to "Reminders",
        CalendarItemType.BIRTHDAY to "Birthdays",
        CalendarItemType.MEETING to "Meetings",
        CalendarItemType.GOAL to "Goals"
    )

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(tabs) { (type, label) ->
            val isActive = selectedType == type && !isSharedSelected
            val count = if (type == null) items.size else items.count { it.type == type }
            
            val icon = when(type) {
                CalendarItemType.EVENT -> Icons.Default.Event
                CalendarItemType.TASK -> Icons.Default.TaskAlt
                CalendarItemType.REMINDER -> Icons.Default.Notifications
                CalendarItemType.BIRTHDAY -> Icons.Default.Cake
                CalendarItemType.MEETING -> Icons.Default.VideoCall
                CalendarItemType.GOAL -> Icons.Default.Flag
                else -> Icons.Default.CalendarToday
            }
            TabItem(label, icon, count, isActive) { onSelect(type); }
        }

        item {
            val sharedCount = items.count { it.attendees.isNotEmpty() }
            TabItem("Shared", Icons.Default.Share, sharedCount, isSharedSelected) { onSelectShared() }
        }
    }
}

@Composable
private fun TabItem(
    label: String,
    icon: ImageVector,
    count: Int,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isActive) CalendarDesign.TabActiveBg else Color.Transparent,
        shape = RoundedCornerShape(40.dp),
        border = if (isActive) BorderStroke(1.dp, Color(0xFFA78BFA).copy(alpha = 0.2f)) else null,
        modifier = if (isActive) Modifier.shadow(elevation = 1.dp, shape = RoundedCornerShape(40.dp), spotColor = Color(0xFFA78BFA).copy(alpha = 0.2f)) else Modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon, 
                contentDescription = null, 
                tint = if (isActive) CalendarDesign.TabActiveText else Color(0xFF8888AA),
                modifier = Modifier.size(14.dp)
            )
            if (LocalConfiguration.current.screenWidthDp >= 700 || isActive) {
                Text(
                    label, 
                    color = if (isActive) CalendarDesign.TabActiveText else Color(0xFF8888AA),
                    fontSize = 13.sp, 
                    fontWeight = FontWeight.Medium
                )
            }
            if (count > 0) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(40.dp))
                        .background(if (isActive) CalendarDesign.Purple.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        count.toString(), 
                        color = if (isActive) CalendarDesign.TabActiveText else Color(0xFFA0A0C0),
                        fontSize = 11.sp, 
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarWidget(
    selectedDate: Long,
    items: List<CalendarItem>,
    onDateSelected: (Long) -> Unit
) {
    val calendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
    val monthYear = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, CalendarDesign.BorderColor, RoundedCornerShape(28.dp))
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(monthYear, color = Color(0xFFE8E8FF), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CalendarNavButton(Icons.Default.ChevronLeft) {
                    calendar.add(Calendar.MONTH, -1)
                    onDateSelected(calendar.timeInMillis)
                }
                CalendarNavButton(Icons.Default.ChevronRight) {
                    calendar.add(Calendar.MONTH, 1)
                    onDateSelected(calendar.timeInMillis)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        Row(modifier = Modifier.fillMaxWidth()) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = Color(0xFF666688),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))

        val displayCal = calendar.clone() as Calendar
        displayCal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDay = (displayCal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Align to Mon=0
        val daysInMonth = displayCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (week in 0 until 6) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (day in 0 until 7) {
                        val dayIdx = week * 7 + day
                        val isCurrentMonth = dayIdx >= firstDay && dayIdx < firstDay + daysInMonth
                        val dayNum = if (isCurrentMonth) dayIdx - firstDay + 1 else 0
                        
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                            if (isCurrentMonth) {
                                val isSelected = dayNum == calendar.get(Calendar.DAY_OF_MONTH)
                                val isToday = Calendar.getInstance().let {
                                    it.get(Calendar.DAY_OF_MONTH) == dayNum &&
                                    it.get(Calendar.MONTH) == calendar.get(Calendar.MONTH) &&
                                    it.get(Calendar.YEAR) == calendar.get(Calendar.YEAR)
                                }
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .scale(if (isSelected) 1.02f else 1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .then(
                                            if (isSelected) Modifier
                                                .background(CalendarDesign.AccentGradient)
                                                .shadow(elevation = 24.dp, shape = RoundedCornerShape(16.dp), spotColor = Color(0xFF7C3AED).copy(alpha = 0.4f))
                                            else if (isToday) Modifier.background(CalendarDesign.DayTodayBg)
                                            else Modifier
                                        )
                                        .clickable { 
                                            calendar.set(Calendar.DAY_OF_MONTH, dayNum)
                                            onDateSelected(calendar.timeInMillis)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = dayNum.toString(),
                                            color = if (isSelected) Color.White else if (isToday) CalendarDesign.DayTodayText else Color(0xFFC8C8E0),
                                            fontSize = 15.sp,
                                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Medium
                                        )
                                        
                                        // Dots
                                        val dayItems = items.filter { 
                                            val c = Calendar.getInstance().apply { timeInMillis = it.startTime ?: 0 }
                                            c.get(Calendar.DAY_OF_MONTH) == dayNum && 
                                            c.get(Calendar.MONTH) == calendar.get(Calendar.MONTH)
                                        }
                                        if (dayItems.isNotEmpty()) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                modifier = Modifier.padding(top = 2.dp)
                                            ) {
                                                dayItems.take(3).forEach { item ->
                                                    Box(
                                                        modifier = Modifier
                                                            .size(5.dp)
                                                            .clip(CircleShape)
                                                            .background(getTypeColor(item.type))
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if ((week + 1) * 7 >= firstDay + daysInMonth) break
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        CalendarLegend()
    }
}

@Composable
private fun Sidebar(
    uiState: CalendarUiState,
    viewModel: CalendarViewModel,
    onInviteClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Upcoming
        val filteredItems = remember(uiState.items, uiState.filterType, uiState.filterShared) {
            uiState.items.filter { item ->
                (uiState.filterType == null || item.type == uiState.filterType) &&
                (!uiState.filterShared || item.attendees.isNotEmpty())
            }
        }

        SidebarCard(
            title = if (uiState.filterShared) "Shared with others" else "Upcoming",
            icon = Icons.AutoMirrored.Filled.List,
            action = "${filteredItems.size} items"
        ) {
            val now = Calendar.getInstance().apply { 
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val tomorrow = now + 24 * 3600 * 1000
            val dayAfterTomorrow = tomorrow + 24 * 3600 * 1000

            val grouped = filteredItems.sortedBy { it.startTime ?: 0 }.groupBy { item ->
                val time = item.startTime ?: 0
                when {
                    time < now -> "Past"
                    time < tomorrow -> "Today"
                    time < dayAfterTomorrow -> "Tomorrow"
                    else -> {
                        val cal = Calendar.getInstance().apply { timeInMillis = time }
                        SimpleDateFormat("EEE MMM d", Locale.getDefault()).format(cal.time)
                    }
                }
            }

            LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                val headerOrder = listOf("Today", "Tomorrow") + 
                    grouped.keys.filter { it !in listOf("Today", "Tomorrow", "Past") }.sorted() + 
                    listOf("Past")
                
                headerOrder.forEach { key ->
                    grouped[key]?.let { groupItems ->
                        item(key = "header_$key") { DateHeader(key) }
                        items(groupItems, key = { "${it.id}_$key" }) { item ->
                            UpcomingItem(item, isPast = key == "Past")
                        }
                    }
                }
            }
        }

        // Overview
        SidebarCard(title = "Overview", icon = Icons.Default.BarChart) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatItem(uiState.items.count { it.type == CalendarItemType.EVENT }.toString(), "Events", CalendarDesign.Purple, Modifier.weight(1f))
                    StatItem(uiState.items.count { it.type == CalendarItemType.TASK }.toString(), "Tasks", CalendarDesign.Green, Modifier.weight(1f))
                    StatItem(uiState.items.count { it.type == CalendarItemType.REMINDER }.toString(), "Reminders", CalendarDesign.Yellow, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatItem(uiState.items.count { it.type == CalendarItemType.BIRTHDAY }.toString(), "Birthdays", CalendarDesign.Pink, Modifier.weight(1f))
                    StatItem(uiState.items.count { it.type == CalendarItemType.MEETING }.toString(), "Meetings", CalendarDesign.Blue, Modifier.weight(1f))
                    StatItem(uiState.items.count { it.type == CalendarItemType.GOAL }.toString(), "Goals", CalendarDesign.Orange, Modifier.weight(1f))
                }
            }
        }

        // Family
        SidebarCard(
            title = "Family & Shared", 
            icon = Icons.Default.Group, 
            action = "Invite",
            onActionClick = onInviteClick
        ) {
            uiState.familyMembers.forEach { member ->
                FamilyMemberItem(
                    member = member,
                    onToggleInclude = { viewModel.toggleFamilyMember(member.id, it) },
                    onTogglePermission = { type, allowed -> 
                        viewModel.toggleFamilyPermission(member.id, type, allowed) 
                    }
                )
            }
        }

        // Analytics
        SidebarCard(title = "Analytics", icon = Icons.Default.PieChart) {
            AnalyticsView(uiState.items, uiState.analyticsPeriod) { viewModel.setAnalyticsPeriod(it) }
        }
    }
}

@Composable
private fun SidebarCard(
    title: String,
    icon: ImageVector,
    action: String = "",
    onActionClick: () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = CalendarDesign.Purple, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, color = Color(0xFFD0D0F0), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            if (action.isNotEmpty()) {
                Text(
                    action, 
                    color = Color(0xFF666688),
                    fontSize = 13.sp, 
                    modifier = Modifier.clickable { onActionClick() }
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        content()
    }
}

@Composable
private fun DateHeader(text: String) {
    Text(
        text = text,
        color = Color(0xFF666688),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun UpcomingItem(item: CalendarItem, isPast: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .alpha(if (isPast) 0.5f else 1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(getTypeColor(item.type)))
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, color = Color(0xFFE8E8FF), fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(item.startTime ?: 0)),
                color = Color(0xFF7777A0),
                fontSize = 11.sp
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(40.dp))
                .background(getTypeColor(item.type).copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(item.type.name.uppercase(), color = getTypeColor(item.type), fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StatItem(number: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(number, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(label.uppercase(), color = Color(0xFF666688), fontSize = 8.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun FamilyMemberItem(
    member: FamilyMember,
    onToggleInclude: (Boolean) -> Unit,
    onTogglePermission: (CalendarItemType, PermissionLevel) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(16.dp))
            .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KeeftalkAvatar(
                avatarUrl = member.member?.avatarUrl, 
                initials = AvatarUtils.getInitials(member.member?.name), 
                seed = member.member?.id,
                size = 36.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(member.member?.name ?: "Unknown", color = Color(0xFFE8E8FF), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(if (member.included) "👁️ Included" else "⛔ Excluded", color = Color(0xFF666688), fontSize = 11.sp)
            }
            Switch(
                checked = member.included,
                onCheckedChange = onToggleInclude,
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = CalendarDesign.Purple)
            )
        }
        
        if (member.included) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CalendarItemType.entries.forEach { type ->
                    val permission = member.permissions.find { it.type == type }
                    val level = permission?.level ?: PermissionLevel.VIEW
                    val isActive = level != PermissionLevel.NONE
                    PermToggle(
                        label = type.name.lowercase().replaceFirstChar { it.uppercase() },
                        color = getTypeColor(type),
                        isActive = isActive,
                        onClick = { 
                            val nextLevel = if (isActive) PermissionLevel.NONE else PermissionLevel.VIEW
                            onTogglePermission(type, nextLevel) 
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PermToggle(
    label: String,
    color: Color,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(40.dp))
            .background(if (isActive) CalendarDesign.Purple.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f))
            .border(1.dp, if (isActive) CalendarDesign.Purple.copy(alpha = 0.2f) else Color.Transparent, RoundedCornerShape(40.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
            Text(label, color = if (isActive) CalendarDesign.TabActiveText else Color(0xFF8888AA), fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun AnalyticsView(
    items: List<CalendarItem>,
    period: AnalyticsPeriod,
    onPeriodChange: (AnalyticsPeriod) -> Unit
) {
    val filteredItems = remember(items, period) {
        val start = Calendar.getInstance()
        when (period) {
            AnalyticsPeriod.WEEK -> start.add(Calendar.DAY_OF_YEAR, -7)
            AnalyticsPeriod.MONTH -> start.add(Calendar.DAY_OF_YEAR, -30)
            AnalyticsPeriod.YEAR -> start.add(Calendar.YEAR, -1)
        }
        items.filter { (it.startTime ?: 0) >= start.timeInMillis }
    }

    var selectedBarIndex by remember { mutableStateOf<Int?>(null) }

    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AnalyticsTab("Week", period == AnalyticsPeriod.WEEK) { onPeriodChange(AnalyticsPeriod.WEEK) }
            AnalyticsTab("Month", period == AnalyticsPeriod.MONTH) { onPeriodChange(AnalyticsPeriod.MONTH) }
            AnalyticsTab("Year", period == AnalyticsPeriod.YEAR) { onPeriodChange(AnalyticsPeriod.YEAR) }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Pie Chart Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.02f))
                    .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                val counts = filteredItems.groupBy { it.type }.mapValues { it.value.size }
                val total = counts.values.sum().toFloat()

                Canvas(modifier = Modifier.fillMaxSize()) {
                    var startAngle = -90f
                    if (total > 0) {
                        CalendarItemType.entries.forEach { type ->
                            val sweepAngle = (counts[type] ?: 0) / total * 360f
                            if (sweepAngle > 0) {
                                drawArc(
                                    color = getTypeColor(type),
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 20f)
                                )
                                startAngle += sweepAngle
                            }
                        }
                    } else {
                        drawCircle(color = Color(0xFF444466), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 20f))
                    }
                }
                
                Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (total > 0) "${total.toInt()}" else "0",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text("TOTAL", style = MaterialTheme.typography.labelSmall, color = Color(0xFF666688))
                }
                
                Text("By Type", modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp), color = Color(0xFF666688), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            // Bar Chart Box
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.02f))
                    .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                val dataPoints = remember(filteredItems, period) {
                    val cal = Calendar.getInstance()
                    when (period) {
                        AnalyticsPeriod.WEEK -> {
                            (0 until 7).map { offset ->
                                val d = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -offset) }
                                filteredItems.count {
                                    val itemCal = Calendar.getInstance().apply { timeInMillis = it.startTime ?: 0 }
                                    itemCal.get(Calendar.YEAR) == d.get(Calendar.YEAR) &&
                                    itemCal.get(Calendar.DAY_OF_YEAR) == d.get(Calendar.DAY_OF_YEAR)
                                }
                            }.reversed()
                        }
                        AnalyticsPeriod.MONTH -> {
                            (0 until 30).map { offset ->
                                val d = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -offset) }
                                filteredItems.count {
                                    val itemCal = Calendar.getInstance().apply { timeInMillis = it.startTime ?: 0 }
                                    itemCal.get(Calendar.YEAR) == d.get(Calendar.YEAR) &&
                                    itemCal.get(Calendar.DAY_OF_YEAR) == d.get(Calendar.DAY_OF_YEAR)
                                }
                            }.reversed()
                        }
                        AnalyticsPeriod.YEAR -> {
                            (0 until 12).map { offset ->
                                val d = (cal.clone() as Calendar).apply { add(Calendar.MONTH, -offset) }
                                filteredItems.count {
                                    val itemCal = Calendar.getInstance().apply { timeInMillis = it.startTime ?: 0 }
                                    itemCal.get(Calendar.YEAR) == d.get(Calendar.YEAR) &&
                                    itemCal.get(Calendar.MONTH) == d.get(Calendar.MONTH)
                                }
                            }.reversed()
                        }
                    }
                }
                
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val width = constraints.maxWidth.toFloat()
                    val barWidth = width / (dataPoints.size * 1.5f)
                    val spacing = (width - (barWidth * dataPoints.size)) / (dataPoints.size + 1)

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(dataPoints) {
                                detectTapGestures { offset ->
                                    val index = ((offset.x - spacing / 2) / (barWidth + spacing)).toInt().coerceIn(0, dataPoints.size - 1)
                                    selectedBarIndex = if (selectedBarIndex == index) null else index
                                }
                            }
                    ) {
                        val maxVal = dataPoints.maxOrNull()?.coerceAtLeast(1) ?: 1
                        
                        dataPoints.forEachIndexed { i, count ->
                            val barHeight = (size.height * (count.toFloat() / maxVal.toFloat())).coerceAtLeast(2f)
                            val isSelected = selectedBarIndex == i
                            drawRect(
                                color = if (isSelected) Color.White else CalendarDesign.Purple,
                                topLeft = androidx.compose.ui.geometry.Offset(spacing + i * (barWidth + spacing), size.height - barHeight),
                                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                                alpha = if (isSelected) 1f else 0.8f
                            )
                        }
                    }
                }
                
                selectedBarIndex?.let { index ->
                    if (index < dataPoints.size) {
                        Text(
                            "${dataPoints[index]} items",
                            modifier = Modifier.align(Alignment.TopCenter).background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp),
                            color = Color.White,
                            fontSize = 9.sp
                        )
                    }
                }

                Text(
                    text = when(period) {
                        AnalyticsPeriod.WEEK -> "By Day (Week)"
                        AnalyticsPeriod.MONTH -> "By Day (Month)"
                        AnalyticsPeriod.YEAR -> "By Month"
                    },
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp), 
                    color = Color(0xFF666688),
                    fontSize = 10.sp, 
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AnalyticsTab(text: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(40.dp))
            .background(if (active) CalendarDesign.Purple.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.05f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(text, color = if (active) CalendarDesign.TabActiveText else Color(0xFF8888AA), fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CalendarNavButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFFA0A0C8), modifier = Modifier.size(16.dp))
    }
}

private fun getTypeColor(type: CalendarItemType): Color = when(type) {
    CalendarItemType.EVENT -> CalendarDesign.Purple
    CalendarItemType.TASK -> CalendarDesign.Green
    CalendarItemType.REMINDER -> CalendarDesign.Yellow
    CalendarItemType.BIRTHDAY -> CalendarDesign.Pink
    CalendarItemType.MEETING -> CalendarDesign.Blue
    CalendarItemType.GOAL -> CalendarDesign.Orange
}

@Composable
private fun CalendarLegend() {
    val items = listOf(
        CalendarItemType.EVENT to "Event",
        CalendarItemType.TASK to "Task",
        CalendarItemType.REMINDER to "Reminder",
        CalendarItemType.BIRTHDAY to "Birthday",
        CalendarItemType.MEETING to "Meeting",
        CalendarItemType.GOAL to "Goal"
    )

    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.forEach { (type, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(getTypeColor(type))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    color = Color(0xFF8888AA),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarItemEditorModal(
    type: CalendarItemType,
    initialDate: Long,
    onDismiss: () -> Unit,
    onSave: (String, Long, Map<String, String>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    val calendarState = remember(initialDate) { 
        mutableStateOf(Calendar.getInstance().apply { timeInMillis = initialDate }) 
    }
    
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Medium") }
    var location by remember { mutableStateOf("") }
    var attendees by remember { mutableStateOf("") }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = calendarState.value.timeInMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = it }
                        val newCal = (calendarState.value.clone() as Calendar).apply {
                            set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
                            set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
                            set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
                        }
                        calendarState.value = newCal
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = calendarState.value.get(Calendar.HOUR_OF_DAY),
            initialMinute = calendarState.value.get(Calendar.MINUTE)
        )
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color(0xFF1E1E2A)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    TimePicker(state = timePickerState)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
                        TextButton(onClick = {
                            val newCal = (calendarState.value.clone() as Calendar).apply {
                                set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                set(Calendar.MINUTE, timePickerState.minute)
                            }
                            calendarState.value = newCal
                            showTimePicker = false
                        }) { Text("OK") }
                    }
                }
            }
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(32.dp),
            color = Color(0xFF1E1E2A)
        ) {
            Column(modifier = Modifier.padding(28.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val icon = when(type) {
                            CalendarItemType.TASK -> Icons.Default.CheckCircle
                            CalendarItemType.REMINDER -> Icons.Default.Notifications
                            CalendarItemType.BIRTHDAY -> Icons.Default.Cake
                            CalendarItemType.MEETING -> Icons.Default.Group
                            CalendarItemType.GOAL -> Icons.Default.Flag
                            else -> Icons.Default.Event
                        }
                        Icon(icon, null, tint = getTypeColor(type), modifier = Modifier.size(24.dp))
                        Text("Add ${type.name.lowercase().replaceFirstChar { it.uppercase() }}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = Color(0xFF8888B0))
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    EditorField(label = "Title", value = title, onValueChange = { title = it }, placeholder = "Enter title...")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Box(modifier = Modifier.weight(1f).clickable { showDatePicker = true }) {
                            EditorField(
                                label = "Date", 
                                value = dateFormatter.format(calendarState.value.time), 
                                onValueChange = {}, 
                                enabled = false
                            )
                        }
                        Box(modifier = Modifier.weight(1f).clickable { showTimePicker = true }) {
                            EditorField(
                                label = "Time", 
                                value = timeFormatter.format(calendarState.value.time), 
                                onValueChange = {}, 
                                enabled = false
                            )
                        }
                    }
                    
                    when(type) {
                        CalendarItemType.TASK -> {
                            EditorField(label = "Priority", value = priority, onValueChange = { priority = it }, placeholder = "Medium")
                            EditorField(label = "Description", value = description, onValueChange = { description = it }, isTextArea = true)
                        }
                        CalendarItemType.MEETING -> {
                            EditorField(label = "Location", value = location, onValueChange = { location = it })
                            EditorField(label = "Attendees", value = attendees, onValueChange = { attendees = it })
                        }
                        else -> {
                            EditorField(label = "Description", value = description, onValueChange = { description = it }, isTextArea = true)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFA0A0C0))) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            val extra = mutableMapOf(
                                "description" to description,
                                "priority" to priority,
                                "location" to location,
                                "attendees" to attendees
                            )
                            onSave(title, calendarState.value.timeInMillis, extra)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 10.dp),
                        modifier = Modifier.clip(CircleShape).background(CalendarDesign.AccentGradient)
                    ) {
                        Icon(Icons.Default.Check, null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarInviteModal(
    viewModel: CalendarViewModel,
    onDismiss: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    val suggestions by viewModel.shareSuggestions.collectAsState()
    var selectedUser by remember { mutableStateOf<User?>(null) }
    var selectedTypes by remember { mutableStateOf(CalendarItemType.entries.toSet()) }
    var sharingKind by remember { mutableStateOf("Family") } // Family, Friend, Work, Other

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1E1E2A)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                when (step) {
                    0 -> { // Search
                        Text("Invite to Share", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedTextField(
                            value = query,
                            onValueChange = { 
                                query = it
                                viewModel.setShareSearchQuery(it)
                            },
                            placeholder = { Text("Username or email", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = CalendarDesign.Purple
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        LazyColumn(
                            modifier = Modifier.heightIn(max = 240.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(suggestions) { user ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            selectedUser = user
                                            step = 1
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.05f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        KeeftalkAvatar(
                                            avatarUrl = user.avatarUrl, 
                                            initials = user.initials, 
                                            seed = user.id,
                                            size = 40.dp
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(user.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                            Text("@${user.username}", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                                        }
                                        Icon(
                                            Icons.Default.ChevronRight,
                                            contentDescription = null,
                                            tint = Color.Gray.copy(alpha = 0.5f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (query.isNotEmpty() && suggestions.isEmpty()) {
                            Button(
                                onClick = { 
                                    // Manual entry case
                                    selectedUser = User(id = query, name = query, username = query, avatarUrl = null, isActive = false, lastSeen = 0, isContact = false)
                                    step = 1
                                },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CalendarDesign.Purple)
                            ) {
                                Text("Invite '$query'")
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                            Text("Cancel", color = Color.Gray)
                        }
                    }
                    1 -> { // Details
                        Text("Sharing Details", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text("Sharing with: ${selectedUser?.name}", color = Color.White, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Relationship:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Family", "Friend", "Work").forEach { kind ->
                                FilterChip(
                                    selected = sharingKind == kind,
                                    onClick = { sharingKind = kind },
                                    label = { Text(kind) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("What to share:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CalendarItemType.entries.forEach { type ->
                                val isSelected = type in selectedTypes
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { 
                                        selectedTypes = if (isSelected) selectedTypes - type else selectedTypes + type
                                    },
                                    label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = { step = 0 }, modifier = Modifier.weight(1f)) {
                                Text("Back")
                            }
                            Button(
                                onClick = { 
                                    selectedUser?.let { viewModel.sendInvitation(it.username, sharingKind, selectedTypes.toList()) }
                                    onDismiss()
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = CalendarDesign.Purple)
                            ) {
                                Text("Send Invite")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    isTextArea: Boolean = false,
    enabled: Boolean = true
) {
    Column(modifier = modifier) {
        Text(label, color = Color(0xFFA0A0C0), fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = Color(0xFF666688), fontSize = 14.sp) },
            modifier = Modifier.fillMaxWidth().then(if (isTextArea) Modifier.heightIn(min = 56.dp) else Modifier),
            shape = RoundedCornerShape(14.dp),
            enabled = enabled,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                disabledTextColor = Color.White,
                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                disabledContainerColor = Color.White.copy(alpha = 0.05f),
                focusedBorderColor = CalendarDesign.Purple,
                unfocusedBorderColor = Color.White.copy(alpha = 0.08f),
                disabledBorderColor = Color.White.copy(alpha = 0.08f)
            )
        )
    }
}

@Composable
fun CalendarShareModal(viewModel: CalendarViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val item = uiState.selectedItemForSharing
    val shareSearchQuery by viewModel.shareSearchQuery.collectAsState()
    val shareSuggestions by viewModel.shareSuggestions.collectAsState()
    
    Dialog(onDismissRequest = { viewModel.setShowShareModal(false) }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(32.dp),
            color = Color(0xFF1E1E2A)
        ) {
            Column(modifier = Modifier.padding(32.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (item != null) "Share Event" else "Share Calendar", 
                        style = MaterialTheme.typography.titleLarge, 
                        fontWeight = FontWeight.Bold, 
                        color = Color.White
                    )
                    IconButton(onClick = { viewModel.setShowShareModal(false) }) {
                        Icon(Icons.Default.Close, null, tint = Color(0xFF8888B0))
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                OutlinedTextField(
                    value = shareSearchQuery,
                    onValueChange = { viewModel.setShareSearchQuery(it) },
                    placeholder = { Text("Search family or contacts...", fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.White.copy(alpha = 0.05f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(shareSuggestions) { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            KeeftalkAvatar(
                                avatarUrl = contact.avatarUrl, 
                                initials = contact.initials, 
                                seed = contact.id,
                                size = 40.dp
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(contact.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White)
                                Text("@${contact.username}", fontSize = 12.sp, color = Color(0xFF8888AA))
                            }
                            IconButton(onClick = { 
                                if (item != null) viewModel.addAttendee(item.id, contact.id, AttendeeRole.EDITOR)
                            }) {
                                Icon(Icons.Default.Add, null, tint = CalendarDesign.Purple)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { viewModel.setShowShareModal(false) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CalendarDesign.Purple),
                    shape = RoundedCornerShape(40.dp)
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
