package com.lputouch.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/** How long (ms) before each data type's cache is considered stale and must be refreshed. */
object CacheTtl {
    val RESULTS      = 24 * 60 * 60 * 1_000L  // 24 hours  — grades change rarely
    val ATTENDANCE   = 30 * 60 * 1_000L        // 30 minutes — updated after each class
    val TIMETABLE    = 7  * 24 * 60 * 60 * 1_000L // 7 days  — schedule changes rarely
    val ANNOUNCEMENTS = 15 * 60 * 1_000L       // 15 minutes — time-sensitive notices
}

@Dao
interface ResultDao {
    // termid encodes session+term chronologically (123241, 123242, 224251...).
    @Query("SELECT * FROM cached_results ORDER BY termId ASC, courseCode ASC")
    suspend fun getAll(): List<CachedResultEntity>

    /** Returns the oldest cachedAt timestamp, or null if the table is empty. */
    @Query("SELECT MIN(cachedAt) FROM cached_results")
    suspend fun oldestCachedAt(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CachedResultEntity>)

    @Query("DELETE FROM cached_results")
    suspend fun clear()
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM cached_attendance")
    suspend fun getAll(): List<CachedAttendanceEntity>

    @Query("SELECT MIN(cachedAt) FROM cached_attendance")
    suspend fun oldestCachedAt(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CachedAttendanceEntity>)

    @Query("DELETE FROM cached_attendance")
    suspend fun clear()
}

@Dao
interface TimetableDao {
    @Query("SELECT * FROM cached_timetable ORDER BY day, attendanceTime")
    suspend fun getAll(): List<CachedTimetableEntity>

    @Query("SELECT MIN(cachedAt) FROM cached_timetable")
    suspend fun oldestCachedAt(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CachedTimetableEntity>)

    @Query("DELETE FROM cached_timetable")
    suspend fun clear()
}

@Dao
interface AnnouncementDao {
    @Query("SELECT * FROM cached_announcements ORDER BY entryDate DESC")
    suspend fun getAll(): List<CachedAnnouncementEntity>

    @Query("SELECT MIN(cachedAt) FROM cached_announcements")
    suspend fun oldestCachedAt(): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CachedAnnouncementEntity>)

    @Query("DELETE FROM cached_announcements")
    suspend fun clear()
}
