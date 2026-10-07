package com.stela.agent.agent

import com.stela.agent.domain.AgentPlan
import com.stela.agent.domain.PlanStep
import com.stela.agent.domain.PlanStepStatus

class AgentPlanner {
    fun plan(prompt: String): AgentPlan {
        val normalized = prompt.trim()
        val taskId = java.util.UUID.randomUUID().toString()
        val steps = mutableListOf<PlanStep>()

        val sensitive = normalized.lowercase().contains("send") ||
            normalized.lowercase().contains("delete") ||
            normalized.lowercase().contains("publish") ||
            normalized.lowercase().contains("purchase") ||
            normalized.lowercase().contains("remove") ||
            normalized.lowercase().contains("ارسال") ||
            normalized.lowercase().contains("حذف") ||
            normalized.lowercase().contains("خرید")

        if (sensitive) {
            steps += PlanStep(
                id = "confirm-1",
                title = "Confirm before sensitive action",
                description = "This request can change data or send content. Confirmation is required before continuing.",
                toolId = "ask_confirmation",
                arguments = mapOf("message" to normalized),
                status = PlanStepStatus.PENDING
            )
        }

        if (normalized.lowercase().contains("open") || normalized.lowercase().contains("launch") || normalized.lowercase().contains("باز کن")) {
            steps += PlanStep(
                id = "launch-1",
                title = "Open target app",
                description = "Launch the package or app requested by the user.",
                toolId = "launch_app",
                arguments = mapOf("package" to "com.android.settings"),
                status = PlanStepStatus.PENDING
            )
        }

        if (normalized.lowercase().contains("back") || normalized.lowercase().contains("return") || normalized.lowercase().contains("بازگشت") || normalized.lowercase().contains("برگشت")) {
            steps += PlanStep(
                id = "back-1",
                title = "Go back",
                description = "Return to the previous screen or state.",
                toolId = "go_back",
                arguments = emptyMap(),
                status = PlanStepStatus.PENDING
            )
        }

        if (normalized.lowercase().contains("scroll") || normalized.lowercase().contains("اسکرول") || normalized.lowercase().contains("up") || normalized.lowercase().contains("down")) {
            steps += PlanStep(
                id = "scroll-1",
                title = "Scroll the current view",
                description = "Navigate through the visible content to reach the target area.",
                toolId = "scroll",
                arguments = mapOf("direction" to if (normalized.lowercase().contains("down")) "down" else "up"),
                status = PlanStepStatus.PENDING
            )
        }

        if (normalized.lowercase().contains("wait") || normalized.lowercase().contains("delay") || normalized.lowercase().contains("منتظر") || normalized.lowercase().contains("تأخیر")) {
            steps += PlanStep(
                id = "wait-1",
                title = "Wait for the screen/state to settle",
                description = "Wait briefly before the next step to allow the target UI to finish updating.",
                toolId = "wait",
                arguments = mapOf("milliseconds" to "1200"),
                status = PlanStepStatus.PENDING
            )
        }

        if (normalized.lowercase().contains("write") || normalized.lowercase().contains("type") || normalized.lowercase().contains("message") || normalized.lowercase().contains("متن") || normalized.lowercase().contains("بنویس")) {
            steps += PlanStep(
                id = "type-1",
                title = "Compose or type text",
                description = "Prepare the user-requested text content for the next action.",
                toolId = "type_text",
                arguments = mapOf("text" to normalized),
                status = PlanStepStatus.PENDING
            )
        }

        if (steps.isEmpty()) {
            steps += PlanStep(
                id = "read-1",
                title = "Read visible context",
                description = "Inspect the current screen to determine the simplest relevant action.",
                toolId = "read_visible_text",
                arguments = mapOf("prompt" to normalized),
                status = PlanStepStatus.PENDING
            )
        }

        return AgentPlan(
            taskId = taskId,
            summary = "Structured plan generated for local execution.",
            steps = steps
        )
    }
}
