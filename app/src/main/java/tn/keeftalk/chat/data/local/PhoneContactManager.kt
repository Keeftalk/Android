package tn.keeftalk.chat.data.local

import android.content.Context
import android.provider.ContactsContract
import tn.keeftalk.chat.domain.model.User
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

    fun getContactName(context: Context, phoneNumber: String): String? {
        try {
            if (context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return null
            }
            val uri = android.net.Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                android.net.Uri.encode(phoneNumber)
            )
            val cursor = context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIdx = it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    if (nameIdx != -1) {
                        return it.getString(nameIdx)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PhoneContactManager", "Error looking up contact name for $phoneNumber", e)
        }
        return null
    }
}
