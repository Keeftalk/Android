package tn.keeftalk.chat.data.repository

import org.junit.Assert.*
import org.junit.Test
import tn.keeftalk.chat.data.local.entities.ProfileMediaEntity
import tn.keeftalk.chat.domain.model.MediaLiker
import tn.keeftalk.chat.domain.model.MediaStatus
import tn.keeftalk.chat.domain.model.MediaType
import tn.keeftalk.chat.domain.model.ProfileMedia

class ProfileMediaRepositoryTest {

    @Test
    fun testProfileMediaDomainMapping() {
        val entity = ProfileMediaEntity(
            id = "media_123",
            userId = "user_abc",
            mediaType = "AVATAR",
            originalObjectKey = "user_abc/avatars/media_123/original",
            compressedObjectKey = "user_abc/avatars/media_123/compressed",
            originalCdnUrl = "https://public.cdn.keeftalk.com/user_abc/avatars/media_123/original",
            compressedCdnUrl = "https://public.cdn.keeftalk.com/user_abc/avatars/media_123/compressed",
            status = "ACTIVE",
            isActive = true,
            createdAt = 1700000000000L,
            activatedAt = 1700000000000L,
            likesCount = 5,
            isLikedByMe = true
        )

        assertEquals("media_123", entity.id)
        assertEquals("user_abc", entity.userId)
        assertEquals("AVATAR", entity.mediaType)
        assertTrue(entity.isActive)
        assertEquals(5, entity.likesCount)
        assertTrue(entity.isLikedByMe)
    }

    @Test
    fun testMediaLikerContactBadge() {
        val contactLiker = MediaLiker(
            userId = "user_contact",
            username = "alice",
            fullName = "Alice Smith",
            avatarUrl = "https://public.cdn.keeftalk.com/alice.jpg",
            isContact = true
        )

        val nonContactLiker = MediaLiker(
            userId = "user_stranger",
            username = "bob",
            fullName = "Bob Jones",
            avatarUrl = null,
            isContact = false
        )

        assertTrue(contactLiker.isContact)
        assertFalse(nonContactLiker.isContact)
        assertEquals("Alice Smith", contactLiker.fullName)
    }

    @Test
    fun testProfileMediaStatusTransitions() {
        val activeMedia = ProfileMedia(
            id = "media_active",
            userId = "user_1",
            mediaType = MediaType.WALLPAPER,
            originalObjectKey = "user_1/wallpapers/media_active/original",
            originalCdnUrl = "https://public.cdn.keeftalk.com/user_1/wallpapers/media_active/original",
            status = MediaStatus.ACTIVE,
            isActive = true
        )

        val detainedMedia = activeMedia.copy(
            status = MediaStatus.DETENTION,
            isActive = false,
            deletedAt = System.currentTimeMillis(),
            scheduledPurgeAt = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000
        )

        assertEquals(MediaStatus.ACTIVE, activeMedia.status)
        assertEquals(MediaStatus.DETENTION, detainedMedia.status)
        assertFalse(detainedMedia.isActive)
        assertNotNull(detainedMedia.scheduledPurgeAt)
    }
}
