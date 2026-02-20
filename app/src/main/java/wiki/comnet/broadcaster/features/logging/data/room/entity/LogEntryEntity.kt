package wiki.comnet.broadcaster.features.logging.data.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "log_entries")
data class LogEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "level")
    val level: Int,

    @ColumnInfo(name = "tag")
    val tag: String,

    @ColumnInfo(name = "message")
    val message: String,

    @ColumnInfo(name = "throwable")
    val throwable: String? = null,

    @ColumnInfo(name = "is_synced", index = true)
    val isSynced: Boolean = false,

    @ColumnInfo(name = "created_at", index = true)
    val createdAt: Long = System.currentTimeMillis(),
)
