package com.example.duralapapp.data.api

import com.example.duralapapp.data.model.BatchPresenceRequest
import com.example.duralapapp.data.model.UserPresenceEvent
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface PresenceApi {

    @GET("api/presence/{userId}")
    suspend fun getUserPresence(
        @Path("userId") userId: String
    ): Response<UserPresenceEvent>

    @POST("api/presence/batch")
    suspend fun getBatchPresence(
        @Body request: BatchPresenceRequest
    ): Response<List<UserPresenceEvent>>

    @GET("api/presence/online/{userId}")
    suspend fun checkIsUserOnline(
        @Path("userId") userId: String
    ): Response<Map<String, Boolean>>
}
