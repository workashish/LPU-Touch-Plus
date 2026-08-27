package com.lputouch.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ResultDao {
    // termid encodes session+term chronologically (123241, 123242, 224251...).
    @Query("SELECT * FROM cached_results ORDER BY termId ASC, courseCode ASC")
    suspend fun getAll(): List<CachedResultEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CachedResultEntity>)

    @Query("DELETE FROM cached_results")
    suspend fun clear()
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM cached_attendance")
    suspend fun getAll(): List<CachedAttendanceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CachedAttendanceEntity>)

    @Query("DELETE FROM cached_attendance")
    suspend fun clear()
}

@Dao
interface TimetableDao {
    @Query("SELECT * FROM cached_timetable ORDER BY day, attendanceTime")
    suspend fun getAll(): List<CachedTimetableEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CachedTimetableEntity>)

    @Query("DELETE FROM cached_timetable")
    suspend fun clear()
}

@Dao
interface AnnouncementDao {
    @Query("SELECT * FROM cached_announcements ORDER BY entryDate DESC")
    suspend fun getAll(): List<CachedAnnouncementEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CachedAnnouncementEntity>)

    @Query("DELETE FROM cached_announcements")
    suspend fun clear()
}
