package com.keeftalk.chat.data.repository

import android.util.Log
import com.keeftalk.chat.data.local.dao.CalendarDao
import com.keeftalk.chat.data.local.dao.CalendarSyncQueueDao
import com.keeftalk.chat.data.local.entities.*
import com.keeftalk.chat.data.remote.*
import com.keeftalk.chat.domain.model.calendar.*
import com.keeftalk.chat.domain.repository.CalendarRepository
import com.keeftalk.chat.di.AppModule
import com.keeftalk.chat.security.crypto.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import java.text.SimpleDateFormat
import java.util.*

class CalendarRepositoryImpl(
    private val context: android.content.Context,
    private val calendarDao: CalendarDao,
    private val calendarSyncQueueDao: CalendarSyncQueueDao,
    private val fileDao: com.keeftalk.chat.data.local.dao.FileDao,
    private val fileUploadManager: com.keeftalk.chat.util.FileUploadManager
) : CalendarRepository {

    private val cryptoManager get() = AppModule.provideCryptoManager(context)

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isoFormatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private suspend fun getSupabase(): SupabaseClient {
        return AppModule.provideSupabaseClientAsync(context)
    }

    override fun getAllItems(): Flow<List<CalendarItem>> = 
        calendarDao.getAllItems().map { entities -> entities.map { it.toDomainInternal() } }

    override suspend fun getItemById(id: String): CalendarItem? =
        calendarDao.getItemById(id)?.toDomainInternal()

    override suspend fun saveItem(item: CalendarItem) {
        // --- E2E ENCRYPTION ---
        val sensitiveData = buildJsonObject {
            put("title", item.title)
            put("description", item.description)
        }

        val encrypted = cryptoManager.encryptForStorage(
            sensitiveData.toString().toByteArray(Charsets.UTF_8),
            com.keeftalk.chat.security.crypto.CryptoManager.StoragePurpose.AGENDA
        )
        
        val encryptedEntity = item.toEntity().copy(
            pendingSync = true,
            encryptedDescription = encrypted.ciphertext,
            iv = encrypted.iv,
            cryptoVersion = encrypted.version,
            title = "[Encrypted]",
            description = ""
        )

        calendarDao.upsertCalendarItem(
            encryptedEntity,
            item.attendees.map { it.toEntity() },
            item.reminders.map { it.toEntity() }
        )

        // Link attachments
        item.attachments.forEach { file ->
            fileDao.insertAgendaAttachment(AgendaAttachmentEntity(
                id = UUID.randomUUID().toString(),
                agendaEventId = item.id,
                fileId = file.id
            ))
        }
        
        val encryptedDto = item.toDto().copy(
            title = "[Encrypted]",
            description = "",
            ciphertext = encrypted.ciphertext,
            iv = encrypted.iv,
            crypto_version = encrypted.version
        )
        
        val payload = Json.encodeToString(encryptedDto)
        calendarSyncQueueDao.insert(CalendarSyncQueueEntity(itemId = item.id, operation = "SAVE_CALENDAR_ITEM", payload = payload))
        scope.launch { processSyncQueue() }
    }

    override suspend fun deleteItem(item: CalendarItem) {
        val attachments = fileDao.getFilesForAgendaEvent(item.id)
        calendarDao.deleteItem(item.toEntity())
        calendarSyncQueueDao.insert(CalendarSyncQueueEntity(itemId = item.id, operation = "DELETE_CALENDAR_ITEM", payload = null))
        
        val referenceManager = AppModule.provideFileReferenceManager(context)
        attachments.forEach { file ->
            referenceManager.removeReference(file.id)
        }
        
        scope.launch { processSyncQueue() }
    }

    override fun getAllHabits(): Flow<List<Habit>> =
        calendarDao.getAllHabits().map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveHabit(habit: Habit) {
        calendarDao.insertHabit(habit.toEntity().copy(pendingSync = true))
        val payload = Json.encodeToString(habit.toDto())
        calendarSyncQueueDao.insert(CalendarSyncQueueEntity(itemId = habit.id, operation = "SAVE_HABIT", payload = payload))
        scope.launch { processSyncQueue() }
    }

    override suspend fun saveHabitLog(log: HabitLog) {
        calendarDao.insertHabitLog(log.toEntity().copy(pendingSync = true))
        val payload = Json.encodeToString(log.toDto())
        calendarSyncQueueDao.insert(CalendarSyncQueueEntity(itemId = log.id, operation = "SAVE_HABIT_LOG", payload = payload))
        scope.launch { processSyncQueue() }
    }

    override fun getLogsForHabit(habitId: String): Flow<List<HabitLog>> =
        calendarDao.getLogsForHabit(habitId).map { entities -> entities.map { it.toDomain() } }

    override fun getAllCategories(): Flow<List<CalendarCategory>> =
        calendarDao.getAllCategories().map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveCategory(category: CalendarCategory) {
        calendarDao.insertCategory(category.toEntity())
        val payload = Json.encodeToString(category.toDto())
        calendarSyncQueueDao.insert(CalendarSyncQueueEntity(itemId = category.id, operation = "SAVE_CATEGORY", payload = payload))
        scope.launch { processSyncQueue() }
    }

    override suspend fun addAttendee(itemId: String, userId: String, role: AttendeeRole) {
        val attendee = CalendarAttendee(
            id = UUID.randomUUID().toString(),
            itemId = itemId,
            userId = userId,
            role = role,
            status = AttendeeStatus.PENDING
        )
        calendarDao.insertAttendee(attendee.toEntity())
        val payload = Json.encodeToString(attendee.toDto())
        calendarSyncQueueDao.insert(CalendarSyncQueueEntity(itemId = attendee.id, operation = "ADD_CALENDAR_ATTENDEE", payload = payload))
        scope.launch { processSyncQueue() }
    }

    override suspend fun removeAttendee(itemId: String, attendeeId: String) {
        calendarDao.deleteAttendee(attendeeId)
        calendarSyncQueueDao.insert(CalendarSyncQueueEntity(itemId = attendeeId, operation = "REMOVE_CALENDAR_ATTENDEE", payload = itemId))
        scope.launch { processSyncQueue() }
    }

    override suspend fun updateAttendeeRole(attendeeId: String, role: AttendeeRole) {
        calendarDao.updateAttendeeRole(attendeeId, role.name)
        val payload = Json.encodeToString(mapOf("role" to role.name))
        calendarSyncQueueDao.insert(CalendarSyncQueueEntity(itemId = attendeeId, operation = "UPDATE_CALENDAR_ATTENDEE_ROLE", payload = payload))
        scope.launch { processSyncQueue() }
    }

    override suspend fun getAttendeesForItem(itemId: String): List<CalendarAttendee> {
        return calendarDao.getAttendeesForItem(itemId).map { it.toDomain() }
    }

    override fun getAllFamilyMembers(): Flow<List<FamilyMember>> {
        return calendarDao.getAllFamilyMembers().map { members ->
            members.map { it.toDomain() }
        }
    }

    override suspend fun saveFamilyMember(member: FamilyMember) {
        calendarDao.upsertFamilyMember(
            member.toEntity(),
            member.permissions.map { it.toEntity() }
        )
    }

    override suspend fun updateFamilyMemberIncluded(id: String, included: Boolean) {
        calendarDao.updateFamilyMemberIncluded(id, included)
        calendarSyncQueueDao.insert(CalendarSyncQueueEntity(itemId = id, operation = "UPDATE_FAMILY_MEMBER_INCLUDED", payload = included.toString()))
        scope.launch { processSyncQueue() }
    }

    override suspend fun updateFamilyPermission(memberId: String, type: CalendarItemType, level: PermissionLevel) {
        val member = calendarDao.getAllFamilyMembers().first().find { it.familyMember.id == memberId }
        val permission = member?.permissions?.find { it.type == type }
        if (permission != null) {
            calendarDao.updateFamilyPermission(permission.id, level.name)
            calendarSyncQueueDao.insert(CalendarSyncQueueEntity(itemId = permission.id, operation = "UPDATE_FAMILY_PERMISSION", payload = level.name))
            scope.launch { processSyncQueue() }
        }
    }

    override suspend fun sendInvitation(usernameOrEmail: String, kind: String, initialPermissions: List<CalendarItemType>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val supabase = getSupabase()
            val fromUserId = supabase.auth.currentUserOrNull()?.id ?: return@withContext Result.failure(Exception("Not logged in"))
            
            val targetUser = supabase.postgrest["profiles"]
                .select { 
                    filter { 
                        or {
                            eq("username", usernameOrEmail)
                            eq("email", usernameOrEmail)
                        }
                    }
                }.decodeSingleOrNull<ProfileDto>() ?: return@withContext Result.failure(Exception("User not found"))

            val invitationId = UUID.randomUUID().toString()
            val invitationDto = CalendarInvitationDto(
                id = invitationId,
                from_user_id = fromUserId,
                to_user_id = targetUser.id,
                status = "PENDING",
                created_at = isoFormatter.format(Date()),
                kind = kind
            )

            supabase.postgrest["calendar_invitations"].insert(invitationDto)
            
            initialPermissions.forEach { type ->
                val permId = UUID.randomUUID().toString()
                supabase.postgrest["invitation_permissions"].insert(mapOf(
                    "id" to permId,
                    "invitation_id" to invitationId,
                    "type" to type.name,
                    "level" to "VIEW"
                ))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getPendingInvitations(): Flow<List<CalendarInvitation>> = flow {
        val supabase = getSupabase()
        val userId = supabase.auth.currentUserOrNull()?.id ?: return@flow
        
        val invitationFlow = callbackFlow {
            suspend fun fetch(): List<CalendarInvitation> {
                val dtos = supabase.postgrest["calendar_invitations"]
                    .select { 
                        filter { 
                            eq("to_user_id", userId)
                            eq("status", "PENDING")
                        }
                    }.decodeList<CalendarInvitationDto>()
                
                return dtos.map { dto ->
                    val fromUserDto = supabase.postgrest["profiles"]
                        .select { filter { eq("id", dto.from_user_id) } }
                        .decodeSingleOrNull<ProfileDto>()
                    dto.toEntity().toDomain(fromUserDto?.toDomain())
                }
            }

            trySend(fetch())

            val channel = supabase.realtime.channel("invitations_$userId")
            val flow = channel.postgresChangeFlow<PostgresAction>(schema = "public") { 
                table = "calendar_invitations"
            }
            
            val job = launch {
                flow.collect { trySend(fetch()) }
            }
            
            channel.subscribe()
            awaitClose {
                job.cancel()
                launch { channel.unsubscribe() }
            }
        }
        emitAll(invitationFlow)
    }

    private fun CalendarInvitationDto.toEntity() = CalendarInvitationEntity(
        id = id,
        fromUserId = from_user_id,
        toUserId = to_user_id,
        status = try { InviteStatus.valueOf(status) } catch(_: Exception) { InviteStatus.PENDING },
        createdAt = isoFormatter.parse(created_at)?.time ?: System.currentTimeMillis(),
        kind = kind
    )

    override suspend fun respondToInvitation(invitationId: String, accept: Boolean) = withContext(Dispatchers.IO) {
        try {
            val status = if (accept) "ACCEPTED" else "REJECTED"
            getSupabase().postgrest["calendar_invitations"].update(mapOf("status" to status)) {
                filter { eq("id", invitationId) }
            }
            
            if (accept) {
                syncCalendar()
            }
        } catch (e: Exception) {
        }
    }

    private suspend fun processSyncQueue() = withContext(Dispatchers.IO) {
        val pending = calendarSyncQueueDao.getPendingItems()
        if (pending.isEmpty()) return@withContext
        
        val supabase = getSupabase()
        pending.forEach { item ->
            try {
                when (item.operation) {
                    "SAVE_CALENDAR_ITEM" -> supabase.postgrest["calendar_items"].upsert(Json.decodeFromString<CalendarItemDto>(item.payload!!))
                    "DELETE_CALENDAR_ITEM" -> supabase.postgrest["calendar_items"].delete { filter { eq("id", item.itemId) } }
                    "SAVE_HABIT" -> supabase.postgrest["habits"].upsert(Json.decodeFromString<HabitDto>(item.payload!!))
                    "SAVE_HABIT_LOG" -> supabase.postgrest["habit_logs"].upsert(Json.decodeFromString<HabitLogDto>(item.payload!!))
                    "SAVE_CATEGORY" -> supabase.postgrest["calendar_categories"].upsert(Json.decodeFromString<CalendarCategoryDto>(item.payload!!))
                    "UPDATE_FAMILY_MEMBER_INCLUDED" -> supabase.postgrest["family_members"].update(mapOf("included" to item.payload!!.toBoolean())) { filter { eq("id", item.itemId) } }
                    "UPDATE_FAMILY_PERMISSION" -> supabase.postgrest["family_permissions"].update(mapOf("level" to item.payload!!)) { filter { eq("id", item.itemId) } }
                }
                calendarSyncQueueDao.delete(item)
            } catch (e: Exception) {
                if (item.retryCount < 5) {
                    calendarSyncQueueDao.update(item.copy(retryCount = item.retryCount + 1))
                } else {
                    calendarSyncQueueDao.update(item.copy(status = "FAILED"))
                }
            }
        }
    }

    override suspend fun syncCalendar() = withContext(Dispatchers.IO) {
        try {
            val supabase = getSupabase()
            processSyncQueue()
            val userId = supabase.auth.currentUserOrNull()?.id ?: return@withContext
            
            val remoteItems = supabase.postgrest["calendar_items"]
                .select {
                }
                .decodeList<CalendarItemDto>()
            
            remoteItems.forEach { dto ->
                val local = calendarDao.getItemById(dto.id)
                val remoteUpdatedAt = isoFormatter.parse(dto.updated_at)?.time ?: 0
                if ((local == null || local.item.updatedAt < remoteUpdatedAt) && local?.item?.pendingSync != true) {
                    calendarDao.upsertCalendarItem(
                        dto.toEntity().copy(pendingSync = false),
                        local?.attendees?.map { it.attendee } ?: emptyList(),
                        local?.reminders ?: emptyList()
                    )
                }
            }
            
            val remoteHabits = supabase.postgrest["habits"]
                .select { filter { eq("owner_id", userId) } }
                .decodeList<HabitDto>()
            remoteHabits.forEach { calendarDao.insertHabit(it.toEntity().copy(pendingSync = false)) }

            val remoteCategories = supabase.postgrest["calendar_categories"]
                .select { filter { eq("owner_id", userId) } }
                .decodeList<CalendarCategoryDto>()
            remoteCategories.forEach { calendarDao.insertCategory(it.toEntity()) }

            val remoteFamily = supabase.postgrest["family_members"]
                .select { filter { eq("user_id", userId) } }
                .decodeList<FamilyMemberDto>()
            
            remoteFamily.forEach { fmDto ->
                calendarDao.insertFamilyMember(fmDto.toEntity())
                val remotePerms = supabase.postgrest["family_permissions"]
                    .select { filter { eq("family_member_id", fmDto.id) } }
                    .decodeList<FamilyPermissionDto>()
                calendarDao.insertFamilyPermissions(remotePerms.map { it.toEntity() })
            }

        } catch (e: Exception) {
        }
    }

    private fun HabitDto.toEntity() = HabitEntity(
        id = id,
        title = title,
        description = description,
        color = color,
        icon = icon,
        recurrenceRule = recurrence_rule,
        ownerId = owner_id,
        createdAt = isoFormatter.parse(created_at)?.time ?: System.currentTimeMillis(),
        updatedAt = isoFormatter.parse(updated_at)?.time ?: System.currentTimeMillis(),
        isActive = is_active
    )

    private fun CalendarCategoryDto.toEntity() = CalendarCategoryEntity(
        id = id,
        name = name,
        color = color,
        icon = icon,
        ownerId = owner_id
    )

    private fun CalendarItemDto.toEntity() = CalendarItemEntity(
        id = id,
        type = try { CalendarItemType.valueOf(type) } catch(e: Exception) { CalendarItemType.EVENT },
        title = title,
        description = "", // Decrypted in domain transformation
        color = color,
        icon = icon,
        location = location,
        startTime = start_time?.let { isoFormatter.parse(it)?.time },
        endTime = end_time?.let { isoFormatter.parse(it)?.time },
        isAllDay = is_all_day,
        timezone = timezone,
        recurrenceRule = recurrence_rule,
        priority = try { CalendarPriority.valueOf(priority) } catch(e: Exception) { CalendarPriority.MEDIUM },
        status = try { CalendarStatus.valueOf(status) } catch(e: Exception) { CalendarStatus.PENDING },
        isPrivate = is_private,
        ownerId = owner_id,
        categoryId = category_id,
        createdAt = isoFormatter.parse(created_at)?.time ?: System.currentTimeMillis(),
        updatedAt = isoFormatter.parse(updated_at)?.time ?: System.currentTimeMillis(),
        meetingType = meeting_type?.let { try { MeetingType.valueOf(it) } catch(e: Exception) { null } },
        meetingLink = meeting_link,
        progress = progress,
        pomodoroCount = pomodoro_count,
        deadline = deadline?.let { isoFormatter.parse(it)?.time },
        parentItemId = parent_item_id,
        encryptedDescription = ciphertext,
        iv = iv,
        cryptoVersion = crypto_version
    )

    private fun CalendarItemWithDetails.toDomainInternal(): CalendarItem {
        val decrypted = decryptAgendaItem(item.toDomain(
            attachments = attachments.map { it.toDomain() },
            attendees = attendees.map { it.toDomain() },
            reminders = reminders.map { it.toDomain() }
        ), item.encryptedDescription, item.iv, item.cryptoVersion)
        return decrypted
    }

    private fun decryptAgendaItem(item: CalendarItem, ciphertext: String?, iv: String?, version: Int): CalendarItem {
        if (ciphertext == null || iv == null) return item
        return try {
            val encryptedObj = EncryptedObject(version = version, keyId = "root", iv = iv, ciphertext = ciphertext)
            val decryptedBytes = cryptoManager.decryptFromStorage(encryptedObj, com.keeftalk.chat.security.crypto.CryptoManager.StoragePurpose.AGENDA)
            val json = Json.parseToJsonElement(String(decryptedBytes, Charsets.UTF_8)).jsonObject
            item.copy(
                title = json["title"]?.jsonPrimitive?.content ?: item.title,
                description = json["description"]?.jsonPrimitive?.content ?: item.description
            )
        } catch (e: Exception) {
            item
        }
    }

    override fun observeCalendarRealtime(): Flow<Unit> = flow {
        val supabase = getSupabase()
        val channel = supabase.realtime.channel("calendar_realtime")
        val itemsFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") { table = "calendar_items" }
        val realtimeFlow = callbackFlow {
            val job = launch {
                itemsFlow.collect {
                    syncCalendar()
                    trySend(Unit)
                }
            }
            launch { channel.subscribe() }
            awaitClose {
                job.cancel()
                launch { channel.unsubscribe() }
            }
        }
        emitAll(realtimeFlow)
    }

    override fun shutdown() {
        Log.i("CalendarRepo", "Shutting down CalendarRepository")
        scope.cancel()
    }

    private fun CalendarItem.toDto() = CalendarItemDto(
        id = id,
        type = type.name,
        title = title,
        description = description,
        color = color,
        icon = icon,
        location = location,
        start_time = startTime?.let { isoFormatter.format(Date(it)) },
        end_time = endTime?.let { isoFormatter.format(Date(it)) },
        is_all_day = isAllDay,
        timezone = timezone,
        recurrence_rule = recurrenceRule,
        priority = priority.name,
        status = status.name,
        is_private = isPrivate,
        owner_id = ownerId,
        category_id = categoryId,
        created_at = isoFormatter.format(Date(createdAt)),
        updated_at = isoFormatter.format(Date(updatedAt)),
        meeting_type = meetingType?.name,
        meeting_link = meetingLink,
        progress = progress,
        pomodoro_count = pomodoroCount,
        deadline = deadline?.let { isoFormatter.format(Date(it)) },
        parent_item_id = parentItemId
    )

    private fun Habit.toDto() = HabitDto(
        id = id,
        title = title,
        description = description,
        color = color,
        icon = icon,
        recurrence_rule = recurrenceRule,
        owner_id = ownerId,
        created_at = isoFormatter.format(Date(createdAt)),
        updated_at = isoFormatter.format(Date(updatedAt)),
        is_active = isActive
    )

    private fun HabitLog.toDto() = HabitLogDto(
        id = id,
        habit_id = habitId,
        date = isoFormatter.format(Date(date)),
        status = status.name,
        note = note
    )

    private fun CalendarCategory.toDto() = CalendarCategoryDto(
        id = id,
        name = name,
        color = color,
        icon = icon,
        owner_id = ownerId
    )

    private fun CalendarAttendee.toDto() = CalendarAttendeeDto(
        id = id,
        item_id = itemId,
        user_id = userId,
        role = role.name,
        status = status.name
    )
}
