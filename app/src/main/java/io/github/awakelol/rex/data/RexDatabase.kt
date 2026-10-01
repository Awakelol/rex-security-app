package io.github.awakelol.rex.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [LogEntry::class], version = 1)
abstract class RexDatabase : RoomDatabase() {
    abstract fun log(): LogDao

    companion object {
        fun build(context: Context): RexDatabase =
            Room.databaseBuilder(context, RexDatabase::class.java, "rex.db").build()
    }
}
