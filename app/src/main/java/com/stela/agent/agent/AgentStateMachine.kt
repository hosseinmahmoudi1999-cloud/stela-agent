package com.stela.agent.agent

import com.stela.agent.domain.AgentPlan
import com.stela.agent.domain.AgentStatus
import com.stela.agent.domain.AgentTask
import com.stela.agent.domain.PlanStep
import java.util.UUID

class AgentStateMachine {
    fun createTask(prompt: String): AgentTask {
        val trimmed = prompt.trim()
        require(trimmed.isNotBlank()) { "Prompt cannot be empty" }
        return AgentTask(prompt = trimmed)
    }

    fun requiresConfirmation(prompt: String): Boolean {
        val normalized = prompt.lowercase()
        return normalized.contains("send") ||
            normalized.contains("delete") ||
            normalized.contains("remove") ||
            normalized.contains("purchase") ||
            normalized.contains("publish") ||
            normalized.contains("تایید") ||
            normalized.contains("ارسال") ||
            normalized.contains("حذف") ||
            normalized.contains("خرید")
    }

    fun transition(currentState: AgentStatus, event: ExecutionEvent): AgentStatus = when (event) {
        ExecutionEvent.START -> when (currentState) {
            AgentStatus.IDLE -> AgentStatus.PLANNING
            else -> currentState
        }
        ExecutionEvent.PLAN_READY -> AgentStatus.EXECUTING
        ExecutionEvent.PERMISSION_REQUIRED -> AgentStatus.WAITING_FOR_PERMISSION
        ExecutionEvent.CONFIRMATION_REQUIRED -> AgentStatus.WAITING_FOR_CONFIRMATION
        ExecutionEvent.PAUSE -> AgentStatus.PAUSED
        ExecutionEvent.RESUME -> AgentStatus.EXECUTING
        ExecutionEvent.SUCCESS -> AgentStatus.SUCCEEDED
        ExecutionEvent.FAILURE -> AgentStatus.FAILED
        ExecutionEvent.CANCEL -> AgentStatus.CANCELLED
    }

    fun validatePlan(plan: AgentPlan): Boolean = plan.steps.isNotEmpty() && plan.steps.all { it.title.isNotBlank() && it.toolId.isNotBlank() }

    fun generateTaskId(): String = UUID.randomUUID().toString()
}

enum class ExecutionEvent {
    START,
    PLAN_READY,
    PERMISSION_REQUIRED,
    CONFIRMATION_REQUIRED,
    PAUSE,
    RESUME,
    SUCCESS,
    FAILURE,
    CANCEL
}
