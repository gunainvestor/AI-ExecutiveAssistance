package com.execos.data.remote

import com.execos.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiService @Inject constructor(
    private val api: OpenAiApi,
) {
    suspend fun reflectionInsights(daySummary: String): Result<String> =
        complete(
            system = "You are a senior engineering leadership coach. Be concise and actionable.",
            user = "Analyze my day and suggest improvements as a senior engineering leader.\n\nWhat I did today:\n$daySummary\n\nRespond with: Key insights, Missed opportunities, Suggestions — use short bullet lists.",
        )

    suspend fun decisionEvaluation(
        title: String,
        context: String,
        options: String,
        decision: String,
        confidence: Int,
    ): Result<String> =
        complete(
            system = "You advise engineering leaders on decisions. Be direct and practical.",
            user = buildString {
                append("Evaluate this decision with trade-offs, risks, and better alternatives.\n\n")
                append("Title: $title\n")
                append("Context: $context\n")
                append("Options considered: $options\n")
                append("Final decision: $decision\n")
                append("Confidence (1-5): $confidence\n\n")
                append("Respond with sections: Risks, Trade-offs, Blind spots, Alternatives to consider.")
            },
        )

    suspend fun weeklySummary(
        completedTasksSummary: String,
        decisionsSummary: String,
        wins: String,
        mistakes: String,
        learnings: String,
    ): Result<String> =
        complete(
            system = "You coach engineering leaders on impact and patterns.",
            user = buildString {
                append("Identify patterns in my work and suggest how I can improve impact.\n\n")
                append("Completed tasks (week): $completedTasksSummary\n")
                append("Decisions (week): $decisionsSummary\n")
                append("Wins: $wins\n")
                append("Mistakes: $mistakes\n")
                append("Learnings: $learnings\n\n")
                append("Respond with: Patterns, Improvement suggestions — concise bullets.")
            },
        )

    private suspend fun complete(system: String, user: String): Result<String> {
        if (BuildConfig.OPENAI_API_KEY.isBlank()) {
            return Result.failure(IllegalStateException("Add OPENAI_API_KEY to local.properties"))
        }
        return try {
            val res = api.chatCompletions(
                ChatCompletionRequest(
                    messages = listOf(
                        ChatMessageDto(role = "system", content = system),
                        ChatMessageDto(role = "user", content = user),
                    ),
                ),
            )
            val text = res.choices?.firstOrNull()?.message?.content?.trim().orEmpty()
            if (text.isEmpty()) Result.failure(IllegalStateException("Empty AI response"))
            else Result.success(text)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
