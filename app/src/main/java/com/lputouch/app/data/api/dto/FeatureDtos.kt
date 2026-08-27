package com.lputouch.app.data.api.dto

import com.google.gson.annotations.SerializedName

/* ------------------------------ Announcements ------------------------------ */

/** GET mobileapi /api/Announcement/GetAnnouncementDetails?AId={id}&tbl={tab} */
data class AnnouncementDetailResponse(
    @SerializedName("item1") val item1: List<AnnouncementDetail>? = null,
)

data class AnnouncementDetail(
    @SerializedName("announcement") val announcement: String? = null,
    @SerializedName("subject") val subject: String? = null,
    @SerializedName("entryDate") val entryDate: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("emp_name") val uploadedBy: String? = null,
    @SerializedName("file") val file: List<AnnouncementFile>? = null,
)

data class AnnouncementFile(
    @SerializedName("filepath") val filePath: String? = null,
    @SerializedName("fileName") val fileName: String? = null,
)

/* ------------------------------ My Messages ------------------------------- */

/** GET ums /StudentMyMessagesForService/{uid}/{token}/{deviceId} */
data class MessageItem(
    @SerializedName("Announcement") val announcement: String? = null,
    @SerializedName("Detail") val detail: String? = null,
    @SerializedName("Error") val error: String? = null,
    @SerializedName("Name") val name: String? = null,
    @SerializedName("RouteName") val routeName: String? = null,
    @SerializedName("SrNo") val srNo: String? = null,
    @SerializedName("Subject") val subject: String? = null,
    @SerializedName("TotalMessages") val totalMessages: String? = null,
)

/** POST ums /GetMyMessagesHistory -> {"GetMyMessagesHistoryResult": [...]} */
data class MessagesHistoryResponse(
    @SerializedName("GetMyMessagesHistoryResult") val result: List<MessageItem>? = null,
)

data class MessagesHistoryRequest(
    @SerializedName("UserId") val userId: String,
    @SerializedName("AccessToken") val accessToken: String,
    @SerializedName("DeviceId") val deviceId: String,
    @SerializedName("PageIndex") val pageIndex: Int = 1,
    @SerializedName("PageSize") val pageSize: Int = 50,
)

/* ------------------------------- Placement -------------------------------- */

/** GET ums /GetPlacementDetails/{uid}/{token}/{deviceId} */
data class PlacementDrive(
    @SerializedName("AttachmentDetails") val attachmentDetails: String? = null,
    @SerializedName("CandidateId") val candidateId: String? = null,
    @SerializedName("CompanyLink") val companyLink: String? = null,
    @SerializedName("CompanyName") val companyName: String? = null,
    @SerializedName("CompleteLastDate") val completeLastDate: String? = null,
    @SerializedName("Completedate") val completeDate: String? = null,
    @SerializedName("Date") val date: String? = null,
    @SerializedName("Designation") val designation: String? = null,
    @SerializedName("DriveCode") val driveCode: String? = null,
    @SerializedName("DriveID") val driveId: String? = null,
    @SerializedName("Id") val id: String? = null,
    @SerializedName("IsActive") val isActive: String? = null,
    @SerializedName("IsEligible") val isEligible: String? = null,
    @SerializedName("IsEligibleText") val isEligibleText: String? = null,
    @SerializedName("IsInterested") val isInterested: String? = null,
    @SerializedName("Lastdate") val lastDate: String? = null,
    @SerializedName("RegistrationOpen") val registrationOpen: String? = null,
    @SerializedName("RegistrationOpenText") val registrationOpenText: String? = null,
    @SerializedName("SalaryPackage") val salaryPackage: String? = null,
    @SerializedName("StreamName") val streamName: String? = null,
    @SerializedName("Venue") val venue: String? = null,
)

/** GET ums /GetJobProfile/{uid}/{token}/{deviceId}/{uid} */
data class JobProfileResponse(
    @SerializedName("jobProfile1") val jobProfile: JobProfile? = null,
)

data class JobProfile(
    @SerializedName("BatchYear") val batchYear: String? = null,
    @SerializedName("BondDetails") val bondDetails: String? = null,
    @SerializedName("CommunicationDate") val communicationDate: String? = null,
    @SerializedName("CompanyName") val companyName: String? = null,
    @SerializedName("Contact1Details") val contact1Details: String? = null,
    @SerializedName("Contact2Details") val contact2Details: String? = null,
    @SerializedName("DOJMonth") val dojMonth: String? = null,
    @SerializedName("DOJYear") val dojYear: String? = null,
    @SerializedName("Date") val date: String? = null,
    @SerializedName("DriveCode") val driveCode: String? = null,
    @SerializedName("DriveType") val driveType: String? = null,
    @SerializedName("DriveTypeName") val driveTypeName: String? = null,
    @SerializedName("Gender") val gender: String? = null,
    @SerializedName("IsAnyBond") val isAnyBond: String? = null,
    @SerializedName("LastDate") val lastDate: String? = null,
    @SerializedName("Provider") val provider: String? = null,
    @SerializedName("Requirements") val requirements: String? = null,
    @SerializedName("SalaryPackage") val salaryPackage: String? = null,
    @SerializedName("Venue") val venue: String? = null,
)

/* ------------------------- Mentor / RMS / Scores --------------------------- */

/** GET mobileapi /api/Parent/GetMentorMeetingRemarks -> {"item1": [...]} */
data class MentorRemarksResponse(
    @SerializedName("item1") val item1: List<MentorRemark>? = null,
)

data class MentorRemark(
    @SerializedName("student") val student: String? = null,
    @SerializedName("mentor") val mentor: String? = null,
    @SerializedName("attendanceStatus") val attendanceStatus: String? = null,
    @SerializedName("meetingDate") val meetingDate: String? = null,
    @SerializedName("remarks") val remarks: String? = null,
)

/** GET ums /GetRMSViewResponseForService/{uid}/{token}/{deviceId} */
data class RmsQuery(
    @SerializedName("Description") val description: String? = null,
    @SerializedName("Id") val id: String? = null,
    @SerializedName("OrginDate") val originDate: String? = null,
    @SerializedName("Rating") val rating: String? = null,
    @SerializedName("RatingStatus") val ratingStatus: String? = null,
    @SerializedName("Remarks") val remarks: String? = null,
    @SerializedName("Status") val status: String? = null,
    @SerializedName("Subject") val subject: String? = null,
    @SerializedName("TicketNo") val ticketNo: String? = null,
)

/** GET mobileapi /api/Student/GetAmcatScore + GetCoCubesScore */
data class AptitudeScore(
    @SerializedName("testName") val testName: String? = null,
    @SerializedName("english") val english: String? = null,
    @SerializedName("quantitativeAbility") val quantitativeAbility: String? = null,
    @SerializedName("logicalAbility") val logicalAbility: String? = null,
    @SerializedName("level") val level: String? = null,
)

/* -------------------------------- RPL -------------------------------------- */

/** GET mobileapi /api/Student/RPLResult */
data class RplResult(
    @SerializedName("termid") val termId: String? = null,
    @SerializedName("courseCode") val courseCode: String? = null,
    @SerializedName("courseName") val courseName: String? = null,
    @SerializedName("grade") val grade: String? = null,
    @SerializedName("rplDesc") val rplDesc: String? = null,
)

/* ------------------------------ Makeup / Adj ------------------------------ */

/** GET ums /GetMakeupandAdjustmentforStudents/{token}/{deviceId}/{uid} */
data class MakeupClass(
    // Fields matching actual UMS API response
    @SerializedName("CourseCode") val courseCode: String? = null,
    @SerializedName("LectureTime") val lectureTime: String? = null,
    @SerializedName("MakeupAdjustmentBy") val makeupBy: String? = null,
    @SerializedName("MakeupAdjustmentDate") val makeupDate: String? = null,
    @SerializedName("RoomNo") val roomNo: String? = null,
    @SerializedName("SectionNo") val sectionNo: String? = null,
    @SerializedName("AttendanceType") val attendanceType: String? = null,
    @SerializedName("Category") val category: String? = null,
    @SerializedName("GroupNo") val groupNo: String? = null,
    // Legacy fallback fields (in case server sends old format)
    @SerializedName("SubjectName") val subjectName: String? = null,
    @SerializedName("CourseName") val courseName: String? = null,
    @SerializedName("AttendanceDate") val attendanceDate: String? = null,
    @SerializedName("AttendanceTime") val attendanceTime: String? = null,
    @SerializedName("Description") val description: String? = null,
    @SerializedName("Day") val day: String? = null,
    @SerializedName("FacultyName") val facultyName: String? = null,
    @SerializedName("Type") val type: String? = null,
)

/* ------------------------------- EduRev ------------------------------------ */

/** GET mobileapi /api/EduRev/Categories + /api/EduRev/Courses/{id} */
data class EduRevResponse(
    @SerializedName("item1") val item1: List<EduRevCategory>? = null,
)

data class EduRevCategory(
    @SerializedName("categoryID") val categoryId: String? = null,
    @SerializedName("categoryTitle") val categoryTitle: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("lastDateforApplication") val lastDate: String? = null,
    @SerializedName("linkURL") val linkUrl: String? = null,
    @SerializedName("isActiveForApplication") val isActive: String? = null,
    @SerializedName("displayOrder") val displayOrder: String? = null,
)

data class EduRevCourse(
    @SerializedName("courseID") val courseId: String? = null,
    @SerializedName("courseTitle") val courseTitle: String? = null,
    @SerializedName("description") val description: String? = null,
    @SerializedName("linkURL") val linkUrl: String? = null,
)
