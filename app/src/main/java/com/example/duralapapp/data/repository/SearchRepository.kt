package com.example.duralapapp.data.repository

import com.example.duralapapp.data.api.SearchApi
import com.example.duralapapp.data.api.safeApiCall
import com.example.duralapapp.data.model.SearchResponse
import com.example.duralapapp.data.model.UserResponse
import javax.inject.Inject
import javax.inject.Singleton

interface SearchRepository {
    suspend fun searchUsers(query: String, cursor: String? = null, limit: Int = 20): Result<SearchResponse<UserResponse>>
}

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val searchApi: SearchApi
) : SearchRepository {

    override suspend fun searchUsers(
        query: String,
        cursor: String?,
        limit: Int
    ): Result<SearchResponse<UserResponse>> {
        return safeApiCall {
            searchApi.searchUsers(query = query, cursor = cursor, limit = limit)
        }
    }
}
