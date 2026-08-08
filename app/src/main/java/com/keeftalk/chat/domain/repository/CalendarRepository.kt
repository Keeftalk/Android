package com.keeftalk.chat.domain.repository

import com.keeftalk.chat.domain.model.calendar.*
import kotlinx.coroutines.flow.Flow

interface CalendarRepository {
    fun getAllItems(): Flow<List<CalendarItem>>
    suspend fun getItemById(id: String): CalendarItem?
    suspend fun saveItem(item: CalendarItem)
    suspend fun deleteItem(item: CalendarItem)

    fun getAllHabits(): Flow<List<Habit>>
    suspend fun saveHabit(habit: Habit)
    suspend fun saveHabitLog(log: HabitLog)
    fun getLogsForHabit(habitId: String): Flow<List<HabitLog>>

    fun getAllCategories(): Flow<List<CalendarCategory>>
    suspend fun saveCategory(category: CalendarCategory)

    suspend fun addAttendee(itemId: String, userId: String, role: AttendeeRole)
    suspend fun removeAttendee(itemId: String, attendeeId: String)
    suspend fun updateAttendeeRole(attendeeId: String, role: AttendeeRole)
    suspend fun getAttendeesForItem(itemId: String): List<CalendarAttendee>

    fun getAllFamilyMembers(): Flow<List<FamilyMember>>
    suspend fun saveFamilyMember(member: FamilyMember)
    suspend fun updateFamilyMemberIncluded(id: String, included: Boolean)
    suspend fun updateFamilyPermission(memberId: String, type: CalendarItemType, level: PermissionLevel)

    suspend fun sendInvitation(usernameOrEmail: String, kind: String, initialPermissions: List<CalendarItemType>): Result<Unit>
    fun getPendingInvitations(): Flow<List<CalendarInvitation>>
    suspend fun respondToInvitation(invitationId: String, accept: Boolean)

    suspend fun syncCalendar()
    fun observeCalendarRealtime(): Flow<Unit>
    fun shutdown()
}
