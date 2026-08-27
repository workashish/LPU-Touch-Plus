package com.lputouch.app.data.api

import com.lputouch.app.data.api.dto.AnnouncementDetailResponse
import com.lputouch.app.data.api.dto.AptitudeScore
import com.lputouch.app.data.api.dto.CreateTokenRequest
import com.lputouch.app.data.api.dto.CreateTokenResponse
import com.lputouch.app.data.api.dto.EduRevCourse
import com.lputouch.app.data.api.dto.EduRevResponse
import com.lputouch.app.data.api.dto.FeeExtensionItem
import com.lputouch.app.data.api.dto.MentorRemarksResponse
import com.lputouch.app.data.api.dto.NewsPost
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

    @GET("api/Student/PlacementUnderTaking")
    suspend fun getPlacementUndertaking(): Map<String, Any?>

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

    /** Fee extension popup — returns list; first item has the display message. */
    @GET("api/FeeExt/GetFeeDateExtensionPopup")
    suspend fun getFeeDateExtensionPopup(): List<FeeExtensionItem>
}

/**
 * WordPress REST API for Happenings/News feed at happenings.lpu.in.
 * Uses a separate Retrofit instance (different base URL).
 */
interface HappeningsApi {
    @GET("wp-json/wp/v2/posts")
    suspend fun getNewsPosts(
        @Query("per_page") perPage: Int = 20,
        @Query("page") page: Int = 1,
        @Query("_embed") embed: Boolean = true,
    ): List<NewsPost>
}
