package com.execos.data.remote

import com.google.gson.JsonObject
import com.google.gson.annotations.SerializedName

data class ChatCompletionRequest(
    val model: String = "gpt-4o-mini",
    val messages: List<ChatMessageDto>,
    val temperature: Double = 0.7,
    /** When true, response is SSE (not supported by Retrofit interface — use [OpenAiStreamingClient]). */
    val stream: Boolean? = null,
    val tools: List<ToolDefinition>? = null,
    @SerializedName("tool_choice") val toolChoice: String? = null,
)

data class ToolDefinition(
    val type: String = "function",
    val function: ToolFunctionDef,
)

data class ToolFunctionDef(
    val name: String,
    val description: String,
    val parameters: JsonObject,
)

data class ChatMessageDto(
    val role: String,
    val content: String? = null,
    @SerializedName("tool_calls") val toolCalls: List<ToolCallPart>? = null,
    @SerializedName("tool_call_id") val toolCallId: String? = null,
)

data class ToolCallPart(
    val id: String,
    val type: String,
    val function: FunctionCallPart,
)

data class FunctionCallPart(
    val name: String,
    val arguments: String,
)

data class ChatCompletionResponse(
    val choices: List<ChoiceDto>? = null,
)

data class ChoiceDto(
    val message: ChatMessageDto? = null,
    @SerializedName("finish_reason") val finishReason: String? = null,
)

// —— Streaming (SSE) chunks ——
data class StreamChunk(
    val choices: List<StreamChoice>? = null,
)

data class StreamChoice(
    val delta: StreamDelta? = null,
)

data class StreamDelta(
    val content: String? = null,
)
