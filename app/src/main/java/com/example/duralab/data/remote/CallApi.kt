package com.example.duralab.data.remote

import com.example.duralab.data.model.CallInitiateRequest
import com.example.duralab.data.model.CallResponse
import retrofit2.Response
import retrofit2.http.*

interface CallApi {
    @POST("api/calls/initiate")
    suspend fun initiateCall(@Body request: CallInitiateRequest): Response<CallResponse>

    @POST("api/calls/{callId}/accept")
    suspend fun acceptCall(
        @Path("callId") callId: String,
        @Query("userId") userId: String
    ): Response<CallResponse>

    @POST("api/calls/{callId}/reject")
    suspend fun rejectCall(
        @Path("callId") callId: String,
        @Query("userId") userId: String
    ): Response<CallResponse>

    @POST("api/calls/{callId}/end")
    suspend fun endCall(
        @Path("callId") callId: String,
        @Query("userId") userId: String
    ): Response<CallResponse>

    @GET("api/calls/{callId}")
    suspend fun getCallById(@Path("callId") callId: String): Response<CallResponse>

    @GET("api/calls/active/{userId}")
    suspend fun getActiveCallsForUser(@Path("userId") userId: String): Response<List<CallResponse>>

    @GET("api/calls/recent/{userId}")
    suspend fun getRecentCallsForUser(
        @Path("userId") userId: String,
        @Query("limit") limit: Int = 20
    ): Response<List<CallResponse>>

    @GET("api/calls/missed/{userId}")
    suspend fun getMissedCallsForUser(@Path("userId") userId: String): Response<List<CallResponse>>

    @GET("api/calls/history")
    suspend fun getCallHistory(
        @Query("user1Id") user1Id: String,
        @Query("user2Id") user2Id: String,
        @Query("limit") limit: Int = 50
    ): Response<List<CallResponse>>

    @GET("api/calls/history/user/{userId}")
    suspend fun getCallHistoryList(
        @Path("userId") userId: String,
        @Query("limit") limit: Int = 50
    ): Response<List<com.example.duralab.data.model.CallHistoryItemResponse>>

    @GET("api/calls/ongoing")
    suspend fun getOngoingCalls(): Response<List<CallResponse>>

    @PATCH("api/calls/{callId}/signaling")
    suspend fun updateCallWithSignaling(
        @Path("callId") callId: String,
        @Query("offer") offer: String? = null,
        @Query("answer") answer: String? = null,
        @Query("iceCandidates") iceCandidates: List<String>? = null
    ): Response<CallResponse>

    @GET("api/calls/stats/{userId}")
    suspend fun getCallStats(@Path("userId") userId: String): Response<Map<String, Any>>
}
