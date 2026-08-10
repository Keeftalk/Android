import com.keeftalk.chat.util.TimestampSerializer
import com.keeftalk.chat.data.local.entities.VaultItemEntity
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

fun main() {
    val entity = VaultItemEntity(
        id = "test-id",
        userId = "user-id",
        fileId = "file-id",
        folderId = null,
        title = "test-title",
        createdAt = 1786206665850L
    )
    val json = Json.encodeToString(entity)
    println("Serialized JSON: $json")
}
