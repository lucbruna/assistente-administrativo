package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [OfficeItem::class, AgendaItem::class], version = 2, exportSchema = false)
abstract class OfficeDatabase : RoomDatabase() {
    abstract fun officeItemDao(): OfficeItemDao
    abstract fun agendaItemDao(): AgendaItemDao

    companion object {
        @Volatile
        private var INSTANCE: OfficeDatabase? = null

        fun getDatabase(context: Context): OfficeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OfficeDatabase::class.java,
                    "office_assistant_database"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
