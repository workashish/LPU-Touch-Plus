package com.lputouch.app.data.repo

import com.lputouch.app.data.api.HappeningsApi
import com.lputouch.app.data.api.MobileApi
import com.lputouch.app.data.api.UmsApi
import com.lputouch.app.data.api.dto.AdmissionDocument
import com.lputouch.app.data.api.dto.Announcement
import com.lputouch.app.data.api.dto.AnnouncementDetail
import com.lputouch.app.data.api.dto.AptitudeScore
import com.lputouch.app.data.api.dto.AttendanceDetailItem
import com.lputouch.app.data.api.dto.AttendanceItem
import com.lputouch.app.data.api.dto.BusRoute
import com.lputouch.app.data.api.dto.CalendarEvent
import com.lputouch.app.data.api.dto.EduRevCategory
import com.lputouch.app.data.api.dto.EduRevCourse
import com.lputouch.app.data.api.dto.FeeBalanceItem
import com.lputouch.app.data.api.dto.FeeExtensionItem
import com.lputouch.app.data.api.dto.HostelLeaveBalance
import com.lputouch.app.data.api.dto.HostelLeaveItem
import com.lputouch.app.data.api.dto.LeaderboardEntry
import com.lputouch.app.data.api.dto.LibraryItem
import com.lputouch.app.data.api.dto.MakeupClass
import com.lputouch.app.data.api.dto.MentorRemark
import com.lputouch.app.data.api.dto.MessageItem
import com.lputouch.app.data.api.dto.MessagesHistoryRequest
import com.lputouch.app.data.api.dto.NewsPost
import com.lputouch.app.data.api.dto.PhoneContact
import com.lputouch.app.data.api.dto.PlacementDrive
import com.lputouch.app.data.api.dto.ResultItem
import com.lputouch.app.data.api.dto.RmsQuery
import com.lputouch.app.data.api.dto.RplResult
import com.lputouch.app.data.api.dto.SeatingPlanItem
import com.lputouch.app.data.api.dto.StudentBasicInfo
import com.lputouch.app.data.api.dto.TimetableItem
import com.lputouch.app.data.db.AppDatabase
import com.lputouch.app.data.db.CachedAnnouncementEntity
import com.lputouch.app.data.db.CachedAttendanceEntity
import com.lputouch.app.data.db.CachedResultEntity
import com.lputouch.app.data.db.CachedTimetableEntity
import com.lputouch.app.data.prefs.SessionStore
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StudentRepository(
    private val mobileApi: MobileApi,
    private val umsApi: UmsApi,
    private val happeningsApi: HappeningsApi,
    private val sessionStore: SessionStore,
    private val db: AppDatabase,
    private val authRepository: AuthRepository,
) {
    private suspend fun session() = Triple(
        sessionStore.userId.first() ?: "",
        sessionStore.accessToken.first() ?: "",
        sessionStore.deviceId.first() ?: "",
    )

    /**
     * Runs [block] with the current session. If the server reports an expired
     * session, silently re-logins (PVR flow with stored credentials) and retries once.
     * Returns null on failure — never returns an expired/empty server object.
     */
    private suspend fun <T> withFreshSession(block: suspend (uid: String, token: String, deviceId: String) -> T?): T? {
        val (uid, token, deviceId) = session()
        if (uid.isEmpty()) return null
        val first = try {
            block(uid, token, deviceId)
        } catch (e: Exception) {
            null
        }
        if (first == null || isExpiredSession(first)) {
            if (authRepository.refreshSession()) {
                val (uid2, token2, deviceId2) = session()
                val retried = try {
                    block(uid2, token2, deviceId2)
                } catch (e: Exception) {
                    null
                }
                // Only accept the retry if it's real data, not another expired response.
                return if (retried != null && !isExpiredSession(retried)) retried else null
            }
            // Refresh failed — never surface the expired/empty object to the UI.
            return null
        }
        return first
    }

    /**
     * UMS returns an object (or a list containing one) with a non-empty Error
     * field when the token expires. Works for both single DTOs and lists.
     */
    private fun isExpiredSession(result: Any?): Boolean {
        return when (result) {
            is StudentBasicInfo -> hasErrorField(result.error)
            is List<*> -> result.firstOrNull() is StudentBasicInfo && hasErrorField((result.first() as StudentBasicInfo).error)
            else -> false
        }
    }

    private fun hasErrorField(err: String?): Boolean =
        !err.isNullOrBlank() && !err.equals("null", ignoreCase = true)

    /** Filters out the error placeholder item the server returns when a session expires. */
    private fun <T> filterExpired(items: List<T>): List<T> =
        items.filterNot { it is StudentBasicInfo && hasErrorField((it as StudentBasicInfo).error) }

    suspend fun getProfile(): StudentBasicInfo? {
        return withFreshSession { uid, token, deviceId ->
            umsApi.studentBasicInfo(uid, token, deviceId)
                .filterNot { hasErrorField(it.error) }
                .firstOrNull()
        }
    }

    suspend fun getResults(forceRefresh: Boolean = false): List<ResultItem> {
        if (forceRefresh || db.resultDao().getAll().isEmpty()) {
            try {
                val response = mobileApi.getStudentResult()
                val flat = response.flatMap { it.result ?: emptyList() }
                if (flat.isNotEmpty()) {
                    db.resultDao().clear()
                    db.resultDao().insertAll(
                        flat.map {
                            CachedResultEntity(
                                id = "${it.termId ?: ""}|${it.courseCode ?: ""}|${it.romanTerm ?: ""}",
                                termId = it.termId ?: "",
                                romanTerm = it.romanTerm ?: "",
                                detailTerm = it.detailTerm ?: "",
                                courseCode = it.courseCode ?: "",
                                course = it.course ?: "",
                                gradeOrMarks = it.gradeOrMarks ?: "",
                                termPercentOrTGPA = it.termPercentOrTGPA ?: "",
                                termPercentOrCGPA = it.termPercentOrCGPA ?: "",
                                gradeExplanation = it.gradeExplanation ?: "",
                                failGradeDescription = it.failGradeDescription ?: "",
                                gradeColor = it.gradeColor ?: "#000000",
                                mulFactor = it.mulFactor ?: "",
                            )
                        }
                    )
                }
            } catch (e: Exception) {
                // fall through to cache
            }
        }
        return db.resultDao().getAll().map {
            ResultItem(
                termId = it.termId,
                romanTerm = it.romanTerm,
                detailTerm = it.detailTerm,
                courseCode = it.courseCode,
                course = it.course,
                gradeOrMarks = it.gradeOrMarks,
                termPercentOrTGPA = it.termPercentOrTGPA,
                termPercentOrCGPA = it.termPercentOrCGPA,
                gradeExplanation = it.gradeExplanation,
                failGradeDescription = it.failGradeDescription,
                gradeColor = it.gradeColor,
                mulFactor = it.mulFactor,
            )
        }
    }

    suspend fun getAttendance(forceRefresh: Boolean = false): List<AttendanceItem> {
        if (forceRefresh || db.attendanceDao().getAll().isEmpty()) {
            val items = withFreshSession { uid, token, deviceId ->
                filterExpired(umsApi.getAttendance(uid, token, deviceId)).takeIf { it.isNotEmpty() }
            }
            if (!items.isNullOrEmpty()) {
                db.attendanceDao().clear()
                db.attendanceDao().insertAll(
                    items.map {
                        CachedAttendanceEntity(
                            courseCode = it.courseCode ?: it.subjectName ?: "",
                            courseName = it.courseName ?: it.subjectName ?: "",
                            courseType = it.courseType ?: "",
                            faculty = it.faculty ?: "",
                            room = it.room ?: "",
                            section = it.section ?: "",
                            totalAttd = it.totalAttd ?: "",
                            totalDelv = it.totalDelv ?: "",
                            totalPerc = it.totalPerc ?: "",
                        )
                    }
                )
            }
        }
        return db.attendanceDao().getAll().map {
            AttendanceItem(
                courseCode = it.courseCode,
                courseName = it.courseName,
                courseType = it.courseType,
                faculty = it.faculty,
                room = it.room,
                section = it.section,
                totalAttd = it.totalAttd,
                totalDelv = it.totalDelv,
                totalPerc = it.totalPerc,
            )
        }
    }

    suspend fun getAttendanceDetail(courseCode: String): List<AttendanceDetailItem> {
        return withFreshSession { uid, token, deviceId ->
            umsApi.getAttendanceDetail(uid, token, deviceId, courseCode).takeIf { it.isNotEmpty() }
        } ?: emptyList()
    }

    suspend fun getTimetable(forceRefresh: Boolean = false): List<TimetableItem> {
        if (forceRefresh || db.timetableDao().getAll().isEmpty()) {
            val items = withFreshSession { uid, token, deviceId ->
                filterExpired(umsApi.getTimetable(uid, token, deviceId)).takeIf { it.isNotEmpty() }
            }
            if (!items.isNullOrEmpty()) {
                db.timetableDao().clear()
                db.timetableDao().insertAll(
                    items.mapIndexed { i, it ->
                        CachedTimetableEntity(
                            id = "${it.day}-${it.attendanceTime}-$i",
                            day = it.day ?: 0,
                            attendanceTime = it.attendanceTime ?: "",
                            courseName = it.courseName ?: it.subjectName ?: "",
                            courseCode = it.courseCode ?: "",
                            facultyName = it.facultyName ?: "",
                            roomNo = it.roomNo ?: "",
                        )
                    }
                )
            }
        }
        return db.timetableDao().getAll().map {
            TimetableItem(
                day = it.day,
                attendanceTime = it.attendanceTime,
                courseName = it.courseName,
                courseCode = it.courseCode,
                facultyName = it.facultyName,
                roomNo = it.roomNo,
            )
        }
    }

    suspend fun getAnnouncements(forceRefresh: Boolean = false): List<Announcement> {
        if (forceRefresh || db.announcementDao().getAll().isEmpty()) {
            val items = withFreshSession { uid, token, deviceId ->
                filterExpired(umsApi.getAnnouncements(uid, token, deviceId)).takeIf { it.isNotEmpty() }
            }
            if (!items.isNullOrEmpty()) {
                db.announcementDao().clear()
                db.announcementDao().insertAll(
                    items.map {
                        CachedAnnouncementEntity(
                            announcementId = it.announcementId ?: "${it.subject}-${it.entryDate}",
                            category = it.category ?: "",
                            subject = it.subject ?: "",
                            description = it.description ?: "",
                            uploadedBy = it.uploadedBy ?: "",
                            entryDate = it.entryDate ?: "",
                            isNew = (it.isNew ?: "").equals("True", ignoreCase = true),
                        )
                    }
                )
            }
        }
        return db.announcementDao().getAll().map {
            Announcement(
                announcementId = it.announcementId,
                category = it.category,
                subject = it.subject,
                description = it.description,
                uploadedBy = it.uploadedBy,
                entryDate = it.entryDate,
                isNew = it.isNew.toString(),
            )
        }
    }

    /** Full announcement body. Runs on mobileapi (JWT auth), no UMS session needed. */
    suspend fun getAnnouncementDetail(announcementId: String, tab: String = "Online"): AnnouncementDetail? {
        return try {
            mobileApi.getAnnouncementDetail(announcementId, tab).item1?.firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getMyMessages(): List<MessageItem> {
        return withFreshSession { uid, token, deviceId ->
            umsApi.getMyMessages(uid, token, deviceId).takeIf { it.isNotEmpty() }
        } ?: emptyList()
    }

    suspend fun getPlacementDrives(): List<PlacementDrive> {
        return withFreshSession { uid, token, deviceId ->
            umsApi.getPlacementDetails(uid, token, deviceId).takeIf { it.isNotEmpty() }
        } ?: emptyList()
    }

    suspend fun getRmsQueries(): List<RmsQuery> {
        return withFreshSession { uid, token, deviceId ->
            umsApi.getRmsQueries(uid, token, deviceId).takeIf { it.isNotEmpty() }
        } ?: emptyList()
    }

    suspend fun getMakeupClasses(): List<MakeupClass> {
        return withFreshSession { uid, token, deviceId ->
            umsApi.getMakeupClasses(token, deviceId, uid).takeIf { it.isNotEmpty() }
        } ?: emptyList()
    }

    suspend fun getMentorRemarks(): List<MentorRemark> {
        return try {
            mobileApi.getMentorRemarks().item1 ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAmcatScore(): List<AptitudeScore> = safeList { mobileApi.getAmcatScore() }
    suspend fun getCoCubesScore(): List<AptitudeScore> = safeList { mobileApi.getCoCubesScore() }
    suspend fun getRplResults(): List<RplResult> = safeList { mobileApi.getRplResult() }

    // ─── New Features ──────────────────────────────────────────────────────────

    suspend fun getAdmissionDocuments(): List<AdmissionDocument> {
        return withFreshSession { uid, token, deviceId ->
            umsApi.getAdmissionDocuments(token, deviceId, uid).takeIf { it.isNotEmpty() }
        } ?: emptyList()
    }

    suspend fun getFeeBalance(): List<FeeBalanceItem> {
        return withFreshSession { uid, token, deviceId ->
            umsApi.getFeeBalance(uid, token, deviceId).takeIf { it.isNotEmpty() }
        } ?: emptyList()
    }

    suspend fun getFeeExtensionPopup(): List<FeeExtensionItem> {
        return safeList { mobileApi.getFeeDateExtensionPopup() }
    }

    suspend fun getSeatingPlan(): List<SeatingPlanItem> {
        return withFreshSession { uid, token, deviceId ->
            umsApi.getSeatingPlan(uid, token, deviceId).takeIf { it.isNotEmpty() }
        } ?: emptyList()
    }

    suspend fun getLibraryData(): List<LibraryItem> {
        return withFreshSession { uid, token, _ ->
            umsApi.getLibraryData(uid, token).takeIf { it.isNotEmpty() }
        } ?: emptyList()
    }

    suspend fun getBusRoutes(): List<BusRoute> {
        return withFreshSession { uid, token, _ ->
            umsApi.getBusRoutes(uid, token).takeIf { it.isNotEmpty() }
        } ?: emptyList()
    }

    suspend fun getHostelLeaveHistory(): List<HostelLeaveItem> {
        return withFreshSession { uid, token, _ ->
            umsApi.getHostelLeaveDetails(uid, token).takeIf { it.isNotEmpty() }
        } ?: emptyList()
    }

    suspend fun getHostelLeaveBalance(): HostelLeaveBalance? {
        return withFreshSession { uid, token, _ ->
            umsApi.getHostelLeaveBalance(uid, token)
        }
    }

    suspend fun getPhoneDirectory(): List<PhoneContact> {
        return withFreshSession { uid, token, deviceId ->
            umsApi.getPhoneDirectory(uid, token, deviceId).takeIf { it.isNotEmpty() }
        } ?: emptyList()
    }

    suspend fun getLeaderboard(monthYear: String? = null): List<LeaderboardEntry> {
        val my = monthYear ?: run {
            val sdf = SimpleDateFormat("MM/yyyy", Locale.US)
            sdf.format(Date())
        }
        return withFreshSession { uid, token, _ ->
            umsApi.getLeaderboard(my, uid, token).takeIf { it.isNotEmpty() }
        } ?: emptyList()
    }

    suspend fun getCalendarEvents(): List<CalendarEvent> {
        return withFreshSession { uid, token, _ ->
            umsApi.getCalendar(uid, token).takeIf { it.isNotEmpty() }
        } ?: emptyList()
    }

    suspend fun getEduRevCategories(): List<EduRevCategory> {
        return try {
            mobileApi.getEduRevCategories().item1 ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getEduRevCourses(categoryId: String): List<EduRevCourse> {
        return safeList { mobileApi.getEduRevCourses(categoryId) }
    }

    suspend fun getNewsPosts(): List<NewsPost> {
        return safeList { happeningsApi.getNewsPosts() }
    }

    // ───────────────────────────────────────────────────────────────────────────

    private suspend fun <T> safeList(block: suspend () -> List<T>): List<T> =
        try { block() } catch (e: Exception) { emptyList() }
}
