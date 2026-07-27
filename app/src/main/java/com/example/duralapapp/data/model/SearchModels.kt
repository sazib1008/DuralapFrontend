package com.example.duralapapp.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SearchResponse<T>(
    @Json(name = "items")
    val items: List<T>,
    @Json(name = "nextCursor")
    val nextCursor: String? = null
)
