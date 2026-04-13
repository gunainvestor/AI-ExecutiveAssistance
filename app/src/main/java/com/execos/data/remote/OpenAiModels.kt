package com.execos.data.remote

import com.google.gson.annotations.SerializedName

data class ChatCompletionRequest(
    val model: String = "gpt-4o-mini",
    val messages: List<ChatMessageDto>,
    val temperature: Double = 0.7,
)

data class ChatMessageDto(
    val role: String,
    val content: String,
)

data class ChatCompletionResponse(
    val choices: List<ChoiceDto>? = null,
)

data class ChoiceDto(
    val message: ChatMessageDto? = null,
    @SerializedName("finish_reason") val finishReason: String? = null,
)
