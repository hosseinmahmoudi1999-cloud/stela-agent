package com.stela.agent.network

import com.stela.agent.agent.ToolRegistry
import com.stela.agent.domain.AgentPlan
import com.stela.agent.domain.AgentPlanValidator
import com.stela.agent.domain.PlanStep
import com.stela.agent.domain.PlanStepStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID

sealed class AiProviderException(message: String, cause: Throwable? = null) : RuntimeException(message, cause) {
    class MissingApiKey : AiProviderException("Gemini API key is missing.")
    class InvalidResponse(message: String) : AiProviderException(message)
    class Unauthorized(message: String) : AiProviderException(message)
    class RateLimited(message: String) : AiProviderException(message)
    class Timeout(message: String) : AiProviderException(message)
    class RequestFailed(message: String) : AiProviderException(message)
}

interface AiProvider {
    suspend fun createPlanningInteraction(prompt: String): AgentPlan
    suspend fun continueInteractionWithToolResult(toolResult: String, context: String? = null): AgentPlan
}

class GeminiAiProvider(
    private val apiKey: String,
    private val client: OkHttpClient = OkHttpClient(),
    private val modelName: String = "gemini-2.5-flash",
    private val timeoutMs: Long = 15000L,
    private val toolRegistry: ToolRegistry = ToolRegistry()
) : AiProvider {
    private val baseUrl = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

    override suspend fun createPlanningInteraction(prompt: String): AgentPlan {
        if (apiKey.isBlank()) throw AiProviderException.MissingApiKey()
        val payload = buildPlanRequest(prompt)
        val responseText = withContext(Dispatchers.IO) {
            withTimeout(timeoutMs) {
                executeRequest(payload)
            }
        }
        val plan = parsePlan(responseText, prompt)
        if (!AgentPlanValidator.validate(plan)) {
            throw AiProviderException.InvalidResponse("Gemini returned an invalid structured plan.")
        }
        return plan
    }

    override suspend fun continueInteractionWithToolResult(toolResult: String, context: String?): AgentPlan {
        if (apiKey.isBlank()) throw AiProviderException.MissingApiKey()
        val prompt = context ?: "Continue from the following tool result."
        val payload = buildContinuationRequest(prompt, toolResult)
        val responseText = withContext(Dispatchers.IO) {
            withTimeout(timeoutMs) {
                executeRequest(payload)
            }
        }
        val plan = parsePlan(responseText, prompt)
        if (!AgentPlanValidator.validate(plan)) {
            throw AiProviderException.InvalidResponse("Gemini continued with an invalid structured plan.")
        }
        return plan
    }

    private fun buildPlanRequest(prompt: String): JSONObject = JSONObject().apply {
        put("contents", listOf(JSONObject().apply {
            put("parts", listOf(JSONObject().apply {
                put("text", "You are a planner for Stela Agent. Return strict JSON only with keys: goal, steps. Each step must include id, tool, arguments, requiresConfirmation, expectedResult. Allowed tool names: ${toolRegistry.listTools().joinToString { it.id }}. If a tool is not in the list, reject the plan. Keep arguments valid for the tool schema. Keep the JSON valid and compact.")
            }))
            put("parts", listOf(JSONObject().apply {
                put("text", prompt)
            }))
        }))
    }

    private fun buildContinuationRequest(context: String, toolResult: String): JSONObject = JSONObject().apply {
        put("contents", listOf(JSONObject().apply {
            put("parts", listOf(JSONObject().apply {
                put("text", "Use the tool result below to continue the task. Return strict JSON only with keys: goal, steps. Ensure each step is valid and uses the approved tool names: ${toolRegistry.listTools().joinToString { it.id }}. Context: $context Tool result: $toolResult")
            }))
        }))
    }

    private fun executeRequest(payload: JSONObject): String {
        val request = Request.Builder()
            .url(baseUrl)
            .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        return when {
            response.code == 401 -> throw AiProviderException.Unauthorized("Gemini API key is unauthorized.")
            response.code == 429 -> throw AiProviderException.RateLimited("Gemini API rate limit reached.")
            response.code >= 500 -> throw AiProviderException.RequestFailed("Gemini server error: ${response.code}")
            !response.isSuccessful -> throw AiProviderException.RequestFailed("Gemini request failed with status ${response.code}")
            else -> response.body?.string() ?: throw AiProviderException.InvalidResponse("Empty Gemini response body.")
        }
    }

    private fun parsePlan(responseBody: String, fallbackPrompt: String): AgentPlan {
        val text = extractTextFromResponse(responseBody)
        val cleaned = text.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val json = JSONObject(cleaned)
        val goal = json.optString("goal", fallbackPrompt).trim()
        val stepsJson = json.optJSONArray("steps") ?: return AgentPlan(
            taskId = UUID.randomUUID().toString(),
            summary = goal,
            steps = listOf(
                PlanStep(
                    id = "gemini-step-1",
                    title = "Review request",
                    description = "A valid plan was not returned by Gemini.",
                    toolId = "read_visible_text",
                    arguments = mapOf("prompt" to fallbackPrompt),
                    status = PlanStepStatus.PENDING
                )
            )
        )

        val steps = mutableListOf<PlanStep>()
        for (i in 0 until stepsJson.length()) {
            val item = stepsJson.getJSONObject(i)
            val toolId = item.optString("tool", "").trim()
            val arguments = item.optJSONObject("arguments")?.asStringMap() ?: emptyMap()
            if (!toolRegistry.validate(toolId, arguments)) {
                throw AiProviderException.InvalidResponse("Gemini supplied an invalid robot tool call: $toolId")
            }
            steps += PlanStep(
                id = item.optString("id", "gemini-step-${i + 1}"),
                title = item.optString("title", toolId),
                description = item.optString("expectedResult", "Execute the model-approved tool."),
                toolId = toolId,
                arguments = arguments,
                status = PlanStepStatus.PENDING
            )
        }

        return AgentPlan(
            taskId = UUID.randomUUID().toString(),
            summary = if (goal.isNotBlank()) goal else fallbackPrompt,
            steps = steps
        )
    }

    private fun extractTextFromResponse(responseBody: String): String {
        val json = JSONObject(responseBody)
        val candidate = json.optJSONArray("candidates")?.optJSONObject(0) ?: return "{}"
        val content = candidate.optJSONObject("content") ?: return "{}"
        val parts = content.optJSONArray("parts") ?: return "{}"
        for (i in 0 until parts.length()) {
            val part = parts.optJSONObject(i) ?: continue
            val text = part.optString("text")
            if (text.isNotBlank()) return text
        }
        return "{}"
    }
}

private fun JSONObject.asStringMap(): Map<String, String> = keys().asSequence().associate { key ->
    key to opt(key).toString()
}
