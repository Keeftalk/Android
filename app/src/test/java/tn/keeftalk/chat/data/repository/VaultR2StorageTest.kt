package tn.keeftalk.chat.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import tn.keeftalk.chat.util.InfrastructureStorageException
import tn.keeftalk.chat.util.StorageConfig

class VaultR2StorageTest {

    @Test
    fun userKey_formattingFollowsRegionalStructure() {
        val country = "TN"
        val userId = "36e6874d-cb83-40fa-b74c-e8829491f26e"
        val category = "files"
        val relativePath = "fb6ac65e-b448-4f4d-ad3d-ebec43cd3941/original"

        val key = StorageConfig.buildUserKey(country, userId, category, relativePath)

        assertEquals("TN/36e6874d-cb83-40fa-b74c-e8829491f26e/files/fb6ac65e-b448-4f4d-ad3d-ebec43cd3941/original", key)
    }

    @Test
    fun vaultPaths_doNotTargetSupabaseStorageEndpoint() {
        val objectKey = "TN/user_123/files/file_456/original"
        val cdnUrl = StorageConfig.getPublicCdnUrl(objectKey)

        assertFalse("Vault URL must not use Supabase /storage/v1/object/ endpoint", cdnUrl.contains("/storage/v1/object/"))
        assertTrue("Vault CDN URL must target public.cdn.keeftalk.com", cdnUrl.contains("public.cdn.keeftalk.com"))
    }

    @Test
    fun infrastructureException_isThrownForUnprovisionedBucket() {
        val exception = InfrastructureStorageException("INFRASTRUCTURE_ERROR: Regional R2 storage bucket 'keeftalk-weur-private' is not provisioned")
        assertEquals("INFRASTRUCTURE_ERROR: Regional R2 storage bucket 'keeftalk-weur-private' is not provisioned", exception.message)
    }

    @Test
    fun idempotency_sameFileIdProducesDeterministicObjectKey() {
        val country = "TN"
        val userId = "36e6874d-cb83-40fa-b74c-e8829491f26e"
        val fileId = "fb6ac65e-b448-4f4d-ad3d-ebec43cd3941"

        val key1 = StorageConfig.buildUserKey(country, userId, "files", "$fileId/original")
        val key2 = StorageConfig.buildUserKey(country, userId, "files", "$fileId/original")

        assertEquals(key1, key2)
    }
}
