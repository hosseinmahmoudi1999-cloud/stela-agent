package com.stela.agent

import com.stela.agent.agent.AgentStateMachine
import com.stela.agent.agent.ToolRegistry
import com.stela.agent.domain.AgentPlan
import com.stela.agent.domain.AgentPlanValidator
import com.stela.agent.domain.PlanStep
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentContractTests {
    @Test
    fun validPlanIsAccepted() {
        val plan = AgentPlan(
            taskId = "task-1",
            summary = "Open settings",
            steps = listOf(
                PlanStep(
                    id = "step-1",
                    title = "Open app",
                    description = "Launch the settings app",
                    toolId = "launch_app",
                    arguments = mapOf("package" to "com.android.settings")
                )
            )
        )

        assertTrue(AgentPlanValidator.validate(plan))
    }

    @Test
    fun unknownToolIsRejected() {
        assertFalse(ToolRegistry().validate("not_a_real_tool", emptyMap()))
    }

    @Test
    fun invalidArgumentsAreRejected() {
        val registry = ToolRegistry()
        assertFalse(registry.validate("launch_app", emptyMap()))
        assertTrue(registry.validate("launch_app", mapOf("package" to "com.android.settings")))
    }

    @Test
    fun confirmationGateIsDetected() {
        val stateMachine = AgentStateMachine()
        assertTrue(stateMachine.requiresConfirmation("Please send this message now"))
        assertFalse(stateMachine.requiresConfirmation("Open the settings app"))
    }
}
