package com.lputouch.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        CachedResultEntity::class,
        CachedAttendanceEntity::class,
        CachedTimetableEntity::class,
        CachedAnnouncementEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun resultDao(): ResultDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun timetableDao(): TimetableDao
    abstract fun announcementDao(): AnnouncementDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lputouch.db"
                )
                    // Cache-only DB — safe to drop and rebuild on schema change.
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}
