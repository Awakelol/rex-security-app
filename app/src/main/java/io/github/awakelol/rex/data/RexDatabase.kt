package io.github.awakelol.rex.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

@Database(entities = [LogEntry::class, PendingApp::class], version = 1)
@TypeConverters(Converters::class)
abstract class RexDatabase : RoomDatabase() {
    abstract fun log(): LogDao
    abstract fun pending(): PendingDao

    companion object {
        fun build(context: Context): RexDatabase =
            Room.databaseBuilder(context, RexDatabase::class.java, "rex.db").build()
    }
}

class Converters {
    @TypeConverter
    fun fromList(list: List<String>): String = list.joinToString("\n")

    @TypeConverter
    fun toList(value: String): List<String> = if (value.isEmpty()) emptyList() else value.split("\n")
}
