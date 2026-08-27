package com.lputouch.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cached_results")
data class CachedResultEntity(
    // Multiple courses share a termid — key must be unique per (term, course).
    @PrimaryKey val id: String,
    val termId: String,
    val romanTerm: String,
    val detailTerm: String,
    val courseCode: String,
    val course: String,
    val gradeOrMarks: String,
    val termPercentOrTGPA: String,
    val termPercentOrCGPA: String,
    val gradeExplanation: String,
    val failGradeDescription: String,
    val gradeColor: String,
    val mulFactor: String,
    val cachedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "cached_attendance")
data class CachedAttendanceEntity(
    @PrimaryKey val courseCode: String,
    val courseName: String,
    val courseType: String,
    val faculty: String,
    val room: String,
    val section: String,
    val totalAttd: String,
    val totalDelv: String,
    val totalPerc: String,
    val cachedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "cached_timetable")
data class CachedTimetableEntity(
    @PrimaryKey val id: String,
    val day: Int,
    val attendanceTime: String,
    val courseName: String,
    val courseCode: String,
    val facultyName: String,
    val roomNo: String,
    val cachedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "cached_announcements")
data class CachedAnnouncementEntity(
    @PrimaryKey val announcementId: String,
    val category: String,
    val subject: String,
    val description: String,
    val uploadedBy: String,
    val entryDate: String,
    val isNew: Boolean,
    val cachedAt: Long = System.currentTimeMillis(),
)
