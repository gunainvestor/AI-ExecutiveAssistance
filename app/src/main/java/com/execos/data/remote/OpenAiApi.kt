package com.execos.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

interface OpenAiApi {
    @POST("v1/chat/completions")
    suspend fun chatCompletions(@Body body: ChatCompletionRequest): ChatCompletionResponse
}
