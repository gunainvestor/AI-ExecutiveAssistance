package com.execos.data.remote

import com.execos.BuildConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Server-Sent Events streaming for Chat Completions.
 * Parses OpenAI `data: {...}` lines and emits text deltas from [StreamDelta.content].
 */
@Singleton
class OpenAiStreamingClient @Inject constructor(
    private val client: OkHttpClient,
    private val gson: Gson,
) {
    fun streamChat(request: ChatCompletionRequest): Flow<String> = callbackFlow {
        if (BuildConfig.OPENAI_API_KEY.isBlank()) {
            close(IllegalStateException("Add OPENAI_API_KEY to local.properties"))
            return@callbackFlow
        }
        val bodyJson = gson.toJson(request.copy(stream = true))
        val httpReq = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .post(bodyJson.toRequestBody(JSON))
            .addHeader("Authorization", "Bearer ${BuildConfig.OPENAI_API_KEY}")
            .addHeader("Accept", "text/event-stream")
            .build()

        try {
            withContext(Dispatchers.IO) {
                client.newCall(httpReq).execute().use { response ->
                    if (!response.isSuccessful) {
                        val err = response.body?.string().orEmpty()
                        throw IllegalStateException(
                            "OpenAI HTTP ${response.code}: ${err.take(500)}",
                        )
                    }
                    response.body?.charStream()?.buffered()?.useLines { lines ->
                        lines.forEach { line ->
                            if (!line.startsWith("data: ")) return@forEach
                            val data = line.removePrefix("data: ").trim()
                            if (data == "[DONE]") return@forEach
                            try {
                                val chunk = gson.fromJson(data, StreamChunk::class.java)
                                val text = chunk.choices?.firstOrNull()?.delta?.content
                                if (!text.isNullOrEmpty()) {
                                    trySend(text)
                                }
                            } catch (_: Exception) {
                                // ignore malformed chunk lines
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            close(e)
            return@callbackFlow
        }
        // Do not use awaitClose() here: it would suspend forever after the body is read,
        // so collectors never complete and callers (e.g. reflection "busy") never clear.
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
