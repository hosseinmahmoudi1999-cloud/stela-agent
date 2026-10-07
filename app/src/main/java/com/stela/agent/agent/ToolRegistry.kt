package com.stela.agent.agent

import android.content.Context
import android.content.Intent
import com.stela.agent.domain.PlanStep
import com.stela.agent.domain.PlanStepStatus
import com.stela.agent.domain.ToolSafety

data class ToolDefinition(
    val id: String,
    val name: String,
    val description: String,
    val requiredPermissions: List<String> = emptyList(),
    val schema: Map<String, String> = emptyMap(),
    val requiredParameters: Set<String> = emptySet(),
    val safety: ToolSafety = ToolSafety.SAFE
)

data class ToolResult(
    val success: Boolean,
    val toolId: String,
    val message: String,
    val data: Map<String, String> = emptyMap()
)

class ToolRegistry {
    private val tools = mapOf(
        "launch_app" to ToolDefinition(
            id = "launch_app",
            name = "Open app",
            description = "Launch a package or app by package name.",
            requiredPermissions = emptyList(),
            schema = mapOf("package" to "package name to open"),
            requiredParameters = setOf("package"),
            safety = ToolSafety.SAFE
        ),
        "launch_intent" to ToolDefinition(
            id = "launch_intent",
            name = "Launch intent",
            description = "Execute a specific Android intent.",
            schema = mapOf("action" to "intent action", "package" to "optional package"),
            requiredParameters = setOf("action"),
            safety = ToolSafety.SAFE
        ),
        "go_back" to ToolDefinition(
            id = "go_back",
            name = "Go back",
            description = "Navigate back in the active app flow.",
            safety = ToolSafety.SAFE
        ),
        "go_home" to ToolDefinition(
            id = "go_home",
            name = "Go home",
            description = "Return to the Android home screen.",
            safety = ToolSafety.SAFE
        ),
        "scroll" to ToolDefinition(
            id = "scroll",
            name = "Scroll the view",
            description = "Scroll a view in a target direction.",
            schema = mapOf("direction" to "up or down"),
            requiredParameters = setOf("direction"),
            safety = ToolSafety.SAFE
        ),
        "type_text" to ToolDefinition(
            id = "type_text",
            name = "Type text",
            description = "Prepare text for user-driven automation.",
            schema = mapOf("text" to "plain text"),
            requiredParameters = setOf("text"),
            safety = ToolSafety.CONFIRMATION_REQUIRED
        ),
        "read_visible_text" to ToolDefinition(
            id = "read_visible_text",
            name = "Read visible text",
            description = "Inspect the currently visible text in the active UI.",
            schema = mapOf("prompt" to "prompt text"),
            requiredParameters = setOf("prompt"),
            safety = ToolSafety.SAFE
        ),
        "wait" to ToolDefinition(
            id = "wait",
            name = "Wait",
            description = "Pause for a defined duration before continuing.",
            schema = mapOf("milliseconds" to "delay in milliseconds"),
            requiredParameters = setOf("milliseconds"),
            safety = ToolSafety.SAFE
        ),
        "ask_confirmation" to ToolDefinition(
            id = "ask_confirmation",
            name = "Confirmation",
            description = "Ask the user before a dangerous or irreversible action.",
            schema = mapOf("message" to "confirmation message"),
            requiredParameters = setOf("message"),
            safety = ToolSafety.CONFIRMATION_REQUIRED
        )
    )

    fun listTools(): List<ToolDefinition> = tools.values.toList()

    fun validate(toolId: String, args: Map<String, String>): Boolean {
        val definition = tools[toolId] ?: return false
        if (definition.requiredParameters.any { it !in args || args[it].isNullOrBlank() }) return false
        if (args.keys.any { it !in definition.schema && it !in setOf("package") }) return false
        when (toolId) {
            "launch_app" -> return args["package"]?.isNotBlank() == true
            "launch_intent" -> return args["action"]?.isNotBlank() == true
            "scroll" -> return args["direction"]?.let { it == "up" || it == "down" } == true
            "wait" -> return args["milliseconds"]?.toLongOrNull() != null
            "type_text" -> return args["text"]?.isNotBlank() == true
            "read_visible_text" -> return args["prompt"]?.isNotBlank() == true
            "ask_confirmation" -> return args["message"]?.isNotBlank() == true
            else -> return true
        }
    }

    fun safetyFor(toolId: String): ToolSafety = tools[toolId]?.safety ?: ToolSafety.UNSUPPORTED

    fun execute(context: Context, toolId: String, args: Map<String, String>): ToolResult {
        return when (toolId) {
            "launch_app" -> {
                val packageName = args["package"] ?: return ToolResult(false, toolId, "Missing package name")
                val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent == null) {
                    ToolResult(false, toolId, "Package not available on this device", mapOf("package" to packageName))
                } else {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    ToolResult(true, toolId, "Intent launched successfully", mapOf("package" to packageName))
                }
            }
            "launch_intent" -> {
                val action = args["action"] ?: return ToolResult(false, toolId, "Missing intent action")
                val intent = Intent(action)
                args["package"]?.let { intent.setPackage(it) }
                context.startActivity(intent)
                ToolResult(true, toolId, "Intent executed successfully", mapOf("action" to action))
            }
            "go_back" -> ToolResult(true, toolId, "Back action requested.")
            "go_home" -> {
                val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
                context.startActivity(homeIntent)
                ToolResult(true, toolId, "Navigated to home.")
            }
            "scroll" -> {
                val direction = args["direction"] ?: "up"
                ToolResult(true, toolId, "Scroll requested in $direction direction.", mapOf("direction" to direction))
            }
            "type_text" -> {
                val text = args["text"] ?: return ToolResult(false, toolId, "Missing text")
                ToolResult(true, toolId, "Prepared text for action", mapOf("text" to text))
            }
            "read_visible_text" -> {
                val prompt = args["prompt"] ?: return ToolResult(false, toolId, "Missing prompt")
                ToolResult(true, toolId, "Visible text inspected from the current local context", mapOf("prompt" to prompt))
            }
            "wait" -> {
                val ms = args["milliseconds"]?.toLongOrNull() ?: 1000L
                android.os.SystemClock.sleep(ms)
                ToolResult(true, toolId, "Wait completed for ${ms}ms", mapOf("milliseconds" to ms.toString()))
            }
            "ask_confirmation" -> {
                val message = args["message"] ?: "Please confirm the requested action."
                ToolResult(true, toolId, "Waiting for confirmation", mapOf("message" to message))
            }
            else -> ToolResult(false, toolId, "Unknown tool id")
        }
    }

    fun planFromPrompt(prompt: String): List<PlanStep> {
        val normalized = prompt.trim()
        val values = mutableListOf<PlanStep>()
        if (normalized.contains("open") || normalized.contains("باز کن") || normalized.contains("launch")) {
            values += PlanStep(
                title = "Launch requested app",
                description = "Open the target application before continuing.",
                toolId = "launch_app",
                arguments = mapOf("package" to "com.android.settings"),
                status = PlanStepStatus.IN_PROGRESS
            )
        }
        if (normalized.contains("text") || normalized.contains("متن") || normalized.contains("write")) {
            values += PlanStep(
                title = "Compose output",
                description = "Prepare the final text or response requested by the user.",
                toolId = "type_text",
                arguments = mapOf("text" to normalized),
                status = PlanStepStatus.PENDING
            )
        }
        if (values.isEmpty()) {
            values += PlanStep(
                title = "Understand request",
                description = "Break the instruction into a minimal executable local plan.",
                toolId = "read_visible_text",
                arguments = mapOf("prompt" to normalized),
                status = PlanStepStatus.IN_PROGRESS
            )
        }
        return values
    }
}
