package com.lputouch.app.data.api

import com.lputouch.app.data.api.dto.Announcement
import com.lputouch.app.data.api.dto.AttendanceItem
import com.lputouch.app.data.api.dto.CheckPinRequest
import com.lputouch.app.data.api.dto.CheckPinResponse
import com.lputouch.app.data.api.dto.MakeupClass
import com.lputouch.app.data.api.dto.MenuListRequest
import com.lputouch.app.data.api.dto.MenuListResponse
import com.lputouch.app.data.api.dto.MessageItem
import com.lputouch.app.data.api.dto.MessagesHistoryRequest
import com.lputouch.app.data.api.dto.MessagesHistoryResponse
import com.lputouch.app.data.api.dto.PlacementDrive
import com.lputouch.app.data.api.dto.PvrResponse
import com.lputouch.app.data.api.dto.RmsQuery
import com.lputouch.app.data.api.dto.StudentBasicInfo
import com.lputouch.app.data.api.dto.TimetableItem
import com.lputouch.app.data.api.dto.UpdateRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Legacy WCF service hosted on ums.lpu.in/umswebservice/umswebservice.svc.
 * Auth is passed via URL path segments: {userId}/{accessToken}/{deviceId}.
 */
interface UmsApi {

    @POST("umswebservice/umswebservice.svc/Update")
    suspend fun update(@Body body: UpdateRequest): String

    /** Encrypted login — response contains the real UMS AccessToken. */
    @POST("umswebservice/umswebservice.svc/PVR")
    suspend fun pvr(@Body body: Map<String, String>): PvrResponse

    @POST("umswebservice/umswebservice.svc/MenuList")
    suspend fun menuList(@Body body: MenuListRequest): MenuListResponse

    @POST("umswebservice/umswebservice.svc/CheckPIN")
    suspend fun checkPin(@Body body: CheckPinRequest): CheckPinResponse

    @GET("umswebservice/umswebservice.svc/StudentBasicInfoForService/{userId}/{accessToken}/{deviceId}/null/null")
    suspend fun studentBasicInfo(
        @Path("userId") userId: String,
        @Path("accessToken") accessToken: String,
        @Path("deviceId") deviceId: String,
    ): List<StudentBasicInfo>

    @GET("umswebservice/umswebservice.svc/GetTiles/{userId}/{accessToken}/{deviceId}")
    suspend fun getTiles(
        @Path("userId") userId: String,
        @Path("accessToken") accessToken: String,
        @Path("deviceId") deviceId: String,
    ): List<com.lputouch.app.data.api.dto.MenuItem>

    @GET("umswebservice/umswebservice.svc/GetAnnouncementsForServiceNew/{userId}/{accessToken}/{deviceId}/S")
    suspend fun getAnnouncements(
        @Path("userId") userId: String,
        @Path("accessToken") accessToken: String,
        @Path("deviceId") deviceId: String,
    ): List<Announcement>

    @GET("umswebservice/umswebservice.svc/StudentAttendanceForServiceNew/{userId}/{accessToken}/{deviceId}")
    suspend fun getAttendance(
        @Path("userId") userId: String,
        @Path("accessToken") accessToken: String,
        @Path("deviceId") deviceId: String,
    ): List<AttendanceItem>

    @GET("umswebservice/umswebservice.svc/StudentTimeTableForService/{userId}/{accessToken}/{deviceId}")
    suspend fun getTimetable(
        @Path("userId") userId: String,
        @Path("accessToken") accessToken: String,
        @Path("deviceId") deviceId: String,
    ): List<TimetableItem>

    @GET("umswebservice/umswebservice.svc/Profile/{accessToken}/{deviceId}/{userId}")
    suspend fun getProfile(
        @Path("accessToken") accessToken: String,
        @Path("deviceId") deviceId: String,
        @Path("userId") userId: String,
    ): List<StudentBasicInfo>

    @GET("umswebservice/umswebservice.svc/StudentMyMessagesForService/{userId}/{accessToken}/{deviceId}")
    suspend fun getMyMessages(
        @Path("userId") userId: String,
        @Path("accessToken") accessToken: String,
        @Path("deviceId") deviceId: String,
    ): List<MessageItem>

    @POST("umswebservice/umswebservice.svc/GetMyMessagesHistory")
    suspend fun getMyMessagesHistory(@Body body: MessagesHistoryRequest): MessagesHistoryResponse

    @GET("umswebservice/umswebservice.svc/GetPlacementDetails/{userId}/{accessToken}/{deviceId}")
    suspend fun getPlacementDetails(
        @Path("userId") userId: String,
        @Path("accessToken") accessToken: String,
        @Path("deviceId") deviceId: String,
    ): List<PlacementDrive>

    @GET("umswebservice/umswebservice.svc/GetJobProfile/{userId}/{accessToken}/{deviceId}/{userId}")
    suspend fun getJobProfile(
        @Path("userId") userId: String,
        @Path("accessToken") accessToken: String,
        @Path("deviceId") deviceId: String,
    ): List<com.lputouch.app.data.api.dto.JobProfileResponse>

    @GET("umswebservice/umswebservice.svc/GetRMSViewResponseForService/{userId}/{accessToken}/{deviceId}")
    suspend fun getRmsQueries(
        @Path("userId") userId: String,
        @Path("accessToken") accessToken: String,
        @Path("deviceId") deviceId: String,
    ): List<RmsQuery>

    @GET("umswebservice/umswebservice.svc/GetMakeupandAdjustmentforStudents/{accessToken}/{deviceId}/{userId}")
    suspend fun getMakeupClasses(
        @Path("accessToken") accessToken: String,
        @Path("deviceId") deviceId: String,
        @Path("userId") userId: String,
    ): List<MakeupClass>
}
