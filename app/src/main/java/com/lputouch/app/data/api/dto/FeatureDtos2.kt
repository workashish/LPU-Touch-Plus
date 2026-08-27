package com.lputouch.app.data.api.dto

import com.google.gson.annotations.SerializedName

/* ----------------------------- Fee Balance --------------------------------- */

data class FeeBalanceItem(
    @SerializedName("FeeHead") val feeHead: String? = null,
    @SerializedName("Amount") val amount: String? = null,
    @SerializedName("DueDate") val dueDate: String? = null,
    @SerializedName("Status") val status: String? = null,
    @SerializedName("Balance") val balance: String? = null,
    @SerializedName("CurrentBalance") val currentBalance: String? = null,
    @SerializedName("Error") val error: String? = null,
)

data class FeeExtensionItem(
    @SerializedName("feeHead") val feeHead: String? = null,
    @SerializedName("feeExtensionID") val feeExtensionId: String? = null,
    @SerializedName("isConsentGivenByStudent") val isConsentGiven: String? = null,
    @SerializedName("feeExtensionDate") val feeExtensionDate: String? = null,
    @SerializedName("consentGivenOnDateTime") val consentGivenOn: String? = null,
    @SerializedName("displayMessage") val displayMessage: String? = null,
    @SerializedName("askForConsent") val askForConsent: String? = null,
    @SerializedName("feeBalance") val feeBalance: String? = null,
)

/* ------------------------ Admission Documents ------------------------------ */

data class AdmissionDocument(
    @SerializedName("ApprovalStatus") val approvalStatus: String? = null,
    @SerializedName("CanStaffChange") val canStaffChange: String? = null,
    @SerializedName("CanStudentChange") val canStudentChange: String? = null,
    @SerializedName("DocumentDescription") val documentDescription: String? = null,
    @SerializedName("FileBase64") val fileBase64: String? = null,
    @SerializedName("DocumentId") val documentId: String? = null,
    @SerializedName("FileName") val fileName: String? = null,
    @SerializedName("FileType") val fileType: String? = null,
    @SerializedName("Remark") val remark: String? = null,
)

/* ------------------------ Exam Seating Plan -------------------------------- */

data class SeatingPlanItem(
    @SerializedName("ExamDate") val examDate: String? = null,
    @SerializedName("ExamTime") val examTime: String? = null,
    @SerializedName("CourseName") val courseName: String? = null,
    @SerializedName("CourseCode") val courseCode: String? = null,
    @SerializedName("ExamHall") val examHall: String? = null,
    @SerializedName("SeatNo") val seatNo: String? = null,
    @SerializedName("Block") val block: String? = null,
    @SerializedName("Row") val row: String? = null,
    @SerializedName("Column") val column: String? = null,
    @SerializedName("ExamType") val examType: String? = null,
    @SerializedName("Error") val error: String? = null,
)

/* ----------------------------- Library ------------------------------------- */

data class LibraryItem(
    @SerializedName("BookName") val bookName: String? = null,
    @SerializedName("Author") val author: String? = null,
    @SerializedName("IssueDate") val issueDate: String? = null,
    @SerializedName("DueDate") val dueDate: String? = null,
    @SerializedName("Fine") val fine: String? = null,
    @SerializedName("BookId") val bookId: String? = null,
    @SerializedName("Status") val status: String? = null,
    @SerializedName("Error") val error: String? = null,
)

/* ------------------------------ Bus Routes --------------------------------- */

data class BusRoute(
    @SerializedName("RouteNo") val routeNo: String? = null,
    @SerializedName("RouteName") val routeName: String? = null,
    @SerializedName("Stops") val stops: String? = null,
    @SerializedName("Timing") val timing: String? = null,
    @SerializedName("Driver") val driver: String? = null,
    @SerializedName("Contact") val contact: String? = null,
    @SerializedName("Error") val error: String? = null,
)

/* ------------------------- Hostel Leave ------------------------------------ */

data class HostelLeaveItem(
    @SerializedName("ApplicationDate") val applicationDate: String? = null,
    @SerializedName("FromDate") val fromDate: String? = null,
    @SerializedName("ToDate") val toDate: String? = null,
    @SerializedName("Status") val status: String? = null,
    @SerializedName("Reason") val reason: String? = null,
    @SerializedName("LeaveType") val leaveType: String? = null,
    @SerializedName("Balance") val balance: String? = null,
    @SerializedName("Error") val error: String? = null,
)

data class HostelLeaveBalance(
    @SerializedName("TotalLeaves") val totalLeaves: String? = null,
    @SerializedName("UsedLeaves") val usedLeaves: String? = null,
    @SerializedName("BalanceLeaves") val balanceLeaves: String? = null,
    @SerializedName("Error") val error: String? = null,
)

/* ----------------------- Phone Directory ----------------------------------- */

data class PhoneContact(
    @SerializedName("Name") val name: String? = null,
    @SerializedName("Designation") val designation: String? = null,
    @SerializedName("Department") val department: String? = null,
    @SerializedName("Phone") val phone: String? = null,
    @SerializedName("Email") val email: String? = null,
    @SerializedName("Extension") val extension: String? = null,
    @SerializedName("Error") val error: String? = null,
)

/* ------------------- Gamification Leaderboard ------------------------------ */

data class LeaderboardEntry(
    @SerializedName("StudentName") val studentName: String? = null,
    @SerializedName("RegistrationNo") val registrationNo: String? = null,
    @SerializedName("Score") val score: String? = null,
    @SerializedName("Rank") val rank: String? = null,
    @SerializedName("Program") val program: String? = null,
    @SerializedName("Section") val section: String? = null,
    @SerializedName("MonthYear") val monthYear: String? = null,
    @SerializedName("Error") val error: String? = null,
)

/* ------------------------ Academic Calendar -------------------------------- */

data class CalendarEvent(
    @SerializedName("EventDate") val eventDate: String? = null,
    @SerializedName("EventName") val eventName: String? = null,
    @SerializedName("EventType") val eventType: String? = null,
    @SerializedName("Description") val description: String? = null,
    @SerializedName("IsHoliday") val isHoliday: String? = null,
    @SerializedName("Error") val error: String? = null,
)

/* ----------------------- News / Happenings --------------------------------- */

data class NewsPost(
    @SerializedName("id") val id: Int? = null,
    @SerializedName("title") val title: NewsRendered? = null,
    @SerializedName("excerpt") val excerpt: NewsRendered? = null,
    @SerializedName("content") val content: NewsRendered? = null,
    @SerializedName("date") val date: String? = null,
    @SerializedName("link") val link: String? = null,
    @SerializedName("featured_media") val featuredMedia: Int? = null,
    @SerializedName("_embedded") val embedded: NewsEmbedded? = null,
)

data class NewsRendered(
    @SerializedName("rendered") val rendered: String? = null,
)

data class NewsEmbedded(
    @SerializedName("wp:featuredmedia") val featuredMedia: List<List<NewsMedia>>? = null,
)

data class NewsMedia(
    @SerializedName("source_url") val sourceUrl: String? = null,
)

/* --------------------- Attendance Detail ----------------------------------- */

data class AttendanceDetailItem(
    @SerializedName("AttendanceDate") val date: String? = null,
    @SerializedName("Day") val day: String? = null,
    @SerializedName("AttendanceTime") val time: String? = null,
    @SerializedName("AttendanceCode") val status: String? = null,
    @SerializedName("Coursecode") val courseCode: String? = null,
    @SerializedName("CourseName") val courseName: String? = null,
    @SerializedName("Name") val facultyName: String? = null,
    @SerializedName("Error") val error: String? = null,
)

/* -------------------- Pop-up Message --------------------------------------- */

data class PopupMessage(
    @SerializedName("Message") val message: String? = null,
    @SerializedName("Title") val title: String? = null,
    @SerializedName("Url") val url: String? = null,
    @SerializedName("IsActive") val isActive: String? = null,
)
