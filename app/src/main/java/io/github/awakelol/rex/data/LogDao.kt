package io.github.awakelol.rex.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Insert
    suspend fun insert(entry: LogEntry)

    @Query("SELECT * FROM log ORDER BY time DESC, id DESC LIMIT :limit")
    fun recent(limit: Int = 500): Flow<List<LogEntry>>

    @Query("DELETE FROM log WHERE id NOT IN (SELECT id FROM log ORDER BY time DESC, id DESC LIMIT :keep)")
    suspend fun trim(keep: Int = 2000)
}
