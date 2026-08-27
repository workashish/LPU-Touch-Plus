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
import com.lputouch.app.data.api.dto.ProfileSection
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
import android.content.Context
import com.lputouch.app.widget.TimetableWidgetReceiver

class StudentRepository(
    private val context: Context,
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
     * session, silently re-logins (refreshes both JWT + UMS AccessToken) and retries once.
     * Returns null on failure — never returns an expired/empty server object.
     */
    /**
     * Returns true if the throwable indicates a session expiry (401, expired JWT, etc.).
     * Other errors (520, 500, network timeouts) are NOT session issues.
     */
    private fun isSessionExpiredError(e: Throwable): Boolean {
        if (e.message == "SESSION_EXPIRED") return true
        if (e is retrofit2.HttpException) {
            val code = e.code()
            return code == 401 || code == 403
        }
        return false
    }

    private suspend fun <T> withFreshSession(block: suspend (uid: String, token: String, deviceId: String) -> T?): T? {
        val (uid, token, deviceId) = session()
        if (uid.isEmpty()) return null
        val first = try {
            block(uid, token, deviceId)
        } catch (e: Exception) {
            // Only treat 401 / explicit session expiry as a refresh trigger.
            // Other errors (520, 500, timeouts, etc.) must propagate so screens
            // can show the real error message instead of silently returning null.
            if (isSessionExpiredError(e)) null else throw e
        }
        if (first == null || isExpiredSession(first)) {
            if (authRepository.refreshSession()) {
                // Re-read session after refresh (JWT + AccessToken both updated)
                val (uid2, token2, deviceId2) = session()
                val retried = try {
                    block(uid2, token2, deviceId2)
                } catch (e: Exception) {
                    // If retry also fails with a non-session error, propagate it
                    if (isSessionExpiredError(e)) null else throw e
                }
                // Only accept the retry if it's real data, not another expired response.
                return if (retried != null && !isExpiredSession(retried)) retried else null
            }
            // Refresh failed — return null (UI shows empty state)
            return null
        }
        return first
    }

    /**
     * Detects server-side session expiry. UMS returns objects with a non-empty Error
     * field when the token expires. This checks ANY DTO that has an `error` field
     * via reflection, not just StudentBasicInfo.
     */
    private fun isExpiredSession(result: Any?): Boolean {
        return when (result) {
            is List<*> -> {
                val first = result.firstOrNull()
                first != null && hasErrorField(extractError(first))
            }
            else -> hasErrorField(extractError(result))
        }
    }

    /**
     * Generic error extraction: checks common field names used across UMS DTOs.
     * Returns the error string if found, null otherwise.
     */
    private fun extractError(obj: Any?): String? {
        if (obj == null) return null
        return try {
            val clazz = obj::class.java
            // Try common error field names
            for (fieldName in listOf("error", "Error")) {
                val field = clazz.getDeclaredField(fieldName)
                field.isAccessible = true
                val value = field.get(obj)
                if (value is String?) return value
            }
            null
        } catch (e: NoSuchFieldException) {
            null // DTO doesn't have an error field — that's fine
        } catch (e: Exception) {
            null
        }
    }

    private fun hasErrorField(err: String?): Boolean =
        !err.isNullOrBlank() && !err.equals("null", ignoreCase = true)

    /** Filters out items where the server returned an error (session expired placeholder). */
    private fun <T> filterExpired(items: List<T>): List<T> =
        items.filterNot { hasErrorField(extractError(it)) }

    suspend fun getProfile(): List<ProfileSection> {
        return withFreshSession { uid, token, deviceId ->
            umsApi.getProfile(token, deviceId, uid)
                .filterNot { hasErrorField(it.error) }
        } ?: emptyList()
    }

    suspend fun clearAllCache() {
        try {
            db.clearAllTables()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getStudentBasicInfo(): StudentBasicInfo? {
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
                            description = it.description ?: "",
                        )
                    }
                )
                TimetableWidgetReceiver.update(context)
            }
        }
        
        val attendanceMap = db.attendanceDao().getAll().associate { it.courseCode.trim().uppercase() to it.faculty }
        
        val regularItems = db.timetableDao().getAll().map {
            val cCode = (it.courseCode.takeIf { c -> c.isNotBlank() } 
                ?: Regex("(?:C:|Course:)\\s*([^ /]+)").find(it.description)?.groupValues?.get(1) 
                ?: "").trim().uppercase()
                
            val stitchedFaculty = it.facultyName.takeIf { f -> f.isNotBlank() } 
                ?: attendanceMap[cCode]?.takeIf { f -> f.isNotBlank() } 
                ?: ""

            TimetableItem(
                day = it.day,
                attendanceTime = it.attendanceTime,
                courseName = it.courseName,
                courseCode = it.courseCode,
                facultyName = stitchedFaculty,
                roomNo = it.roomNo,
                description = it.description,
            )
        }

        // Fetch makeup/adjustment classes and inject into timetable
        val makeupItems = try {
            getMakeupClasses().mapNotNull { makeup ->
                val dayOfWeek = parseMakeupDayOfWeek(makeup.makeupDate) ?: return@mapNotNull null
                val time = makeup.lectureTime ?: makeup.attendanceTime ?: return@mapNotNull null
                val courseCode = makeup.courseCode?.substringBefore(":")?.trim()
                    ?: return@mapNotNull null
                val courseName = makeup.courseCode?.substringAfter(":")?.trim() ?: ""
                val faculty = makeup.makeupBy?.substringBefore(":")?.trim()
                    ?: makeup.facultyName ?: ""
                val category = makeup.category?.takeIf { it.isNotBlank() } ?: makeup.type ?: ""
                val section = makeup.sectionNo?.takeIf { it.isNotBlank() } ?: ""

                TimetableItem(
                    day = dayOfWeek,
                    attendanceTime = time,
                    courseName = courseName,
                    courseCode = courseCode,
                    facultyName = faculty,
                    roomNo = makeup.roomNo ?: "",
                    description = "[MAKEUP] $category${if (section.isNotBlank()) " - Sec $section" else ""} (by $faculty)",
                )
            }
        } catch (_: Exception) { emptyList() }

        return regularItems + makeupItems
    }

    /**
     * Parses a makeup date string (MM/dd/yy or MM/dd/yyyy) and returns
     * the app day-of-week (1=Mon...7=Sun) if it falls within the current week.
     * Returns null if the date can't be parsed or is in the past.
     */
    private fun parseMakeupDayOfWeek(dateStr: String?): Int? {
        if (dateStr.isNullOrBlank()) return null
        return try {
            val parts = dateStr.split("/")
            if (parts.size < 3) return null
            val month = parts[0].toIntOrNull() ?: return null
            val day = parts[1].toIntOrNull() ?: return null
            val year = parts[2].toIntOrNull()?.let {
                if (it < 100) 2000 + it else it
            } ?: return null

            val cal = java.util.Calendar.getInstance()
            val makeupCal = java.util.Calendar.getInstance().apply {
                set(year, month - 1, day, 0, 0, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }

            // Only show if the makeup date is within ±7 days of today
            val diffMs = makeupCal.timeInMillis - cal.timeInMillis
            if (kotlin.math.abs(diffMs) > 7L * 24 * 60 * 60 * 1000) return null

            // Convert Java Calendar day (1=Sun) to app day (1=Mon...7=Sun)
            val javaDay = makeupCal.get(java.util.Calendar.DAY_OF_WEEK)
            if (javaDay == java.util.Calendar.SUNDAY) 7 else javaDay - 1
        } catch (_: Exception) {
            null
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
                            isNew = it.isNew == "1" || it.isNew.equals("true", ignoreCase = true),
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
                // Normalize to "true"/"false" string so UI badge check is reliable
                isNew = if (it.isNew) "true" else "false",
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
        return emptyList()
    }

    suspend fun getFeeExtensionPopup(): List<FeeExtensionItem> {
        return safeList { mobileApi.getFeeDateExtensionPopup() }
    }

    suspend fun getSeatingPlan(): List<SeatingPlanItem> {
        return emptyList()
    }

    suspend fun getLibraryData(): List<LibraryItem> {
        return emptyList()
    }

    suspend fun getBusRoutes(): List<BusRoute> {
        return emptyList()
    }

    suspend fun getHostelLeaveHistory(): List<HostelLeaveItem> {
        return emptyList()
    }

    suspend fun getHostelLeaveBalance(): HostelLeaveBalance? {
        return null
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
        return emptyList()
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
