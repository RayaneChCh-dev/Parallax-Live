package com.example.parallaxlive.models

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface ClaudeApiService {
    @Headers(
        "Content-Type: application/json",
        "anthropic-version: 2023-06-01",
        "x-api-key: \${BuildConfig.CLAUDE_API_KEY}" // Reference from BuildConfig
    )
    @POST("v1/messages")
    suspend fun generateMessage(@Body request: ClaudeRequest): Response<ClaudeResponse>
}