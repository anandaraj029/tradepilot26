package com.example.data.ai

import java.util.UUID

/**
 * Chat message model for multi-turn conversation thread with Gemini.
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String, // "user" or "model"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val modelUsed: String = "gemini-3.5-flash"
)
