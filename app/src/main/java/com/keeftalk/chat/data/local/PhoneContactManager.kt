package com.keeftalk.chat.data.local

import android.content.Context
import android.provider.ContactsContract
import com.keeftalk.chat.domain.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

class PhoneContactManager(private val context: Context) {
    suspend fun fetchPhoneContacts(): List<User> = withContext(Dispatchers.IO) {
        val contacts = mutableListOf<User>()
        try {
            if (context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return@withContext emptyList()
            }

            val cursor = context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
                ),
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
            )

            cursor?.use {
                val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)

                while (it.moveToNext()) {
                    val id = it.getString(idIndex)
                    val name = it.getString(nameIndex)
                    val number = it.getString(numberIndex)
                    val photoUri = it.getString(photoIndex)

                    contacts.add(
                        User(
                            id = "phone_$id",
                            name = name ?: "Unknown",
                            username = name?.lowercase()?.replace(" ", "_") ?: "unknown",
                            phone = number,
                            avatarUrl = photoUri,
                            isActive = false,
                            lastSeen = 0,
                            isContact = true
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("PhoneContactManager", "Error fetching contacts", e)
        }
        contacts.distinctBy { it.phone }
    }
}
