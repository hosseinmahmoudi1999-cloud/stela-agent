package com.stela.agent

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stela.agent.agent.AgentExecutor
import com.stela.agent.agent.AgentPlanner
import com.stela.agent.agent.AgentStateMachine
import com.stela.agent.agent.ExecutionEvent
import com.stela.agent.domain.AgentPlan
import com.stela.agent.domain.AgentStatus
import com.stela.agent.domain.AgentTask
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AppUiState(
    val prompt: String = "",
    val currentStatus: AgentStatus = AgentStatus.IDLE,
    val currentTaskId: String = "",
    val apiKey: String = "",
    val voiceEnabled: Boolean = false,
    val accessibilityEnabled: Boolean = false,
    val latestStep: StepSummary? = null,
    val errorMessage: String? = null,
    val confirmationRequired: Boolean = false,
    val planSummary: String = ""
)

data class StepSummary(
    val title: String,
    val description: String
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val stateMachine = AgentStateMachine()
    private val planner = AgentPlanner()
    private val executor = AgentExecutor(application.applicationContext)
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()
    private var pendingPlan: AgentPlan? = null
    private var pendingTask: AgentTask? = null

    fun updatePrompt(prompt: String) {
        _uiState.value = _uiState.value.copy(prompt = prompt)
    }

    fun updateApiKey(key: String) {
        _uiState.value = _uiState.value.copy(apiKey = key)
    }

    fun toggleVoice() {
        _uiState.value = _uiState.value.copy(voiceEnabled = !_uiState.value.voiceEnabled)
    }

    fun runPrompt() {
        val prompt = _uiState.value.prompt.trim()
        if (prompt.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a task.")
            return
        }

        viewModelScope.launch {
            val task = stateMachine.createTask(prompt)
            val plan = planner.plan(prompt)
            pendingTask = task
            pendingPlan = plan

            _uiState.value = _uiState.value.copy(
                currentStatus = stateMachine.transition(AgentStatus.IDLE, ExecutionEvent.START),
                currentTaskId = task.id,
                planSummary = plan.summary,
                latestStep = plan.steps.firstOrNull()?.let { StepSummary(it.title, it.description) },
                confirmationRequired = stateMachine.requiresConfirmation(prompt),
                errorMessage = null
            )

            _uiState.value = _uiState.value.copy(
                currentStatus = stateMachine.transition(_uiState.value.currentStatus, ExecutionEvent.PLAN_READY),
                latestStep = plan.steps.firstOrNull()?.let { StepSummary(it.title, it.description) }
            )

            if (stateMachine.requiresConfirmation(prompt)) {
                _uiState.value = _uiState.value.copy(
                    currentStatus = stateMachine.transition(_uiState.value.currentStatus, ExecutionEvent.CONFIRMATION_REQUIRED),
                    confirmationRequired = true
                )
                return@launch
            }

            executeReadyPlan(task, plan)
        }
    }

    fun confirmExecution() {
        val task = pendingTask ?: return
        val plan = pendingPlan ?: return
        executeReadyPlan(task, plan)
    }

    fun cancelExecution() {
        _uiState.value = _uiState.value.copy(
            currentStatus = AgentStatus.CANCELLED,
            confirmationRequired = false,
            errorMessage = "Execution cancelled by the user."
        )
    }

    private fun executeReadyPlan(task: AgentTask, plan: AgentPlan) {
        _uiState.value = _uiState.value.copy(
            currentStatus = AgentStatus.EXECUTING,
            confirmationRequired = false,
            latestStep = plan.steps.firstOrNull()?.let { StepSummary(it.title, it.description) }
        )

        val report = executor.execute(task, plan)
        val nextStatus = if (report.status == AgentStatus.SUCCEEDED) AgentStatus.SUCCEEDED else AgentStatus.FAILED

        _uiState.value = _uiState.value.copy(
            currentStatus = nextStatus,
            planSummary = report.summary,
            latestStep = report.stepResults.lastOrNull()?.let { StepSummary("Execution result", it.message) },
            errorMessage = report.error
        )
    }
}
