package com.keeftalk.chat.util

import com.keeftalk.chat.data.local.entities.UserEntity
import java.util.UUID

object VcfUtils {

    fun parseVcf(content: String): List<UserEntity> {
        val contacts = mutableListOf<UserEntity>()
        val cards = content.split("BEGIN:VCARD")
        
        for (card in cards) {
            if (card.isBlank()) continue
            
            var name = ""
            var phone = ""
            var email = ""
            var username = ""
            
            val lines = card.split("\n")
            for (line in lines) {
                val trimmed = line.trim()
                when {
                    trimmed.startsWith("FN:") -> name = trimmed.substring(3)
                    trimmed.startsWith("TEL:") -> phone = trimmed.substring(4)
                    trimmed.startsWith("EMAIL:") -> email = trimmed.substring(6)
                    trimmed.startsWith("NICKNAME:") -> username = trimmed.substring(9)
                }
            }
            
            if (name.isNotBlank() || phone.isNotBlank()) {
                contacts.add(
                    UserEntity(
                        id = UUID.randomUUID().toString(),
                        name = name.ifBlank { phone },
                        username = username.ifBlank { name.lowercase().replace(" ", "_") },
                        phone = phone,
                        email = email,
                        avatarUrl = null,
                        isContact = true
                    )
                )
            }
        }
        return contacts
    }

    fun generateVcf(contacts: List<UserEntity>): String {
        val sb = StringBuilder()
        for (contact in contacts) {
            sb.append("BEGIN:VCARD\n")
            sb.append("VERSION:3.0\n")
            sb.append("FN:${contact.name}\n")
            contact.phone?.let { sb.append("TEL:$it\n") }
            contact.email?.let { sb.append("EMAIL:$it\n") }
            sb.append("NICKNAME:${contact.username}\n")
            sb.append("END:VCARD\n")
        }
        return sb.toString()
    }
}
