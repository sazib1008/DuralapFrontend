package com.example.duralapapp.data.api

import com.example.duralapapp.data.model.AuthResponse
import com.example.duralapapp.data.model.SearchResponse
import com.example.duralapapp.data.model.UserResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface SearchApi {

    @GET("api/search/users")
    suspend fun searchUsers(
        @Query("q") query: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 20
    ): Response<SearchResponse<UserResponse>>
}
