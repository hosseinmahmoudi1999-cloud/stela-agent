package com.stela.agent.agent

import android.content.Context
import com.stela.agent.domain.AgentExecutionReport
import com.stela.agent.domain.AgentStatus
import com.stela.agent.domain.AgentTask
import com.stela.agent.domain.AgentPlan
import com.stela.agent.domain.PlanStepStatus
import com.stela.agent.domain.ToolExecutionResult

class AgentExecutor(
    private val context: Context,
    private val toolRegistry: ToolRegistry = ToolRegistry()
) {
    fun execute(task: AgentTask, plan: AgentPlan): AgentExecutionReport {
        if (!com.stela.agent.domain.AgentPlanValidator.validate(plan)) {
            return AgentExecutionReport(
                taskId = task.id,
                status = com.stela.agent.domain.AgentStatus.FAILED,
                summary = "Plan rejected because it is malformed or contains unknown tools.",
                error = "invalid_plan"
            )
        }

        val results = mutableListOf<ToolExecutionResult>()
        for (step in plan.steps) {
            if (step.toolId == "ask_confirmation") {
                results += ToolExecutionResult(
                    toolId = step.toolId,
                    success = true,
                    message = "Waiting for user confirmation before the sensitive action."
                )
                continue
            }

            if (!toolRegistry.validate(step.toolId, step.arguments)) {
                results += ToolExecutionResult(
                    toolId = step.toolId,
                    success = false,
                    message = "Invalid arguments for tool: ${step.toolId}",
                    error = "invalid_arguments"
                )
                continue
            }

            val result = toolRegistry.execute(context, step.toolId, step.arguments)
            results += ToolExecutionResult(
                toolId = step.toolId,
                success = result.success,
                message = result.message,
                resultData = result.data,
                error = if (result.success) null else "tool_failed"
            )

            if (!result.success) {
                return AgentExecutionReport(
                    taskId = task.id,
                    status = AgentStatus.FAILED,
                    summary = "Execution failed while handling tool ${step.toolId}.",
                    stepResults = results,
                    error = result.message
                )
            }
        }

        return AgentExecutionReport(
            taskId = task.id,
            status = AgentStatus.SUCCEEDED,
            summary = "Execution finished successfully.",
            stepResults = results
        )
    }
}
