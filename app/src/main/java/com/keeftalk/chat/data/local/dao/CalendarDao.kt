package com.keeftalk.chat.data.local.dao

import androidx.room.*
import com.keeftalk.chat.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CalendarDao {
    @Transaction
    @Query("SELECT * FROM calendar_items ORDER BY startTime ASC")
    fun getAllItems(): Flow<List<CalendarItemWithDetails>>

    @Transaction
    @Query("SELECT * FROM calendar_items WHERE id = :id")
    suspend fun getItemById(id: String): CalendarItemWithDetails?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: CalendarItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendees(attendees: List<CalendarItemAttendeeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<CalendarReminderEntity>)

    @Delete
    suspend fun deleteItem(item: CalendarItemEntity)

    @Transaction
    suspend fun upsertCalendarItem(
        item: CalendarItemEntity,
        attendees: List<CalendarItemAttendeeEntity>,
        reminders: List<CalendarReminderEntity>
    ) {
        insertItem(item)
        deleteAttendeesForItem(item.id)
        insertAttendees(attendees)
        deleteRemindersForItem(item.id)
        insertReminders(reminders)
    }

    @Query("DELETE FROM calendar_item_attendees WHERE itemId = :itemId")
    suspend fun deleteAttendeesForItem(itemId: String)

    @Query("DELETE FROM calendar_reminders WHERE itemId = :itemId")
    suspend fun deleteRemindersForItem(itemId: String)

    @Transaction
    @Query("SELECT * FROM habits")
    fun getAllHabits(): Flow<List<HabitWithLogs>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabitLog(log: HabitLogEntity)

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId")
    fun getLogsForHabit(habitId: String): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM calendar_categories")
    fun getAllCategories(): Flow<List<CalendarCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CalendarCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendee(attendee: CalendarItemAttendeeEntity)

    @Query("DELETE FROM calendar_item_attendees WHERE id = :attendeeId")
    suspend fun deleteAttendee(attendeeId: String)

    @Query("UPDATE calendar_item_attendees SET role = :role WHERE id = :attendeeId")
    suspend fun updateAttendeeRole(attendeeId: String, role: String)

    @Transaction
    @Query("SELECT * FROM calendar_item_attendees WHERE itemId = :itemId")
    suspend fun getAttendeesForItem(itemId: String): List<CalendarAttendeeWithUser>

    @Transaction
    @Query("SELECT * FROM family_members")
    fun getAllFamilyMembers(): Flow<List<FamilyMemberWithDetails>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFamilyMember(member: FamilyMemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFamilyPermissions(permissions: List<FamilyPermissionEntity>)

    @Query("DELETE FROM family_permissions WHERE familyMemberId = :familyMemberId")
    suspend fun deleteFamilyPermissions(familyMemberId: String)

    @Transaction
    suspend fun upsertFamilyMember(
        member: FamilyMemberEntity,
        permissions: List<FamilyPermissionEntity>
    ) {
        insertFamilyMember(member)
        deleteFamilyPermissions(member.id)
        insertFamilyPermissions(permissions)
    }

    @Query("UPDATE family_members SET included = :included WHERE id = :id")
    suspend fun updateFamilyMemberIncluded(id: String, included: Boolean)

    @Query("UPDATE family_permissions SET level = :level WHERE id = :id")
    suspend fun updateFamilyPermission(id: String, level: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvitation(invitation: CalendarInvitationEntity)

    @Query("SELECT * FROM calendar_invitations WHERE toUserId = :userId AND status = 'PENDING'")
    fun getPendingInvitations(userId: String): Flow<List<CalendarInvitationEntity>>

    @Query("UPDATE calendar_invitations SET status = :status WHERE id = :id")
    suspend fun updateInvitationStatus(id: String, status: String)

    @Query("DELETE FROM calendar_invitations WHERE id = :id")
    suspend fun deleteInvitation(id: String)

    @Transaction
    @Query("SELECT * FROM calendar_items WHERE pendingSync = 1")
    suspend fun getPendingItems(): List<CalendarItemWithDetails>

    @Transaction
    @Query("SELECT * FROM habits WHERE pendingSync = 1")
    suspend fun getPendingHabits(): List<HabitWithLogs>

    @Query("SELECT * FROM habit_logs WHERE pendingSync = 1")
    suspend fun getPendingHabitLogs(): List<HabitLogEntity>

    @Query("UPDATE calendar_items SET pendingSync = :pending WHERE id = :id")
    suspend fun updateItemPendingStatus(id: String, pending: Boolean)

    @Query("UPDATE habits SET pendingSync = :pending WHERE id = :id")
    suspend fun updateHabitPendingStatus(id: String, pending: Boolean)

    @Query("UPDATE habit_logs SET pendingSync = :pending WHERE id = :id")
    suspend fun updateHabitLogPendingStatus(id: String, pending: Boolean)
}
