package com.example.duralapapp.data.model

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import java.time.Instant
import java.time.format.DateTimeFormatter

class InstantAdapter {
    @ToJson
    fun toJson(writer: JsonWriter, value: Instant?) {
        if (value == null) {
            writer.nullValue()
        } else {
            writer.value(value.toString())
        }
    }

    @FromJson
    fun fromJson(reader: JsonReader): Instant? {
        if (reader.peek() == JsonReader.Token.NULL) {
            return reader.nextNull()
        }
        if (reader.peek() == JsonReader.Token.NUMBER) {
            val num = reader.nextDouble()
            return try {
                if (num > 100_000_000_000.0) {
                    Instant.ofEpochMilli(num.toLong())
                } else {
                    val sec = num.toLong()
                    val nano = ((num - sec) * 1_000_000_000).toLong()
                    Instant.ofEpochSecond(sec, nano)
                }
            } catch (e: Exception) {
                null
            }
        }
        val value = reader.nextString()
        if (value.isNullOrBlank()) return null
        return try {
            Instant.parse(value)
        } catch (e: Exception) {
            try {
                DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(value, Instant::from)
            } catch (e2: Exception) {
                try {
                    val epochLong = value.toLongOrNull()
                    if (epochLong != null) {
                        if (epochLong > 100_000_000_000L) Instant.ofEpochMilli(epochLong) else Instant.ofEpochSecond(epochLong)
                    } else {
                        null
                    }
                } catch (e3: Exception) {
                    null
                }
            }
        }
    }
}
