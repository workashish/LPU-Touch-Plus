package com.lputouch.app.data.api

import com.lputouch.app.data.api.dto.AnnouncementDetailResponse
import com.lputouch.app.data.api.dto.AptitudeScore
import com.lputouch.app.data.api.dto.CreateTokenRequest
import com.lputouch.app.data.api.dto.CreateTokenResponse
import com.lputouch.app.data.api.dto.EduRevCourse
import com.lputouch.app.data.api.dto.EduRevResponse
import com.lputouch.app.data.api.dto.MentorRemarksResponse
import com.lputouch.app.data.api.dto.PinExistsResponse
import com.lputouch.app.data.api.dto.RplResult
import com.lputouch.app.data.api.dto.StudentResultResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * REST API hosted on mobileapi.lpu.in (used for auth + modern endpoints).
 * Auth endpoints are unauthenticated; the rest require `Authorization: Bearer <JWT>`.
 */
interface MobileApi {

    @POST("security/createToken")
    suspend fun createToken(@Body body: CreateTokenRequest): CreateTokenResponse

    @GET("api/Student/PinExists")
    suspend fun pinExists(): PinExistsResponse

    @GET("api/Student/GetStudentResult")
    suspend fun getStudentResult(): List<StudentResultResponse>

    @GET("api/Student/RPLResult")
    suspend fun getRplResult(): List<RplResult>

    @GET("api/Student/GetYourDost")
    suspend fun getYourDost(): Map<String, Any?>

    @GET("api/Parent/GetMentorMeetingRemarks")
    suspend fun getMentorRemarks(): MentorRemarksResponse

    @GET("api/Student/GetAmcatScore")
    suspend fun getAmcatScore(): List<AptitudeScore>

    @GET("api/Student/GetCoCubesScore")
    suspend fun getCoCubesScore(): List<AptitudeScore>

    /** Full announcement body + attachments. Auth header is the UMS access token. */
    @GET("api/Announcement/GetAnnouncementDetails")
    suspend fun getAnnouncementDetail(
        @Query("AId") announcementId: String,
        @Query("tbl") tab: String,
    ): AnnouncementDetailResponse

    @GET("api/EduRev/Categories")
    suspend fun getEduRevCategories(): EduRevResponse

    @GET("api/EduRev/Courses/{categoryId}")
    suspend fun getEduRevCourses(@Path("categoryId") categoryId: String): List<EduRevCourse>
}
