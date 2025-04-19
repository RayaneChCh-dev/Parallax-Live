package com.example.parallaxlive.models

data class ClaudeRequest(
    val model: String,
    val messages: List<ClaudeMessage>,
    val max_tokens: Int,
    val temperature: Double = 0.7
)

data class ClaudeMessage(
    val role: String,
    val content: String
)

data class ClaudeResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<ClaudeContent>,
    val model: String,
    val usage: ClaudeUsage
)

data class ClaudeContent(
    val type: String,
    val text: String
)

data class ClaudeUsage(
    val input_tokens: Int,
    val output_tokens: Int
)