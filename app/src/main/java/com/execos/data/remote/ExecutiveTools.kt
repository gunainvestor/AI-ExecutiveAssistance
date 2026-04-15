package com.execos.data.remote

import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * OpenAI function-calling tool definitions + JSON-schema parameters.
 */
object ExecutiveTools {
    private val emptyObjectSchema: JsonObject =
        JsonParser.parseString("""{"type":"object","properties":{},"required":[]}""").asJsonObject

    fun definitions(): List<ToolDefinition> = listOf(
        ToolDefinition(
            function = ToolFunctionDef(
                name = "get_recent_decisions",
                description = "Returns the user's most recent decision log entries (title, date, confidence, summary).",
                parameters = emptyObjectSchema,
            ),
        ),
        ToolDefinition(
            function = ToolFunctionDef(
                name = "get_today_priorities",
                description = "Returns today's priority tasks with impact scores and completion status.",
                parameters = emptyObjectSchema,
            ),
        ),
        ToolDefinition(
            function = ToolFunctionDef(
                name = "get_recent_reflections",
                description = "Returns recent daily reflection snippets for continuity.",
                parameters = emptyObjectSchema,
            ),
        ),
    )
}
