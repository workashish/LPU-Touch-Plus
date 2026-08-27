package com.lputouch.app

import com.lputouch.app.data.api.ApiClient
import com.lputouch.app.data.api.dto.LoginRequest
import com.lputouch.app.data.api.dto.StudentBaseRequest
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.UUID

class ApiTest {

    @Test
    fun testLoginAndFetchData() = runBlocking {
        println("Starting API test...")
        
        // Use a dummy session store that we don't need for the ApiClient creation if we provide manual headers,
        // or just manually build a Retrofit instance for testing.
        // For simplicity, we'll build our own instances here to avoid mocking SessionStore.
        
        val retrofitMobile = retrofit2.Retrofit.Builder()
            .baseUrl("https://mobileapi.lpu.in/")
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            
        val mobileApi = retrofitMobile.create(com.lputouch.app.data.api.MobileApi::class.java)

        val retrofitUms = retrofit2.Retrofit.Builder()
            .baseUrl("https://ums.lpu.in/RestWebApi/api/")
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            
        val umsApi = retrofitUms.create(com.lputouch.app.data.api.UmsApi::class.java)
        
        val credentials = listOf("12323758" to "Nidhi@2000", "12607696" to "Nidhi@2000")
        
        for ((userId, password) in credentials) {
            println("\n=============================================")
            println("Testing account: $userId")
            
            try {
                // 1. Login to Mobile API (JWT)
                val jwtResponse = mobileApi.login(LoginRequest(userId = userId, password = password))
                println("JWT Login success: ${jwtResponse.isSuccessful}")
                val jwtToken = jwtResponse.body()?.token ?: throw Exception("Failed to get JWT token")
                println("JWT Token retrieved successfully")
                
                // 2. Login to UMS API (WCF)
                val deviceId = UUID.randomUUID().toString()
                val wcfResponse = umsApi.login(userId, password, deviceId)
                val wcfToken = wcfResponse.firstOrNull()?.token ?: throw Exception("Failed to get WCF token")
                println("WCF Token retrieved successfully")
                
                // 3. Test Profile API
                val profile = mobileApi.getStudentInfo(StudentBaseRequest(userId = userId), "Bearer $jwtToken")
                println("Profile fetched: ${profile.studentName}, Program: ${profile.programName}")
                
                // 4. Test Attendance API
                val attendance = umsApi.getAttendance(userId, wcfToken, deviceId)
                println("Attendance records fetched: ${attendance.size}")
                if (attendance.isNotEmpty()) {
                    println(" - First record: ${attendance.first().subjectName} (${attendance.first().totalAttendance})")
                }
                
                // 5. Test Timetable API
                val timetable = umsApi.getTimetable(userId, wcfToken, deviceId)
                println("Timetable records fetched: ${timetable.size}")
                
                // 6. Test Fee Balance
                val fee = mobileApi.getFeeBalancePopup(StudentBaseRequest(userId = userId), "Bearer $jwtToken")
                println("Fee balance records: ${fee.item1?.size ?: 0}")
                
                // 7. Test Documents
                val docs = mobileApi.getAdmissionDocuments(StudentBaseRequest(userId = userId), "Bearer $jwtToken")
                println("Admission documents fetched: ${docs.item1?.size ?: 0}")
                
                println("Account $userId passed all checks! ✅")
                
            } catch (e: Exception) {
                println("Error testing account $userId: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
