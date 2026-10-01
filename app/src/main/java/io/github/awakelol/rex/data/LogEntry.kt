package io.github.awakelol.rex.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class LogKind { INSTALLED, REMOVED, APPROVED, NOTIFICATION_BLOCKED, INFO }

@Entity(tableName = "log")
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val time: Long,
    val kind: LogKind,
    val packageName: String?,
    val text: String,
)
