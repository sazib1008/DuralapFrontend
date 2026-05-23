package com.example.duralapapp.data.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StaleWhileRevalidateInterceptor @Inject constructor() : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestWithCacheHint = if (request.method == "GET") {
            request.newBuilder()
                .header("Cache-Control", "public, max-age=60, stale-while-revalidate=300")
                .build()
        } else {
            request
        }

        val response = chain.proceed(requestWithCacheHint)
        if (request.method != "GET" || !response.isSuccessful) return response

        return response.newBuilder()
            .header("Cache-Control", "public, max-age=60, stale-while-revalidate=300")
            .build()
    }
}
