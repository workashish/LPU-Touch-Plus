package com.lputouch.app.data.api.dto

import com.google.gson.annotations.SerializedName

/** POST /security/createToken request body */
data class CreateTokenRequest(
    @SerializedName("userName") val userName: String,
    @SerializedName("password") val password: String,
)

/** POST /security/createToken response */
data class CreateTokenResponse(
    @SerializedName("message") val message: String? = null,
    @SerializedName("status") val status: Boolean? = null,
    @SerializedName("token") val token: String? = null,
    @SerializedName("tokenParam") val tokenParam: String? = null,
)

/** GET /api/Student/PinExists response -> {"item1":[{"message":"askpin"}]} */
data class PinExistsResponse(
    @SerializedName("item1") val item1: List<PinMessage>? = null,
)

data class PinMessage(
    @SerializedName("message") val message: String? = null,
)

/** POST /umswebservice/umswebservice.svc/CheckPIN request body */
data class CheckPinRequest(
    @SerializedName("UserId") val userId: String,
    @SerializedName("DeviceId") val deviceId: String,
    @SerializedName("PIN") val pin: String,
    @SerializedName("PlayerId") val playerId: String,
    @SerializedName("DeviceType") val deviceType: String = "aphone",
    @SerializedName("AccessToken") val accessToken: String,
)

/** Menu item returned by MenuList / CheckPIN / GetTiles */
data class MenuItem(
    @SerializedName("AccessToken") val accessToken: String? = null,
    @SerializedName("ApplicationId") val applicationId: String? = null,
    @SerializedName("Component") val component: String? = null,
    @SerializedName("Description") val description: String? = null,
    @SerializedName("ImageName") val imageName: String? = null,
    @SerializedName("IsNew") val isNew: String? = null,
    @SerializedName("MenuId") val menuId: String? = null,
    @SerializedName("MenuText") val menuText: String? = null,
    @SerializedName("PageType") val pageType: String? = null,
    @SerializedName("RouteName") val routeName: String? = null,
    @SerializedName("Url") val url: String? = null,
    @SerializedName("UserType") val userType: String? = null,
    @SerializedName("lstChildMenus") val childMenus: List<MenuItem>? = null,
)

data class MenuListResponse(
    @SerializedName("MenuListResult") val menuListResult: List<MenuItem>? = null,
)

data class CheckPinResponse(
    @SerializedName("CheckPINResult") val checkPinResult: List<MenuItem>? = null,
)

/** POST /umswebservice/umswebservice.svc/PVR response — PVRResult is a JSON string of the menu list. */
data class PvrResponse(
    @SerializedName("PVRResult") val pvrResult: String? = null,
)

/** POST /umswebservice/umswebservice.svc/MenuList request body */
data class MenuListRequest(
    @SerializedName("AccessToken") val accessToken: String,
    @SerializedName("deviceId") val deviceId: String,
    @SerializedName("UserId") val userId: String,
    @SerializedName("password") val password: String,
    @SerializedName("Identity") val identity: String = "aphone",
)

/** POST /umswebservice/umswebservice.svc/Update request body (device registration) */
data class UpdateRequest(
    @SerializedName("UID") val uid: String,
    @SerializedName("AccessToken") val accessToken: String,
    @SerializedName("DeviceId") val deviceId: String,
    @SerializedName("PlayerId") val playerId: String,
)
