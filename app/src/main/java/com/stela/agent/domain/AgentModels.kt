package com.stela.agent.domain

import java.util.UUID

enum class AgentStatus {
    IDLE,
    PLANNING,
    WAITING_FOR_PERMISSION,
    WAITING_FOR_CONFIRMATION,
    EXECUTING,
    PAUSED,
    RECOVERING,
    SUCCEEDED,
    FAILED,
    CANCELLED
}

enum class PlanStepStatus {
    PENDING,
    IN_PROGRESS,
    SUCCEEDED,
    FAILED
}

enum class ToolSafety {
    SAFE,
    CONFIRMATION_REQUIRED,
    UNSUPPORTED
}

data class ToolCall(
    val id: String = UUID.randomUUID().toString(),
    val toolId: String,
    val arguments: Map<String, String> = emptyMap()
)

data class PlanStep(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val toolId: String,
    val arguments: Map<String, String> = emptyMap(),
    val status: PlanStepStatus = PlanStepStatus.PENDING,
    val retryCount: Int = 0
)

data class AgentPlan(
    val taskId: String,
    val summary: String,
    val steps: List<PlanStep>
)

data class AgentTask(
    val id: String = UUID.randomUUID().toString(),
    val prompt: String,
    val status: AgentStatus = AgentStatus.IDLE,
    val plan: AgentPlan? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val summary: String? = null,
    val error: String? = null
)

data class ConfirmationRequest(
    val taskId: String,
    val message: String,
    val details: String
)

data class ToolExecutionResult(
    val toolId: String,
    val success: Boolean,
    val message: String,
    val resultData: Map<String, String> = emptyMap(),
    val error: String? = null
)

data class AgentExecutionReport(
    val taskId: String,
    val status: AgentStatus,
    val summary: String,
    val stepResults: List<ToolExecutionResult> = emptyList(),
    val error: String? = null
)

object AgentPlanValidator {
    fun validate(plan: AgentPlan): Boolean {
        if (plan.taskId.isBlank() || plan.summary.isBlank() || plan.steps.isEmpty()) return false
        return plan.steps.all { validateStep(it) }
    }

    fun validateStep(step: PlanStep): Boolean {
        if (step.id.isBlank() || step.title.isBlank() || step.description.isBlank() || step.toolId.isBlank()) {
            return false
        }

        val toolId = step.toolId
        val registry = com.stela.agent.agent.ToolRegistry()
        if (!registry.validate(toolId, step.arguments)) {
            return false
        }

        return step.arguments.values.none { it.isBlank() }
    }
}

