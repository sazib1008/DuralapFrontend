package com.example.duralapapp.data.model

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import java.time.Instant
import java.time.format.DateTimeFormatter

class InstantAdapter {
    @ToJson
    fun toJson(value: Instant?): String? {
        return value?.toString()
    }

    @FromJson
    fun fromJson(value: String?): Instant? {
        if (value.isNullOrBlank()) return null
        return try {
            Instant.parse(value)
        } catch (e: Exception) {
            try {
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(value, Instant::from)
            } catch (e2: Exception) {
                null
            }
        }
    }
}
