package com.lputouch.app.data.api.dto

import com.google.gson.annotations.SerializedName

/** GET /umswebservice.svc/StudentBasicInfoForService/{uid}/{token}/{deviceId}/null/null */
data class StudentBasicInfo(
    @SerializedName("AdmissionSession") val admissionSession: String? = null,
    @SerializedName("AggAttendance") val aggAttendance: String? = null,
    @SerializedName("AnnouncementCount") val announcementCount: String? = null,
    @SerializedName("AssignmentCount") val assignmentCount: String? = null,
    @SerializedName("BatchYear") val batchYear: String? = null,
    @SerializedName("CGPA") val cgpa: String? = null,
    @SerializedName("CategoryCode") val categoryCode: String? = null,
    @SerializedName("CorrespondingAddress") val correspondingAddress: Address? = null,
    @SerializedName("CoursesCount") val coursesCount: String? = null,
    @SerializedName("CurrentBalance") val currentBalance: String? = null,
    @SerializedName("DateofBirth") val dateOfBirth: String? = null,
    @SerializedName("FatherName") val fatherName: String? = null,
    @SerializedName("FeeText") val feeText: String? = null,
    @SerializedName("Gender") val gender: String? = null,
    @SerializedName("Hostel") val hostel: String? = null,
    @SerializedName("IsBirthdayToday") val isBirthdayToday: Boolean? = null,
    @SerializedName("IsFee") val isFee: String? = null,
    @SerializedName("IsFeedbackPending") val isFeedbackPending: String? = null,
    @SerializedName("IsOdl") val isOdl: Boolean? = null,
    @SerializedName("MotherName") val motherName: String? = null,
    @SerializedName("MyMessagesCount") val myMessagesCount: String? = null,
    @SerializedName("Nationality") val nationality: String? = null,
    @SerializedName("PermanentAddress") val permanentAddress: Address? = null,
    @SerializedName("Picture") val picture: String? = null,
    @SerializedName("ProgramName") val programName: String? = null,
    @SerializedName("RegisterationNumber") val registrationNumber: String? = null,
    @SerializedName("Section") val section: String? = null,
    @SerializedName("StudentEmail") val studentEmail: String? = null,
    @SerializedName("StudentMobile") val studentMobile: String? = null,
    @SerializedName("StudentName") val studentName: String? = null,
    @SerializedName("StudentPicture") val studentPicture: String? = null,
    @SerializedName("StudentStatus") val studentStatus: String? = null,
    @SerializedName("Error") val error: String? = null,
    @SerializedName("TermId") val termId: String? = null,
    // Server sends a JSON array here (may be empty); unused by the app.
    @SerializedName("TimeTable") val timetable: Any? = null,
)

data class Address(
    @SerializedName("CityName") val cityName: String? = null,
    @SerializedName("Colony") val colony: String? = null,
    @SerializedName("CountryName") val countryName: String? = null,
    @SerializedName("DistrictName") val districtName: String? = null,
    @SerializedName("HNo_Building") val houseNo: String? = null,
    @SerializedName("PinCode") val pinCode: String? = null,
    @SerializedName("StateName") val stateName: String? = null,
)

/** GET /api/Student/GetStudentResult */
data class StudentResultResponse(
    @SerializedName("result") val result: List<ResultItem>? = null,
    @SerializedName("semester") val semester: String? = null,
)

data class ResultItem(
    @SerializedName("romanTerm") val romanTerm: String? = null,
    @SerializedName("detailTerm") val detailTerm: String? = null,
    @SerializedName("termid") val termId: String? = null,
    @SerializedName("courseCode") val courseCode: String? = null,
    @SerializedName("course") val course: String? = null,
    @SerializedName("gradeOrMarks") val gradeOrMarks: String? = null,
    @SerializedName("termPercentOrTGPA") val termPercentOrTGPA: String? = null,
    @SerializedName("termPercentOrCGPA") val termPercentOrCGPA: String? = null,
    @SerializedName("gradeExplanation") val gradeExplanation: String? = null,
    @SerializedName("failGradeDescription") val failGradeDescription: String? = null,
    @SerializedName("gradeColor") val gradeColor: String? = null,
    @SerializedName("mulFactor") val mulFactor: String? = null,
)

/** GET /umswebservice.svc/StudentAttendanceForServiceNew/{uid}/{token}/{deviceId} */
data class AttendanceItem(
    @SerializedName("AttendanceDay") val attendanceDay: String? = null,
    @SerializedName("AttendanceTime") val attendanceTime: String? = null,
    @SerializedName("CourseCode") val courseCode: String? = null,
    @SerializedName("CourseName") val courseName: String? = null,
    @SerializedName("Description") val description: String? = null,
    @SerializedName("Day") val day: Int? = null,
    @SerializedName("SubjectName") val subjectName: String? = null,
    @SerializedName("TotalAttendance") val totalAttendance: String? = null,
    @SerializedName("TheoryAttendance") val theoryAttendance: String? = null,
    @SerializedName("PracticalAttendance") val practicalAttendance: String? = null,
)

/** GET /umswebservice.svc/StudentTimeTableForService/{uid}/{token}/{deviceId} */
data class TimetableItem(
    @SerializedName("AttendanceDay") val attendanceDay: String? = null,
    @SerializedName("AttendanceTime") val attendanceTime: String? = null,
    @SerializedName("CourseCode") val courseCode: String? = null,
    @SerializedName("CourseName") val courseName: String? = null,
    @SerializedName("Day") val day: Int? = null,
    @SerializedName("Description") val description: String? = null,
    @SerializedName("FacultyName") val facultyName: String? = null,
    @SerializedName("RoomNo") val roomNo: String? = null,
    @SerializedName("SubjectName") val subjectName: String? = null,
)

/** GET /umswebservice.svc/GetAnnouncementsForServiceNew/{uid}/{token}/{deviceId}/S */
data class Announcement(
    @SerializedName("AnnouncementId") val announcementId: String? = null,
    @SerializedName("Category") val category: String? = null,
    @SerializedName("Description") val description: String? = null,
    @SerializedName("DisplayByOrder") val displayByOrder: String? = null,
    @SerializedName("EntryDate") val entryDate: String? = null,
    @SerializedName("IsNew") val isNew: String? = null,
    @SerializedName("IsOdl") val isOdl: Boolean? = null,
    @SerializedName("RoleType") val roleType: String? = null,
    @SerializedName("Subject") val subject: String? = null,
    @SerializedName("Tab") val tab: String? = null,
    @SerializedName("Type") val type: String? = null,
    @SerializedName("UploadedBy") val uploadedBy: String? = null,
)
