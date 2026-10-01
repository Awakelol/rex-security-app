package io.github.awakelol.rex.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.awakelol.rex.core.RiskLevel

@Entity(tableName = "pending")
data class PendingApp(
    @PrimaryKey val packageName: String,
    val label: String,
    val installer: String?,
    val level: RiskLevel,
    val reasons: List<String>,
    val detectedAt: Long,
    val lastWarnedAt: Long,
)
