package com.execos.data.remote

import com.execos.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiService @Inject constructor(
    private val api: OpenAiApi,
    private val ragContextProvider: RagContextProvider,
    private val streamClient: OpenAiStreamingClient,
    private val toolExecutor: ExecutiveToolExecutor,
) {
    /**
     * Non-streaming reflection (optional fallback).
     */
    suspend fun reflectionInsights(daySummary: String): Result<String> =
        complete(
            system = "You are a senior engineering leadership coach. Be concise and actionable.",
            user = "Analyze my day and suggest improvements as a senior engineering leader.\n\nWhat I did today:\n$daySummary\n\nRespond with: Key insights, Missed opportunities, Suggestions — use short bullet lists.",
        )

    /**
     * Streaming reflection with the same prompts as [reflectionInsights], plus RAG context.
     */
    fun reflectionInsightsStream(daySummary: String): Flow<String> = flow {
        val system = "You are a senior engineering leadership coach. Be concise and actionable."
        val userPrompt =
            "Analyze my day and suggest improvements as a senior engineering leader.\n\nWhat I did today:\n$daySummary\n\nRespond with: Key insights, Missed opportunities, Suggestions — use short bullet lists."
        val rag = ragContextProvider.buildContextBlock()
        val userWithRag = if (rag.isBlank()) {
            userPrompt
        } else {
            "---\nContext from your ExecOS journal:\n$rag\n---\n\n$userPrompt"
        }
        val request = ChatCompletionRequest(
            messages = listOf(
                ChatMessageDto(role = "system", content = system),
                ChatMessageDto(role = "user", content = userWithRag),
            ),
            temperature = 0.7,
        )
        emitAll(streamClient.streamChat(request))
    }

    suspend fun decisionEvaluation(
        title: String,
        context: String,
        options: String,
        decision: String,
        confidence: Int,
    ): Result<String> {
        val system = "You advise engineering leaders on decisions. Be direct and practical."
        val user = buildString {
            append("Evaluate this decision with trade-offs, risks, and better alternatives.\n\n")
            append("Title: $title\n")
            append("Context: $context\n")
            append("Options considered: $options\n")
            append("Final decision: $decision\n")
            append("Confidence (1-5): $confidence\n\n")
            append("Respond with sections: Risks, Trade-offs, Blind spots, Alternatives to consider.")
            append(" You may call tools to read the user's recent decisions, today's priorities, or reflections if helpful.")
        }
        return runToolChatWithRag(system = system, user = user)
    }

    suspend fun weeklySummary(
        completedTasksSummary: String,
        decisionsSummary: String,
        plannedGoalsSummary: String,
        wins: String,
        mistakes: String,
        learnings: String,
    ): Result<String> {
        val system = "You coach engineering leaders on impact and patterns."
        val user = buildString {
            append("Identify patterns in my work and suggest how I can improve impact.\n\n")
            append("Completed tasks (week): $completedTasksSummary\n")
            append("Decisions (week): $decisionsSummary\n")
            append("Planned goals (quarter/month/week): $plannedGoalsSummary\n")
            append("Wins: $wins\n")
            append("Mistakes: $mistakes\n")
            append("Learnings: $learnings\n\n")
            append("Respond with: Alignment verdict (Aligned/Partially aligned/Not aligned), Evidence, Gaps, Improvement suggestions.")
            append(" You may call tools to read recent decisions, today's priorities, or reflections if helpful.")
        }
        return runToolChatWithRag(system = system, user = user)
    }

    suspend fun usageCoach(
        minutesToday: Int,
        minutesThisWeek: Int,
        priorityTitles: List<String>,
        weekGoals: List<String>,
        monthGoals: List<String>,
        quarterGoals: List<String>,
    ): Result<String> {
        val system = "You are a friendly, high-agency productivity coach. Be concise, encouraging, and practical."
        val user = buildString {
            append("I spent ")
            append(minutesToday)
            append(" minutes today (and ")
            append(minutesThisWeek)
            append(" minutes this week) on social media + web browsing. ")
            append("Help me reframe this without shame.\n\n")
            append("My goals (in priority order):\n")
            append("Week goals:\n")
            if (weekGoals.isEmpty()) append("- (none)\n") else weekGoals.take(3).forEach { append("- ").append(it).append('\n') }
            append("Month goals:\n")
            if (monthGoals.isEmpty()) append("- (none)\n") else monthGoals.take(3).forEach { append("- ").append(it).append('\n') }
            append("Quarter goals:\n")
            if (quarterGoals.isEmpty()) append("- (none)\n") else quarterGoals.take(3).forEach { append("- ").append(it).append('\n') }
            append("\nDecision rule:\n")
            append("- Prefer WEEK goals first (most actionable), then MONTH, then QUARTER.\n")
            append("- If no goals exist, base suggestions on today's priorities.\n\n")
            append("My prioritized activities for today are:\n")
            if (priorityTitles.isEmpty()) {
                append("- (none set)\n")
            } else {
                priorityTitles.take(3).forEach { append("- ").append(it).append('\n') }
            }
            append("\nReturn ONLY valid minified JSON (no markdown) with this schema:\n")
            append("{")
            append("\"emojiHeadline\":\"(6-12 emoji chars)\",")
            append("\"realityCheck\":\"(max 16 words)\",")
            append("\"focusGoal\":\"(single chosen goal title)\",")
            append("\"focusHorizon\":\"(WEEK|MONTH|QUARTER|TODAY)\",")
            append("\"timeCouldHaveBeen\":[{\"emoji\":\"\",\"title\":\"\",\"minutes\":0}],")
            append("\"next2Hours\":[{\"emoji\":\"\",\"title\":\"\",\"minutes\":0}]")
            append("}\n")
            append("Rules:\n")
            append("- Provide exactly 3 items in timeCouldHaveBeen and 3 items in next2Hours.\n")
            append("- Keep titles <= 5 words each.\n")
            append("- Minutes should be realistic and sum roughly to min(120, minutesToday).\n")
            append("- Use emojis that match the action.\n")
            append("- Every item should clearly support the focusGoal.\n")
        }
        return complete(system = system, user = user)
    }

    suspend fun endOfDayFeedback(
        daySummary: String,
        minutesSocialAndBrowsingToday: Int,
    ): Result<String> {
        val system = "You are an executive coach. Be kind, direct, and actionable."
        val user = buildString {
            appendLine("Give me end-of-day feedback based on my planned priorities and what actually happened.")
            appendLine()
            appendLine("Inputs:")
            appendLine(daySummary)
            appendLine()
            appendLine("Write a short popup-friendly summary with:")
            appendLine("- 1 sentence verdict (how the day went vs plan)")
            appendLine("- 3 bullet 'What went well'")
            appendLine("- 3 bullet 'What to improve tomorrow'")
            appendLine("- 1 tiny recommendation if social+browsing minutes is high (today=$minutesSocialAndBrowsingToday)")
            appendLine()
            appendLine("Constraints:")
            appendLine("- Keep it under 180 words")
            appendLine("- No markdown")
        }
        return complete(system = system, user = user)
    }

    private suspend fun complete(system: String, user: String): Result<String> {
        if (BuildConfig.OPENAI_API_KEY.isBlank()) {
            return Result.failure(IllegalStateException("Add OPENAI_API_KEY to local.properties"))
        }
        return try {
            val fullUser = userWithRag(user)
            val res = api.chatCompletions(
                ChatCompletionRequest(
                    messages = listOf(
                        ChatMessageDto(role = "system", content = system),
                        ChatMessageDto(role = "user", content = fullUser),
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

    private suspend fun userWithRag(body: String): String {
        val rag = ragContextProvider.buildContextBlock()
        if (rag.isBlank()) return body
        return "---\nContext from your ExecOS journal:\n$rag\n---\n\n$body"
    }

    private suspend fun runToolChatWithRag(system: String, user: String): Result<String> {
        if (BuildConfig.OPENAI_API_KEY.isBlank()) {
            return Result.failure(IllegalStateException("Add OPENAI_API_KEY to local.properties"))
        }
        val tools = ExecutiveTools.definitions()
        val messages = mutableListOf(
            ChatMessageDto(role = "system", content = system),
            ChatMessageDto(role = "user", content = userWithRag(user)),
        )
        repeat(5) {
            val res = try {
                api.chatCompletions(
                    ChatCompletionRequest(
                        messages = messages,
                        tools = tools,
                        toolChoice = "auto",
                    ),
                )
            } catch (e: Exception) {
                return Result.failure(e)
            }
            val msg = res.choices?.firstOrNull()?.message
                ?: return Result.failure(IllegalStateException("No assistant message"))
            messages.add(msg)
            if (msg.toolCalls.isNullOrEmpty()) {
                val text = msg.content?.trim().orEmpty()
                if (text.isEmpty()) return Result.failure(IllegalStateException("Empty AI response"))
                return Result.success(text)
            }
            for (call in msg.toolCalls) {
                val out = toolExecutor.execute(
                    call.function.name,
                    call.function.arguments,
                )
                messages.add(
                    ChatMessageDto(
                        role = "tool",
                        content = out,
                        toolCallId = call.id,
                    ),
                )
            }
        }
        return Result.failure(IllegalStateException("Tool loop limit"))
    }
}
