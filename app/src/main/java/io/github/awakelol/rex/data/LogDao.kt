package io.github.awakelol.rex.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Insert
    suspend fun insert(entry: LogEntry)

    @Query("SELECT * FROM log ORDER BY time DESC LIMIT :limit")
    fun recent(limit: Int = 500): Flow<List<LogEntry>>
}
