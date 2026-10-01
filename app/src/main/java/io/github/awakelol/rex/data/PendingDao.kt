package io.github.awakelol.rex.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingDao {
    @Query("SELECT * FROM pending ORDER BY detectedAt DESC")
    fun observeAll(): Flow<List<PendingApp>>

    @Query("SELECT * FROM pending")
    suspend fun all(): List<PendingApp>

    @Query("SELECT packageName FROM pending")
    suspend fun names(): List<String>

    @Query("SELECT * FROM pending WHERE packageName = :pkg")
    suspend fun get(pkg: String): PendingApp?

    @Query("SELECT * FROM pending WHERE packageName = :pkg")
    fun observe(pkg: String): Flow<PendingApp?>

    @Upsert
    suspend fun upsert(app: PendingApp)

    @Query("DELETE FROM pending WHERE packageName = :pkg")
    suspend fun delete(pkg: String)

    @Query("DELETE FROM pending")
    suspend fun clear()

    @Query("UPDATE pending SET lastWarnedAt = :time WHERE packageName = :pkg")
    suspend fun markWarned(pkg: String, time: Long)
}
